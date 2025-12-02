package com.example.springboot.service;

import com.example.springboot.dto.navigation.NavigationRouteResponse;
import com.example.springboot.dto.navigation.FloorMapResponse;
import com.example.springboot.dto.navigation.MapNodeResponse;
import java.util.List;

public interface NavigationService {

    // 计算两点之间的最短路径
    NavigationRouteResponse findRoute(Integer startNodeId, Integer endNodeId, Boolean preferAccessible);

    // 根据预约ID获取导航路径
    NavigationRouteResponse findRouteByAppointment(Integer appointmentId);

    // 获取指定楼层的地图数据
    FloorMapResponse getFloorMap(Integer floorLevel);

    // 搜索地点
    List<MapNodeResponse> searchNodes(String keyword);

    // 获取所有入口节点
    List<MapNodeResponse> getEntranceNodes();
}