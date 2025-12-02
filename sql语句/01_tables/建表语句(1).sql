-- 校医院挂号系统数据库表结构创建脚本 (v4.2)
-- 完整可重复执行版本 - 增强初始数据

SET FOREIGN_KEY_CHECKS = 0;

-- 一、核心用户模型

-- 1. patients (患者表)
DROP TABLE IF EXISTS `patients`;
CREATE TABLE `patients` (
  `patient_id` INT NOT NULL AUTO_INCREMENT,
  `identifier` VARCHAR(100) NOT NULL COMMENT '学号或工号',
  `patient_type` ENUM('student','teacher','staff') NOT NULL COMMENT '患者类型',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '哈希加盐后的密码',
  `full_name` VARCHAR(100) NOT NULL COMMENT '真实姓名',
  `phone_number` VARCHAR(20) NOT NULL COMMENT '存储E.164标准格式',
  `status` ENUM('active','inactive','locked','deleted') NOT NULL DEFAULT 'active' COMMENT '账户状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息最后更新时间',
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `uk_patients_identifier` (`identifier`),
  UNIQUE KEY `uk_patients_phone` (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者表';

-- 2. doctors (医生表)
DROP TABLE IF EXISTS `doctors`;
CREATE TABLE `doctors` (
  `doctor_id` INT NOT NULL AUTO_INCREMENT,
  `department_id` INT NOT NULL COMMENT '所属科室',
  `identifier` VARCHAR(100) NOT NULL COMMENT '医生的工号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '哈希加盐后的密码',
  `full_name` VARCHAR(100) NOT NULL COMMENT '真实姓名',
  `id_card_number` VARCHAR(18) NOT NULL COMMENT '身份证号, 建议加密',
  `phone_number` VARCHAR(20) NOT NULL COMMENT '存储E.164标准格式',
  `title` VARCHAR(100) NOT NULL COMMENT '职称',
  `specialty` TEXT COMMENT '擅长领域描述',
  `bio` TEXT COMMENT '个人简介',
  `photo_url` VARCHAR(255) COMMENT '头像照片URL',
  `status` ENUM('active','inactive','locked','deleted') NOT NULL DEFAULT 'active' COMMENT '账户状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息最后更新时间',
  PRIMARY KEY (`doctor_id`),
  UNIQUE KEY `uk_doctors_identifier` (`identifier`),
  UNIQUE KEY `uk_doctors_id_card` (`id_card_number`),
  UNIQUE KEY `uk_doctors_phone` (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- 3. admins (管理员表)
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `admin_id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL COMMENT '管理员登录用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '哈希加盐后的密码',
  `full_name` VARCHAR(100) NOT NULL COMMENT '管理员真实姓名',
  `status` ENUM('active','inactive','locked') NOT NULL DEFAULT 'active' COMMENT '账户状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_admins_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 二、组织架构与排班模型

-- 4. parent_departments (父科室表)
DROP TABLE IF EXISTS `parent_departments`;
CREATE TABLE `parent_departments` (
  `parent_department_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '父科室名称',
  `description` TEXT COMMENT '科室职能描述',
  PRIMARY KEY (`parent_department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='父科室表';

-- 5. departments (子科室表)
DROP TABLE IF EXISTS `departments`;
CREATE TABLE `departments` (
  `department_id` INT NOT NULL AUTO_INCREMENT,
  `parent_id` INT NOT NULL COMMENT '父科室ID',
  `name` VARCHAR(100) NOT NULL COMMENT '子科室名称',
  `description` TEXT COMMENT '科室职能描述',
  PRIMARY KEY (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='子科室表';

-- 6. time_slots (固定时间段表)
DROP TABLE IF EXISTS `time_slots`;
CREATE TABLE `time_slots` (
  `slot_id` INT NOT NULL AUTO_INCREMENT,
  `slot_name` VARCHAR(100) NOT NULL COMMENT '时段名称',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间',
  PRIMARY KEY (`slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='固定时间段表';

-- 7. locations (诊室表)
DROP TABLE IF EXISTS `locations`;
CREATE TABLE `locations` (
  `location_id` INT NOT NULL AUTO_INCREMENT,
  `location_name` VARCHAR(100) NOT NULL COMMENT '诊室名称，如：门诊楼201室',
  `department_id` INT COMMENT '所属科室',
  `floor_level` INT COMMENT '楼层',
  `building` VARCHAR(50) COMMENT '楼栋',
  `room_number` VARCHAR(20) COMMENT '房间号',
  `capacity` INT COMMENT '容纳人数',
  `map_node_id` INT COMMENT '对应的地图节点',
  PRIMARY KEY (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诊室表';

-- 8. schedules (医生排班表)
DROP TABLE IF EXISTS `schedules`;
CREATE TABLE `schedules` (
  `schedule_id` INT NOT NULL AUTO_INCREMENT,
  `doctor_id` INT NOT NULL COMMENT '医生ID',
  `schedule_date` DATE NOT NULL COMMENT '出诊日期',
  `slot_id` INT NOT NULL COMMENT '时间段ID',
  `location_id` INT NOT NULL COMMENT '就诊地点ID',
  `total_slots` INT NOT NULL COMMENT '总号源数',
  `booked_slots` INT NOT NULL DEFAULT 0 COMMENT '已预约数',
  `fee` DECIMAL(10,2) NOT NULL DEFAULT 5.00 COMMENT '挂号费用',
  `status` ENUM('available','full','cancelled') NOT NULL DEFAULT 'available' COMMENT '排班状态',
  `remarks` TEXT COMMENT '排班要求或备注信息',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `uk_schedules_doctor_date_slot` (`doctor_id`, `schedule_date`, `slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生排班表';

-- 三、核心业务与流程模型

-- 9. patient_profiles (患者信息扩展表)
DROP TABLE IF EXISTS `patient_profiles`;
CREATE TABLE `patient_profiles` (
  `patient_id` INT NOT NULL COMMENT '患者ID',
  `id_card_number` VARCHAR(18) COMMENT '身份证号, 建议加密存储',
  `allergies` TEXT COMMENT '过敏史, 建议加密存储',
  `medical_history` TEXT COMMENT '基础病史, 建议加密存储',
  `no_show_count` INT NOT NULL DEFAULT 0 COMMENT '爽约次数',
  `blacklist_status` ENUM('normal','blacklisted') NOT NULL DEFAULT 'normal' COMMENT '黑名单状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息扩展表';

-- 10. appointments (预约挂号表)
DROP TABLE IF EXISTS `appointments`;
CREATE TABLE `appointments` (
  `appointment_id` INT NOT NULL AUTO_INCREMENT,
  `patient_id` INT NOT NULL COMMENT '患者ID',
  `schedule_id` INT NOT NULL COMMENT '排班ID',
  `appointment_number` INT NOT NULL COMMENT '就诊序号',
  `status` ENUM(
      'PENDING_PAYMENT',  -- 待支付
      'scheduled',        -- 已预约（未签到）
      'CHECKED_IN',       -- 已签到（未就诊）
      'completed',        -- 已完成
      'cancelled',        -- 已取消
      'NO_SHOW'          -- 爽约
  ) NOT NULL DEFAULT 'scheduled' COMMENT '预约状态',
  `payment_status` ENUM('unpaid','paid','refunded') NOT NULL DEFAULT 'unpaid' COMMENT '支付状态',
  `payment_method` VARCHAR(50) COMMENT '支付方式',
  `transaction_id` VARCHAR(255) COMMENT '支付流水号',
  `check_in_time` DATETIME COMMENT '现场签到时间',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预约生成时间',
  `updated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后更新时间（status变为completed时由触发器更新）',
  -- 叫号相关字段
  `called_at` DATETIME NULL COMMENT '叫号时间（NULL表示未叫号）',
  `is_on_time` TINYINT(1) DEFAULT 0 COMMENT '是否按时签到（预约时段开始后20分钟内）',
  `missed_call_count` INT DEFAULT 0 COMMENT '过号次数',
  `recheck_in_time` DATETIME NULL COMMENT '过号后重新签到时间',
  -- 排队相关字段
  `is_walk_in` TINYINT(1) DEFAULT 0 COMMENT '是否现场挂号（0=预约，1=现场挂号）',
  `real_time_queue_number` INT NULL COMMENT '实时候诊序号（在时段内按签到时间分配）',
  `is_late` TINYINT(1) DEFAULT 0 COMMENT '是否迟到（超过时段结束时间+软关门时间）',
  -- 预约类型相关字段
  `appointment_type` ENUM(
      'APPOINTMENT',        -- 预约挂号
      'WALK_IN',            -- 现场挂号
      'SAME_DAY_FOLLOW_UP', -- 当日复诊号
      'ADD_ON'              -- 加号
  ) NULL DEFAULT 'APPOINTMENT' COMMENT '预约类型',
  `original_appointment_id` INT NULL COMMENT '原始预约ID（用于复诊号关联，复诊号关联到原始预约）',
  `is_add_on` TINYINT(1) DEFAULT 0 COMMENT '是否加号（0=否，1=是）',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_check_in_time` (`check_in_time`),
  KEY `idx_called_at` (`called_at`),
  KEY `idx_is_on_time` (`is_on_time`),
  KEY `idx_recheck_in_time` (`recheck_in_time`),
  KEY `idx_is_walk_in` (`is_walk_in`),
  KEY `idx_real_time_queue_number` (`real_time_queue_number`),
  KEY `idx_is_late` (`is_late`),
  KEY `idx_appointment_type` (`appointment_type`),
  KEY `idx_original_appointment_id` (`original_appointment_id`),
  KEY `idx_is_add_on` (`is_add_on`),
  CONSTRAINT `fk_original_appointment` FOREIGN KEY (`original_appointment_id`) 
      REFERENCES `appointments` (`appointment_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';

-- 11. waitlist (候补表)
DROP TABLE IF EXISTS `waitlist`;
CREATE TABLE `waitlist` (
  `waitlist_id` INT NOT NULL AUTO_INCREMENT,
  `patient_id` INT NOT NULL COMMENT '患者ID',
  `schedule_id` INT NOT NULL COMMENT '排班ID',
  `status` ENUM('waiting','notified','expired','booked') NOT NULL DEFAULT 'waiting' COMMENT '候补状态',
  `notification_sent_at` DATETIME COMMENT '系统发送通知的时间',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入队列的时间',
  PRIMARY KEY (`waitlist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候补表';

-- 12. symptom_department_mapping (症状-科室映射表)
DROP TABLE IF EXISTS `symptom_department_mapping`;
CREATE TABLE `symptom_department_mapping` (
  `mapping_id` INT NOT NULL AUTO_INCREMENT,
  `symptom_keywords` VARCHAR(255) NOT NULL COMMENT '症状关键词',
  `department_id` INT NOT NULL COMMENT '推荐科室',
  `priority` INT NOT NULL DEFAULT 1 COMMENT '推荐优先级',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='症状-科室映射表';

-- 13. medical_guidelines (就医规范表)
DROP TABLE IF EXISTS `medical_guidelines`;
CREATE TABLE `medical_guidelines` (
  `guideline_id` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '规范标题',
  `content` TEXT NOT NULL COMMENT '规范内容',
  `category` VARCHAR(100) NOT NULL COMMENT '规范分类',
  `status` ENUM('active','inactive') NOT NULL DEFAULT 'active' COMMENT '状态',
  `created_by` INT NOT NULL COMMENT '创建人管理员ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`guideline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='就医规范表';

-- 14. notifications (通知消息表)
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '接收用户ID',
  `user_type` ENUM('patient','doctor','admin') NOT NULL COMMENT '用户类型',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型: appointment_reminder, cancellation, waitlist_available, schedule_change, system_notice等',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `related_entity` VARCHAR(50) COMMENT '相关实体类型 (如: appointment, schedule)',
  `related_id` INT COMMENT '相关实体ID',
  `status` ENUM('unread','read','deleted') NOT NULL DEFAULT 'unread' COMMENT '通知状态',
  `priority` ENUM('low','normal','high','urgent') NOT NULL DEFAULT 'normal' COMMENT '优先级',
  `sent_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_at` DATETIME COMMENT '阅读时间',
  PRIMARY KEY (`notification_id`),
  INDEX `idx_user` (`user_id`, `user_type`),
  INDEX `idx_status` (`status`),
  INDEX `idx_sent_at` (`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- 四、权限与管理模型 (RBAC)

-- 15. roles (角色表)
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `role_id` INT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` TEXT COMMENT '角色职责描述',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_roles_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 16. permissions (权限表)
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `permission_id` INT NOT NULL AUTO_INCREMENT,
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `description` TEXT COMMENT '权限详细描述',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permissions_name` (`permission_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 17. role_permissions (角色-权限映射表)
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
  `role_id` INT NOT NULL COMMENT '角色ID',
  `permission_id` INT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限映射表';

-- 18. admin_roles (管理员-角色映射表)
DROP TABLE IF EXISTS `admin_roles`;
CREATE TABLE `admin_roles` (
  `admin_id` INT NOT NULL COMMENT '管理员ID',
  `role_id` INT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`admin_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员-角色映射表';

-- 五、辅助与日志模型

-- 19. leave_requests (调班/休假申请表)
DROP TABLE IF EXISTS `leave_requests`;
CREATE TABLE `leave_requests` (
  `request_id` INT NOT NULL AUTO_INCREMENT,
  `doctor_id` INT NOT NULL COMMENT '申请人医生ID',
  `request_type` ENUM('leave','schedule_change') NOT NULL COMMENT '申请类型',
  `start_time` DATETIME NOT NULL COMMENT '申请开始时间',
  `end_time` DATETIME NOT NULL COMMENT '申请结束时间',
  `reason` TEXT NOT NULL COMMENT '申请事由',
  `status` ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `approver_id` INT COMMENT '审批人管理员ID',
  `approver_comments` TEXT COMMENT '审批人员的意见或备注',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '申请更新时间',
  PRIMARY KEY (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调班/休假申请表';

-- 20. audit_logs (审计日志表)
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `actor_id` INT NOT NULL COMMENT '操作者ID',
  `actor_type` ENUM('patient','doctor','admin') NOT NULL COMMENT '操作者类型',
  `action` VARCHAR(255) NOT NULL COMMENT '操作行为描述',
  `target_entity` VARCHAR(100) NOT NULL COMMENT '被操作对象所在的表名',
  `target_id` INT NOT NULL COMMENT '被操作对象的ID',
  `details` TEXT COMMENT '操作详细信息',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 六、地图导航模型

-- 21. map_nodes (地图节点表)
DROP TABLE IF EXISTS `map_nodes`;
CREATE TABLE `map_nodes` (
  `node_id` INT NOT NULL AUTO_INCREMENT,
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` ENUM('room','hallway','elevator','stairs','entrance') NOT NULL COMMENT '节点类型',
  `coordinates_x` DECIMAL(10,2) NOT NULL COMMENT 'X坐标',
  `coordinates_y` DECIMAL(10,2) NOT NULL COMMENT 'Y坐标',
  `floor_level` INT NOT NULL COMMENT '楼层',
  `description` TEXT COMMENT '节点描述',
  `is_accessible` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否无障碍通行',
  PRIMARY KEY (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图节点表';

-- 22. map_edges (地图路径表)
DROP TABLE IF EXISTS `map_edges`;
CREATE TABLE `map_edges` (
  `edge_id` INT NOT NULL AUTO_INCREMENT,
  `start_node_id` INT NOT NULL COMMENT '起始节点',
  `end_node_id` INT NOT NULL COMMENT '结束节点',
  `distance` DECIMAL(10,2) NOT NULL COMMENT '距离(米)',
  `walk_time` INT NOT NULL COMMENT '步行时间(秒)',
  `is_bidirectional` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否双向通行',
  `accessibility_info` TEXT COMMENT '无障碍设施信息',
  PRIMARY KEY (`edge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图路径表';

-- 添加外键约束
ALTER TABLE `doctors` ADD CONSTRAINT `fk_doctors_department` 
  FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `departments` ADD CONSTRAINT `fk_departments_parent` 
  FOREIGN KEY (`parent_id`) REFERENCES `parent_departments` (`parent_department_id`);

ALTER TABLE `locations` ADD CONSTRAINT `fk_locations_department` 
  FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `locations` ADD CONSTRAINT `fk_locations_map_node` 
  FOREIGN KEY (`map_node_id`) REFERENCES `map_nodes` (`node_id`);

ALTER TABLE `schedules` ADD CONSTRAINT `fk_schedules_doctor` 
  FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`);

ALTER TABLE `schedules` ADD CONSTRAINT `fk_schedules_slot` 
  FOREIGN KEY (`slot_id`) REFERENCES `time_slots` (`slot_id`);

ALTER TABLE `schedules` ADD CONSTRAINT `fk_schedules_location` 
  FOREIGN KEY (`location_id`) REFERENCES `locations` (`location_id`);

ALTER TABLE `patient_profiles` ADD CONSTRAINT `fk_patient_profiles_patient` 
  FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`);

ALTER TABLE `appointments` ADD CONSTRAINT `fk_appointments_patient` 
  FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`);

ALTER TABLE `appointments` ADD CONSTRAINT `fk_appointments_schedule` 
  FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`schedule_id`);

ALTER TABLE `waitlist` ADD CONSTRAINT `fk_waitlist_patient` 
  FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`);

ALTER TABLE `waitlist` ADD CONSTRAINT `fk_waitlist_schedule` 
  FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`schedule_id`);

ALTER TABLE `symptom_department_mapping` ADD CONSTRAINT `fk_symptom_mapping_department` 
  FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `medical_guidelines` ADD CONSTRAINT `fk_guidelines_created_by` 
  FOREIGN KEY (`created_by`) REFERENCES `admins` (`admin_id`);

ALTER TABLE `role_permissions` ADD CONSTRAINT `fk_role_permissions_role` 
  FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);

ALTER TABLE `role_permissions` ADD CONSTRAINT `fk_role_permissions_permission` 
  FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`);

ALTER TABLE `admin_roles` ADD CONSTRAINT `fk_admin_roles_admin` 
  FOREIGN KEY (`admin_id`) REFERENCES `admins` (`admin_id`);

ALTER TABLE `admin_roles` ADD CONSTRAINT `fk_admin_roles_role` 
  FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);

ALTER TABLE `leave_requests` ADD CONSTRAINT `fk_leave_requests_doctor` 
  FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`);

ALTER TABLE `leave_requests` ADD CONSTRAINT `fk_leave_requests_approver` 
  FOREIGN KEY (`approver_id`) REFERENCES `admins` (`admin_id`);

ALTER TABLE `map_edges` ADD CONSTRAINT `fk_edges_start_node` 
  FOREIGN KEY (`start_node_id`) REFERENCES `map_nodes` (`node_id`);

ALTER TABLE `map_edges` ADD CONSTRAINT `fk_edges_end_node` 
  FOREIGN KEY (`end_node_id`) REFERENCES `map_nodes` (`node_id`);

-- =============================================
-- 增强初始数据插入
-- =============================================

-- 插入父科室数据
INSERT IGNORE INTO `parent_departments` (`parent_department_id`, `name`, `description`) VALUES
(1, '内科', '负责内科疾病诊断和治疗'),
(2, '外科', '负责外科手术和创伤处理'),
(3, '专科', '各类专科门诊'),
(4, '医技科室', '辅助检查和治疗科室'),
(5, '行政科室', '医院行政管理科室'),
(999, '未分配父科室', '用于存放未分配到具体父科室的医生');

-- 插入子科室数据
INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(1, 1, '呼吸内科', '呼吸道疾病诊断治疗'),
(2, 1, '心血管内科', '心脏和血管疾病治疗'),
(3, 1, '消化内科', '消化道疾病诊断治疗'),
(4, 1, '神经内科', '神经系统疾病治疗'),
(5, 2, '普外科', '普通外科手术'),
(6, 2, '骨科', '骨骼疾病和创伤治疗'),
(7, 2, '泌尿外科', '泌尿系统疾病治疗'),
(8, 3, '眼科', '眼部疾病诊断治疗'),
(9, 3, '耳鼻喉科', '耳鼻喉相关疾病治疗'),
(10, 3, '口腔科', '口腔疾病诊断治疗'),
(11, 3, '皮肤科', '皮肤疾病诊断治疗'),
(12, 3, '妇科', '女性生殖系统疾病治疗'),
(13, 4, '检验科', '临床检验分析'),
(14, 4, '放射科', '医学影像检查'),
(15, 4, '药房', '药品管理和发放'),
(16, 5, '医务科', '医疗事务管理'),
(17, 5, '财务科', '医院财务管理'),
(999, 999, '未分配科室', '用于存放未分配到具体科室的医生');

-- 插入时间段数据
INSERT IGNORE INTO `time_slots` (`slot_id`, `slot_name`, `start_time`, `end_time`) VALUES
(1, '上午 08:00-12:00', '08:00:00', '12:00:00'),
(2, '上午 08:30-12:00', '08:30:00', '12:00:00'),
(3, '下午 14:00-18:00', '14:00:00', '18:00:00'),
(4, '下午 14:30-18:00', '14:30:00', '18:00:00');

-- 插入诊室数据
INSERT IGNORE INTO `locations` (`location_id`, `location_name`, `department_id`, `floor_level`, `building`, `room_number`, `capacity`, `map_node_id`) VALUES
(1, '门诊楼201室', 1, 2, '门诊楼', '201', 15, 5),
(2, '门诊楼202室', 1, 2, '门诊楼', '202', 15, 5),
(3, '门诊楼301室', 2, 3, '门诊楼', '301', 12, 5),
(4, '门诊楼302室', 2, 3, '门诊楼', '302', 12, 5),
(5, '门诊楼203室', 3, 2, '门诊楼', '203', 15, 5),
(6, '门诊楼303室', 4, 3, '门诊楼', '303', 15, 5),
(7, '外科楼101室', 5, 1, '外科楼', '101', 12, 6),
(8, '外科楼102室', 6, 1, '外科楼', '102', 12, 6),
(9, '外科楼103室', 7, 1, '外科楼', '103', 10, 6),
(10, '专科楼201室', 8, 2, '专科楼', '201', 20, 7),
(11, '专科楼202室', 9, 2, '专科楼', '202', 15, 7),
(12, '专科楼203室', 10, 2, '专科楼', '203', 15, 7),
(13, '专科楼204室', 11, 2, '专科楼', '204', 15, 7),
(14, '专科楼205室', 12, 2, '专科楼', '205', 12, 7);

-- 插入管理员数据
INSERT IGNORE INTO `admins` (`admin_id`, `username`, `password_hash`, `full_name`, `status`) VALUES
(1, 'admin', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '系统管理员', 'active'),
(2, 'zhangwei', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '张伟', 'active'),
(3, 'liumei', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '刘梅', 'active'),
(4, 'wanggang', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '王刚', 'active');

-- 插入医生数据
INSERT IGNORE INTO `doctors` (`doctor_id`, `department_id`, `identifier`, `password_hash`, `full_name`, `id_card_number`, `phone_number`, `title`, `specialty`, `bio`, `photo_url`, `status`) VALUES
(1, 1, 'D1001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '李明', '110101198001010011', '+8613810010001', '主任医师', '慢性阻塞性肺疾病、支气管哮喘', '从事呼吸内科临床工作20年，经验丰富', '/images/doctors/liming.jpg', 'active'),
(2, 1, 'D1002', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '王小红', '110101198102020022', '+8613810010002', '副主任医师', '肺炎、肺结核', '医学博士，擅长呼吸系统感染性疾病诊治', '/images/doctors/wangxiaohong.jpg', 'active'),
(3, 2, 'D2001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '张建国', '110101197503030033', '+8613810010003', '主任医师', '冠心病、高血压', '心血管内科专家，完成心脏介入手术千余例', '/images/doctors/zhangjianguo.jpg', 'active'),
(4, 2, 'D2002', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '陈静', '110101198204040044', '+8613810010004', '主治医师', '心律失常、心力衰竭', '擅长心血管疾病药物治疗和康复指导', '/images/doctors/chenjing.jpg', 'active'),
(5, 3, 'D3001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '刘强', '110101197805050055', '+8613810010005', '副主任医师', '胃炎、消化性溃疡', '消化内镜专家，擅长胃肠道疾病内镜诊断', '/images/doctors/liuqiang.jpg', 'active'),
(6, 4, 'D4001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '赵敏', '110101198506060066', '+8613810010006', '主治医师', '头痛、脑血管疾病', '神经内科专业，擅长头痛和脑血管病诊治', '/images/doctors/zhaomin.jpg', 'active'),
(7, 5, 'D5001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '周伟', '110101197907070077', '+8613810010007', '主任医师', '甲状腺疾病、乳腺疾病', '普外科手术专家，完成各类手术数千例', '/images/doctors/zhouwei.jpg', 'active'),
(8, 6, 'D6001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '孙志强', '110101198108080088', '+8613810010008', '副主任医师', '骨折、关节损伤', '骨科创伤专家，擅长骨折微创手术治疗', '/images/doctors/sunzhiqiang.jpg', 'active'),
(9, 8, 'D8001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '杨光', '110101198309090099', '+8613810010009', '主治医师', '白内障、青光眼', '眼科专业，擅长眼部疾病诊断和手术治疗', '/images/doctors/yangguang.jpg', 'active'),
(10, 9, 'D9001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '吴晓丽', '110101198410100010', '+8613810010010', '副主任医师', '中耳炎、鼻炎', '耳鼻喉科专家，擅长耳鼻喉疾病微创治疗', '/images/doctors/wuxiaoli.jpg', 'active');

-- 插入患者数据
INSERT IGNORE INTO `patients` (`patient_id`, `identifier`, `patient_type`, `password_hash`, `full_name`, `phone_number`, `status`) VALUES
(1, '2021001001', 'student', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '张三', '+8613810011001', 'active'),
(2, '2021001002', 'student', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '李四', '+8613810011002', 'active'),
(3, '2021001003', 'student', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '王五', '+8613810011003', 'active'),
(4, 'T2021001', 'teacher', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '赵教授', '+8613810011004', 'active'),
(5, 'T2021002', 'teacher', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '钱老师', '+8613810011005', 'active'),
(6, 'S2021001', 'staff', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '孙师傅', '+8613810011006', 'active'),
(7, '2021001004', 'student', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '周小华', '+8613810011007', 'active'),
(8, '2021001005', 'student', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '吴小明', '+8613810011008', 'active'),
(9, 'T2021003', 'teacher', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '郑教授', '+8613810011009', 'active'),
(10, 'S2021002', 'staff', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '王阿姨', '+8613810011010', 'active');

-- 插入患者扩展信息
INSERT IGNORE INTO `patient_profiles` (`patient_id`, `id_card_number`, `allergies`, `medical_history`, `no_show_count`, `blacklist_status`) VALUES
(1, '110101200001010011', '青霉素过敏', '无', 0, 'normal'),
(2, '110101200002020022', '无', '季节性哮喘', 1, 'normal'),
(3, '110101200003030033', '海鲜过敏', '胃炎', 0, 'normal'),
(4, '110101197001010044', '无', '高血压', 0, 'normal'),
(5, '110101197502020055', '头孢过敏', '糖尿病', 2, 'normal'),
(6, '110101196503030066', '无', '关节炎', 0, 'normal'),
(7, '110101200004040077', '花粉过敏', '无', 0, 'normal'),
(8, '110101200005050088', '无', '过敏性鼻炎', 1, 'normal'),
(9, '110101197803030099', '无', '高血脂', 0, 'normal'),
(10, '110101196904040000', '磺胺类药物过敏', '骨质疏松', 0, 'normal');

-- 插入角色数据
INSERT IGNORE INTO `roles` (`role_id`, `role_name`, `description`) VALUES
(1, 'super_admin', '超级管理员，拥有所有权限'),
(2, 'medical_admin', '医务管理员，管理医生和排班'),
(3, 'financial_admin', '财务管理员，管理收费和报表'),
(4, 'patient_admin', '患者管理员，管理患者信息'),
(5, 'system_operator', '系统操作员，日常运维');

-- 插入权限数据
INSERT IGNORE INTO `permissions` (`permission_id`, `permission_name`, `description`) VALUES
(1, 'user_manage', '用户管理权限'),
(2, 'doctor_manage', '医生管理权限'),
(3, 'patient_manage', '患者管理权限'),
(4, 'schedule_manage', '排班管理权限'),
(5, 'appointment_manage', '预约管理权限'),
(6, 'financial_manage', '财务管理权限'),
(7, 'medical_guide_manage', '就医规范管理权限'),
(8, 'system_config', '系统配置权限'),
(9, 'audit_log_view', '审计日志查看权限'),
(10, 'report_generate', '报表生成权限');

-- 插入角色权限映射
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(2, 2), (2, 4), (2, 5), (2, 9),
(3, 6), (3, 10),
(4, 3), (4, 5), (4, 9),
(5, 4), (5, 5), (5, 9);

-- 插入管理员角色映射
INSERT IGNORE INTO `admin_roles` (`admin_id`, `role_id`) VALUES
(1, 1),  -- 系统管理员拥有超级管理员权限
(2, 2),  -- 张伟拥有医务管理员权限
(2, 4),  -- 张伟同时拥有患者管理员权限
(3, 3),  -- 刘梅拥有财务管理员权限
(4, 5);  -- 王刚拥有系统操作员权限

-- 插入排班数据 (未来一周的排班)
INSERT IGNORE INTO `schedules` (`schedule_id`, `doctor_id`, `schedule_date`, `slot_id`, `location_id`, `total_slots`, `booked_slots`, `fee`, `status`, `remarks`) VALUES
-- 李明医生 (呼吸内科)
(1, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, 10, 3, 10.00, 'available', '专家门诊'),
(2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 1, 10, 2, 10.00, 'available', '专家门诊'),
(3, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, 1, 10, 0, 10.00, 'available', '专家门诊'),

-- 王小红医生 (呼吸内科)
(4, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 3, 2, 15, 5, 8.00, 'available', '普通门诊'),
(5, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, 2, 15, 1, 8.00, 'available', '普通门诊'),

-- 张建国医生 (心血管内科)
(6, 3, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 3, 8, 6, 15.00, 'available', '专家门诊，限号'),
(7, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 1, 3, 8, 2, 15.00, 'available', '专家门诊，限号'),

-- 陈静医生 (心血管内科)
(8, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 4, 4, 12, 4, 8.00, 'available', '普通门诊'),
(9, 4, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 3, 4, 12, 3, 8.00, 'available', '普通门诊'),

-- 刘强医生 (消化内科)
(10, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, 5, 10, 2, 10.00, 'available', '专家门诊'),

-- 赵敏医生 (神经内科)
(11, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 5, 6, 10, 1, 8.00, 'available', '普通门诊'),

-- 周伟医生 (普外科)
(12, 7, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, 7, 10, 7, 12.00, 'available', '专家门诊'),

-- 孙志强医生 (骨科)
(13, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 3, 8, 10, 4, 12.00, 'available', '专家门诊'),

-- 杨光医生 (眼科)
(14, 9, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 6, 10, 15, 8, 8.00, 'available', '普通门诊'),

-- 吴晓丽医生 (耳鼻喉科)
(15, 10, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 2, 11, 12, 3, 8.00, 'available', '普通门诊');

-- 插入预约数据
INSERT IGNORE INTO `appointments` (`appointment_id`, `patient_id`, `schedule_id`, `appointment_number`, `status`, `payment_status`, `payment_method`, `transaction_id`, `check_in_time`) VALUES
(1, 1, 1, 1, 'scheduled', 'paid', 'wechat', 'WX202312010001', NULL),
(2, 2, 1, 2, 'scheduled', 'paid', 'alipay', 'AL202312010002', NULL),
(3, 3, 1, 3, 'scheduled', 'unpaid', NULL, NULL, NULL),
(4, 4, 6, 1, 'scheduled', 'paid', 'wechat', 'WX202312020001', NULL),
(5, 5, 6, 2, 'scheduled', 'paid', 'alipay', 'AL202312020002', NULL),
(6, 6, 12, 1, 'scheduled', 'paid', 'cash', NULL, NULL),
(7, 7, 14, 1, 'completed', 'paid', 'wechat', 'WX202311280001', '2023-11-28 08:15:00'),
(8, 8, 14, 2, 'completed', 'paid', 'alipay', 'AL202311280002', '2023-11-28 08:45:00'),
(9, 9, 4, 1, 'cancelled', 'refunded', 'wechat', 'WX202311290001', NULL),
(10, 10, 8, 1, 'scheduled', 'paid', 'alipay', 'AL202312010003', NULL);

-- 插入症状-科室映射数据
INSERT IGNORE INTO `symptom_department_mapping` (`mapping_id`, `symptom_keywords`, `department_id`, `priority`) VALUES
(1, '咳嗽,咳痰,气喘,呼吸困难,胸闷', 1, 1),
(2, '心悸,心慌,胸痛,血压高,头晕', 2, 1),
(3, '胃痛,腹痛,腹泻,恶心,呕吐,消化不良', 3, 1),
(4, '头痛,头晕,失眠,手脚麻木,记忆力减退', 4, 1),
(5, '骨折,扭伤,关节痛,腰腿痛,肿胀', 6, 1),
(6, '视力模糊,眼痛,眼红,流泪,眼干', 8, 1),
(7, '耳鸣,听力下降,鼻塞,流鼻涕,咽喉痛', 9, 1),
(8, '牙痛,牙龈出血,口腔溃疡,牙齿松动', 10, 1),
(9, '皮疹,瘙痒,皮肤红肿,脱皮,痘痘', 11, 1),
(10, '尿频,尿急,尿痛,血尿,腰酸', 7, 1),
(11, '发热,感冒,流鼻涕,喉咙痛,全身酸痛', 1, 2),
(12, '胸闷,气短,呼吸困难,心绞痛', 2, 1),
(13, '腹痛,腹胀,便秘,便血,食欲不振', 3, 1),
(14, '头晕,眩晕,耳鸣,听力下降,平衡障碍', 4, 1);

-- 插入就医规范数据
INSERT IGNORE INTO `medical_guidelines` (`guideline_id`, `title`, `content`, `category`, `status`, `created_by`) VALUES
(1, '门诊就诊流程规范', '1. 预约挂号 → 2. 候诊区等待叫号 → 3. 医生诊室就诊 → 4. 缴费 → 5. 检查/取药 → 6. 离院', '就诊流程', 'active', 1),
(2, '急诊就诊须知', '急诊患者优先就诊，按病情危重程度分诊。生命危险患者立即进入抢救室。', '急诊流程', 'active', 1),
(3, '药品领取规范', '凭处方和缴费单到药房取药，核对药品信息，了解用药方法和注意事项。', '药品管理', 'active', 1),
(4, '检查注意事项', '空腹检查前8小时禁食，B超检查需憋尿，CT检查需去除金属物品。', '检查规范', 'active', 1),
(5, '医保报销流程', '持医保卡和身份证就诊，费用实时结算。特殊项目需提前审批。', '医保政策', 'active', 1),
(6, '住院办理流程', '医生开具住院证 → 住院处办理手续 → 缴纳押金 → 入住病房', '住院流程', 'active', 1),
(7, '转诊转院规范', '需经主治医生评估，填写转诊单，医务科审批，接收医院同意。', '转诊规范', 'active', 1);

-- 插入通知消息示例数据
INSERT IGNORE INTO `notifications` (`notification_id`, `user_id`, `user_type`, `type`, `title`, `content`, `related_entity`, `related_id`, `status`, `priority`, `sent_at`, `read_at`) VALUES
(1, 1, 'patient', 'appointment_reminder', '就诊提醒', '您预约的李明医生门诊将于明天上午08:00-08:30开始，请提前15分钟到达门诊楼201室候诊。', 'appointment', 1, 'read', 'high', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(2, 1, 'patient', 'appointment_success', '预约成功通知', '您已成功预约呼吸内科李明医生，就诊时间：明天上午08:00-08:30，请按时就诊。', 'appointment', 1, 'read', 'normal', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 2, 'patient', 'appointment_reminder', '就诊提醒', '您预约的李明医生门诊将于明天上午08:30-09:00开始，地点：门诊楼201室。', 'appointment', 2, 'unread', 'high', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(4, 4, 'patient', 'waitlist_available', '候补号源通知', '您候补的张建国医生号源已释放，请在2小时内登录系统完成预约，逾期将自动取消。', 'schedule', 6, 'read', 'urgent', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 1, 'doctor', 'schedule_change', '排班变更通知', '您明天上午的排班已由管理员调整，请登录系统查看最新排班信息。', 'schedule', 1, 'read', 'high', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(6, 7, 'patient', 'cancellation', '预约取消通知', '您预约的王小红医生门诊已取消，挂号费已原路退回。如需就诊请重新预约。', 'appointment', 9, 'read', 'high', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 1, 'patient', 'system_notice', '系统维护通知', '系统将于本周六凌晨2:00-6:00进行维护升级，期间无法使用预约功能，请提前安排。', NULL, NULL, 'unread', 'normal', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL),
(8, 3, 'patient', 'appointment_reminder', '明日就诊提醒', '温馨提示：您明天有一个预约，请携带身份证和就诊卡按时就诊。', 'appointment', 3, 'unread', 'normal', NOW(), NULL),
(9, 2, 'admin', 'system_notice', '待审批提醒', '有2条医生休假申请待您审批，请及时处理。', 'leave_requests', NULL, 'unread', 'normal', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL),
(10, 5, 'patient', 'appointment_success', '预约成功', '您已成功预约心血管内科张建国医生的专家号，挂号费15元已支付。', 'appointment', 5, 'read', 'normal', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 插入地图节点数据
INSERT IGNORE INTO `map_nodes` (`node_id`, `node_name`, `node_type`, `coordinates_x`, `coordinates_y`, `floor_level`, `description`, `is_accessible`) VALUES
(1, '医院正门', 'entrance', 0, 0, 1, '医院主要出入口', TRUE),
(2, '门诊大厅', 'room', 50, 30, 1, '门诊综合服务大厅', TRUE),
(3, '挂号收费处', 'room', 60, 40, 1, '挂号、缴费窗口', TRUE),
(4, '药房', 'room', 70, 50, 1, '西药房、中药房', TRUE),
(5, '内科诊区', 'room', 100, 30, 2, '内科各科室诊室', TRUE),
(6, '外科诊区', 'room', 150, 30, 2, '外科各科室诊室', TRUE),
(7, '专科诊区', 'room', 200, 30, 2, '专科门诊区域', TRUE),
(8, '检验科', 'room', 80, 80, 1, '抽血、化验室', TRUE),
(9, '放射科', 'room', 120, 80, 1, 'X光、CT检查室', TRUE),
(10, '急诊科', 'room', 30, 100, 1, '急诊抢救室', TRUE),
(11, '楼梯1', 'stairs', 40, 20, 1, '1-2楼楼梯', TRUE),
(12, '电梯1', 'elevator', 90, 20, 1, '1-3楼电梯', TRUE),
(13, '走廊1', 'hallway', 75, 25, 1, '主走廊', TRUE),
(14, '走廊2', 'hallway', 125, 25, 2, '二楼主走廊', TRUE);

-- 插入地图路径数据
INSERT IGNORE INTO `map_edges` (`edge_id`, `start_node_id`, `end_node_id`, `distance`, `walk_time`, `is_bidirectional`, `accessibility_info`) VALUES
(1, 1, 2, 50, 60, TRUE, '无障碍通道'),
(2, 2, 3, 15, 20, TRUE, '平坦地面'),
(3, 2, 4, 25, 30, TRUE, '平坦地面'),
(4, 2, 11, 40, 50, TRUE, '有缓坡'),
(5, 11, 5, 60, 70, TRUE, '楼梯，有无障碍电梯替代'),
(6, 2, 12, 40, 50, TRUE, '无障碍通道'),
(7, 12, 5, 10, 15, TRUE, '平坦地面'),
(8, 5, 6, 50, 60, TRUE, '走廊'),
(9, 6, 7, 50, 60, TRUE, '走廊'),
(10, 2, 8, 55, 65, TRUE, '平坦地面'),
(11, 2, 9, 85, 100, TRUE, '平坦地面'),
(12, 2, 10, 70, 85, TRUE, '无障碍通道'),
(13, 5, 14, 5, 10, TRUE, '走廊入口'),
(14, 14, 6, 50, 60, TRUE, '二楼主走廊'),
(15, 14, 7, 50, 60, TRUE, '二楼主走廊');

-- 插入审计日志示例数据
INSERT IGNORE INTO `audit_logs` (`log_id`, `actor_id`, `actor_type`, `action`, `target_entity`, `target_id`, `details`, `created_at`) VALUES
(1, 1, 'admin', '用户登录', 'admins', 1, 'IP: 192.168.1.100', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 1, 'admin', '创建医生账户', 'doctors', 5, '医生姓名：刘强', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, 'admin', '修改排班', 'schedules', 4, '调整号源数量从12到15', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, 1, 'patient', '预约挂号', 'appointments', 1, '预约呼吸内科李明医生', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(5, 3, 'doctor', '更新患者病历', 'patient_profiles', 2, '更新过敏史信息', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 验证"未分配科室"记录插入结果
SELECT 'parent_departments' as table_name, parent_department_id, name FROM parent_departments WHERE parent_department_id = 999
UNION ALL
SELECT 'departments' as table_name, department_id, name FROM departments WHERE department_id = 999;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 创建触发器
-- =====================================================

-- 创建触发器：当 appointments.status 更新为 'completed' 时，自动更新 updated_at
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

COMMIT;