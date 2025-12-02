-- 更新时间段并清空排班表
-- 执行前请备份数据

-- 1. 先删除所有预约记录（因为它们引用了schedules）
DELETE FROM `appointments`;

-- 2. 再删除所有排班记录（清空schedules表）
DELETE FROM `schedules`;

-- 3. 清空现有的time_slots表数据
DELETE FROM `time_slots`;

-- 4. 重置自增ID（可选，让ID从1开始）
ALTER TABLE `time_slots` AUTO_INCREMENT = 1;

-- 5. 插入新的四条时间段记录
INSERT INTO `time_slots` (`slot_id`, `slot_name`, `start_time`, `end_time`) VALUES
(1, '上午 08:00-12:00', '08:00:00', '12:00:00'),
(2, '上午 08:30-12:00', '08:30:00', '12:00:00'),
(3, '下午 14:00-18:00', '14:00:00', '18:00:00'),
(4, '下午 14:30-18:00', '14:30:00', '18:00:00');

-- 6. 验证插入结果
SELECT * FROM `time_slots` ORDER BY `slot_id`;

-- 7. 验证schedules表已清空
SELECT COUNT(*) as schedule_count FROM `schedules`;


