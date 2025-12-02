package com.example.springboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 症状同义词表
 */
@Entity
@Table(name = "symptom_synonyms")
@Data
public class SymptomSynonym {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "synonym_id")
    private Integer synonymId;
    
    @Column(name = "symptom_keyword", nullable = false, length = 100)
    private String symptomKeyword;
    
    @Column(name = "synonym", nullable = false, length = 100)
    private String synonym;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}