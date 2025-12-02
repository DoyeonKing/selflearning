package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.AppointmentStatus; // 导入路径调整
import com.example.springboot.entity.enums.AppointmentType;   // 导入路径调整
import com.example.springboot.entity.enums.PaymentStatus;     // 导入路径调整
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 预约挂号表
 */
@Entity
@Table(name = "appointments")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient; // 患者ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule; // 排班ID

    @Column(name = "appointment_number")
    private Integer appointmentNumber; // 就诊序号

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status; // 预约状态

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus; // 支付状态

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // 支付方式

    @Column(name = "transaction_id")
    private String transactionId; // 支付流水号

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime; // 现场签到时间

    @Column(name = "called_at")
    private LocalDateTime calledAt; // 叫号时间（NULL表示未叫号）

    @Column(name = "is_on_time")
    private Boolean isOnTime = false; // 是否按时签到（预约时段开始后20分钟内）

    @Column(name = "missed_call_count")
    private Integer missedCallCount = 0; // 过号次数

    @Column(name = "recheck_in_time")
    private LocalDateTime recheckInTime; // 过号后重新签到时间

    @Column(name = "is_walk_in")
    private Boolean isWalkIn = false; // 是否现场挂号（false=预约，true=现场挂号）

    @Column(name = "real_time_queue_number")
    private Integer realTimeQueueNumber; // 实时候诊序号（在时段内按签到时间分配）

    @Column(name = "is_late")
    private Boolean isLate = false; // 是否迟到（超过时段结束时间+软关门时间）

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type")
    private AppointmentType appointmentType = AppointmentType.APPOINTMENT; // 预约类型

    @Column(name = "original_appointment_id")
    private Integer originalAppointmentId; // 原始预约ID（用于复诊号关联，复诊号关联到原始预约）

    @Column(name = "is_add_on")
    private Boolean isAddOn = false; // 是否加号

    // ===== 加号支付截止时间字段 =====
    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline; // 支付截止时间（加号专用，类似候补的notification_sent_at）

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 预约生成时间

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 最后更新时间（status变为completed时由触发器自动更新）
}
