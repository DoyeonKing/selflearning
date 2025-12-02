package com.example.springboot.dto.recommendation;

import lombok.Data;
import java.util.List;

/**
 * 按科室推荐医生请求DTO
 */
@Data
public class DoctorRecommendationByDepartmentRequest {
    private Integer departmentId; // 科室ID
    private Long patientId; // 患者ID（用于病史关联分析）
    private List<String> symptomKeywords; // 症状关键词（可选）
    private Integer topN = 10; // 返回Top-N个推荐
}