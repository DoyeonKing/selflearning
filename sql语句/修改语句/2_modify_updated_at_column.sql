-- 修改 updated_at 字段定义，移除 ON UPDATE CURRENT_TIMESTAMP
-- 因为我们要用触发器精确控制 completed 时的更新时间
-- 如果字段已经有 ON UPDATE CURRENT_TIMESTAMP，执行此 SQL 移除它

ALTER TABLE `appointments` 
MODIFY COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后更新时间（status变为completed时由触发器更新）';

