package com.example.springboot.entity;

import com.example.springboot.entity.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生排班表
 */
@Entity
@Table(name = "schedules", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"doctor_id", "schedule_date", "slot_id"})
})
@Data
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor; // 医生ID

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate; // 出诊日期

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot; // 时间段ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location; // 就诊地点ID

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots; // 总号源数

    @Column(name = "booked_slots", nullable = false)
    private Integer bookedSlots = 0; // 已预约数

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee; // 挂号费用

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status; // 排班状态

    private String remarks; // 特别的排班要求或备注信息

    // ===== 加号虚拟号源相关字段 =====
    @Column(name = "is_add_on_slot")
    private Boolean isAddOnSlot = false; // 是否为加号虚拟号源

    @Column(name = "reserved_for_patient_id")
    private Long reservedForPatientId; // 预留给指定患者ID（加号专用）

    @Column(name = "slot_application_id")
    private Integer slotApplicationId; // 关联的加号申请ID

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
