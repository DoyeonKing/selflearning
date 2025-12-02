-- =====================================================
-- 加号功能性能优化索引
-- 用于提升加号相关查询的性能
-- =====================================================

-- 1. 优化待支付加号统计查询
-- 用于 ScheduleAvailabilityService.getActualAvailableSlots()
-- 查询条件：schedule_id + appointment_type + status
CREATE INDEX IF NOT EXISTS idx_addon_pending 
ON appointments(schedule_id, appointment_type, status);

-- 2. 优化加号序号查询
-- 用于 AddOnSlotService.getNextAppointmentNumber()
-- 查询条件：schedule_id + appointment_type，排序：appointment_number DESC
CREATE INDEX IF NOT EXISTS idx_appointment_number 
ON appointments(schedule_id, appointment_type, appointment_number);

-- 3. 优化支付截止时间查询
-- 用于 AddOnPaymentExpirationTask.checkExpiredAddOnPayments()
-- 查询条件：appointment_type + status + payment_deadline
CREATE INDEX IF NOT EXISTS idx_payment_deadline_check 
ON appointments(appointment_type, status, payment_deadline);

-- =====================================================
-- 验证索引是否创建成功
-- =====================================================

-- 查看 appointments 表的所有索引
SHOW INDEX FROM appointments;

-- 查看索引使用情况（需要在查询后执行）
-- EXPLAIN SELECT COUNT(*) FROM appointments 
-- WHERE schedule_id = 1 AND appointment_type = 'ADD_ON' AND status = 'PENDING_PAYMENT';

-- =====================================================
-- 性能测试查询
-- =====================================================

-- 测试1：统计待支付加号（应该使用 idx_addon_pending）
EXPLAIN SELECT COUNT(*) FROM appointments 
WHERE schedule_id = 1 
  AND appointment_type = 'ADD_ON' 
  AND status = 'PENDING_PAYMENT';

-- 测试2：查询最大加号序号（应该使用 idx_appointment_number）
EXPLAIN SELECT MAX(appointment_number) FROM appointments 
WHERE schedule_id = 1 
  AND appointment_type = 'ADD_ON';

-- 测试3：查询超时加号（应该使用 idx_payment_deadline_check）
EXPLAIN SELECT * FROM appointments 
WHERE appointment_type = 'ADD_ON' 
  AND status = 'PENDING_PAYMENT' 
  AND payment_deadline < NOW();

-- =====================================================
-- 索引大小查询（监控索引占用空间）
-- =====================================================

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    ROUND(STAT_VALUE * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE TABLE_NAME = 'appointments'
  AND DATABASE_NAME = 'hospital_system'
  AND STAT_NAME = 'size'
ORDER BY size_mb DESC;

-- =====================================================
-- 注意事项
-- =====================================================

-- 1. 索引会占用额外的存储空间
-- 2. 索引会略微降低INSERT/UPDATE/DELETE的性能
-- 3. 但对于加号功能的查询性能提升是显著的
-- 4. 建议在生产环境的低峰期执行索引创建
-- 5. 创建索引期间表会被锁定，可能影响正在进行的操作
