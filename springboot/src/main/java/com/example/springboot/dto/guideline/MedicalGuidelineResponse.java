package com.example.springboot.dto.guideline;

import com.example.springboot.entity.MedicalGuideline;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalGuidelineResponse {
    private Integer guidelineId;
    private String title;
    private String content;
    private String category;
    private MedicalGuideline.GuidelineStatus status;
    private Integer createdBy;
    private String createdByName;  // 创建人姓名
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}