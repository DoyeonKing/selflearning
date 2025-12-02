package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.MapNode;
import com.example.springboot.service.MapNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地图节点管理控制器
 * 用于管理途径点、设置节点连接关系等
 */
@RestController
@RequestMapping("/api/map/nodes")
@CrossOrigin(origins = "*")
public class MapNodeController {
    
    @Autowired
    private MapNodeService mapNodeService;
    
    /**
     * 获取所有地图节点（包括途径点）
     * GET /api/map/nodes
     */
    @GetMapping("")
    public ResponseEntity<Result> getAllNodes() {
        try {
            List<MapNode> nodes = mapNodeService.getAllNodes();
            return ResponseEntity.ok(Result.success(nodes));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取节点列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据节点类型获取节点列表
     * GET /api/map/nodes/type/{nodeType}
     * nodeType: room, hallway, elevator, stairs, entrance
     */
    @GetMapping("/type/{nodeType}")
    public ResponseEntity<Result> getNodesByType(@PathVariable String nodeType) {
        try {
            MapNode.NodeType type = MapNode.NodeType.valueOf(nodeType.toUpperCase());
            List<MapNode> nodes = mapNodeService.getNodesByType(type);
            return ResponseEntity.ok(Result.success(nodes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", "无效的节点类型: " + nodeType));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取节点列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有途径点（走廊、楼梯、电梯等非房间节点）
     * GET /api/map/nodes/waypoints
     */
    @GetMapping("/waypoints")
    public ResponseEntity<Result> getWaypoints() {
        try {
            List<MapNode> waypoints = mapNodeService.getWaypoints();
            return ResponseEntity.ok(Result.success(waypoints));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取途径点列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建新的地图节点（途径点）
     * POST /api/map/nodes
     */
    @PostMapping("")
    public ResponseEntity<Result> createNode(@RequestBody Map<String, Object> nodeData) {
        try {
            MapNode node = mapNodeService.createNode(nodeData);
            return ResponseEntity.ok(Result.success(node));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("400", "创建节点失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新节点信息
     * PUT /api/map/nodes/{nodeId}
     */
    @PutMapping("/{nodeId}")
    public ResponseEntity<Result> updateNode(@PathVariable Integer nodeId, 
                                             @RequestBody Map<String, Object> nodeData) {
        try {
            MapNode node = mapNodeService.updateNode(nodeId, nodeData);
            return ResponseEntity.ok(Result.success(node));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("400", "更新节点失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除节点
     * DELETE /api/map/nodes/{nodeId}
     */
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Result> deleteNode(@PathVariable Integer nodeId) {
        try {
            mapNodeService.deleteNode(nodeId);
            return ResponseEntity.ok(Result.success("节点删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("400", "删除节点失败: " + e.getMessage()));
        }
    }
}



