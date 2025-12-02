-- 为 appointments 表添加 updated_at 字段
-- 注意：不使用 ON UPDATE CURRENT_TIMESTAMP，因为我们要用触发器精确控制 completed 时的更新时间
ALTER TABLE `appointments` 
ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后更新时间（status变为completed时由触发器更新）';

-- 修改 updated_at 字段定义，移除 ON UPDATE CURRENT_TIMESTAMP
-- 因为我们要用触发器精确控制 completed 时的更新时间
-- 如果字段已经有 ON UPDATE CURRENT_TIMESTAMP，执行此 SQL 移除它

ALTER TABLE `appointments` 
MODIFY COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后更新时间（status变为completed时由触发器更新）';

-- =====================================================
-- 添加叫号功能相关字段到 appointments 表
-- 1. called_at: 叫号时间（NULL表示未叫号）
-- 2. is_on_time: 是否按时签到（预约时段开始后20分钟内）
-- 3. missed_call_count: 过号次数
-- 4. recheck_in_time: 过号后重新签到时间
-- =====================================================

-- 步骤1: 添加叫号相关字段
ALTER TABLE `appointments` 
ADD COLUMN `called_at` DATETIME NULL COMMENT '叫号时间（NULL表示未叫号）',
ADD COLUMN `is_on_time` TINYINT(1) DEFAULT 0 COMMENT '是否按时签到（预约时段开始后20分钟内）',
ADD COLUMN `missed_call_count` INT DEFAULT 0 COMMENT '过号次数',
ADD COLUMN `recheck_in_time` DATETIME NULL COMMENT '过号后重新签到时间';

-- 步骤2: 添加索引优化查询性能
ALTER TABLE `appointments` 
ADD INDEX `idx_called_at` (`called_at`),
ADD INDEX `idx_is_on_time` (`is_on_time`),
ADD INDEX `idx_recheck_in_time` (`recheck_in_time`);

-- 步骤3: 对于已有签到的记录，根据签到时间判断是否按时（可选，用于历史数据）
-- 注意：这个更新需要根据实际的预约时段开始时间来判断，这里只是示例
-- UPDATE `appointments` a
-- JOIN `schedules` s ON a.schedule_id = s.schedule_id
-- JOIN `time_slots` ts ON s.slot_id = ts.slot_id
-- SET a.is_on_time = CASE 
--     WHEN a.check_in_time IS NOT NULL 
--         AND a.check_in_time >= DATE_ADD(CONCAT(s.schedule_date, ' ', ts.start_time), INTERVAL -20 MINUTE)
--         AND a.check_in_time <= DATE_ADD(CONCAT(s.schedule_date, ' ', ts.start_time), INTERVAL 20 MINUTE)
--     THEN 1 
--     ELSE 0 
-- END
-- WHERE a.check_in_time IS NOT NULL;

-- =====================================================
-- 验证字段添加结果
-- =====================================================
-- DESCRIBE `appointments`;

