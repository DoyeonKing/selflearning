package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.ActorType; // 导入路径调整
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 审计日志表
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(name = "actor_id")
    private Integer actorId; // 操作者ID

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type")
    private ActorType actorType; // 操作者类型

    @Column(nullable = false)
    private String action; // 操作行为描述

    @Column(name = "target_entity", length = 100)
    private String targetEntity; // 被操作对象所在的表名

    @Column(name = "target_id")
    private Integer targetId; // 被操作对象的ID

    private String details; // 操作详细信息 (如JSON)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 操作发生时间
}
