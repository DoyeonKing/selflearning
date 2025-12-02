-- 为 leave_requests 表添加请假证明文件字段
-- 执行日期: 2025-11-25
-- 用途: 支持医生上传请假证明文件（图片或PDF）

USE hospital_db;

-- 添加请假证明文件URL字段
ALTER TABLE `leave_requests` 
ADD COLUMN `proof_document_url` VARCHAR(500) NULL COMMENT '请假证明文件URL（图片或PDF）' AFTER `reason`;

-- 验证字段是否添加成功
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'hospital_db' 
  AND TABLE_NAME = 'leave_requests' 
  AND COLUMN_NAME = 'proof_document_url';
