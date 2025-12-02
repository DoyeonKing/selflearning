package com.example.springboot.dto.map;

import lombok.Data;
import java.util.List;

/**
 * 地图配置响应DTO
 * 包含网格数据和所有节点信息
 */
@Data
public class MapConfigResponse {
    /**
     * 地图网格数据
     */
    private MapGridDTO grid;
    
    /**
     * 所有节点列表
     */
    private List<MapNodeDTO> nodes;
    
    public MapConfigResponse() {
    }
    
    public MapConfigResponse(MapGridDTO grid, List<MapNodeDTO> nodes) {
        this.grid = grid;
        this.nodes = nodes;
    }
}


















