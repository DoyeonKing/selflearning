@echo off
chcp 65001 >nul
echo ============================================
echo 检查和修复 location 和 map_node 关联问题
echo ============================================
echo.

REM 请修改以下数据库连接信息
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=你的数据库名
set DB_USER=root
set DB_PASS=你的密码

echo 正在检查数据库...
echo.

REM 检查 location_id=1 的信息
echo [1] 检查 location_id=1 的信息：
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT location_id, location_name, map_node_id, floor_level FROM locations WHERE location_id = 1;"

echo.
echo [2] 检查 map_node_id=5 是否存在：
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT * FROM map_nodes WHERE node_id = 5;"

echo.
echo [3] 如果节点5不存在，将自动创建...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "INSERT INTO map_nodes (node_id, node_name, node_type, coordinates_x, coordinates_y, floor_level, description, is_accessible) SELECT 5, CONCAT((SELECT location_name FROM locations WHERE location_id = 1), '入口'), 'room', 15.0, 15.0, 2, '诊室入口节点', TRUE WHERE NOT EXISTS (SELECT 1 FROM map_nodes WHERE node_id = 5);"

echo.
echo [4] 确保关联正确：
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "UPDATE locations SET map_node_id = 5 WHERE location_id = 1;"

echo.
echo [5] 验证修复结果：
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT l.location_id, l.location_name, l.map_node_id, m.node_name, m.floor_level, m.coordinates_x, m.coordinates_y FROM locations l LEFT JOIN map_nodes m ON l.map_node_id = m.node_id WHERE l.location_id = 1;"

echo.
echo ============================================
echo 检查完成！
echo ============================================
pause

