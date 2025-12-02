package com.example.springboot.dto.recommendation;

import lombok.Data;

/**
 * 医生推荐响应DTO
 */
@Data
public class DoctorRecommendationDTO {
    private Integer doctorId;
    private String doctorName;
    private Integer departmentId;
    private String departmentName;
    private String title;
    private Integer titleLevel;
    private String specialty;
    private String photoUrl;
    private Double score;
    private String reason;
}