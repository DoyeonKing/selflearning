package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.BlacklistStatus; // 导入路径调整
import jakarta.persistence.*;
import lombok.Data;

/**
 * 患者信息扩展表
 */
@Entity
@Table(name = "patient_profiles")
@Data
public class PatientProfile {
    @Id
    @Column(name = "patient_id")
    private Long patientId; // 与 Patient 表共享主键

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 表示这个实体的主键是从关联的实体中映射过来的
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "id_card_number", length = 18)
    private String idCardNumber; // 身份证号, 建议加密

    private String allergies; // 过敏史, 建议加密
    @Column(name = "medical_history")
    private String medicalHistory; // 基础病史, 建议加密

    @Column(name = "no_show_count", nullable = false)
    private Integer noShowCount = 0; // 爽约次数

    @Enumerated(EnumType.STRING)
    @Column(name = "blacklist_status", nullable = false)
    private BlacklistStatus blacklistStatus; // 黑名单状态

}
