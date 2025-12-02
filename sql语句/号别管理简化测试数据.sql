-- 号别管理简化测试数据
-- 只包含号别管理功能必需的数据

-- 1. 插入科室数据
INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(1, 1, '呼吸内科', '呼吸道疾病诊断治疗'),
(2, 1, '心血管内科', '心脏和血管疾病治疗'),
(3, 1, '消化内科', '消化道疾病诊断治疗');

-- 2. 插入医生数据
INSERT IGNORE INTO `doctors` (`doctor_id`, `department_id`, `identifier`, `password_hash`, `full_name`, `phone_number`, `title`, `status`) VALUES
(1, 1, 'DOC001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '张医生', '+8613800138001', '主任医师', 'active'),
(2, 2, 'DOC002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '李医生', '+8613800138002', '副主任医师', 'active'),
(3, 3, 'DOC003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '王医生', '+8613800138003', '主治医师', 'active');

-- 3. 插入时间段数据
INSERT IGNORE INTO `time_slots` (`slot_id`, `slot_name`, `start_time`, `end_time`) VALUES
(1, '上午 08:00-12:00', '08:00:00', '12:00:00'),
(2, '下午 14:00-18:00', '14:00:00', '18:00:00');

-- 4. 插入诊室数据
INSERT IGNORE INTO `locations` (`location_id`, `location_name`, `department_id`, `floor_level`, `building`, `room_number`, `capacity`) VALUES
(1, '门诊楼201', 1, 2, '门诊楼', '201', 20),
(2, '门诊楼202', 2, 2, '门诊楼', '202', 25),
(3, '门诊楼203', 3, 2, '门诊楼', '203', 30);

-- 5. 插入排班数据（本周数据）
INSERT IGNORE INTO `schedules` (`schedule_id`, `doctor_id`, `schedule_date`, `slot_id`, `location_id`, `total_slots`, `booked_slots`, `fee`, `status`, `remarks`) VALUES
-- 呼吸内科排班
(1, 1, '2025-10-20', 1, 1, 20, 5, 15.00, 'available', ''),
(2, 1, '2025-10-20', 2, 1, 25, 8, 15.00, 'available', ''),
(3, 1, '2025-10-21', 1, 1, 20, 3, 15.00, 'available', ''),
(4, 1, '2025-10-21', 2, 1, 25, 7, 15.00, 'available', ''),

-- 心血管内科排班
(5, 2, '2025-10-20', 1, 2, 30, 12, 25.00, 'available', ''),
(6, 2, '2025-10-20', 2, 2, 20, 3, 25.00, 'available', ''),
(7, 2, '2025-10-21', 1, 2, 30, 8, 25.00, 'available', ''),
(8, 2, '2025-10-21', 2, 2, 20, 5, 25.00, 'available', ''),

-- 消化内科排班
(9, 3, '2025-10-20', 1, 3, 25, 0, 20.00, 'available', ''),
(10, 3, '2025-10-20', 2, 3, 20, 5, 20.00, 'available', ''),
(11, 3, '2025-10-21', 1, 3, 25, 2, 20.00, 'available', ''),
(12, 3, '2025-10-21', 2, 3, 20, 8, 20.00, 'available', ''),

-- 下周排班数据 (2025-10-27 到 2025-10-31)

-- 呼吸内科下周排班
(13, 1, '2025-10-27', 1, 1, 20, 0, 15.00, 'available', ''),
(14, 1, '2025-10-27', 2, 1, 25, 0, 15.00, 'available', ''),
(15, 1, '2025-10-28', 1, 1, 20, 0, 15.00, 'available', ''),
(16, 1, '2025-10-28', 2, 1, 25, 0, 15.00, 'available', ''),
(17, 1, '2025-10-29', 1, 1, 20, 0, 15.00, 'available', ''),
(18, 1, '2025-10-29', 2, 1, 25, 0, 15.00, 'available', ''),
(19, 1, '2025-10-30', 1, 1, 20, 0, 15.00, 'available', ''),
(20, 1, '2025-10-30', 2, 1, 25, 0, 15.00, 'available', ''),
(21, 1, '2025-10-31', 1, 1, 20, 0, 15.00, 'available', ''),
(22, 1, '2025-10-31', 2, 1, 25, 0, 15.00, 'available', ''),

-- 心血管内科下周排班
(23, 2, '2025-10-27', 1, 2, 30, 0, 25.00, 'available', ''),
(24, 2, '2025-10-27', 2, 2, 20, 0, 25.00, 'available', ''),
(25, 2, '2025-10-28', 1, 2, 30, 0, 25.00, 'available', ''),
(26, 2, '2025-10-28', 2, 2, 20, 0, 25.00, 'available', ''),
(27, 2, '2025-10-29', 1, 2, 30, 0, 25.00, 'available', ''),
(28, 2, '2025-10-29', 2, 2, 20, 0, 25.00, 'available', ''),
(29, 2, '2025-10-30', 1, 2, 30, 0, 25.00, 'available', ''),
(30, 2, '2025-10-30', 2, 2, 20, 0, 25.00, 'available', ''),
(31, 2, '2025-10-31', 1, 2, 30, 0, 25.00, 'available', ''),
(32, 2, '2025-10-31', 2, 2, 20, 0, 25.00, 'available', ''),

-- 消化内科下周排班
(33, 3, '2025-10-27', 1, 3, 25, 0, 20.00, 'available', ''),
(34, 3, '2025-10-27', 2, 3, 20, 0, 20.00, 'available', ''),
(35, 3, '2025-10-28', 1, 3, 25, 0, 20.00, 'available', ''),
(36, 3, '2025-10-28', 2, 3, 20, 0, 20.00, 'available', ''),
(37, 3, '2025-10-29', 1, 3, 25, 0, 20.00, 'available', ''),
(38, 3, '2025-10-29', 2, 3, 20, 0, 20.00, 'available', ''),
(39, 3, '2025-10-30', 1, 3, 25, 0, 20.00, 'available', ''),
(40, 3, '2025-10-30', 2, 3, 20, 0, 20.00, 'available', ''),
(41, 3, '2025-10-31', 1, 3, 25, 0, 20.00, 'available', ''),
(42, 3, '2025-10-31', 2, 3, 20, 0, 20.00, 'available', '');

-- 6. 验证数据
SELECT 
    s.schedule_id,
    s.schedule_date,
    d.full_name as doctor_name,
    d.title as doctor_title,
    dept.name as department_name,
    ts.slot_name,
    l.location_name,
    s.total_slots,
    s.booked_slots,
    s.fee,
    s.status
FROM schedules s
LEFT JOIN doctors d ON s.doctor_id = d.doctor_id
LEFT JOIN departments dept ON d.department_id = dept.department_id
LEFT JOIN time_slots ts ON s.slot_id = ts.slot_id
LEFT JOIN locations l ON s.location_id = l.location_id
ORDER BY s.schedule_date, ts.start_time;
