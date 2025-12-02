-- ============================================
-- 生成所有二维码的完整清单
-- 包含所有节点和诊室的二维码信息
-- ============================================

-- 一、所有节点二维码清单（14个节点）
SELECT 
    CONCAT('节点', n.`node_id`) as '编号',
    n.`node_name` as '地点名称',
    CONCAT('HOSPITAL_NODE_', n.`node_id`) as '二维码内容',
    CASE 
        WHEN EXISTS (SELECT 1 FROM `locations` WHERE `map_node_id` = n.`node_id`) 
        THEN CONCAT('是（', 
            (SELECT COUNT(*) FROM `locations` WHERE `map_node_id` = n.`node_id`), 
            '个诊室）')
        ELSE '否'
    END as '是否关联诊室',
    n.`qrcode_status` as '状态'
FROM `map_nodes` n
ORDER BY n.`node_id`;

-- 二、诊室二维码清单（14个诊室，按楼栋分组）
SELECT 
    l.`building` as '楼栋',
    l.`location_name` as '诊室名称',
    l.`room_number` as '房间号',
    l.`floor_level` as '楼层',
    CONCAT('HOSPITAL_NODE_', n.`node_id`) as '二维码内容',
    n.`node_name` as '对应节点'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY l.`building`, l.`floor_level`, l.`room_number`;

-- 三、二维码生成汇总（用于批量生成）
SELECT 
    CONCAT('HOSPITAL_NODE_', `node_id`) as '二维码内容',
    `node_name` as '地点名称',
    `floor_level` as '楼层',
    CASE 
        WHEN EXISTS (SELECT 1 FROM `locations` WHERE `map_node_id` = `node_id`) 
        THEN CONCAT('诊室区域（', 
            (SELECT COUNT(*) FROM `locations` WHERE `map_node_id` = `node_id`), 
            '个诊室）')
        ELSE '关键地点'
    END as '类型'
FROM `map_nodes`
ORDER BY `node_id`;

-- 四、统计信息
SELECT 
    '总节点数' as '项目',
    COUNT(*) as '数量'
FROM `map_nodes`
UNION ALL
SELECT 
    '总诊室数' as '项目',
    COUNT(*) as '数量'
FROM `locations`
UNION ALL
SELECT 
    '已生成二维码的节点' as '项目',
    COUNT(*) as '数量'
FROM `map_nodes`
WHERE `qrcode_content` IS NOT NULL
UNION ALL
SELECT 
    '已激活的二维码' as '项目',
    COUNT(*) as '数量'
FROM `map_nodes`
WHERE `qrcode_status` = 'ACTIVE';








