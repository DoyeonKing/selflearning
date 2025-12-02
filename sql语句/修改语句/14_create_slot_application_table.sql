-- 创建加号申请表
-- 用于医生申请临时增加号源
-- 医生只能为自己的排班申请加号

DROP TABLE IF EXISTS `slot_application`;

CREATE TABLE `slot_application` (
  `application_id` INT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `doctor_id` INT NOT NULL COMMENT '申请医生ID',
  `schedule_id` INT NOT NULL COMMENT '关联的排班ID（必填，从医生排班中选择）',
  `added_slots` INT NOT NULL COMMENT '加号数量',
  `patient_id` BIGINT NOT NULL COMMENT '指定患者ID（必填）',
  `urgency_level` ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM' COMMENT '紧急程度',
  `reason` TEXT NOT NULL COMMENT '申请理由',
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
  `approver_id` INT NULL COMMENT '审批人ID',
  `approver_comments` TEXT NULL COMMENT '审批意见',
  `approved_at` TIMESTAMP NULL COMMENT '审批时间',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`application_id`),
  KEY `idx_doctor_id` (`doctor_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_slot_application_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_slot_application_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`schedule_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_slot_application_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_slot_application_approver` FOREIGN KEY (`approver_id`) REFERENCES `admins` (`admin_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_added_slots` CHECK (`added_slots` >= 1 AND `added_slots` <= 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加号申请表';
