-- 为所有地图节点初始化二维码数据
-- 包括诊室和关键地点（医院大门、分诊台、电梯口等）

-- 首先确保map_nodes表有二维码字段（如果还没有执行add_qrcode_to_map_nodes.sql）
-- 如果已经执行过，可以忽略字段已存在的错误

-- 为所有现有节点生成二维码内容
UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = 'PENDING',
    `qrcode_generated_at` = NOW()
WHERE `qrcode_content` IS NULL OR `qrcode_content` = '';

-- 查看更新结果
SELECT 
    `node_id`,
    `node_name`,
    `qrcode_content`,
    `qrcode_status`,
    `qrcode_generated_at`
FROM `map_nodes`
ORDER BY `node_id`;

-- 如果需要为特定节点设置状态为ACTIVE（已有二维码图片的），可以执行：
-- UPDATE `map_nodes` SET `qrcode_status` = 'ACTIVE' WHERE `node_id` IN (1, 2, 3, 4, 5);








