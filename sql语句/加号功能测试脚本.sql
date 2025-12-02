-- =====================================================
-- 加号功能测试脚本
-- 用于快速测试加号批准功能
-- =====================================================

-- 1. 查看现有的医生排班（用于创建加号申请）
SELECT 
    s.schedule_id,
    d.full_name AS doctor_name,
    s.schedule_date,
    ts.start_time,
    ts.end_time,
    l.location_name,
    s.total_slots,
    s.booked_slots,
    s.fee,
    s.status,
    s.remarks
FROM schedules s
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN time_slots ts ON s.slot_id = ts.slot_id
JOIN locations l ON s.location_id = l.location_id
WHERE s.schedule_date >= CURDATE()
  AND s.status = 'available'
ORDER BY s.schedule_date, ts.start_time
LIMIT 10;

-- 2. 查看现有患者（用于指定加号患者）
SELECT 
    patient_id,
    full_name,
    phone_number,
    id_number
FROM patients
LIMIT 10;

-- 3. 查看所有加号申请
SELECT 
    sa.application_id,
    sa.status,
    d.full_name AS doctor_name,
    p.full_name AS patient_name,
    s.schedule_date,
    sa.added_slots,
    sa.urgency_level,
    sa.reason,
    sa.created_at,
    sa.approved_at,
    sa.appointment_id
FROM slot_application sa
JOIN doctors d ON sa.doctor_id = d.doctor_id
JOIN patients p ON sa.patient_id = p.patient_id
JOIN schedules s ON sa.schedule_id = s.schedule_id
ORDER BY sa.created_at DESC;

-- 4. 查看待审批的加号申请
SELECT 
    sa.application_id,
    d.full_name AS doctor_name,
    p.full_name AS patient_name,
    p.phone_number AS patient_phone,
    s.schedule_date,
    ts.start_time,
    ts.end_time,
    sa.added_slots,
    sa.urgency_level,
    sa.reason,
    sa.created_at
FROM slot_application sa
JOIN doctors d ON sa.doctor_id = d.doctor_id
JOIN patients p ON sa.patient_id = p.patient_id
JOIN schedules s ON sa.schedule_id = s.schedule_id
JOIN time_slots ts ON s.slot_id = ts.slot_id
WHERE sa.status = 'PENDING'
ORDER BY sa.urgency_level DESC, sa.created_at ASC;

-- 5. 查看加号生成的预约记录
SELECT 
    a.appointment_id,
    a.appointment_number,
    p.full_name AS patient_name,
    d.full_name AS doctor_name,
    s.schedule_date,
    a.status,
    a.payment_status,
    a.payment_deadline,
    CASE 
        WHEN a.payment_deadline IS NULL THEN NULL
        WHEN a.payment_deadline < NOW() THEN '已超时'
        ELSE CONCAT(TIMESTAMPDIFF(MINUTE, NOW(), a.payment_deadline), '分钟')
    END AS remaining_time,
    a.is_add_on,
    a.created_at
FROM appointments a
JOIN patients p ON a.patient_id = p.patient_id
JOIN schedules s ON a.schedule_id = s.schedule_id
JOIN doctors d ON s.doctor_id = d.doctor_id
WHERE a.appointment_type = 'ADD_ON'
ORDER BY a.created_at DESC;

-- 6. 查看待支付的加号预约（含倒计时）
SELECT 
    a.appointment_id,
    p.full_name AS patient_name,
    p.phone_number,
    d.full_name AS doctor_name,
    s.schedule_date,
    a.payment_deadline,
    TIMESTAMPDIFF(MINUTE, NOW(), a.payment_deadline) AS remaining_minutes,
    CASE 
        WHEN TIMESTAMPDIFF(MINUTE, NOW(), a.payment_deadline) < 0 THEN '已超时'
        WHEN TIMESTAMPDIFF(MINUTE, NOW(), a.payment_deadline) < 60 THEN '即将超时'
        ELSE '正常'
    END AS status_desc
FROM appointments a
JOIN patients p ON a.patient_id = p.patient_id
JOIN schedules s ON a.schedule_id = s.schedule_id
JOIN doctors d ON s.doctor_id = d.doctor_id
WHERE a.appointment_type = 'ADD_ON'
  AND a.status = 'PENDING_PAYMENT'
  AND a.payment_deadline IS NOT NULL
ORDER BY a.payment_deadline ASC;

-- 7. 查看已超时但未处理的加号预约
SELECT 
    a.appointment_id,
    p.full_name AS patient_name,
    a.payment_deadline,
    TIMESTAMPDIFF(MINUTE, a.payment_deadline, NOW()) AS overdue_minutes,
    a.status
FROM appointments a
JOIN patients p ON a.patient_id = p.patient_id
WHERE a.appointment_type = 'ADD_ON'
  AND a.status = 'PENDING_PAYMENT'
  AND a.payment_deadline < NOW()
ORDER BY a.payment_deadline ASC;

-- 8. 查看排班号源变化（批准前后对比）
SELECT 
    s.schedule_id,
    d.full_name AS doctor_name,
    s.schedule_date,
    s.total_slots,
    s.booked_slots,
    s.total_slots - s.booked_slots AS available_slots,
    s.remarks,
    s.updated_at
FROM schedules s
JOIN doctors d ON s.doctor_id = d.doctor_id
WHERE s.remarks LIKE '%加号%'
ORDER BY s.updated_at DESC;

-- 9. 查看加号相关的通知
SELECT 
    n.notification_id,
    n.user_type,
    n.title,
    n.content,
    n.related_entity,
    n.related_id,
    n.priority,
    n.is_read,
    n.created_at
FROM notifications n
WHERE n.related_entity IN ('slot_application', 'appointment')
  AND (n.title LIKE '%加号%' OR n.content LIKE '%加号%')
ORDER BY n.created_at DESC
LIMIT 20;

-- =====================================================
-- 测试辅助脚本
-- =====================================================

-- 手动创建一个测试加号申请（如果需要）
-- INSERT INTO slot_application (doctor_id, schedule_id, added_slots, patient_id, urgency_level, reason, status)
-- VALUES (1, 1, 2, 1, 'MEDIUM', '测试加号申请', 'PENDING');

-- 手动设置支付截止时间为过去时间（测试超时处理）
-- UPDATE appointments 
-- SET payment_deadline = DATE_SUB(NOW(), INTERVAL 1 HOUR)
-- WHERE appointment_id = ? AND appointment_type = 'ADD_ON';

-- 手动触发支付成功（测试支付流程）
-- UPDATE appointments 
-- SET status = 'scheduled', 
--     payment_status = 'paid',
--     payment_method = 'alipay',
--     transaction_id = 'TEST_' || UUID()
-- WHERE appointment_id = ? AND appointment_type = 'ADD_ON';

-- 查看特定申请的完整流程
-- SELECT 
--     'slot_application' AS source,
--     sa.application_id AS id,
--     sa.status,
--     sa.created_at AS time,
--     sa.appointment_id
-- FROM slot_application sa
-- WHERE sa.application_id = ?
-- UNION ALL
-- SELECT 
--     'appointment' AS source,
--     a.appointment_id AS id,
--     a.status,
--     a.created_at AS time,
--     NULL
-- FROM appointments a
-- WHERE a.appointment_id = (SELECT appointment_id FROM slot_application WHERE application_id = ?)
-- ORDER BY time;

-- =====================================================
-- 数据清理（谨慎使用）
-- =====================================================

-- 删除测试数据（仅在测试环境使用）
-- DELETE FROM appointments WHERE appointment_type = 'ADD_ON' AND created_at > '2025-11-27';
-- DELETE FROM slot_application WHERE created_at > '2025-11-27';
