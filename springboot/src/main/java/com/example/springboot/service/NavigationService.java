package com.example.springboot.service;

import com.example.springboot.entity.MapNode;
import com.example.springboot.entity.MapEdge;
import com.example.springboot.repository.MapNodeRepository;
import com.example.springboot.repository.MapEdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 导航服务
 * 基于map_edges表进行路径规划和导航指引生成
 */
@Service
public class NavigationService {
    
    @Autowired
    private MapNodeRepository mapNodeRepository;
    
    @Autowired
    private MapEdgeRepository mapEdgeRepository;
    
    /**
     * 路径规划结果
     */
    public static class NavigationPath {
        private List<PathStep> steps; // 路径步骤
        private double totalDistance; // 总距离（米）
        private int totalTime; // 总时间（秒）
        
        public NavigationPath(List<PathStep> steps, double totalDistance, int totalTime) {
            this.steps = steps;
            this.totalDistance = totalDistance;
            this.totalTime = totalTime;
        }
        
        // Getters
        public List<PathStep> getSteps() { return steps; }
        public double getTotalDistance() { return totalDistance; }
        public int getTotalTime() { return totalTime; }
    }
    
    /**
     * 路径步骤
     */
    public static class PathStep {
        private Integer fromNodeId;
        private String fromNodeName;
        private Integer toNodeId;
        private String toNodeName;
        private double distance; // 距离（米）
        private int walkTime; // 步行时间（秒）
        private String instruction; // 导航指引
        private Integer fromFloor;
        private Integer toFloor;
        private String nodeType; // 节点类型（用于判断是否需要特殊提示）
        
        public PathStep(Integer fromNodeId, String fromNodeName, Integer toNodeId, String toNodeName,
                       double distance, int walkTime, String instruction, 
                       Integer fromFloor, Integer toFloor, String nodeType) {
            this.fromNodeId = fromNodeId;
            this.fromNodeName = fromNodeName;
            this.toNodeId = toNodeId;
            this.toNodeName = toNodeName;
            this.distance = distance;
            this.walkTime = walkTime;
            this.instruction = instruction;
            this.fromFloor = fromFloor;
            this.toFloor = toFloor;
            this.nodeType = nodeType;
        }
        
        // Getters
        public Integer getFromNodeId() { return fromNodeId; }
        public String getFromNodeName() { return fromNodeName; }
        public Integer getToNodeId() { return toNodeId; }
        public String getToNodeName() { return toNodeName; }
        public double getDistance() { return distance; }
        public int getWalkTime() { return walkTime; }
        public String getInstruction() { return instruction; }
        public Integer getFromFloor() { return fromFloor; }
        public Integer getToFloor() { return toFloor; }
        public String getNodeType() { return nodeType; }
    }
    
    /**
     * 计算从起点到终点的导航路径（使用Dijkstra算法）
     * 
     * @param startNodeId 起点节点ID
     * @param endNodeId 终点节点ID
     * @return 导航路径
     */
    public NavigationPath calculatePath(Integer startNodeId, Integer endNodeId) {
        // 获取所有节点和边
        List<MapNode> allNodes = mapNodeRepository.findAll();
        List<MapEdge> allEdges = mapEdgeRepository.findAll();
        
        // 构建图（邻接表）
        Map<Integer, List<MapEdge>> graph = new HashMap<>();
        Map<Integer, MapNode> nodeMap = new HashMap<>();
        
        for (MapNode node : allNodes) {
            nodeMap.put(node.getNodeId(), node);
            graph.put(node.getNodeId(), new ArrayList<>());
        }
        
        for (MapEdge edge : allEdges) {
            graph.get(edge.getStartNodeId()).add(edge);
            // 如果是双向的，添加反向边
            if (edge.getIsBidirectional()) {
                MapEdge reverseEdge = new MapEdge();
                reverseEdge.setStartNodeId(edge.getEndNodeId());
                reverseEdge.setEndNodeId(edge.getStartNodeId());
                reverseEdge.setDistance(edge.getDistance());
                reverseEdge.setWalkTime(edge.getWalkTime());
                reverseEdge.setIsBidirectional(true);
                graph.get(edge.getEndNodeId()).add(reverseEdge);
            }
        }
        
        // Dijkstra算法
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, MapEdge> prev = new HashMap<>();
        PriorityQueue<Map.Entry<Integer, Double>> pq = new PriorityQueue<>(
            Map.Entry.<Integer, Double>comparingByValue()
        );
        
        // 初始化
        for (Integer nodeId : graph.keySet()) {
            dist.put(nodeId, Double.MAX_VALUE);
        }
        dist.put(startNodeId, 0.0);
        pq.offer(new AbstractMap.SimpleEntry<>(startNodeId, 0.0));
        
        // 主循环
        while (!pq.isEmpty()) {
            Map.Entry<Integer, Double> current = pq.poll();
            Integer currentNodeId = current.getKey();
            double currentDist = current.getValue();
            
            if (currentDist > dist.get(currentNodeId)) {
                continue;
            }
            
            if (currentNodeId.equals(endNodeId)) {
                break; // 找到终点
            }
            
            // 遍历邻居
            for (MapEdge edge : graph.get(currentNodeId)) {
                Integer neighborId = edge.getEndNodeId();
                double newDist = currentDist + edge.getDistance();
                
                if (newDist < dist.get(neighborId)) {
                    dist.put(neighborId, newDist);
                    prev.put(neighborId, edge);
                    pq.offer(new AbstractMap.SimpleEntry<>(neighborId, newDist));
                }
            }
        }
        
        // 如果无法到达终点
        if (dist.get(endNodeId) == Double.MAX_VALUE) {
            throw new RuntimeException("无法找到从节点" + startNodeId + "到节点" + endNodeId + "的路径");
        }
        
        // 重构路径
        List<PathStep> steps = new ArrayList<>();
        Integer currentNodeId = endNodeId;
        double totalDistance = 0;
        int totalTime = 0;
        
        while (currentNodeId != null && !currentNodeId.equals(startNodeId)) {
            MapEdge edge = prev.get(currentNodeId);
            if (edge == null) break;
            
            MapNode fromNode = nodeMap.get(edge.getStartNodeId());
            MapNode toNode = nodeMap.get(edge.getEndNodeId());
            
            // 只有整体路径的最后一个节点才是真正的“目的地”
            boolean isFinalStep = toNode != null && toNode.getNodeId() != null
                    && toNode.getNodeId().equals(endNodeId);
            
            String instruction = generateInstruction(fromNode, toNode, edge, isFinalStep);
            
            PathStep step = new PathStep(
                fromNode.getNodeId(),
                fromNode.getNodeName(),
                toNode.getNodeId(),
                toNode.getNodeName(),
                edge.getDistance(),
                edge.getWalkTime(),
                instruction,
                fromNode.getFloorLevel(),
                toNode.getFloorLevel(),
                toNode.getNodeType() != null ? toNode.getNodeType().name() : null
            );
            
            steps.add(0, step); // 插入到开头
            totalDistance += edge.getDistance();
            totalTime += edge.getWalkTime();
            
            currentNodeId = edge.getStartNodeId();
        }
        
        return new NavigationPath(steps, totalDistance, totalTime);
    }
    
    /**
     * 生成导航指引（增强版：更人性化的指引）
     */
    private String generateInstruction(MapNode fromNode, MapNode toNode, MapEdge edge, boolean isFinalStep) {
        // 判断楼层变化
        if (!fromNode.getFloorLevel().equals(toNode.getFloorLevel())) {
            int floorDiff = toNode.getFloorLevel() - fromNode.getFloorLevel();
            String direction = floorDiff > 0 ? "上" : "下";
            String method = "电梯";
            
            // 判断是电梯还是楼梯
            if (toNode.getNodeType() == MapNode.NodeType.STAIRS) {
                method = "楼梯";
                return String.format("🚶 走%s到%d楼，预计%d秒", method, toNode.getFloorLevel(), edge.getWalkTime());
            } else if (toNode.getNodeType() == MapNode.NodeType.ELEVATOR) {
                method = "电梯";
                return String.format("🛗 乘坐%s%s到%d楼", method, direction, toNode.getFloorLevel());
            }
            
            return String.format("%s%s到%d楼", method, direction, toNode.getFloorLevel());
        }
        
        // 同楼层移动 - 计算方向和距离
        double distance = edge.getDistance();
        int walkTime = edge.getWalkTime();
        String distanceText = distance < 1 ? "几步" : String.format("约%.0f米", distance);
        String timeText = walkTime < 60 ? String.format("%d秒", walkTime) : String.format("%.1f分钟", walkTime / 60.0);
        
        // 计算详细方向（八个方向）
        String direction = calculateDetailedDirection(fromNode, toNode);
        
        // 根据节点类型生成人性化指引
        if (toNode.getNodeType() == MapNode.NodeType.ELEVATOR) {
            return String.format("🧭 %s走%s到【%s】，预计%s", direction, distanceText, toNode.getNodeName(), timeText);
        } else if (toNode.getNodeType() == MapNode.NodeType.STAIRS) {
            return String.format("🧭 %s走%s到【%s】，预计%s", direction, distanceText, toNode.getNodeName(), timeText);
        } else if (toNode.getNodeType() == MapNode.NodeType.ROOM) {
            // 只有整条路径的最后一个房间才是“目的地”，中间经过的房间不应该被标记为目的地
            if (isFinalStep) {
                return String.format("🎯 %s走%s，即可到达【%s】（目的地）", direction, distanceText, toNode.getNodeName());
            } else {
                return String.format("🧭 %s走%s，经过【%s】", direction, distanceText, toNode.getNodeName());
            }
        } else if (toNode.getNodeType() == MapNode.NodeType.HALLWAY) {
            // 走廊节点，提供路标信息
            return String.format("🧭 沿走廊%s走%s，经过【%s】", direction, distanceText, toNode.getNodeName());
        } else if (toNode.getNodeType() == MapNode.NodeType.ENTRANCE) {
            return String.format("🚪 从【%s】进入，然后%s走%s", toNode.getNodeName(), direction, distanceText);
        } else {
            return String.format("🧭 %s走%s到【%s】", direction, distanceText, toNode.getNodeName());
        }
    }
    
    /**
     * 计算详细方向（八个方向：前、后、左、右、左前、右前、左后、右后）
     */
    private String calculateDetailedDirection(MapNode fromNode, MapNode toNode) {
        double dx = toNode.getCoordinatesX() - fromNode.getCoordinatesX();
        double dy = toNode.getCoordinatesY() - fromNode.getCoordinatesY();
        
        // 计算角度（以正北为0度，顺时针）
        double angle = Math.atan2(dx, -dy) * 180 / Math.PI;
        if (angle < 0) angle += 360;
        
        // 根据角度判断方向（八个方向）
        if (angle >= 337.5 || angle < 22.5) {
            return "向前";
        } else if (angle >= 22.5 && angle < 67.5) {
            return "右前方";
        } else if (angle >= 67.5 && angle < 112.5) {
            return "向右";
        } else if (angle >= 112.5 && angle < 157.5) {
            return "右后方";
        } else if (angle >= 157.5 && angle < 202.5) {
            return "向后";
        } else if (angle >= 202.5 && angle < 247.5) {
            return "左后方";
        } else if (angle >= 247.5 && angle < 292.5) {
            return "向左";
        } else {
            return "左前方";
        }
    }
    
    /**
     * 计算简单方向（四个方向：前、后、左、右）
     */
    private String calculateSimpleDirection(MapNode fromNode, MapNode toNode) {
        double dx = toNode.getCoordinatesX() - fromNode.getCoordinatesX();
        double dy = toNode.getCoordinatesY() - fromNode.getCoordinatesY();
        
        // 判断主要方向
        if (Math.abs(dx) > Math.abs(dy)) {
            // 主要是水平移动
            if (dx > 0) {
                return "向右";
            } else {
                return "向左";
            }
        } else {
            // 主要是垂直移动
            if (dy > 0) {
                return "向前";
            } else {
                return "向后";
            }
        }
    }
    
    /**
     * 获取下一步指引（用于实时导航）
     */
    public PathStep getNextStep(Integer currentNodeId, Integer targetNodeId) {
        NavigationPath path = calculatePath(currentNodeId, targetNodeId);
        if (path.getSteps().isEmpty()) {
            return null;
        }
        return path.getSteps().get(0); // 返回第一步
    }
}

