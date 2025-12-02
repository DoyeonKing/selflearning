-- 为地图节点表添加二维码相关字段
-- 用于存储二维码图片路径和二维码内容

ALTER TABLE `map_nodes` 
ADD COLUMN `qrcode_content` VARCHAR(255) NULL COMMENT '二维码内容（格式：HOSPITAL_NODE_{nodeId}）' AFTER `is_accessible`,
ADD COLUMN `qrcode_image_path` VARCHAR(500) NULL COMMENT '二维码图片存储路径' AFTER `qrcode_content`,
ADD COLUMN `qrcode_generated_at` TIMESTAMP NULL COMMENT '二维码生成时间' AFTER `qrcode_image_path`,
ADD COLUMN `qrcode_status` ENUM('active','inactive','pending') NOT NULL DEFAULT 'pending' COMMENT '二维码状态：active=已激活，inactive=已停用，pending=待生成' AFTER `qrcode_generated_at`;

-- 为现有节点生成默认二维码内容
UPDATE `map_nodes` 
SET `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`)
WHERE `qrcode_content` IS NULL;

-- 创建索引以提高查询性能
CREATE INDEX `idx_qrcode_content` ON `map_nodes` (`qrcode_content`);
CREATE INDEX `idx_qrcode_status` ON `map_nodes` (`qrcode_status`);

-- 添加注释说明
ALTER TABLE `map_nodes` 
MODIFY COLUMN `qrcode_content` VARCHAR(255) NULL COMMENT '二维码内容（格式：HOSPITAL_NODE_{nodeId}），用于扫码定位';








