package com.example.springboot.service;

import com.example.springboot.entity.MapNode;
import com.example.springboot.repository.MapNodeRepository;
import com.example.springboot.repository.MapEdgeRepository;
import com.example.springboot.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地图节点服务
 * 用于管理途径点和节点连接关系
 */
@Service
public class MapNodeService {
    
    @Autowired
    private MapNodeRepository mapNodeRepository;
    
    @Autowired
    private MapEdgeRepository mapEdgeRepository;
    
    /**
     * 获取所有节点
     */
    public List<MapNode> getAllNodes() {
        return mapNodeRepository.findAll();
    }
    
    /**
     * 根据节点类型获取节点列表
     */
    public List<MapNode> getNodesByType(MapNode.NodeType nodeType) {
        return mapNodeRepository.findAll().stream()
                .filter(node -> node.getNodeType() == nodeType)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有途径点（非房间节点）
     * 途径点包括：走廊、电梯、楼梯、入口等
     */
    public List<MapNode> getWaypoints() {
        return mapNodeRepository.findAll().stream()
                .filter(node -> node.getNodeType() != MapNode.NodeType.ROOM)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建新节点
     */
    @Transactional
    public MapNode createNode(Map<String, Object> nodeData) {
        MapNode node = new MapNode();
        
        // 设置节点名称
        if (nodeData.containsKey("nodeName")) {
            node.setNodeName((String) nodeData.get("nodeName"));
        } else {
            throw new IllegalArgumentException("节点名称不能为空");
        }
        
        // 设置节点类型
        if (nodeData.containsKey("nodeType")) {
            String typeStr = nodeData.get("nodeType").toString().toUpperCase();
            try {
                node.setNodeType(MapNode.NodeType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的节点类型: " + typeStr);
            }
        } else {
            node.setNodeType(MapNode.NodeType.HALLWAY); // 默认为走廊
        }
        
        // 设置坐标
        if (nodeData.containsKey("coordinatesX")) {
            node.setCoordinatesX(Double.parseDouble(nodeData.get("coordinatesX").toString()));
        } else {
            throw new IllegalArgumentException("X坐标不能为空");
        }
        
        if (nodeData.containsKey("coordinatesY")) {
            node.setCoordinatesY(Double.parseDouble(nodeData.get("coordinatesY").toString()));
        } else {
            throw new IllegalArgumentException("Y坐标不能为空");
        }
        
        // 设置楼层
        if (nodeData.containsKey("floorLevel")) {
            node.setFloorLevel(Integer.parseInt(nodeData.get("floorLevel").toString()));
        } else {
            node.setFloorLevel(1); // 默认1楼
        }
        
        // 设置描述
        if (nodeData.containsKey("description")) {
            node.setDescription((String) nodeData.get("description"));
        }
        
        // 设置是否可通行
        if (nodeData.containsKey("isAccessible")) {
            node.setIsAccessible(Boolean.parseBoolean(nodeData.get("isAccessible").toString()));
        } else {
            node.setIsAccessible(true);
        }
        
        return mapNodeRepository.save(node);
    }
    
    /**
     * 更新节点信息
     */
    @Transactional
    public MapNode updateNode(Integer nodeId, Map<String, Object> nodeData) {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在，ID: " + nodeId));
        
        // 更新节点名称
        if (nodeData.containsKey("nodeName")) {
            node.setNodeName((String) nodeData.get("nodeName"));
        }
        
        // 更新节点类型
        if (nodeData.containsKey("nodeType")) {
            String typeStr = nodeData.get("nodeType").toString().toUpperCase();
            try {
                node.setNodeType(MapNode.NodeType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的节点类型: " + typeStr);
            }
        }
        
        // 更新坐标
        if (nodeData.containsKey("coordinatesX")) {
            node.setCoordinatesX(Double.parseDouble(nodeData.get("coordinatesX").toString()));
        }
        
        if (nodeData.containsKey("coordinatesY")) {
            node.setCoordinatesY(Double.parseDouble(nodeData.get("coordinatesY").toString()));
        }
        
        // 更新楼层
        if (nodeData.containsKey("floorLevel")) {
            node.setFloorLevel(Integer.parseInt(nodeData.get("floorLevel").toString()));
        }
        
        // 更新描述
        if (nodeData.containsKey("description")) {
            node.setDescription((String) nodeData.get("description"));
        }
        
        // 更新是否可通行
        if (nodeData.containsKey("isAccessible")) {
            node.setIsAccessible(Boolean.parseBoolean(nodeData.get("isAccessible").toString()));
        }
        
        return mapNodeRepository.save(node);
    }
    
    /**
     * 删除节点
     */
    @Transactional
    public void deleteNode(Integer nodeId) {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在，ID: " + nodeId));
        
        // 删除与该节点相关的所有路径
        mapEdgeRepository.deleteByStartNodeId(nodeId);
        mapEdgeRepository.deleteByEndNodeId(nodeId);
        
        // 删除节点
        mapNodeRepository.delete(node);
    }
}



