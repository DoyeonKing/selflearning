package com.example.springboot.dto.navigation;

import com.example.springboot.entity.enums.NodeType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MapNodeResponse {
    private Integer nodeId;
    private String nodeName;
    private NodeType nodeType;
    private BigDecimal coordinatesX;
    private BigDecimal coordinatesY;
    private Integer floorLevel;
    private String description;
    private Boolean isAccessible;
}


