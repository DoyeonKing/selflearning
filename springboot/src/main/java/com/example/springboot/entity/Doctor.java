package com.example.springboot.entity;

import com.example.springboot.entity.enums.DoctorStatus; // 导入路径调整
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 医生表 (与最新 SQL CREATE 语句匹配)
 */
@Entity
@Table(name = "doctors")
@Data
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id") // 明确指定列名
    private Integer doctorId;

    // 根据最新 SQL，只保留了 department_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department; // 外键, 所属科室

    @Column(name = "identifier", nullable = false, unique = true, length = 100)
    private String identifier; // 医生的工号

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // 哈希加盐后的密码

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName; // 真实姓名

    @Column(name = "id_card_number", unique = true, length = 18)
    private String idCardNumber; // 身份证号

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber; // 存储E.164标准格式

    @Column(name = "title", length = 100)
    private String title; // 职称 (如："主治医师")

    @Column(name = "title_level")
    private Integer titleLevel; // 职称等级：0-主任医师，1-副主任医师，2-主治医师

    @Lob // 适用于 TEXT 类型
    @Column(name = "specialty")
    private String specialty; // 擅长领域描述

    @Lob // 适用于 TEXT 类型
    @Column(name = "bio")
    private String bio; // 个人简介

    @Column(name = "photo_url", length = 255)
    private String photoUrl; // 头像照片URL

    @Enumerated(EnumType.STRING)
    // 明确列名和约束，虽然 ENUM 类型 JPA 可能需要 columnDefinition
    @Column(name = "status", nullable = false)
    private DoctorStatus status; // 账户状态

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 账户创建时间

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 信息最后更新时间
}
