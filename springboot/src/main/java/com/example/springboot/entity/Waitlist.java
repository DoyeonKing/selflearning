package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.WaitlistStatus; // 导入路径调整
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 候补表
 */
@Entity
@Table(name = "waitlist")
@Data
public class Waitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer waitlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient; // 患者ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule; // 排班ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status; // 候补状态

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt; // 系统发送通知的时间

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 加入队列的时间 (用于排序)
}
