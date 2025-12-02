package com.example.springboot.entity;

import com.example.springboot.entity.enums.NotificationPriority;
import com.example.springboot.entity.enums.NotificationStatus;
import com.example.springboot.entity.enums.NotificationType;
import com.example.springboot.entity.enums.UserType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 通知消息表
 */
@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 接收用户ID

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType; // 用户类型

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type; // 通知类型

    @Column(name = "title", nullable = false, length = 200)
    private String title; // 通知标题

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 通知内容

    @Column(name = "related_entity", length = 50)
    private String relatedEntity; // 相关实体类型

    @Column(name = "related_id")
    private Integer relatedId; // 相关实体ID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.unread; // 通知状态，默认未读

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private NotificationPriority priority = NotificationPriority.normal; // 优先级，默认普通

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt; // 发送时间

    @Column(name = "read_at")
    private LocalDateTime readAt; // 阅读时间
}

