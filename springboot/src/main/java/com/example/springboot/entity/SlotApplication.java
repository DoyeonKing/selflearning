package com.example.springboot.entity;

import com.example.springboot.entity.enums.SlotApplicationStatus;
import com.example.springboot.entity.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 加号申请实体类
 */
@Entity
@Table(name = "slot_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "doctor_id", nullable = false)
    private Integer doctorId;

    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @Column(name = "added_slots", nullable = false)
    private Integer addedSlots;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false)
    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotApplicationStatus status = SlotApplicationStatus.PENDING;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_comments", columnDefinition = "TEXT")
    private String approverComments;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ===== 关联生成的预约ID =====
    @Column(name = "appointment_id")
    private Integer appointmentId; // 审批通过后自动生成的预约记录ID

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
