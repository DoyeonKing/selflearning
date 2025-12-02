package com.example.springboot.service;

import com.example.springboot.dto.map.MapConfigResponse;
import com.example.springboot.dto.map.MapGridDTO;
import com.example.springboot.dto.map.MapNodeDTO;
import com.example.springboot.entity.Location;
import com.example.springboot.entity.MapNode;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.LocationRepository;
import com.example.springboot.repository.MapNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 地图服务
 * 提供地图配置和节点查询功能
 */
@Service
public class MapService {
    
    // 地图网格尺寸
    private static final int GRID_WIDTH = 40;
    private static final int GRID_HEIGHT = 30;
    
    // 每个楼层的网格矩阵（楼层号 -> 网格矩阵）
    // 0=通路，1=障碍物
    private Map<Integer, int[][]> floorGridMatrices;
    
    // 默认网格矩阵（用于没有特定配置的楼层）
    private int[][] defaultGridMatrix;
    
    // 节点列表
    private List<MapNodeDTO> nodes;
    
    private final LocationRepository locationRepository;
    private final MapNodeRepository mapNodeRepository;
    
    /**
     * 构造函数：初始化地图数据
     */
    @Autowired
    public MapService(LocationRepository locationRepository, MapNodeRepository mapNodeRepository) {
        this.locationRepository = locationRepository;
        this.mapNodeRepository = mapNodeRepository;
        System.out.println("[MapService] 开始初始化地图数据...");
        initializeMapData();
        System.out.println("[MapService] 地图数据初始化完成，节点数量: " + nodes.size());
    }
    
    /**
     * 初始化地图数据
     * 创建不同楼层的40x30网格，设置各楼层障碍物，并预设关键节点
     */
    private void initializeMapData() {
        // 初始化楼层地图容器
        floorGridMatrices = new HashMap<>();
        
        // 初始化1楼地图
        floorGridMatrices.put(1, initializeFloor1());
        
        // 初始化2楼地图
        floorGridMatrices.put(2, initializeFloor2());
        
        // 初始化3楼地图
        floorGridMatrices.put(3, initializeFloor3());
        
        // 设置默认网格（1楼）
        defaultGridMatrix = floorGridMatrices.get(1);
        
        // 初始化节点列表（完全从数据库加载）
        nodes = new ArrayList<>();
        
        // 从数据库加载真实的节点信息（包括途径点和诊室）
        try {
            List<MapNode> allMapNodes = mapNodeRepository.findAll();
            System.out.println("[MapService] 从数据库加载了 " + allMapNodes.size() + " 个地图节点");
            
            if (allMapNodes.isEmpty()) {
                System.err.println("[MapService] ⚠️ 警告：数据库中没有任何地图节点！请先执行 init_waypoints_and_paths.sql 脚本");
            }
            
            // 先加载所有locations，建立locationId到location的映射
            List<Location> allLocations = locationRepository.findAll();
            Map<Integer, Location> locationMap = new HashMap<>();
            for (Location loc : allLocations) {
                if (loc.getMapNodeId() != null) {
                    locationMap.put(loc.getMapNodeId(), loc);
                }
            }
            System.out.println("[MapService] 加载了 " + allLocations.size() + " 个location，其中 " + locationMap.size() + " 个关联了map_node");
            
            // 将所有MapNode转换为MapNodeDTO
            for (MapNode mapNode : allMapNodes) {
                // 查找关联的locationId
                Integer locationId = null;
                Location linkedLocation = locationMap.get(mapNode.getNodeId());
                if (linkedLocation != null) {
                    locationId = linkedLocation.getLocationId();
                }
                
                // 转换坐标（从数据库的coordinates_x/y转换为网格坐标）
                int gridX = mapNode.getCoordinatesX() != null ? mapNode.getCoordinatesX().intValue() : 0;
                int gridY = mapNode.getCoordinatesY() != null ? mapNode.getCoordinatesY().intValue() : 0;
                
                // 使用数据库中的真实节点名称
                MapNodeDTO node = new MapNodeDTO(
                    mapNode.getNodeId(),
                    mapNode.getNodeName(),  // 真实的节点名称
                    gridX,
                    gridY,
                    locationId
                );
                nodes.add(node);
                
                System.out.println("[MapService] 添加节点: " + mapNode.getNodeName() + 
                    " (nodeId=" + mapNode.getNodeId() + 
                    ", 类型=" + mapNode.getNodeType() +
                    ", 楼层=" + mapNode.getFloorLevel() +
                    ", locationId=" + locationId +
                    ", 坐标=[" + gridX + "," + gridY + "])");
            }
            
            if (nodes.isEmpty()) {
                System.err.println("[MapService] ❌ 错误：数据库中没有节点数据，请先执行初始化脚本！");
            }
        } catch (Exception e) {
            System.err.println("[MapService] ❌ 从数据库加载节点失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("地图服务初始化失败：无法加载节点数据。请检查数据库连接和数据是否正确。", e);
        }
    }
    
    /**
     * 初始化1楼地图（门诊大厅、急诊、内科等）
     */
    private int[][] initializeFloor1() {
        int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];
        
        // 左侧墙壁（x=0到x=3），留出通道y=8到y=12
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < 4; x++) {
                if (y < 8 || y > 12) {
                    grid[y][x] = 1;
                }
            }
        }
        
        // 右侧墙壁（x=36到x=39），留出通道y=18到y=22
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 36; x < GRID_WIDTH; x++) {
                if (y < 18 || y > 22) {
                    grid[y][x] = 1;
                }
            }
        }
        
        // 中间横向墙壁（y=15到y=17，x=10到x=30）
        for (int y = 15; y < 18; y++) {
            for (int x = 10; x < 31; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 上方横向墙壁（y=0到y=2）
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 下方横向墙壁（y=27到y=29），起点(20,29)留通道
        for (int y = 27; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (!(y == 29 && x >= 18 && x <= 22)) {
                    grid[y][x] = 1;
                }
            }
        }
        
        // 中间纵向墙壁（x=15到x=17，y=5到y=12）
        for (int y = 5; y < 13; y++) {
            for (int x = 15; x < 18; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 确保关键节点是通路
        ensureNodeWalkable(grid, 20, 29); // 医院大门
        ensureNodeWalkable(grid, 20, 20); // 分诊台
        ensureNodeWalkable(grid, 35, 20); // 电梯口
        ensureNodeWalkable(grid, 5, 10);  // 内科诊室
        ensureNodeWalkable(grid, 5, 5);   // 外科诊室
        
        // 创建主要通道
        for (int y = 20; y <= 29; y++) grid[y][20] = 0; // 中央纵向通道
        for (int x = 5; x <= 20; x++) grid[20][x] = 0;  // 横向通道
        for (int y = 10; y <= 20; y++) grid[y][5] = 0;  // 左侧纵向通道
        
        return grid;
    }
    
    /**
     * 初始化2楼地图（外科、妇产科等，布局与1楼不同）
     */
    private int[][] initializeFloor2() {
        int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];
        
        // 2楼布局：更多诊室，走廊布局不同
        
        // 左侧墙壁（更少的通道）
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < 4; x++) {
                if (y < 12 || y > 18) {
                    grid[y][x] = 1;
                }
            }
        }
        
        // 右侧墙壁
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 36; x < GRID_WIDTH; x++) {
                if (y < 10 || y > 20) {
                    grid[y][x] = 1;
                }
            }
        }
        
        // 上方横向墙壁
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 下方横向墙壁
        for (int y = 27; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 2楼特有的中间区域障碍（模拟护士站或设施）
        for (int y = 12; y < 18; y++) {
            for (int x = 18; x < 22; x++) {
                grid[y][x] = 1;
            }
        }
        
        // 电梯位置必须通路
        ensureNodeWalkable(grid, 35, 20);
        ensureNodeWalkable(grid, 5, 20);
        
        // 2楼主要走廊（环形走廊）
        for (int x = 5; x < 35; x++) {
            grid[10][x] = 0; // 上方走廊
            grid[25][x] = 0; // 下方走廊
        }
        for (int y = 10; y <= 25; y++) {
            grid[y][10] = 0; // 左侧走廊
            grid[y][30] = 0; // 右侧走廊
        }
        
        return grid;
    }
    
    /**
     * 初始化3楼地图（特殊科室，布局更简单）
     */
    private int[][] initializeFloor3() {
        int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];
        
        // 3楼布局：更简单，主要是一条长走廊
        
        // 四周墙壁
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 1; // 上墙
            }
        }
        for (int y = 27; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 1; // 下墙
            }
        }
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < 3; x++) {
                grid[y][x] = 1; // 左墙
            }
            for (int x = 37; x < GRID_WIDTH; x++) {
                grid[y][x] = 1; // 右墙
            }
        }
        
        // 中央走廊（3楼主要通道）
        for (int x = 5; x < 35; x++) {
            grid[15][x] = 0;
            grid[14][x] = 0;
            grid[16][x] = 0;
        }
        
        // 电梯连接通道
        for (int y = 10; y <= 20; y++) {
            grid[y][35] = 0;
            grid[y][5] = 0;
        }
        
        // 电梯位置
        ensureNodeWalkable(grid, 35, 15);
        ensureNodeWalkable(grid, 5, 15);
        
        return grid;
    }
    
    /**
     * 确保节点周围可通行
     */
    private void ensureNodeWalkable(int[][] grid, int x, int y) {
        if (y >= 0 && y < GRID_HEIGHT && x >= 0 && x < GRID_WIDTH) {
            grid[y][x] = 0;
            // 周围一圈也设为通路
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ny = y + dy;
                    int nx = x + dx;
                    if (ny >= 0 && ny < GRID_HEIGHT && nx >= 0 && nx < GRID_WIDTH) {
                        grid[ny][nx] = 0;
                    }
                }
            }
        }
    }
    
    /**
     * 获取地图配置（默认返回1楼）
     * @return 包含网格和所有节点的配置响应
     */
    public MapConfigResponse getMapConfig() {
        return getMapConfig(1); // 默认返回1楼
    }
    
    /**
     * 获取指定楼层的地图配置
     * @param floorLevel 楼层号
     * @return 包含网格和所有节点的配置响应
     */
    public MapConfigResponse getMapConfig(Integer floorLevel) {
        System.out.println("[MapService] 获取地图配置，楼层: " + floorLevel + "，节点数量: " + (nodes != null ? nodes.size() : 0));
        
        if (floorGridMatrices == null || floorGridMatrices.isEmpty() || nodes == null) {
            System.err.println("[MapService] 警告：地图数据未初始化，重新初始化...");
            initializeMapData();
        }
        
        // 获取指定楼层的网格，如果不存在则使用默认网格
        int[][] gridMatrix = floorGridMatrices.getOrDefault(floorLevel, defaultGridMatrix);
        if (gridMatrix == null) {
            System.err.println("[MapService] 警告：楼层" + floorLevel + "的网格不存在，使用默认网格");
            gridMatrix = defaultGridMatrix;
        }
        
        // 过滤该楼层的节点（只返回当前楼层的节点）
        List<MapNodeDTO> floorNodes = nodes.stream()
                .filter(node -> {
                    // 从数据库查询节点的楼层信息
                    try {
                        MapNode mapNode = mapNodeRepository.findById(node.getNodeId()).orElse(null);
                        if (mapNode != null) {
                            return mapNode.getFloorLevel() != null && mapNode.getFloorLevel().equals(floorLevel);
                        }
                    } catch (Exception e) {
                        System.err.println("[MapService] 查询节点楼层信息失败: " + e.getMessage());
                    }
                    // 如果查询失败，默认返回所有节点
                    return true;
                })
                .collect(Collectors.toList());
        
        MapGridDTO grid = new MapGridDTO(GRID_WIDTH, GRID_HEIGHT, gridMatrix);
        MapConfigResponse response = new MapConfigResponse(grid, floorNodes);
        
        System.out.println("[MapService] 返回配置，楼层: " + floorLevel + ", grid宽度: " + grid.getWidth() + ", 高度: " + grid.getHeight());
        System.out.println("[MapService] 返回该楼层节点数量: " + response.getNodes().size());
        
        return response;
    }
    
    /**
     * 根据locationId查询对应的地图节点
     * @param locationId 诊室ID
     * @return 对应的地图节点，如果不存在则抛出异常
     */
    public MapNodeDTO getNodeByLocationId(Integer locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("locationId不能为空");
        }
        
        return nodes.stream()
                .filter(node -> locationId.equals(node.getLocationId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "未找到locationId为" + locationId + "的地图节点"));
    }
    
    /**
     * 根据nodeId查询节点
     * @param nodeId 节点ID
     * @return 对应的地图节点
     */
    public MapNodeDTO getNodeByNodeId(Integer nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("nodeId不能为空");
        }
        
        return nodes.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "未找到nodeId为" + nodeId + "的地图节点"));
    }
}

