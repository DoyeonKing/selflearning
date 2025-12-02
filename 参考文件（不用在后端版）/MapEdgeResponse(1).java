package com.example.springboot.dto.navigation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MapEdgeResponse {
    private Integer edgeId;
    private Integer startNodeId;
    private Integer endNodeId;
    private BigDecimal distance;
    private Integer walkTime;
    private Boolean isBidirectional;
    private String accessibilityInfo;
}


