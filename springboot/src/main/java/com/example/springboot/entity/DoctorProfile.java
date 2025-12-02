package com.example.springboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 医生画像表（用于缓存医生特征，提升推荐性能）
 */
@Entity
@Table(name = "doctor_profiles")
@Data
public class DoctorProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Integer profileId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false, unique = true)
    private Doctor doctor;
    
    @Lob
    @Column(name = "extracted_keywords")
    private String extractedKeywords; // 从specialty提取的关键词（逗号分隔）
    
    @Column(name = "appointment_count", nullable = false)
    private Integer appointmentCount = 0;
    
    @Column(name = "avg_rating", precision = 3, scale = 2)
    private Double avgRating = 0.0;
    
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}