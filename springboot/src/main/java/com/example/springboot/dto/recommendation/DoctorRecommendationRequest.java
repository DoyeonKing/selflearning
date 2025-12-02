package com.example.springboot.dto.recommendation;

import lombok.Data;

/**
 * 医生推荐请求DTO
 */
@Data
public class DoctorRecommendationRequest {
    private String symptomDescription; // 患者症状描述
    private Integer topN = 5; // 返回Top-N个推荐，默认5个
}