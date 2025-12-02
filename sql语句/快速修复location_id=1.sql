-- ============================================
-- 快速修复：location_id=1 的地图节点问题
-- ============================================

-- 1️⃣ 先看看location_id=1的详细信息
SELECT 
    location_id,
    location_name,
    map_node_id,
    floor_level
FROM locations 
WHERE location_id = 1;

-- 2️⃣ 检查map_node_id=5是否存在
SELECT * FROM map_nodes WHERE node_id = 5;

-- 3️⃣ 如果节点5不存在，创建它
-- 根据你的数据：location_id=1在2楼，map_node_id应该是5
INSERT INTO map_nodes (
    node_id,
    node_name, 
    node_type, 
    coordinates_x, 
    coordinates_y, 
    floor_level, 
    description, 
    is_accessible
)
SELECT 
    5 AS node_id,
    CONCAT((SELECT location_name FROM locations WHERE location_id = 1), '入口') AS node_name,
    'room' AS node_type,
    15.0 AS coordinates_x,  -- 2楼坐标，可以调整
    15.0 AS coordinates_y,
    2 AS floor_level,
    CONCAT((SELECT location_name FROM locations WHERE location_id = 1), '入口节点') AS description,
    TRUE AS is_accessible
WHERE NOT EXISTS (SELECT 1 FROM map_nodes WHERE node_id = 5);

-- 4️⃣ 确保location_id=1关联到node_id=5
UPDATE locations 
SET map_node_id = 5 
WHERE location_id = 1;

-- 5️⃣ 验证修复结果
SELECT 
    l.location_id,
    l.location_name,
    l.map_node_id,
    m.node_name,
    m.floor_level,
    m.coordinates_x,
    m.coordinates_y
FROM locations l
LEFT JOIN map_nodes m ON l.map_node_id = m.node_id
WHERE l.location_id = 1;

