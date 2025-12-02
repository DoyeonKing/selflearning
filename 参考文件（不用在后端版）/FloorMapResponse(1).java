package com.example.springboot.dto.navigation;

import lombok.Data;

import java.util.List;

@Data
public class FloorMapResponse {
    /**
     * 楼层号
     */
    private Integer floorLevel;

    /**
     * 该楼层的所有节点
     */
    private List<MapNodeResponse> nodes;

    /**
     * 该楼层的所有边（路径）
     */
    private List<MapEdgeResponse> edges;
}


