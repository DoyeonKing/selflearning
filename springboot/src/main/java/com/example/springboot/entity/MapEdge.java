package com.example.springboot.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 地图路径实体类
 * 对应数据库表 map_edges
 * 用于存储节点之间的连接关系，支持路径规划
 */
@Entity
@Table(name = "map_edges")
@Data
public class MapEdge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edge_id")
    private Integer edgeId;
    
    /**
     * 起始节点ID
     */
    @Column(name = "start_node_id", nullable = false)
    private Integer startNodeId;
    
    /**
     * 结束节点ID
     */
    @Column(name = "end_node_id", nullable = false)
    private Integer endNodeId;
    
    /**
     * 距离（米）
     */
    @Column(name = "distance", nullable = false)
    private Double distance;
    
    /**
     * 步行时间（秒）
     */
    @Column(name = "walk_time", nullable = false)
    private Integer walkTime;
    
    /**
     * 是否双向通行
     */
    @Column(name = "is_bidirectional", nullable = false)
    private Boolean isBidirectional = true;
    
    /**
     * 无障碍设施信息
     */
    @Column(name = "accessibility_info", columnDefinition = "TEXT")
    private String accessibilityInfo;
    
    /**
     * 关联的起始节点（可选，用于查询）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_node_id", insertable = false, updatable = false)
    private MapNode startNode;
    
    /**
     * 关联的结束节点（可选，用于查询）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_node_id", insertable = false, updatable = false)
    private MapNode endNode;
}



