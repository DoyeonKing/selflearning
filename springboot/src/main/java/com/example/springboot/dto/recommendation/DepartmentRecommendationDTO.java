package com.example.springboot.dto.recommendation;

import lombok.Data;

/**
 * 科室推荐响应DTO
 */
@Data
public class DepartmentRecommendationDTO {
    private Integer departmentId;
    private String departmentName;
    private String description;
    private Double score; // 匹配度分数
    private String reason; // 推荐理由
    private Integer priority; // 优先级
}