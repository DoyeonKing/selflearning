-- ============================================
-- 二维码数据完整初始化脚本
-- 为所有地图节点和诊室生成二维码内容
-- ============================================

-- 步骤1: 确保所有节点都有二维码内容
-- 如果节点已经有二维码内容，则跳过；如果没有，则生成
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

-- 步骤2: 查看所有节点的二维码信息（用于生成二维码图片）
SELECT 
    `node_id` as '节点ID',
    `node_name` as '节点名称',
    `coordinates_x` as 'X坐标',
    `coordinates_y` as 'Y坐标',
    `floor_level` as '楼层',
    `qrcode_content` as '二维码内容',
    `qrcode_status` as '状态'
FROM `map_nodes`
ORDER BY `node_id`;

-- 步骤3: 查看诊室对应的二维码信息
SELECT 
    l.`location_id` as '诊室ID',
    l.`location_name` as '诊室名称',
    l.`building` as '楼栋',
    l.`room_number` as '房间号',
    l.`floor_level` as '楼层',
    n.`node_id` as '节点ID',
    n.`node_name` as '节点名称',
    n.`qrcode_content` as '二维码内容',
    n.`qrcode_status` as '状态'
FROM `locations` l
LEFT JOIN `map_nodes` n ON l.`map_node_id` = n.`node_id`
WHERE n.`node_id` IS NOT NULL
ORDER BY l.`location_id`;

-- 步骤4: 生成二维码内容汇总（复制这些内容去生成二维码）
-- 格式：节点ID | 节点名称 | 二维码内容
SELECT 
    CONCAT('节点', `node_id`, ' | ', `node_name`, ' | ', `qrcode_content`) as '二维码生成列表'
FROM `map_nodes`
ORDER BY `node_id`;

-- 步骤5: 统计信息
SELECT 
    COUNT(*) as '总节点数',
    SUM(CASE WHEN `qrcode_content` IS NOT NULL THEN 1 ELSE 0 END) as '已生成二维码内容',
    SUM(CASE WHEN `qrcode_status` = 'ACTIVE' THEN 1 ELSE 0 END) as '已激活二维码',
    SUM(CASE WHEN `qrcode_status` = 'PENDING' THEN 1 ELSE 0 END) as '待生成二维码',
    SUM(CASE WHEN `qrcode_status` = 'INACTIVE' THEN 1 ELSE 0 END) as '已停用二维码'
FROM `map_nodes`;








