package com.example.springboot.entity;

import com.example.springboot.entity.enums.NodeType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "map_edge")
@Data
public class MapEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer edgeId;
    private Integer startNodeId;
    private Integer endNodeId;
    private BigDecimal distance;
    private Integer walkTime;
    private Boolean isBidirectional;
    private String accessibilityInfo;
}