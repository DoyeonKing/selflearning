-- ============================================
-- 补全 map_edges 连接，确保所有节点都能连通
-- 解决后端路径规划返回空的问题
-- ============================================

USE hospital_05;

-- 1. 补全1楼走廊之间的连接（确保走廊节点连成网络）
-- 1.1 主走廊连接到东/西走廊起点
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼东走廊起点' AND floor_level = 1),
    10.0, 12, TRUE, '主走廊连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼东走廊起点' AND floor_level = 1);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼西走廊起点' AND floor_level = 1),
    10.0, 12, TRUE, '主走廊连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼西走廊起点' AND floor_level = 1);

-- 1.2 分诊台连接到主走廊和电梯/楼梯
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1),
    5.0, 6, TRUE, '分诊台连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼主走廊' AND floor_level = 1);

-- 1.3 门诊大厅连接到分诊台
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '门诊大厅' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1),
    5.0, 6, TRUE, '门诊大厅连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '门诊大厅' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1);

-- 1.4 医院正门连接到门诊大厅
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '医院正门' AND floor_level = 1 AND coordinates_x = 20.00),
    (SELECT node_id FROM map_nodes WHERE node_name = '门诊大厅' AND floor_level = 1),
    4.0, 5, TRUE, '入口连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '医院正门' AND floor_level = 1 AND coordinates_x = 20.00)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '门诊大厅' AND floor_level = 1);

-- 1.5 电梯/楼梯连接到最近的走廊节点
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼东走廊起点' AND floor_level = 1),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 1 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 5 AND 10
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼东走廊起点' AND floor_level = 1);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 1 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 15 AND 25
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '分诊台' AND floor_level = 1);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '1楼西走廊起点' AND floor_level = 1),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 1 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 30 AND 40
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1楼西走廊起点' AND floor_level = 1);

-- 2. 补全2楼走廊之间的连接
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2),
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼东走廊起点' AND floor_level = 2),
    10.0, 12, TRUE, '主走廊连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼东走廊起点' AND floor_level = 2);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2),
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼西走廊起点' AND floor_level = 2),
    10.0, 12, TRUE, '主走廊连接'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼西走廊起点' AND floor_level = 2);

-- 2楼电梯/楼梯连接
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼东走廊起点' AND floor_level = 2),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 2 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 5 AND 10
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼东走廊起点' AND floor_level = 2);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 2 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 15 AND 25
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼主走廊' AND floor_level = 2);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '2楼西走廊起点' AND floor_level = 2),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 2 
  AND n.node_type IN ('elevator', 'stairs')
  AND n.coordinates_x BETWEEN 30 AND 40
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2楼西走廊起点' AND floor_level = 2);

-- 3. 补全3楼连接
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    n.node_id,
    (SELECT node_id FROM map_nodes WHERE node_name = '3楼主走廊' AND floor_level = 3),
    5.0, 6, TRUE, '电梯/楼梯连接'
FROM map_nodes n
WHERE n.floor_level = 3 
  AND n.node_type IN ('elevator', 'stairs')
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '3楼主走廊' AND floor_level = 3);

-- 4. 补全跨楼层连接（电梯/楼梯的上下层连接）
-- 1号电梯：1楼 <-> 2楼 <-> 3楼
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '1号电梯-1楼' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '1号电梯-2楼' AND floor_level = 2),
    0.0, 30, TRUE, '电梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号电梯-1楼' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号电梯-2楼' AND floor_level = 2);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '1号电梯-2楼' AND floor_level = 2),
    (SELECT node_id FROM map_nodes WHERE node_name = '1号电梯-3楼' AND floor_level = 3),
    0.0, 30, TRUE, '电梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号电梯-2楼' AND floor_level = 2)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号电梯-3楼' AND floor_level = 3);

-- 2号电梯：1楼 <-> 2楼 <-> 3楼
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '2号电梯-1楼' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '2号电梯-2楼' AND floor_level = 2),
    0.0, 30, TRUE, '电梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号电梯-1楼' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号电梯-2楼' AND floor_level = 2);

INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '2号电梯-2楼' AND floor_level = 2),
    (SELECT node_id FROM map_nodes WHERE node_name = '2号电梯-3楼' AND floor_level = 3),
    0.0, 30, TRUE, '电梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号电梯-2楼' AND floor_level = 2)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号电梯-3楼' AND floor_level = 3);

-- 1号楼梯：1楼 <-> 2楼
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '1号楼梯-1楼' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '1号楼梯-2楼' AND floor_level = 2),
    0.0, 45, TRUE, '楼梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号楼梯-1楼' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '1号楼梯-2楼' AND floor_level = 2);

-- 2号楼梯：1楼 <-> 2楼
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    (SELECT node_id FROM map_nodes WHERE node_name = '2号楼梯-1楼' AND floor_level = 1),
    (SELECT node_id FROM map_nodes WHERE node_name = '2号楼梯-2楼' AND floor_level = 2),
    0.0, 45, TRUE, '楼梯跨层'
WHERE EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号楼梯-1楼' AND floor_level = 1)
  AND EXISTS (SELECT 1 FROM map_nodes WHERE node_name = '2号楼梯-2楼' AND floor_level = 2);

-- 5. 查看补全后的统计
SELECT 
    '补全完成' AS status,
    COUNT(*) AS total_edges,
    COUNT(DISTINCT start_node_id) AS unique_start_nodes,
    COUNT(DISTINCT end_node_id) AS unique_end_nodes
FROM map_edges;

-- 6. 检查是否有孤立节点（没有连接的节点）
SELECT 
    n.node_id,
    n.node_name,
    n.floor_level,
    COUNT(e.edge_id) AS connection_count
FROM map_nodes n
LEFT JOIN map_edges e ON (e.start_node_id = n.node_id OR e.end_node_id = n.node_id)
WHERE n.node_type IN ('hallway', 'elevator', 'stairs', 'entrance')
GROUP BY n.node_id, n.node_name, n.floor_level
HAVING connection_count = 0
ORDER BY n.floor_level, n.node_id;

