package com.example.springboot.dto.recommendation;

import lombok.Data;

/**
 * 科室推荐请求DTO
 */
@Data
public class DepartmentRecommendationRequest {
    private String symptomDescription; // 患者症状描述
    private Long patientId; // 患者ID（可选，用于病史关联分析）
}