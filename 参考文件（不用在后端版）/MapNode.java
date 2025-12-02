package com.example.springboot.entity;

import com.example.springboot.entity.enums.NodeType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "map_node")
@Data
public class MapNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer nodeId;
    private String nodeName;
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;
    @Column(name = "coordinates_x")
    private BigDecimal coordinatesX;
    @Column(name = "coordinates_y")
    private BigDecimal coordinatesY;
    private Integer floorLevel;
    private String description;
    private Boolean isAccessible;
}