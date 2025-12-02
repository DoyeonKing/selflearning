package com.example.springboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 地图节点实体类
 * 对应数据库表 map_nodes
 */
@Entity
@Table(name = "map_nodes")
@Data
public class MapNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Integer nodeId;
    
    @Column(name = "node_name", length = 100, nullable = false)
    private String nodeName;
    
    @Column(name = "node_type", nullable = false)
    private String nodeTypeStr;  // 存储数据库原始值（小写）
    
    @Transient
    private NodeType nodeType;  // 转换后的枚举值
    
    // 节点类型（通过getter/setter转换大小写）
    public NodeType getNodeType() {
        if (nodeType != null) {
            return nodeType;
        }
        if (nodeTypeStr == null) return null;
        try {
            nodeType = NodeType.valueOf(nodeTypeStr.toUpperCase());
            return nodeType;
        } catch (IllegalArgumentException e) {
            nodeType = NodeType.ROOM; // 默认值
            return nodeType;
        }
    }
    
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        if (nodeType == null) {
            this.nodeTypeStr = null;
        } else {
            this.nodeTypeStr = nodeType.name().toLowerCase();
        }
    }
    
    @PostLoad
    private void postLoad() {
        // 从数据库加载后，自动转换nodeTypeStr为枚举
        if (nodeTypeStr != null) {
            try {
                this.nodeType = NodeType.valueOf(nodeTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.nodeType = NodeType.ROOM;
            }
        }
        // 转换qrcodeStatusStr为枚举
        if (qrcodeStatusStr != null) {
            try {
                this.qrcodeStatus = QRCodeStatus.valueOf(qrcodeStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.qrcodeStatus = QRCodeStatus.PENDING;
            }
        } else {
            this.qrcodeStatus = QRCodeStatus.PENDING;
        }
    }
    
    @PrePersist
    @PreUpdate
    private void prePersist() {
        // 保存前，将枚举转换为小写字符串
        if (nodeType != null) {
            this.nodeTypeStr = nodeType.name().toLowerCase();
        }
        if (qrcodeStatus != null) {
            this.qrcodeStatusStr = qrcodeStatus.name().toLowerCase();
        } else {
            this.qrcodeStatusStr = "pending";
        }
    }
    
    @Column(name = "coordinates_x", nullable = false)
    private Double coordinatesX;
    
    @Column(name = "coordinates_y", nullable = false)
    private Double coordinatesY;
    
    @Column(name = "floor_level", nullable = false)
    private Integer floorLevel;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_accessible", nullable = false)
    private Boolean isAccessible = true;
    
    // 二维码相关字段
    @Column(name = "qrcode_content", length = 255)
    private String qrcodeContent;
    
    @Column(name = "qrcode_image_path", length = 500)
    private String qrcodeImagePath;
    
    @Column(name = "qrcode_generated_at")
    private LocalDateTime qrcodeGeneratedAt;
    
    @Column(name = "qrcode_status", nullable = false)
    private String qrcodeStatusStr;  // 存储数据库原始值（小写）
    
    @Transient
    private QRCodeStatus qrcodeStatus;  // 转换后的枚举值
    
    public QRCodeStatus getQrcodeStatus() {
        if (qrcodeStatus != null) {
            return qrcodeStatus;
        }
        if (qrcodeStatusStr == null) return QRCodeStatus.PENDING;
        try {
            qrcodeStatus = QRCodeStatus.valueOf(qrcodeStatusStr.toUpperCase());
            return qrcodeStatus;
        } catch (IllegalArgumentException e) {
            qrcodeStatus = QRCodeStatus.PENDING;
            return qrcodeStatus;
        }
    }
    
    public void setQrcodeStatus(QRCodeStatus qrcodeStatus) {
        this.qrcodeStatus = qrcodeStatus;
        if (qrcodeStatus == null) {
            this.qrcodeStatusStr = "pending";
        } else {
            this.qrcodeStatusStr = qrcodeStatus.name().toLowerCase();
        }
    }
    
    /**
     * 节点类型枚举
     */
    public enum NodeType {
        ROOM,      // 房间
        HALLWAY,   // 走廊
        ELEVATOR,  // 电梯
        STAIRS,    // 楼梯
        ENTRANCE   // 入口
    }
    
    /**
     * 二维码状态枚举
     */
    public enum QRCodeStatus {
        ACTIVE,    // 已激活
        INACTIVE,  // 已停用
        PENDING    // 待生成
    }
}



