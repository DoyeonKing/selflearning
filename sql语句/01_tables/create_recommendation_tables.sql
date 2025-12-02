-- 症状同义词表
CREATE TABLE IF NOT EXISTS `symptom_synonyms` (
  `synonym_id` INT NOT NULL AUTO_INCREMENT,
  `symptom_keyword` VARCHAR(100) NOT NULL COMMENT '症状关键词',
  `synonym` VARCHAR(100) NOT NULL COMMENT '同义词',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`synonym_id`),
  INDEX `idx_symptom_keyword` (`symptom_keyword`),
  INDEX `idx_synonym` (`synonym`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='症状同义词表';

-- 医生画像表（用于缓存医生特征，提升推荐性能）
CREATE TABLE IF NOT EXISTS `doctor_profiles` (
  `profile_id` INT NOT NULL AUTO_INCREMENT,
  `doctor_id` INT NOT NULL COMMENT '医生ID',
  `extracted_keywords` TEXT COMMENT '从specialty提取的关键词（逗号分隔）',
  `appointment_count` INT DEFAULT 0 COMMENT '总预约数（用于热度计算）',
  `avg_rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分（预留）',
  `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_doctor_id` (`doctor_id`),
  FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`doctor_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生画像表';

-- 插入常用症状同义词数据
INSERT INTO `symptom_synonyms` (`symptom_keyword`, `synonym`) VALUES
('头痛', '头疼'),
('头痛', '头昏'),
('头痛', '头部疼痛'),
('恶心', '想吐'),
('恶心', '反胃'),
('咳嗽', '咳痰'),
('咳嗽', '干咳'),
('发热', '发烧'),
('发热', '体温升高'),
('腹痛', '肚子疼'),
('腹痛', '腹部疼痛'),
('失眠', '睡不着'),
('失眠', '睡眠障碍'),
('头晕', '头昏'),
('头晕', '眩晕'),
('胸闷', '胸痛'),
('胸闷', '胸部不适');

