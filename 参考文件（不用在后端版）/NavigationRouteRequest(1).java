package com.example.springboot.dto.navigation;

import lombok.Data;

@Data
public class NavigationRouteRequest {
    /**
     * 起始节点ID
     */
    private Integer startNodeId;

    /**
     * 目标节点ID
     */
    private Integer endNodeId;

    /**
     * 是否优先使用无障碍通道（默认false）
     */
    private Boolean preferAccessible = false;
}


