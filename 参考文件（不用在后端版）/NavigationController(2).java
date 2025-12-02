package com.example.springboot.controller;

import com.example.springboot.dto.navigation.*;
import com.example.springboot.entity.MapNode;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.MapNodeRepository;
import com.example.springboot.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;
    private final MapNodeRepository nodeRepository;  // 添加Repository声明

    @Autowired  // 构造函数注入两个依赖
    public NavigationController(NavigationService navigationService, MapNodeRepository mapNodeRepository) {
        this.navigationService = navigationService;
        this.nodeRepository = mapNodeRepository;  // 初始化Repository
    }

    private MapNodeResponse convertToNodeResponse(MapNode node) {
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


    /**
     * 计算两点之间的最短路径
     * GET /api/navigation/route?startNodeId={start}&endNodeId={end}&preferAccessible={prefer}
     */
    @GetMapping("/route")
    public ResponseEntity<NavigationRouteResponse> findRoute(
            @RequestParam Integer startNodeId,
            @RequestParam Integer endNodeId,
            @RequestParam(required = false, defaultValue = "false") Boolean preferAccessible) {
        NavigationRouteResponse response = navigationService.findRoute(startNodeId, endNodeId, preferAccessible);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据预约ID获取导航路径（从入口到诊室）
     * GET /api/navigation/route-by-appointment/{appointmentId}
     */
    @GetMapping("/route-by-appointment/{appointmentId}")
    public ResponseEntity<NavigationRouteResponse> findRouteByAppointment(
            @PathVariable Integer appointmentId) {
        NavigationRouteResponse response = navigationService.findRouteByAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定楼层的地图数据
     * GET /api/navigation/floor-map/{floorLevel}
     */
    @GetMapping("/floor-map/{floorLevel}")
    public ResponseEntity<FloorMapResponse> getFloorMap(@PathVariable Integer floorLevel) {
        FloorMapResponse response = navigationService.getFloorMap(floorLevel);
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索地点
     * GET /api/navigation/search?keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<List<MapNodeResponse>> searchNodes(@RequestParam String keyword) {
        List<MapNodeResponse> nodes = navigationService.searchNodes(keyword);
        return ResponseEntity.ok(nodes);
    }

    /**
     * 获取所有入口节点（通常作为导航起点）
     * GET /api/navigation/entrances
     */
    @GetMapping("/entrances")
    public ResponseEntity<List<MapNodeResponse>> getEntranceNodes() {
        List<MapNodeResponse> nodes = navigationService.getEntranceNodes();
        return ResponseEntity.ok(nodes);
    }

    @PostMapping("/scan-location")
    public ResponseEntity<LocationScanResponse> scanLocation(@RequestBody LocationScanRequest request) {
        String qrCode = request.getQrCode();
        // 解析二维码格式：LOCATION_NODE_{node_id}
        if (qrCode.matches("LOCATION_NODE_\\d+")) {
            Integer nodeId = Integer.parseInt(qrCode.split("_")[2]);
            MapNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new ResourceNotFoundException("节点不存在"));
            LocationScanResponse response = new LocationScanResponse();
            response.setCurrentNode(convertToNodeResponse(node));
            response.setMessage("定位成功");
            return ResponseEntity.ok(response);
        }
        throw new BadRequestException("无效的二维码格式");
    }

}

