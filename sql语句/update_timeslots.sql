-- 更新time_slots表，只保留四条记录
-- 执行前请备份数据

-- 1. 先删除所有预约记录（因为它们引用了schedules）
DELETE FROM `appointments`;

-- 2. 再删除所有排班记录（因为它们引用了time_slots）
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

-- 7. 重新插入一些示例排班数据（使用新的时间段ID）
INSERT INTO `schedules` (`schedule_id`, `doctor_id`, `schedule_date`, `slot_id`, `location_id`, `total_slots`, `booked_slots`, `fee`, `status`, `remarks`) VALUES
-- 张建国医生 (doctor_id=3) 的排班示例
(1, 3, '2025-10-27', 1, 1, 10, 0, 15.00, 'available', '排班:张建国'),
(2, 3, '2025-10-28', 3, 1, 10, 0, 15.00, 'available', '排班:张建国'),
(3, 3, '2025-10-29', 2, 1, 10, 0, 15.00, 'available', '排班:张建国'),
(4, 3, '2025-10-30', 4, 1, 10, 0, 15.00, 'available', '排班:张建国');

-- 8. 验证排班数据
SELECT 
    s.schedule_id,
    s.doctor_id,
    s.schedule_date,
    s.slot_id,
    ts.slot_name,
    s.remarks
FROM schedules s
LEFT JOIN time_slots ts ON s.slot_id = ts.slot_id
ORDER BY s.schedule_date, s.slot_id;
