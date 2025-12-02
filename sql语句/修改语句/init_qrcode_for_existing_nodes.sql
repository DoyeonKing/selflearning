-- 为现有节点初始化二维码内容
-- 执行此脚本后，所有现有节点都会有默认的二维码内容

UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = 'PENDING',
    `qrcode_generated_at` = NOW()
WHERE `qrcode_content` IS NULL;

-- 查看更新结果
SELECT 
    `node_id`,
    `node_name`,
    `qrcode_content`,
    `qrcode_status`,
    `qrcode_generated_at`
FROM `map_nodes`
ORDER BY `node_id`;








