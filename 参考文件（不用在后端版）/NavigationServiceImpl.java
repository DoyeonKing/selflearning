package com.example.springboot.service.impl;

import com.example.springboot.dto.navigation.FloorMapResponse;
import com.example.springboot.dto.navigation.MapEdgeResponse;
import com.example.springboot.dto.navigation.MapNodeResponse;
import com.example.springboot.dto.navigation.NavigationRouteResponse;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Location;
import com.example.springboot.entity.MapEdge;
import com.example.springboot.entity.MapNode;
import com.example.springboot.entity.enums.NodeType;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.LocationRepository;
import com.example.springboot.repository.MapEdgeRepository;
import com.example.springboot.repository.MapNodeRepository;
import com.example.springboot.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NavigationServiceImpl implements NavigationService {

    @Autowired
    private MapNodeRepository nodeRepository;
    @Autowired
    private MapEdgeRepository edgeRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private LocationRepository locationRepository;

    // 加载地图数据
    @Override
    public FloorMapResponse getFloorMap(Integer floorLevel) {
        FloorMapResponse response = new FloorMapResponse();
        response.setFloorLevel(floorLevel);

        // 加载楼层节点
        List<MapNode> nodes = nodeRepository.findByFloorLevel(floorLevel);
        response.setNodes(nodes.stream()
                .map(this::convertToNodeResponse)
                .collect(Collectors.toList()));

        // 加载楼层边（路径）
        List<MapEdge> edges = new ArrayList<>();
        nodes.forEach(node -> {
            edges.addAll(edgeRepository.findByStartNodeId(node.getNodeId()));
            edges.addAll(edgeRepository.findByEndNodeIdAndIsBidirectionalTrue(node.getNodeId()));
        });
        response.setEdges(edges.stream()
                .map(this::convertToEdgeResponse)
                .collect(Collectors.toList()));

        return response;
    }

    // 路径搜索功能
    @Override
    public List<MapNodeResponse> searchNodes(String keyword) {
        return nodeRepository.findByNodeNameContainingIgnoreCase(keyword).stream()
                .map(this::convertToNodeResponse)
                .collect(Collectors.toList());
    }

    // 获取入口节点
    @Override
    public List<MapNodeResponse> getEntranceNodes() {
        return nodeRepository.findByNodeType(NodeType.ENTRANCE).stream()
                .map(this::convertToNodeResponse)
                .collect(Collectors.toList());
    }

    // 预约关联导航
    @Override
    public NavigationRouteResponse findRouteByAppointment(Integer appointmentId) {
        // 1. 获取预约对应的诊室节点ID
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("预约不存在"));

        Location location = appointment.getSchedule().getLocation();
        Integer endNodeId = location.getMapNodeId();
        if (endNodeId == null) {
            throw new RuntimeException("诊室未配置地图节点");
        }

        // 2. 获取默认入口节点（这里取第一个入口）
        List<MapNodeResponse> entrances = getEntranceNodes();
        if (entrances.isEmpty()) {
            throw new RuntimeException("未配置入口节点");
        }
        Integer startNodeId = entrances.get(0).getNodeId();

        // 3. 计算路径
        return findRoute(startNodeId, endNodeId, false);
    }

    // 最短路径算法实现
    @Override
    public NavigationRouteResponse findRoute(Integer startNodeId, Integer endNodeId, Boolean preferAccessible) {
        // 1. 构建图结构
        Map<Integer, List<Edge>> graph = buildGraph(preferAccessible);

        // 2. 执行A*算法计算最短路径
        List<Integer> nodeIds = aStarAlgorithm(graph, startNodeId, endNodeId);
        if (nodeIds == null || nodeIds.size() < 2) {
            throw new RuntimeException("无法计算路径");
        }

        // 3. 转换为完整路径信息
        List<MapNodeResponse> pathNodes = nodeIds.stream()
                .map(id -> convertToNodeResponse(nodeRepository.findById(id).orElse(null)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 4. 生成导航指引
        List<NavigationRouteResponse.NavigationInstruction> instructions = generateInstructions(pathNodes, graph);

        // 5. 计算总距离和时间
        NavigationRouteResponse response = new NavigationRouteResponse();
        response.setPathNodes(pathNodes);
        response.setInstructions(instructions);
        response.setTotalDistance(calculateTotalDistance(instructions));
        response.setTotalWalkTime(calculateTotalTime(instructions));
        response.setFormattedWalkTime(formatTime(response.getTotalWalkTime()));

        return response;
    }

    // A*算法实现
    private List<Integer> aStarAlgorithm(Map<Integer, List<Edge>> graph, Integer start, Integer end) {
        // 启发函数：欧氏距离估算
        Map<Integer, Double> heuristic = new HashMap<>();
        MapNode endNode = nodeRepository.findById(end).orElse(null);
        if (endNode != null) {
            graph.keySet().forEach(nodeId -> {
                MapNode node = nodeRepository.findById(nodeId).orElse(null);
                if (node != null) {
                    double dx = node.getCoordinatesX().subtract(endNode.getCoordinatesX()).doubleValue();
                    double dy = node.getCoordinatesY().subtract(endNode.getCoordinatesY()).doubleValue();
                    heuristic.put(nodeId, Math.sqrt(dx*dx + dy*dy));
                }
            });
        }

        // 开放列表和关闭列表
        PriorityQueue<NodeScore> openList = new PriorityQueue<>(Comparator.comparingDouble(a -> a.fScore));
        Set<Integer> closedList = new HashSet<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Double> gScore = new HashMap<>();

        // 初始化
        gScore.put(start, 0.0);
        openList.add(new NodeScore(start, heuristic.getOrDefault(start, 0.0)));

        while (!openList.isEmpty()) {
            Integer current = openList.poll().nodeId;

            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }

            closedList.add(current);

            for (Edge edge : graph.getOrDefault(current, Collections.emptyList())) {
                Integer neighbor = edge.target;
                if (closedList.contains(neighbor)) continue;

                double tentativeGScore = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + edge.weight;
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic.getOrDefault(neighbor, 0.0);
                    openList.add(new NodeScore(neighbor, fScore));
                }
            }
        }
        return null; // 无路径
    }

    // 生成导航指引
    private List<NavigationRouteResponse.NavigationInstruction> generateInstructions(
            List<MapNodeResponse> pathNodes, Map<Integer, List<Edge>> graph) {

        List<NavigationRouteResponse.NavigationInstruction> instructions = new ArrayList<>();

        for (int i = 0; i < pathNodes.size() - 1; i++) {
            MapNodeResponse current = pathNodes.get(i);
            MapNodeResponse next = pathNodes.get(i + 1);

            // 查找当前到下一个节点的边
            Edge edge = graph.get(current.getNodeId()).stream()
                    .filter(e -> e.target.equals(next.getNodeId()))
                    .findFirst().orElse(null);

            if (edge == null) continue;

            NavigationRouteResponse.NavigationInstruction instruction = new NavigationRouteResponse.NavigationInstruction();
            instruction.setStep(i + 1);
            instruction.setCurrentNode(current);
            instruction.setNextNode(next);
            instruction.setDistance(edge.distance != null ? edge.distance.doubleValue() : 0.0);
            instruction.setWalkTime(edge.walkTime);

            // 生成指引文本 - 传递当前索引 i
            instruction.setInstruction(generateInstructionText(current, next, edge, i, pathNodes.size()));
            instructions.add(instruction);
        }

        return instructions;
    }

    // 生成指引文本 - 修复：添加 i 和 totalSteps 参数
    private String generateInstructionText(MapNodeResponse current, MapNodeResponse next, Edge edge,
                                           int currentIndex, int totalSteps) {
        String direction = getDirection(current, next);
        String nodeTypeDesc = getNodeTypeDescription(next.getNodeType());

        // 使用 doubleValue() 将 BigDecimal 转换为 double
        double distance = edge.distance.doubleValue();

        if (currentIndex == 0) {
            return String.format("从%s出发，%s走%.1f米到%s",
                    current.getNodeName(), direction, distance, nodeTypeDesc);
        } else if (currentIndex == totalSteps - 2) {
            return String.format("最后%s走%.1f米到达目的地：%s",
                    direction, distance, next.getNodeName());
        } else {
            return String.format("%s走%.1f米到%s",
                    direction, distance, nodeTypeDesc);
        }
    }

    // 辅助方法：构建图结构
    private Map<Integer, List<Edge>> buildGraph(Boolean preferAccessible) {
        Map<Integer, List<Edge>> graph = new HashMap<>();
        List<MapEdge> edges = edgeRepository.findAll();

        for (MapEdge edge : edges) {
            // 处理无障碍偏好
            if (preferAccessible && !isAccessibleEdge(edge)) {
                continue;
            }

            // 添加正向边 - 确保正确的类型转换（使用doubleValue()方法）
            graph.computeIfAbsent(edge.getStartNodeId(), k -> new ArrayList<>())
                    .add(new Edge(edge.getEndNodeId(),
                            edge.getDistance() != null ? edge.getDistance().doubleValue() : 0.0,
                            edge.getDistance(),  // 这里如果需要保留BigDecimal原始值可以继续传
                            edge.getWalkTime()));

            // 双向边添加反向边
            if (edge.getIsBidirectional()) {
                graph.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>())
                        .add(new Edge(edge.getStartNodeId(),
                                edge.getDistance() != null ? edge.getDistance().doubleValue() : 0.0,
                                edge.getDistance(),  // 同上
                                edge.getWalkTime()));
            }
        }
        return graph;
    }
    // 辅助方法：判断边是否为无障碍通道
    private boolean isAccessibleEdge(MapEdge edge) {
        return "accessible".equals(edge.getAccessibilityInfo()) ||
                edge.getAccessibilityInfo() == null;
    }

    // 辅助方法：计算总距离
    private Double calculateTotalDistance(List<NavigationRouteResponse.NavigationInstruction> instructions) {
        return instructions.stream()
                .mapToDouble(instruction -> instruction.getDistance().doubleValue()) // 使用 doubleValue() 方法
                .sum();
    }

    // 辅助方法：计算总时间
    private Integer calculateTotalTime(List<NavigationRouteResponse.NavigationInstruction> instructions) {
        return instructions.stream()
                .mapToInt(NavigationRouteResponse.NavigationInstruction::getWalkTime)
                .sum();
    }

    // 辅助方法：格式化时间
    private String formatTime(Integer seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else {
            return (seconds / 60) + "分" + (seconds % 60) + "秒";
        }
    }

    // 辅助方法：获取方向描述
    private String getDirection(MapNodeResponse current, MapNodeResponse next) {
        BigDecimal dx = next.getCoordinatesX().subtract(current.getCoordinatesX());
        BigDecimal dy = next.getCoordinatesY().subtract(current.getCoordinatesY());

        if (dx.compareTo(BigDecimal.ZERO) > 0) {
            return "向东";
        } else if (dx.compareTo(BigDecimal.ZERO) < 0) {
            return "向西";
        } else if (dy.compareTo(BigDecimal.ZERO) > 0) {
            return "向北";
        } else {
            return "向南";
        }
    }

    private String getNodeTypeDescription(NodeType type) {
        switch (type) {
            case ENTRANCE: return "入口";
            case EXIT: return "出口";
            case DEPARTMENT: return "科室";
            case DOCTOR_ROOM: return "诊室";
            case REGISTRATION: return "挂号处";
            case PHARMACY: return "药房";
            case PAYMENT: return "缴费处";
            case LABORATORY: return "化验室";
            case IMAGING: return "影像科";
            case NURSE_STATION: return "护士站";
            case RESTROOM: return "卫生间";
            case ELEVATOR: return "电梯";
            case STAIR: return "楼梯";
            case HALL: return "大厅";
            case CORRIDOR: return "走廊";
            default: return type.name();
        }
    }


    // 内部类：路径重建
    private List<Integer> reconstructPath(Map<Integer, Integer> cameFrom, Integer current) {
        List<Integer> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    // 内部类：图边缘
    private static class Edge {
        Integer target;
        double weight;
        BigDecimal distance;
        Integer walkTime;

        Edge(Integer target, double weight, BigDecimal distance, Integer walkTime) {
            this.target = target;
            this.weight = weight;
            this.distance = distance;
            this.walkTime = walkTime;
        }
    }

    // 内部类：A*算法评分节点
    private static class NodeScore {
        Integer nodeId;
        double fScore;

        NodeScore(Integer nodeId, double fScore) {
            this.nodeId = nodeId;
            this.fScore = fScore;
        }
    }

    // 实体转DTO
    private MapNodeResponse convertToNodeResponse(MapNode node) {
        if (node == null) return null;
        MapNodeResponse response = new MapNodeResponse();
        response.setNodeId(node.getNodeId());
        response.setNodeName(node.getNodeName());
        response.setNodeType(node.getNodeType());
        response.setCoordinatesX(node.getCoordinatesX());
        response.setCoordinatesY(node.getCoordinatesY());
        response.setFloorLevel(node.getFloorLevel());
        response.setDescription(node.getDescription());
        response.setIsAccessible(node.getIsAccessible());
        return response;
    }

    private MapEdgeResponse convertToEdgeResponse(MapEdge edge) {
        MapEdgeResponse response = new MapEdgeResponse();
        response.setEdgeId(edge.getEdgeId());
        response.setStartNodeId(edge.getStartNodeId());
        response.setEndNodeId(edge.getEndNodeId());
        response.setDistance(edge.getDistance());
        response.setWalkTime(edge.getWalkTime());
        response.setIsBidirectional(edge.getIsBidirectional());
        response.setAccessibilityInfo(edge.getAccessibilityInfo());
        return response;
    }
}