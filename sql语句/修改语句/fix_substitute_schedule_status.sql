-- 修复替班排班的状态
-- 这个脚本用于修复已经分配了替班医生但状态仍为 cancelled 的排班

-- 查看当前有问题的排班（状态为cancelled但有预约的排班）
SELECT 
    s.schedule_id,
    s.doctor_id,
    d.full_name AS doctor_name,
    s.schedule_date,
    ts.slot_name,
    s.status,
    s.booked_slots,
    s.total_slots,
    s.remarks
FROM schedules s
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN time_slots ts ON s.slot_id = ts.slot_id
WHERE s.status = 'cancelled'
  AND s.booked_slots > 0
ORDER BY s.schedule_date, ts.start_time;

-- 修复这些排班的状态
-- 如果已约满，设置为 full；否则设置为 available
UPDATE schedules s
SET status = CASE 
    WHEN s.booked_slots >= s.total_slots THEN 'full'
    ELSE 'available'
END
WHERE s.status = 'cancelled'
  AND s.booked_slots > 0;

-- 验证修复结果
SELECT 
    s.schedule_id,
    s.doctor_id,
    d.full_name AS doctor_name,
    s.schedule_date,
    ts.slot_name,
    s.status,
    s.booked_slots,
    s.total_slots
FROM schedules s
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN time_slots ts ON s.slot_id = ts.slot_id
WHERE s.booked_slots > 0
  AND s.status IN ('available', 'full')
ORDER BY s.schedule_date, ts.start_time;
