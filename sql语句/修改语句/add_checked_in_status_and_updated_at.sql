-- =====================================================
-- 修改 appointments 表结构
-- 1. 添加 CHECKED_IN 状态到 status 枚举
-- 2. 添加/修改 updated_at 字段
-- 3. 数据迁移：将已签到的记录状态改为 CHECKED_IN
-- =====================================================

-- 步骤1: 修改 status 枚举，添加 CHECKED_IN 状态
-- 注意：MySQL 修改 ENUM 需要先删除再添加，或者使用 ALTER TABLE MODIFY
ALTER TABLE `appointments` 
MODIFY COLUMN `status` ENUM(
    'PENDING_PAYMENT',  -- 待支付
    'scheduled',        -- 已预约（未签到）
    'CHECKED_IN',       -- 已签到（未就诊）
    'completed',        -- 已完成
    'cancelled',        -- 已取消
    'NO_SHOW'          -- 爽约
) NOT NULL DEFAULT 'scheduled' COMMENT '预约状态';

-- 步骤2: 检查并添加 updated_at 字段（如果不存在）
-- 如果字段已存在，此语句会报错，可以忽略
ALTER TABLE `appointments` 
ADD COLUMN IF NOT EXISTS `updated_at` TIMESTAMP NULL DEFAULT NULL 
COMMENT '最后更新时间（status变为completed时由触发器更新）';

-- 如果 updated_at 字段已存在但需要修改定义，执行以下语句：
-- ALTER TABLE `appointments` 
-- MODIFY COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL 
-- COMMENT '最后更新时间（status变为completed时由触发器更新）';

-- 步骤3: 数据迁移 - 将已签到的记录状态改为 CHECKED_IN
-- 将 check_in_time 不为 NULL 且状态为 scheduled 的记录改为 CHECKED_IN
-- UPDATE `appointments` 
-- SET `status` = 'CHECKED_IN' 
-- WHERE `check_in_time` IS NOT NULL 
--   AND `status` = 'scheduled';

-- 步骤4: 验证数据迁移结果
-- SELECT 
--     status,
--     COUNT(*) as count,
--     COUNT(CASE WHEN check_in_time IS NOT NULL THEN 1 END) as with_check_in_time
-- FROM `appointments`
-- GROUP BY status;

-- =====================================================
-- 完整的建表语句（包含所有修改）
-- =====================================================
/*
DROP TABLE IF EXISTS `appointments`;
CREATE TABLE `appointments` (
  `appointment_id` INT NOT NULL AUTO_INCREMENT,
  `patient_id` INT NOT NULL COMMENT '患者ID',
  `schedule_id` INT NOT NULL COMMENT '排班ID',
  `appointment_number` INT NOT NULL COMMENT '就诊序号',
  `status` ENUM(
      'PENDING_PAYMENT',  -- 待支付
      'scheduled',        -- 已预约（未签到）
      'CHECKED_IN',       -- 已签到（未就诊）
      'completed',        -- 已完成
      'cancelled',        -- 已取消
      'NO_SHOW'          -- 爽约
  ) NOT NULL DEFAULT 'scheduled' COMMENT '预约状态',
  `payment_status` ENUM('unpaid','paid','refunded') NOT NULL DEFAULT 'unpaid' COMMENT '支付状态',
  `payment_method` VARCHAR(50) COMMENT '支付方式',
  `transaction_id` VARCHAR(255) COMMENT '支付流水号',
  `check_in_time` DATETIME COMMENT '现场签到时间',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预约生成时间',
  `updated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后更新时间（status变为completed时由触发器更新）',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_check_in_time` (`check_in_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';
*/


