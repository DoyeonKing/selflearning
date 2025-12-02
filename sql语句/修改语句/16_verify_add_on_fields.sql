-- =====================================================
-- 验证加号功能所需的数据库字段
-- 检查是否所有字段都已正确添加
-- =====================================================

-- 1. 检查 schedules 表的加号相关字段
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hospital_system'
  AND TABLE_NAME = 'schedules'
  AND COLUMN_NAME IN ('is_add_on_slot', 'reserved_for_patient_id', 'slot_application_id');

-- 2. 检查 appointments 表的 payment_deadline 字段
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hospital_system'
  AND TABLE_NAME = 'appointments'
  AND COLUMN_NAME = 'payment_deadline';

-- 3. 检查 slot_application 表的 appointment_id 字段
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hospital_system'
  AND TABLE_NAME = 'slot_application'
  AND COLUMN_NAME = 'appointment_id';

-- 4. 检查外键约束
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'hospital_system'
  AND (CONSTRAINT_NAME = 'fk_schedule_slot_application' 
       OR CONSTRAINT_NAME = 'fk_slot_application_appointment');

-- =====================================================
-- 如果上述查询返回空结果，说明字段未添加，需要执行以下语句
-- =====================================================

-- 如果 schedules 表缺少字段，执行：
-- ALTER TABLE `schedules` 
-- ADD COLUMN `is_add_on_slot` TINYINT(1) DEFAULT 0 COMMENT '是否为加号虚拟号源（0=否，1=是）',
-- ADD COLUMN `reserved_for_patient_id` BIGINT NULL COMMENT '预留给指定患者ID（加号专用，锁定该号源）',
-- ADD COLUMN `slot_application_id` INT NULL COMMENT '关联的加号申请ID（追溯来源）',
-- ADD INDEX `idx_reserved_patient` (`reserved_for_patient_id`),
-- ADD INDEX `idx_slot_application` (`slot_application_id`);

-- 如果 appointments 表缺少字段，执行：
-- ALTER TABLE `appointments` 
-- ADD COLUMN `payment_deadline` DATETIME NULL COMMENT '支付截止时间（加号专用）',
-- ADD INDEX `idx_payment_deadline` (`payment_deadline`);

-- 如果 slot_application 表缺少字段，执行：
-- ALTER TABLE `slot_application`
-- ADD COLUMN `appointment_id` INT NULL COMMENT '关联生成的预约ID（审批通过后自动创建）',
-- ADD INDEX `idx_appointment` (`appointment_id`);
