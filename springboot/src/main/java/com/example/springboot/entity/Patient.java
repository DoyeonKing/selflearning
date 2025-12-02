package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.PatientStatus; // 导入路径调整
import com.example.springboot.entity.enums.PatientType;   // 导入路径调整
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 患者表 - 学生/老师
 */
@Entity
@Table(name = "patients")
@Data
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;

    @Column(nullable = false, unique = true, length = 100)
    private String identifier; // 学号或工号

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_type", nullable = false)
    private PatientType patientType; // 患者类型

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash; // 哈希加盐后的密码

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName; // 真实姓名

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber; // 存储E.164标准格式

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status; // 账户状态

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 账户创建时间

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 信息最后更新时间

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private PatientProfile patientProfile;

    // Bi-directional relationship with Appointments
    // @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Set<Appointment> appointments = new HashSet<>();
}
