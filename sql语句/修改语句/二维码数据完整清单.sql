-- ============================================
-- 二维码数据完整清单
-- 包含所有节点和诊室的二维码信息
-- ============================================

-- 一、所有节点列表（包括关键地点和诊室节点）
SELECT 
    '节点' as '类型',
    CAST(n.`node_id` AS CHAR) as 'ID',
    n.`node_name` as '名称',
    CONCAT(n.`coordinates_x`, ', ', n.`coordinates_y`) as '坐标',
    CAST(n.`floor_level` AS CHAR) as '楼层',
    n.`qrcode_content` as '二维码内容',
    n.`qrcode_status` as '状态'
FROM `map_nodes` n
UNION ALL
SELECT 
    '诊室' as '类型',
    CAST(l.`location_id` AS CHAR) as 'ID',
    l.`location_name` as '名称',
    CONCAT('节点', l.`map_node_id`) as '坐标',
    CAST(l.`floor_level` AS CHAR) as '楼层',
    n.`qrcode_content` as '二维码内容',
    n.`qrcode_status` as '状态'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY `类型`, `ID`;

-- 二、诊室二维码清单（按楼栋分组）
SELECT 
    l.`building` as '楼栋',
    l.`location_name` as '诊室名称',
    l.`room_number` as '房间号',
    l.`floor_level` as '楼层',
    n.`node_id` as '节点ID',
    n.`qrcode_content` as '二维码内容',
    CONCAT('HOSPITAL_NODE_', n.`node_id`) as '用于生成二维码'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY l.`building`, l.`floor_level`, l.`room_number`;

-- 三、关键地点二维码清单（非诊室节点）
SELECT 
    n.`node_id` as '节点ID',
    n.`node_name` as '地点名称',
    n.`floor_level` as '楼层',
    n.`qrcode_content` as '二维码内容',
    CONCAT('HOSPITAL_NODE_', n.`node_id`) as '用于生成二维码'
FROM `map_nodes` n
WHERE NOT EXISTS (
    SELECT 1 FROM `locations` WHERE `map_node_id` = n.`node_id`
)
ORDER BY n.`node_id`;

-- 四、所有需要生成二维码的地点汇总
SELECT 
    CONCAT('HOSPITAL_NODE_', `node_id`) as '二维码内容',
    `node_name` as '地点名称',
    `floor_level` as '楼层'
FROM `map_nodes`
ORDER BY `node_id`;








