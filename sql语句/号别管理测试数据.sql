-- 号别管理测试数据SQL
-- 用于测试排班费用管理功能

-- 1. 插入科室数据
INSERT IGNORE INTO `parent_departments` (`parent_department_id`, `name`, `description`) VALUES
(1, '内科', '负责内科疾病诊断和治疗'),
(2, '外科', '负责外科手术和创伤处理'),
(3, '专科', '各类专科门诊');

INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(1, 1, '呼吸内科', '呼吸道疾病诊断治疗'),
(2, 1, '心血管内科', '心脏和血管疾病治疗'),
(3, 1, '消化内科', '消化道疾病诊断治疗'),
(4, 2, '普外科', '普通外科手术'),
(5, 2, '骨科', '骨科疾病治疗'),
(6, 3, '口腔科', '口腔疾病治疗'),
(7, 3, '眼科', '眼科疾病治疗');

-- 2. 插入医生数据
INSERT IGNORE INTO `doctors` (`doctor_id`, `department_id`, `identifier`, `password_hash`, `full_name`, `id_card_number`, `phone_number`, `title`, `specialty`, `bio`, `status`) VALUES
(1, 1, 'DOC001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '张医生', '110101199001011001', '+8613800138001', '主任医师', '呼吸系统疾病', '擅长呼吸系统疾病的诊断和治疗', 'active'),
(2, 2, 'DOC002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '李医生', '110101199002021002', '+8613800138002', '副主任医师', '心血管疾病', '擅长心血管疾病的诊断和治疗', 'active'),
(3, 3, 'DOC003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '王医生', '110101199003031003', '+8613800138003', '主治医师', '消化系统疾病', '擅长消化系统疾病的诊断和治疗', 'active'),
(4, 4, 'DOC004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '赵医生', '110101199004041004', '+8613800138004', '主任医师', '外科手术', '擅长各种外科手术', 'active'),
(5, 5, 'DOC005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '陈医生', '110101199005051005', '+8613800138005', '副主任医师', '骨科疾病', '擅长骨科疾病的诊断和治疗', 'active');

-- 3. 插入时间段数据
INSERT IGNORE INTO `time_slots` (`slot_id`, `slot_name`, `start_time`, `end_time`) VALUES
(1, '上午 08:00-08:30', '08:00:00', '08:30:00'),
(2, '上午 08:30-09:00', '08:30:00', '09:00:00'),
(3, '上午 09:00-12:00', '09:00:00', '12:00:00'),
(4, '下午 14:00-14:30', '14:00:00', '14:30:00'),
(5, '下午 14:30-15:00', '14:30:00', '15:00:00'),
(6, '下午 15:00-18:00', '15:00:00', '18:00:00'),
(7, '晚上 19:00-21:00', '19:00:00', '21:00:00');

-- 4. 插入诊室数据
INSERT IGNORE INTO `locations` (`location_id`, `location_name`, `department_id`, `floor_level`, `building`, `room_number`, `capacity`) VALUES
(1, '门诊楼201', 1, 2, '门诊楼', '201', 20),
(2, '门诊楼202', 1, 2, '门诊楼', '202', 20),
(3, '门诊楼203', 2, 2, '门诊楼', '203', 25),
(4, '门诊楼204', 2, 2, '门诊楼', '204', 25),
(5, '门诊楼301', 3, 3, '门诊楼', '301', 30),
(6, '门诊楼302', 4, 3, '门诊楼', '302', 20),
(7, '门诊楼303', 5, 3, '门诊楼', '303', 15);

-- 5. 插入排班数据（本周和下周的数据）
INSERT IGNORE INTO `schedules` (`schedule_id`, `doctor_id`, `schedule_date`, `slot_id`, `location_id`, `total_slots`, `booked_slots`, `fee`, `status`, `remarks`) VALUES
-- 本周数据 (2025-10-20 到 2025-10-26)
(1, 1, '2025-10-20', 3, 1, 20, 5, 15.00, 'available', ''),
(2, 1, '2025-10-20', 6, 1, 25, 8, 15.00, 'available', ''),
(3, 2, '2025-10-20', 3, 3, 30, 12, 25.00, 'available', ''),
(4, 2, '2025-10-20', 6, 3, 20, 3, 25.00, 'available', ''),
(5, 3, '2025-10-21', 3, 5, 25, 0, 20.00, 'available', ''),
(6, 3, '2025-10-21', 6, 5, 20, 5, 20.00, 'available', ''),
(7, 4, '2025-10-21', 3, 6, 15, 2, 30.00, 'available', ''),
(8, 4, '2025-10-21', 6, 6, 18, 7, 30.00, 'available', ''),
(9, 5, '2025-10-22', 3, 7, 12, 1, 35.00, 'available', ''),
(10, 5, '2025-10-22', 6, 7, 15, 4, 35.00, 'available', ''),

-- 下周数据 (2025-10-27 到 2025-11-02)
(11, 1, '2025-10-27', 3, 1, 20, 0, 15.00, 'available', ''),
(12, 1, '2025-10-27', 6, 1, 25, 0, 15.00, 'available', ''),
(13, 2, '2025-10-27', 3, 3, 30, 0, 25.00, 'available', ''),
(14, 2, '2025-10-27', 6, 3, 20, 0, 25.00, 'available', ''),
(15, 3, '2025-10-28', 3, 5, 25, 0, 20.00, 'available', ''),
(16, 3, '2025-10-28', 6, 5, 20, 0, 20.00, 'available', ''),
(17, 4, '2025-10-28', 3, 6, 15, 0, 30.00, 'available', ''),
(18, 4, '2025-10-28', 6, 6, 18, 0, 30.00, 'available', ''),
(19, 5, '2025-10-29', 3, 7, 12, 0, 35.00, 'available', ''),
(20, 5, '2025-10-29', 6, 7, 15, 0, 35.00, 'available', '');

-- 6. 插入一些预约数据（用于测试已预约数）
INSERT IGNORE INTO `appointments` (`appointment_id`, `patient_id`, `schedule_id`, `appointment_number`, `status`, `created_at`) VALUES
(1, 1, 1, 'A001', 'confirmed', NOW()),
(2, 2, 1, 'A002', 'confirmed', NOW()),
(3, 3, 1, 'A003', 'confirmed', NOW()),
(4, 4, 1, 'A004', 'confirmed', NOW()),
(5, 5, 1, 'A005', 'confirmed', NOW()),
(6, 6, 3, 'A006', 'confirmed', NOW()),
(7, 7, 3, 'A007', 'confirmed', NOW()),
(8, 8, 3, 'A008', 'confirmed', NOW()),
(9, 9, 3, 'A009', 'confirmed', NOW()),
(10, 10, 3, 'A010', 'confirmed', NOW()),
(11, 11, 3, 'A011', 'confirmed', NOW()),
(12, 12, 3, 'A012', 'confirmed', NOW());

-- 7. 插入患者数据（用于预约）
INSERT IGNORE INTO `patients` (`patient_id`, `identifier`, `patient_type`, `password_hash`, `full_name`, `phone_number`, `status`) VALUES
(1, 'P001', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者1', '+8613800139001', 'active'),
(2, 'P002', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者2', '+8613800139002', 'active'),
(3, 'P003', 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者3', '+8613800139003', 'active'),
(4, 'P004', 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者4', '+8613800139004', 'active'),
(5, 'P005', 'staff', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者5', '+8613800139005', 'active'),
(6, 'P006', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者6', '+8613800139006', 'active'),
(7, 'P007', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者7', '+8613800139007', 'active'),
(8, 'P008', 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者8', '+8613800139008', 'active'),
(9, 'P009', 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者9', '+8613800139009', 'active'),
(10, 'P010', 'staff', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者10', '+8613800139010', 'active'),
(11, 'P011', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者11', '+8613800139011', 'active'),
(12, 'P012', 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '患者12', '+8613800139012', 'active');

-- 8. 更新排班表中的已预约数
UPDATE schedules s 
SET booked_slots = (
    SELECT COUNT(*) 
    FROM appointments a 
    WHERE a.schedule_id = s.schedule_id 
    AND a.status = 'confirmed'
);

-- 9. 查询测试数据
-- 查看排班数据
SELECT 
    s.schedule_id,
    s.schedule_date,
    d.full_name as doctor_name,
    d.title as doctor_title,
    dept.name as department_name,
    ts.slot_name,
    ts.start_time,
    ts.end_time,
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

-- 查看科室排班统计
SELECT 
    dept.name as department_name,
    COUNT(*) as total_schedules,
    SUM(s.total_slots) as total_slots,
    SUM(s.booked_slots) as total_booked,
    AVG(s.fee) as avg_fee
FROM schedules s
LEFT JOIN doctors d ON s.doctor_id = d.doctor_id
LEFT JOIN departments dept ON d.department_id = dept.department_id
GROUP BY dept.department_id, dept.name
ORDER BY total_schedules DESC;
