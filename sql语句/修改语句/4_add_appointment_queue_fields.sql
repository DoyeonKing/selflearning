-- =====================================================
-- 添加预约排队功能相关字段到 appointments 表
-- 1. is_walk_in: 是否现场挂号（false=预约，true=现场挂号）
-- 2. real_time_queue_number: 实时候诊序号（在时段内按签到时间分配）
-- 3. is_late: 是否迟到（超过时段结束时间+软关门时间）
-- =====================================================

-- 步骤1: 添加排队相关字段
ALTER TABLE `appointments` 
ADD COLUMN `is_walk_in` TINYINT(1) DEFAULT 0 COMMENT '是否现场挂号（0=预约，1=现场挂号）',
ADD COLUMN `real_time_queue_number` INT NULL COMMENT '实时候诊序号（在时段内按签到时间分配）',
ADD COLUMN `is_late` TINYINT(1) DEFAULT 0 COMMENT '是否迟到（超过时段结束时间+软关门时间）';

-- 步骤2: 添加索引优化查询性能
ALTER TABLE `appointments` 
ADD INDEX `idx_is_walk_in` (`is_walk_in`),
ADD INDEX `idx_real_time_queue_number` (`real_time_queue_number`),
ADD INDEX `idx_is_late` (`is_late`);

-- 步骤3: 对于已有签到的记录，初始化字段值（可选，用于历史数据）
-- 默认所有已有预约都是预约类型（is_walk_in=0）
-- UPDATE `appointments` SET `is_walk_in` = 0 WHERE `is_walk_in` IS NULL;

-- =====================================================
-- 验证字段添加结果
-- =====================================================
-- DESCRIBE `appointments`;



