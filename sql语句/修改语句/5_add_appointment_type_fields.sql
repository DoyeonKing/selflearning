-- =====================================================
-- 添加预约类型相关字段到 appointments 表
-- 1. appointment_type: 预约类型（预约/现场/复诊/加号/优先号）
-- 2. original_appointment_id: 原始预约ID（用于复诊号关联）
-- 3. is_priority: 是否优先号（军人、老年人等）
-- 4. is_add_on: 是否加号
-- =====================================================

-- 步骤1: 添加预约类型字段
ALTER TABLE `appointments` 
ADD COLUMN `appointment_type` ENUM(
    'APPOINTMENT',        -- 预约挂号
    'WALK_IN',            -- 现场挂号
    'SAME_DAY_FOLLOW_UP', -- 当日复诊号
    'ADD_ON'              -- 加号
) NULL DEFAULT 'APPOINTMENT' COMMENT '预约类型';

-- 步骤2: 添加原始预约ID字段（用于复诊号关联）
ALTER TABLE `appointments` 
ADD COLUMN `original_appointment_id` INT NULL COMMENT '原始预约ID（用于复诊号关联，复诊号关联到原始预约）',
ADD CONSTRAINT `fk_original_appointment` FOREIGN KEY (`original_appointment_id`) 
    REFERENCES `appointments` (`appointment_id`) ON DELETE SET NULL;

-- 步骤3: 添加加号字段
ALTER TABLE `appointments` 
ADD COLUMN `is_add_on` TINYINT(1) DEFAULT 0 COMMENT '是否加号（0=否，1=是）';

-- 步骤4: 添加索引优化查询性能
ALTER TABLE `appointments` 
ADD INDEX `idx_appointment_type` (`appointment_type`),
ADD INDEX `idx_original_appointment_id` (`original_appointment_id`),
ADD INDEX `idx_is_add_on` (`is_add_on`);

-- 步骤6: 数据迁移 - 根据现有字段初始化appointment_type
-- 如果is_walk_in=1，则appointment_type=WALK_IN
UPDATE `appointments` 
SET `appointment_type` = 'WALK_IN' 
WHERE `is_walk_in` = 1 AND `appointment_type` IS NULL;

-- 如果is_walk_in=0或NULL，则appointment_type=APPOINTMENT（默认值）
UPDATE `appointments` 
SET `appointment_type` = 'APPOINTMENT' 
WHERE (`is_walk_in` = 0 OR `is_walk_in` IS NULL) AND `appointment_type` IS NULL;

-- =====================================================
-- 验证字段添加结果
-- =====================================================
-- DESCRIBE `appointments`;

