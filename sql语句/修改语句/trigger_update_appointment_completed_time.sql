-- 创建触发器：当 appointments.status 更新为 'completed' 时，自动更新 updated_at
-- 注意：需要先确保 appointments 表有 updated_at 字段

DELIMITER $$

DROP TRIGGER IF EXISTS `trg_appointment_completed_update_time`$$

CREATE TRIGGER `trg_appointment_completed_update_time`
BEFORE UPDATE ON `appointments`
FOR EACH ROW
BEGIN
    -- 当 status 从非 completed 变为 completed 时，更新 updated_at 为当前时间
    IF NEW.status = 'completed' AND (OLD.status IS NULL OR OLD.status != 'completed') THEN
        SET NEW.updated_at = NOW();
    END IF;
END$$

DELIMITER ;

