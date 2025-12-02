package com.example.springboot.dto.department;

import lombok.Data;

/**
 * 医生数据DTO（用于科室管理）
 */
@Data
public class DoctorDataDTO {
    private String identifier;
    private String fullName;
    private String title;
}
