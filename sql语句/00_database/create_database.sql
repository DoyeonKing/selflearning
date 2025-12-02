-- 创建医院管理系统数据库
-- 如果数据库已存在则删除（谨慎使用）
-- DROP DATABASE IF EXISTS `hospital_db`;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `hospital_db` 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `hospital_db`;

-- 显示创建成功信息
SELECT 'Database hospital_db created successfully!' AS message;

