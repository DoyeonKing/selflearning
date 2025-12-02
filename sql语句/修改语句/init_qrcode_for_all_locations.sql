-- ============================================
-- 为所有诊室生成二维码数据
-- 包括14个诊室和其他关键地点
-- ============================================

-- 步骤1: 确保所有节点都有二维码内容
UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = CASE 
        WHEN `qrcode_status` IS NULL THEN 'PENDING'
        ELSE `qrcode_status`
    END,
    `qrcode_generated_at` = CASE 
        WHEN `qrcode_generated_at` IS NULL THEN NOW()
        ELSE `qrcode_generated_at`
    END
WHERE `qrcode_content` IS NULL OR `qrcode_content` = '';

-- 步骤2: 查看所有诊室及其对应的二维码信息
SELECT 
    l.`location_id` as '诊室ID',
    l.`location_name` as '诊室名称',
    l.`building` as '楼栋',
    l.`room_number` as '房间号',
    l.`floor_level` as '楼层',
    n.`node_id` as '节点ID',
    n.`node_name` as '节点名称',
    n.`qrcode_content` as '二维码内容',
    n.`qrcode_status` as '状态',
    CONCAT('节点', n.`node_id`, ': ', l.`location_name`, ' -> ', n.`qrcode_content`) as '二维码生成信息'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
ORDER BY l.`location_id`;

-- 步骤3: 查看所有节点（包括非诊室的关键地点）
SELECT 
    n.`node_id` as '节点ID',
    n.`node_name` as '节点名称',
    n.`coordinates_x` as 'X坐标',
    n.`coordinates_y` as 'Y坐标',
    n.`floor_level` as '楼层',
    n.`qrcode_content` as '二维码内容',
    n.`qrcode_status` as '状态',
    CASE 
        WHEN EXISTS (SELECT 1 FROM `locations` WHERE `map_node_id` = n.`node_id`) 
        THEN '是'
        ELSE '否'
    END as '是否关联诊室',
    CONCAT('节点', n.`node_id`, ': ', n.`node_name`, ' -> ', n.`qrcode_content`) as '二维码生成信息'
FROM `map_nodes` n
ORDER BY n.`node_id`;

-- 步骤4: 生成完整的二维码内容列表（用于批量生成二维码）
SELECT 
    CONCAT('HOSPITAL_NODE_', `node_id`) as '二维码内容',
    `node_name` as '节点名称',
    CONCAT('节点ID: ', `node_id`, ' | 名称: ', `node_name`, ' | 二维码: HOSPITAL_NODE_', `node_id`) as '详细信息'
FROM `map_nodes`
ORDER BY `node_id`;

-- 步骤5: 按诊室分组显示二维码信息
SELECT 
    l.`location_name` as '诊室名称',
    l.`building` as '楼栋',
    l.`room_number` as '房间号',
    n.`qrcode_content` as '二维码内容',
    CONCAT('诊室: ', l.`location_name`, ' -> 二维码: ', n.`qrcode_content`) as '二维码生成信息'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY l.`building`, l.`room_number`;








