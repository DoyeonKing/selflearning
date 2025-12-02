-- ============================================
-- 补全诊室到最近走廊节点的直接连接
-- 解决"明明很近却要绕远路"的问题
-- ============================================

USE hospital_05;

-- 1. 为每个诊室找到最近的走廊节点，并创建直接连接
-- 1楼：诊室连接到1楼主走廊或最近的走廊节点
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    l.map_node_id AS start_node_id,
    n.node_id AS end_node_id,
    -- 计算实际距离（米）：坐标差 * 1米/网格
    SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) AS distance,
    -- 步行时间：每米约1.2秒
    ROUND(SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) * 1.2) AS walk_time,
    TRUE AS is_bidirectional,
    'direct_room_corridor' AS accessibility_info
FROM locations l
JOIN map_nodes l_coords ON l.map_node_id = l_coords.node_id
JOIN map_nodes n ON n.floor_level = l.floor_level
WHERE l.map_node_id IS NOT NULL
  AND l.floor_level = 1
  AND n.node_type = 'hallway'
  AND n.node_name IN ('1楼主走廊', '1楼东走廊起点', '1楼西走廊起点', '分诊台')
  -- 只连接距离小于20米的（避免连接太远的走廊）
  AND SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) < 20
  -- 只选择最近的走廊节点
  AND NOT EXISTS (
    SELECT 1 FROM map_nodes n2
    WHERE n2.floor_level = l.floor_level
      AND n2.node_type = 'hallway'
      AND n2.node_name IN ('1楼主走廊', '1楼东走廊起点', '1楼西走廊起点', '分诊台')
      AND SQRT(POWER((l_coords.coordinates_x - n2.coordinates_x), 2) + POWER((l_coords.coordinates_y - n2.coordinates_y), 2)) < 
          SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2))
      AND SQRT(POWER((l_coords.coordinates_x - n2.coordinates_x), 2) + POWER((l_coords.coordinates_y - n2.coordinates_y), 2)) < 20
  )
  -- 避免重复连接
  AND NOT EXISTS (
    SELECT 1 FROM map_edges e
    WHERE (e.start_node_id = l.map_node_id AND e.end_node_id = n.node_id)
       OR (e.end_node_id = l.map_node_id AND e.start_node_id = n.node_id)
  );

-- 2楼：诊室连接到2楼主走廊或最近的走廊节点
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    l.map_node_id AS start_node_id,
    n.node_id AS end_node_id,
    SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) AS distance,
    ROUND(SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) * 1.2) AS walk_time,
    TRUE AS is_bidirectional,
    'direct_room_corridor' AS accessibility_info
FROM locations l
JOIN map_nodes l_coords ON l.map_node_id = l_coords.node_id
JOIN map_nodes n ON n.floor_level = l.floor_level
WHERE l.map_node_id IS NOT NULL
  AND l.floor_level = 2
  AND n.node_type = 'hallway'
  AND n.node_name IN ('2楼主走廊', '2楼东走廊起点', '2楼西走廊起点')
  AND SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) < 20
  AND NOT EXISTS (
    SELECT 1 FROM map_nodes n2
    WHERE n2.floor_level = l.floor_level
      AND n2.node_type = 'hallway'
      AND n2.node_name IN ('2楼主走廊', '2楼东走廊起点', '2楼西走廊起点')
      AND SQRT(POWER((l_coords.coordinates_x - n2.coordinates_x), 2) + POWER((l_coords.coordinates_y - n2.coordinates_y), 2)) < 
          SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2))
      AND SQRT(POWER((l_coords.coordinates_x - n2.coordinates_x), 2) + POWER((l_coords.coordinates_y - n2.coordinates_y), 2)) < 20
  )
  AND NOT EXISTS (
    SELECT 1 FROM map_edges e
    WHERE (e.start_node_id = l.map_node_id AND e.end_node_id = n.node_id)
       OR (e.end_node_id = l.map_node_id AND e.start_node_id = n.node_id)
  );

-- 3楼：诊室连接到3楼主走廊
INSERT IGNORE INTO map_edges (start_node_id, end_node_id, distance, walk_time, is_bidirectional, accessibility_info)
SELECT 
    l.map_node_id AS start_node_id,
    n.node_id AS end_node_id,
    SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) AS distance,
    ROUND(SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) * 1.2) AS walk_time,
    TRUE AS is_bidirectional,
    'direct_room_corridor' AS accessibility_info
FROM locations l
JOIN map_nodes l_coords ON l.map_node_id = l_coords.node_id
JOIN map_nodes n ON n.floor_level = l.floor_level
WHERE l.map_node_id IS NOT NULL
  AND l.floor_level = 3
  AND n.node_type = 'hallway'
  AND n.node_name = '3楼主走廊'
  AND SQRT(POWER((l_coords.coordinates_x - n.coordinates_x), 2) + POWER((l_coords.coordinates_y - n.coordinates_y), 2)) < 20
  AND NOT EXISTS (
    SELECT 1 FROM map_edges e
    WHERE (e.start_node_id = l.map_node_id AND e.end_node_id = n.node_id)
       OR (e.end_node_id = l.map_node_id AND e.start_node_id = n.node_id)
  );

-- 查看补全后的连接统计
SELECT 
    '补全完成' AS status,
    COUNT(*) AS total_edges,
    COUNT(DISTINCT CASE WHEN accessibility_info = 'direct_room_corridor' THEN edge_id END) AS direct_room_connections
FROM map_edges;

-- 检查是否有诊室仍然没有连接到走廊
SELECT 
    l.location_id,
    l.location_name,
    l.floor_level,
    l.map_node_id,
    COUNT(e.edge_id) AS connection_count
FROM locations l
LEFT JOIN map_edges e ON (e.start_node_id = l.map_node_id OR e.end_node_id = l.map_node_id)
WHERE l.map_node_id IS NOT NULL
GROUP BY l.location_id, l.location_name, l.floor_level, l.map_node_id
HAVING connection_count = 0
ORDER BY l.floor_level, l.location_id;

