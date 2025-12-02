-- ============================================
-- 二维码数据初始化脚本
-- 为所有地图节点和诊室生成二维码内容
-- ============================================

-- 1. 首先确保map_nodes表有二维码字段（如果还没有执行add_qrcode_to_map_nodes.sql）
-- 如果已经执行过，可以忽略字段已存在的错误

-- 2. 为所有现有节点生成二维码内容
UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = 'PENDING',
    `qrcode_generated_at` = NOW()
WHERE `qrcode_content` IS NULL OR `qrcode_content` = '';

-- 3. 查看所有节点的二维码信息
SELECT 
    `node_id`,
    `node_name`,
    `coordinates_x`,
    `coordinates_y`,
    `floor_level`,
    `qrcode_content`,
    `qrcode_status`,
    `qrcode_generated_at`
FROM `map_nodes`
ORDER BY `node_id`;

-- 4. 查看诊室和对应的节点信息
SELECT 
    l.`location_id`,
    l.`location_name`,
    l.`building`,
    l.`room_number`,
    l.`floor_level` as location_floor,
    l.`map_node_id`,
    n.`node_name` as map_node_name,
    n.`qrcode_content`,
    n.`qrcode_status`
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
ORDER BY l.`location_id`;

-- 5. 如果需要为关键节点（如医院大门、分诊台等）设置状态为ACTIVE，可以执行：
-- UPDATE `map_nodes` 
-- SET `qrcode_status` = 'ACTIVE' 
-- WHERE `node_id` IN (
--     SELECT `node_id` FROM `map_nodes` 
--     WHERE `node_name` LIKE '%大门%' 
--        OR `node_name` LIKE '%分诊%'
--        OR `node_name` LIKE '%电梯%'
-- );

-- 6. 生成二维码内容列表（用于生成二维码图片）
SELECT 
    CONCAT('节点ID: ', `node_id`, ' | 名称: ', `node_name`, ' | 二维码内容: ', `qrcode_content`) as qrcode_info
FROM `map_nodes`
ORDER BY `node_id`;








