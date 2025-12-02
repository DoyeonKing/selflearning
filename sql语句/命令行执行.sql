-- ============================================
-- 命令行执行：检查和修复 location_id=1
-- 使用方法：mysql -u用户名 -p密码 数据库名 < 命令行执行.sql
-- ============================================

-- 1. 检查 location_id=1 的信息
SELECT '=== 检查 location_id=1 ===' AS '';
SELECT location_id, location_name, map_node_id, floor_level 
FROM locations 
WHERE location_id = 1;

-- 2. 检查 map_node_id=5 是否存在
SELECT '=== 检查 map_node_id=5 ===' AS '';
SELECT * FROM map_nodes WHERE node_id = 5;

-- 3. 如果节点5不存在，创建它
SELECT '=== 创建节点5（如果不存在）===' AS '';
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
    5,
    CONCAT((SELECT location_name FROM locations WHERE location_id = 1), '入口'),
    'room',
    15.0,
    15.0,
    2,
    '诊室入口节点',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM map_nodes WHERE node_id = 5);

-- 4. 确保关联正确
SELECT '=== 更新关联 ===' AS '';
UPDATE locations 
SET map_node_id = 5 
WHERE location_id = 1;

-- 5. 验证修复结果
SELECT '=== 验证结果 ===' AS '';
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

-- 6. 显示所有有问题的关联
SELECT '=== 所有有问题的关联 ===' AS '';
SELECT 
    l.location_id,
    l.location_name,
    l.map_node_id,
    CASE 
        WHEN l.map_node_id IS NULL THEN '❌ 未关联节点'
        WHEN m.node_id IS NULL THEN '❌ 节点不存在'
        ELSE '✅ 正常'
    END AS 状态
FROM locations l
LEFT JOIN map_nodes m ON l.map_node_id = m.node_id
WHERE l.map_node_id IS NULL OR m.node_id IS NULL;

