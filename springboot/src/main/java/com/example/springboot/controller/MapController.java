package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.dto.map.MapConfigResponse;
import com.example.springboot.dto.map.MapNodeDTO;
import com.example.springboot.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地图控制器
 * 提供地图配置和节点查询接口
 */
@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "*")
public class MapController {
    
    private final MapService mapService;
    
    @Autowired
    public MapController(MapService mapService) {
        this.mapService = mapService;
    }
    
    /**
     * 获取地图配置（支持指定楼层）
     * 返回网格数据和所有节点信息
     * 
     * @param floorLevel 楼层号（可选，默认1楼）
     * @return 地图配置响应
     */
    @GetMapping("/config")
    public ResponseEntity<Result> getMapConfig(@RequestParam(required = false, defaultValue = "1") Integer floorLevel) {
        try {
            System.out.println("[MapController] 收到获取地图配置请求，楼层: " + floorLevel);
            MapConfigResponse config = mapService.getMapConfig(floorLevel);
            System.out.println("[MapController] 地图配置获取成功，准备返回");
            System.out.println("[MapController] 配置数据: grid=" + (config.getGrid() != null) + ", nodes=" + (config.getNodes() != null ? config.getNodes().size() : 0));
            return ResponseEntity.ok(Result.success(config));
        } catch (Exception e) {
            System.err.println("[MapController] 获取地图配置失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("500", "获取地图配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据诊室ID查询对应的地图节点坐标
     * 
     * @param locationId 诊室ID
     * @return 对应的地图节点信息
     */
    @GetMapping("/target/{locationId}")
    public ResponseEntity<Result> getTargetNode(@PathVariable Integer locationId) {
        try {
            MapNodeDTO node = mapService.getNodeByLocationId(locationId);
            return ResponseEntity.ok(Result.success(node));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "查询目标节点失败: " + e.getMessage()));
        }
    }
}

