-- =====================================================
-- 加号功能数据库表结构调整
-- 参考候补（waitlist）的15分钟倒计时实现方式
-- =====================================================

-- 1. 修改 schedules 表，添加加号虚拟号源相关字段
ALTER TABLE `schedules` 
ADD COLUMN `is_add_on_slot` TINYINT(1) DEFAULT 0 COMMENT '是否为加号虚拟号源（0=否，1=是）',
ADD COLUMN `reserved_for_patient_id` BIGINT NULL COMMENT '预留给指定患者ID（加号专用，锁定该号源）',
ADD COLUMN `slot_application_id` INT NULL COMMENT '关联的加号申请ID（追溯来源）',
ADD INDEX `idx_reserved_patient` (`reserved_for_patient_id`),
ADD INDEX `idx_slot_application` (`slot_application_id`),
ADD CONSTRAINT `fk_schedule_slot_application` 
    FOREIGN KEY (`slot_application_id`) 
    REFERENCES `slot_application` (`application_id`) 
    ON DELETE SET NULL;

-- 说明：
-- is_add_on_slot: 标记这是一个加号创建的虚拟号源，与普通排班区分
-- reserved_for_patient_id: 锁定给特定患者，只有该患者能使用此号源
-- slot_application_id: 追溯到原始加号申请，便于审计和管理
-- status字段使用现有的'locked'状态表示锁定

-- =====================================================

-- 2. 修改 appointments 表，添加支付截止时间字段
ALTER TABLE `appointments` 
ADD COLUMN `payment_deadline` DATETIME NULL COMMENT '支付截止时间（加号专用，参考候补的notification_sent_at+15分钟模式）',
ADD INDEX `idx_payment_deadline` (`payment_deadline`);

-- 说明：
-- payment_deadline: 支付截止时间，类似候补的 notification_sent_at
-- 加号审批通过后，设置为 当前时间 + 24小时（可配置）
-- 定时任务检查此字段，超时后自动取消预约并释放虚拟号源
-- 现有字段复用：
--   - appointment_type = 'ADD_ON' 标记为加号类型
--   - is_add_on = 1 标记为加号
--   - status = 'PENDING_PAYMENT' 待支付状态
--   - payment_status = 'unpaid' 未支付

-- =====================================================

-- 3. 为 slot_application 表添加关联预约字段（可选，便于追溯）
ALTER TABLE `slot_application`
ADD COLUMN `appointment_id` INT NULL COMMENT '关联生成的预约ID（审批通过后自动创建）',
ADD INDEX `idx_appointment` (`appointment_id`),
ADD CONSTRAINT `fk_slot_application_appointment` 
    FOREIGN KEY (`appointment_id`) 
    REFERENCES `appointments` (`appointment_id`) 
    ON DELETE SET NULL;

-- 说明：
-- appointment_id: 审批通过后自动生成的预约记录ID
-- 便于从加号申请直接查询到对应的预约记录

-- =====================================================

-- 数据验证查询
-- 查看加号虚拟号源
SELECT 
    s.schedule_id,
    s.schedule_date,
    s.is_add_on_slot,
    s.reserved_for_patient_id,
    s.slot_application_id,
    s.status,
    p.full_name AS patient_name
FROM schedules s
LEFT JOIN patients p ON s.reserved_for_patient_id = p.patient_id
WHERE s.is_add_on_slot = 1;

-- 查看待支付的加号预约
SELECT 
    a.appointment_id,
    a.appointment_type,
    a.is_add_on,
    a.status,
    a.payment_status,
    a.payment_deadline,
    TIMESTAMPDIFF(MINUTE, NOW(), a.payment_deadline) AS remaining_minutes,
    p.full_name AS patient_name,
    d.full_name AS doctor_name
FROM appointments a
JOIN patients p ON a.patient_id = p.patient_id
JOIN schedules s ON a.schedule_id = s.schedule_id
JOIN doctors d ON s.doctor_id = d.doctor_id
WHERE a.appointment_type = 'ADD_ON' 
  AND a.status = 'PENDING_PAYMENT'
  AND a.payment_deadline IS NOT NULL
ORDER BY a.payment_deadline ASC;

-- 查看已超时但未处理的加号预约
SELECT 
    a.appointment_id,
    a.payment_deadline,
    TIMESTAMPDIFF(MINUTE, a.payment_deadline, NOW()) AS overdue_minutes,
    a.status,
    p.full_name AS patient_name
FROM appointments a
JOIN patients p ON a.patient_id = p.patient_id
WHERE a.appointment_type = 'ADD_ON' 
  AND a.status = 'PENDING_PAYMENT'
  AND a.payment_deadline < NOW()
ORDER BY a.payment_deadline ASC;

-- =====================================================
-- 执行说明：
-- 1. 此脚本需要在 14_create_slot_application_table.sql 之后执行
-- 2. 执行前请备份数据库
-- 3. 执行后运行验证查询确认字段添加成功
-- 4. 后续需要实现定时任务类似 WaitlistExpirationTask
-- =====================================================
