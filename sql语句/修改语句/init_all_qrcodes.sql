-- ============================================
-- 二维码数据完整初始化脚本
-- 为所有地图节点生成二维码内容
-- ============================================

-- 步骤1: 确保所有节点都有二维码内容
UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = 'PENDING',
    `qrcode_generated_at` = NOW()
WHERE `qrcode_content` IS NULL OR `qrcode_content` = '';

-- 步骤2: 查看所有节点的二维码信息（用于生成二维码图片）
SELECT 
    `node_id` as '节点ID',
    `node_name` as '节点名称',
    `qrcode_content` as '二维码内容',
    `qrcode_status` as '状态',
    CONCAT('HOSPITAL_NODE_', `node_id`) as '用于生成二维码的文本'
FROM `map_nodes`
ORDER BY `node_id`;

-- 步骤3: 查看诊室对应的二维码信息
SELECT 
    l.`location_id` as '诊室ID',
    l.`location_name` as '诊室名称',
    l.`building` as '楼栋',
    l.`room_number` as '房间号',
    n.`node_id` as '节点ID',
    n.`qrcode_content` as '二维码内容'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY l.`location_id`;

-- 步骤4: 生成二维码内容汇总（复制这些内容去生成二维码）
SELECT 
    CONCAT('节点', `node_id`, ': ', `node_name`, ' -> ', `qrcode_content`) as '二维码生成列表'
FROM `map_nodes`
ORDER BY `node_id`;








