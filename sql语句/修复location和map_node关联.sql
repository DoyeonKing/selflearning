-- ============================================
-- 修复location和map_node关联问题
-- 检查并修复所有诊室的地图节点关联
-- ============================================

-- 1️⃣ 检查问题：查看所有locations及其关联的map_node
SELECT 
    l.location_id AS '诊室ID',
    l.location_name AS '诊室名称',
    l.map_node_id AS '关联的节点ID',
    l.floor_level AS '楼层',
    m.node_id AS '节点是否存在',
    m.node_name AS '节点名称',
    m.coordinates_x AS 'X坐标',
    m.coordinates_y AS 'Y坐标',
    CASE 
        WHEN l.map_node_id IS NULL THEN '❌ 未关联节点'
        WHEN m.node_id IS NULL THEN '❌ 节点不存在'
        ELSE '✅ 正常'
    END AS '状态'
FROM locations l
LEFT JOIN map_nodes m ON l.map_node_id = m.node_id
ORDER BY l.location_id;

-- 2️⃣ 检查map_nodes表中有哪些节点
SELECT 
    node_id,
    node_name,
    node_type,
    floor_level,
    coordinates_x,
    coordinates_y
FROM map_nodes
ORDER BY node_id;

-- 3️⃣ 修复：为没有节点的location创建节点
-- 假设location_id=1在2楼，坐标设为(15, 15)
-- 先检查这个location的详细信息
SELECT location_id, location_name, floor_level 
FROM locations 
WHERE location_id = 1;

-- 如果map_node_id=5的节点不存在，创建它
-- 注意：需要根据实际的location_name来设置节点名称
INSERT INTO map_nodes (
    node_name, 
    node_type, 
    coordinates_x, 
    coordinates_y, 
    floor_level, 
    description, 
    is_accessible
)
SELECT 
    CONCAT(location_name, '入口') AS node_name,
    'room' AS node_type,
    15.0 AS coordinates_x,  -- 2楼坐标，可以根据实际情况调整
    15.0 AS coordinates_y,
    floor_level,
    CONCAT(location_name, '入口节点') AS description,
    TRUE AS is_accessible
FROM locations
WHERE location_id = 1
  AND map_node_id NOT IN (SELECT node_id FROM map_nodes);

-- 4️⃣ 更新location的map_node_id为刚创建的节点
UPDATE locations l
INNER JOIN map_nodes m ON m.node_name = CONCAT(l.location_name, '入口')
SET l.map_node_id = m.node_id
WHERE l.location_id = 1
  AND l.map_node_id IS NULL;

-- 5️⃣ 如果map_node_id=5已经存在但节点不存在，需要修复关联
-- 方案A：删除错误的map_node_id，重新创建
UPDATE locations 
SET map_node_id = NULL 
WHERE location_id = 1 
  AND map_node_id NOT IN (SELECT node_id FROM map_nodes);

-- 然后重新创建节点并关联（执行上面的INSERT和UPDATE）

-- 方案B：如果map_node_id=5应该对应其他节点，直接更新
-- UPDATE locations SET map_node_id = 正确的节点ID WHERE location_id = 1;

-- 6️⃣ 批量修复：为所有没有节点的location创建节点
-- 注意：这个脚本会为每个location创建节点，坐标需要根据实际情况调整
INSERT INTO map_nodes (
    node_name, 
    node_type, 
    coordinates_x, 
    coordinates_y, 
    floor_level, 
    description, 
    is_accessible
)
SELECT 
    CONCAT(location_name, '入口') AS node_name,
    'room' AS node_type,
    -- 根据楼层设置不同的坐标
    CASE floor_level
        WHEN 1 THEN 10.0 + (location_id % 10) * 2  -- 1楼：x坐标10-30
        WHEN 2 THEN 15.0 + (location_id % 10) * 2  -- 2楼：x坐标15-35
        WHEN 3 THEN 20.0 + (location_id % 10) * 2  -- 3楼：x坐标20-40
        ELSE 10.0
    END AS coordinates_x,
    CASE floor_level
        WHEN 1 THEN 10.0 + (location_id % 10) * 2  -- 1楼：y坐标10-30
        WHEN 2 THEN 15.0 + (location_id % 10) * 2  -- 2楼：y坐标15-35
        WHEN 3 THEN 20.0 + (location_id % 10) * 2  -- 3楼：y坐标20-40
        ELSE 10.0
    END AS coordinates_y,
    floor_level,
    CONCAT(location_name, '入口节点') AS description,
    TRUE AS is_accessible
FROM locations
WHERE map_node_id IS NULL
   OR map_node_id NOT IN (SELECT node_id FROM map_nodes);

-- 7️⃣ 批量更新：关联所有新创建的节点
UPDATE locations l
INNER JOIN map_nodes m ON m.node_name = CONCAT(l.location_name, '入口')
SET l.map_node_id = m.node_id
WHERE l.map_node_id IS NULL
   OR l.map_node_id NOT IN (SELECT node_id FROM map_nodes WHERE node_id = l.map_node_id);

-- 8️⃣ 最终验证：检查所有关联是否正确
SELECT 
    l.location_id AS '诊室ID',
    l.location_name AS '诊室名称',
    l.map_node_id AS '节点ID',
    m.node_name AS '节点名称',
    m.floor_level AS '节点楼层',
    m.coordinates_x AS 'X坐标',
    m.coordinates_y AS 'Y坐标',
    CASE 
        WHEN l.map_node_id IS NULL THEN '❌ 未关联'
        WHEN m.node_id IS NULL THEN '❌ 节点不存在'
        WHEN l.floor_level != m.floor_level THEN '⚠️ 楼层不匹配'
        ELSE '✅ 正常'
    END AS '状态'
FROM locations l
LEFT JOIN map_nodes m ON l.map_node_id = m.node_id
ORDER BY l.location_id;

