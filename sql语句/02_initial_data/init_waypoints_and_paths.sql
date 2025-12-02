-- ============================================
-- 途径点和路径连接数据初始化脚本
-- 用于地理导航系统的路径规划
-- ============================================

-- 说明：
-- 1. 途径点（waypoints）是导航路径中的关键节点，包括：
--    - 走廊节点（hallway）：用于路径规划
--    - 电梯节点（elevator）：跨楼层导航
--    - 楼梯节点（stairs）：跨楼层导航
--    - 入口节点（entrance）：起点位置
-- 2. 路径连接（map_edges）定义节点之间的连接关系
-- 3. 这些数据用于A*路径规划算法

-- ============================================
-- 第一部分：添加途径点节点
-- ============================================

-- 1楼途径点
INSERT IGNORE INTO `map_nodes` (`node_name`, `node_type`, `coordinates_x`, `coordinates_y`, `floor_level`, `description`, `is_accessible`) VALUES
-- 入口和关键位置
('医院正门', 'entrance', 20.0, 29.0, 1, '医院主要出入口，导航起点', TRUE),
('门诊大厅', 'hallway', 20.0, 25.0, 1, '门诊综合服务大厅，主要通道', TRUE),
('分诊台', 'hallway', 20.0, 20.0, 1, '分诊服务台，重要导航节点', TRUE),

-- 走廊节点（1楼）
('1楼东走廊起点', 'hallway', 10.0, 20.0, 1, '1楼东侧走廊起点', TRUE),
('1楼东走廊中段', 'hallway', 10.0, 15.0, 1, '1楼东侧走廊中段', TRUE),
('1楼东走廊终点', 'hallway', 10.0, 10.0, 1, '1楼东侧走廊终点', TRUE),
('1楼西走廊起点', 'hallway', 30.0, 20.0, 1, '1楼西侧走廊起点', TRUE),
('1楼西走廊中段', 'hallway', 30.0, 15.0, 1, '1楼西侧走廊中段', TRUE),
('1楼西走廊终点', 'hallway', 30.0, 10.0, 1, '1楼西侧走廊终点', TRUE),
('1楼主走廊', 'hallway', 20.0, 15.0, 1, '1楼主走廊，连接东西两侧', TRUE),

-- 电梯和楼梯（1楼）
('1号电梯-1楼', 'elevator', 35.0, 20.0, 1, '1号电梯1楼位置', TRUE),
('2号电梯-1楼', 'elevator', 5.0, 20.0, 1, '2号电梯1楼位置', TRUE),
('1号楼梯-1楼', 'stairs', 15.0, 20.0, 1, '1号楼梯1楼位置', TRUE),
('2号楼梯-1楼', 'stairs', 25.0, 20.0, 1, '2号楼梯1楼位置', TRUE);

-- 2楼途径点
INSERT IGNORE INTO `map_nodes` (`node_name`, `node_type`, `coordinates_x`, `coordinates_y`, `floor_level`, `description`, `is_accessible`) VALUES
-- 2楼走廊节点
('2楼东走廊起点', 'hallway', 10.0, 20.0, 2, '2楼东侧走廊起点', TRUE),
('2楼东走廊中段', 'hallway', 10.0, 15.0, 2, '2楼东侧走廊中段', TRUE),
('2楼东走廊终点', 'hallway', 10.0, 10.0, 2, '2楼东侧走廊终点', TRUE),
('2楼西走廊起点', 'hallway', 30.0, 20.0, 2, '2楼西侧走廊起点', TRUE),
('2楼西走廊中段', 'hallway', 30.0, 15.0, 2, '2楼西侧走廊中段', TRUE),
('2楼西走廊终点', 'hallway', 30.0, 10.0, 2, '2楼西侧走廊终点', TRUE),
('2楼主走廊', 'hallway', 20.0, 15.0, 2, '2楼主走廊，连接东西两侧', TRUE),

-- 电梯和楼梯（2楼）
('1号电梯-2楼', 'elevator', 35.0, 20.0, 2, '1号电梯2楼位置', TRUE),
('2号电梯-2楼', 'elevator', 5.0, 20.0, 2, '2号电梯2楼位置', TRUE),
('1号楼梯-2楼', 'stairs', 15.0, 20.0, 2, '1号楼梯2楼位置', TRUE),
('2号楼梯-2楼', 'stairs', 25.0, 20.0, 2, '2号楼梯2楼位置', TRUE);

-- 3楼途径点（如果需要）
INSERT IGNORE INTO `map_nodes` (`node_name`, `node_type`, `coordinates_x`, `coordinates_y`, `floor_level`, `description`, `is_accessible`) VALUES
('3楼主走廊', 'hallway', 20.0, 15.0, 3, '3楼主走廊', TRUE),
('1号电梯-3楼', 'elevator', 35.0, 20.0, 3, '1号电梯3楼位置', TRUE),
('2号电梯-3楼', 'elevator', 5.0, 20.0, 3, '2号电梯3楼位置', TRUE);

-- ============================================
-- 第二部分：为途径点生成二维码内容
-- ============================================

-- 为所有新添加的途径点生成二维码内容
UPDATE `map_nodes` 
SET 
    `qrcode_content` = CONCAT('HOSPITAL_NODE_', `node_id`),
    `qrcode_status` = 'PENDING',
    `qrcode_generated_at` = NOW()
WHERE `qrcode_content` IS NULL OR `qrcode_content` = '';

-- ============================================
-- 第三部分：添加路径连接关系
-- ============================================

-- 获取节点ID（使用子查询）
-- 注意：这里假设节点已经存在，如果不存在需要先执行上面的INSERT语句

-- 1楼路径连接
INSERT IGNORE INTO `map_edges` (`start_node_id`, `end_node_id`, `distance`, `walk_time`, `is_bidirectional`, `accessibility_info`) 
SELECT 
    n1.node_id, n2.node_id, 
    10.0, 12, TRUE, '无障碍通道'
FROM `map_nodes` n1, `map_nodes` n2
WHERE n1.node_name = '医院正门' AND n2.node_name = '门诊大厅'
   OR (n1.node_name = '门诊大厅' AND n2.node_name = '分诊台')
   OR (n1.node_name = '分诊台' AND n2.node_name = '1楼主走廊')
   OR (n1.node_name = '1楼主走廊' AND n2.node_name = '1楼东走廊起点')
   OR (n1.node_name = '1楼主走廊' AND n2.node_name = '1楼西走廊起点')
   OR (n1.node_name = '1楼东走廊起点' AND n2.node_name = '1楼东走廊中段')
   OR (n1.node_name = '1楼东走廊中段' AND n2.node_name = '1楼东走廊终点')
   OR (n1.node_name = '1楼西走廊起点' AND n2.node_name = '1楼西走廊中段')
   OR (n1.node_name = '1楼西走廊中段' AND n2.node_name = '1楼西走廊终点')
   OR (n1.node_name = '分诊台' AND n2.node_name = '1号电梯-1楼')
   OR (n1.node_name = '分诊台' AND n2.node_name = '2号电梯-1楼')
   OR (n1.node_name = '分诊台' AND n2.node_name = '1号楼梯-1楼')
   OR (n1.node_name = '分诊台' AND n2.node_name = '2号楼梯-1楼');

-- 跨楼层连接（电梯和楼梯）
INSERT IGNORE INTO `map_edges` (`start_node_id`, `end_node_id`, `distance`, `walk_time`, `is_bidirectional`, `accessibility_info`) 
SELECT 
    n1.node_id, n2.node_id, 
    0.0, 30, TRUE, '电梯/楼梯连接'
FROM `map_nodes` n1, `map_nodes` n2
WHERE (n1.node_name = '1号电梯-1楼' AND n2.node_name = '1号电梯-2楼')
   OR (n1.node_name = '1号电梯-1楼' AND n2.node_name = '1号电梯-3楼')
   OR (n1.node_name = '1号电梯-2楼' AND n2.node_name = '1号电梯-3楼')
   OR (n1.node_name = '2号电梯-1楼' AND n2.node_name = '2号电梯-2楼')
   OR (n1.node_name = '2号电梯-1楼' AND n2.node_name = '2号电梯-3楼')
   OR (n1.node_name = '2号电梯-2楼' AND n2.node_name = '2号电梯-3楼')
   OR (n1.node_name = '1号楼梯-1楼' AND n2.node_name = '1号楼梯-2楼')
   OR (n1.node_name = '2号楼梯-1楼' AND n2.node_name = '2号楼梯-2楼');

-- 2楼路径连接
INSERT IGNORE INTO `map_edges` (`start_node_id`, `end_node_id`, `distance`, `walk_time`, `is_bidirectional`, `accessibility_info`) 
SELECT 
    n1.node_id, n2.node_id, 
    10.0, 12, TRUE, '无障碍通道'
FROM `map_nodes` n1, `map_nodes` n2
WHERE (n1.node_name = '1号电梯-2楼' AND n2.node_name = '2楼主走廊')
   OR (n1.node_name = '2号电梯-2楼' AND n2.node_name = '2楼主走廊')
   OR (n1.node_name = '1号楼梯-2楼' AND n2.node_name = '2楼主走廊')
   OR (n1.node_name = '2号楼梯-2楼' AND n2.node_name = '2楼主走廊')
   OR (n1.node_name = '2楼主走廊' AND n2.node_name = '2楼东走廊起点')
   OR (n1.node_name = '2楼主走廊' AND n2.node_name = '2楼西走廊起点')
   OR (n1.node_name = '2楼东走廊起点' AND n2.node_name = '2楼东走廊中段')
   OR (n1.node_name = '2楼东走廊中段' AND n2.node_name = '2楼东走廊终点')
   OR (n1.node_name = '2楼西走廊起点' AND n2.node_name = '2楼西走廊中段')
   OR (n1.node_name = '2楼西走廊中段' AND n2.node_name = '2楼西走廊终点');

-- ============================================
-- 第四部分：连接诊室到最近的途径点
-- ============================================

-- 将诊室（location）连接到最近的走廊节点
-- 这里需要根据实际的location数据来设置
-- 示例：假设location_id=1的诊室在1楼，连接到1楼东走廊终点
INSERT IGNORE INTO `map_edges` (`start_node_id`, `end_node_id`, `distance`, `walk_time`, `is_bidirectional`, `accessibility_info`) 
SELECT 
    n.node_id, l.map_node_id,
    5.0, 6, TRUE, '诊室连接'
FROM `map_nodes` n, `locations` l
WHERE n.node_name = '1楼东走廊终点' 
  AND l.map_node_id IS NOT NULL
  AND l.floor_level = 1
  AND l.location_id <= 5;  -- 示例：前5个诊室

-- ============================================
-- 第五部分：查看数据
-- ============================================

-- 查看所有途径点
SELECT 
    `node_id`,
    `node_name`,
    `node_type`,
    `coordinates_x`,
    `coordinates_y`,
    `floor_level`,
    `qrcode_content`,
    `qrcode_status`
FROM `map_nodes`
WHERE `node_type` != 'room'
ORDER BY `floor_level`, `node_type`, `node_id`;

-- 查看所有路径连接
SELECT 
    e.`edge_id`,
    n1.`node_name` AS `起点`,
    n2.`node_name` AS `终点`,
    e.`distance` AS `距离(米)`,
    e.`walk_time` AS `步行时间(秒)`,
    e.`is_bidirectional` AS `双向`,
    e.`accessibility_info` AS `无障碍信息`
FROM `map_edges` e
JOIN `map_nodes` n1 ON e.`start_node_id` = n1.`node_id`
JOIN `map_nodes` n2 ON e.`end_node_id` = n2.`node_id`
ORDER BY e.`edge_id`;

-- 统计信息
SELECT 
    `node_type`,
    COUNT(*) AS `数量`
FROM `map_nodes`
GROUP BY `node_type`
ORDER BY `数量` DESC;

SELECT 
    COUNT(*) AS `总路径数`,
    SUM(CASE WHEN `is_bidirectional` = TRUE THEN 1 ELSE 0 END) AS `双向路径数`
FROM `map_edges`;



