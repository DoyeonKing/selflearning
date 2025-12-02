package com.example.springboot.dto.patient; // 包名调整

import com.example.springboot.entity.enums.PatientStatus; // 导入路径调整
import com.example.springboot.entity.enums.PatientType;   // 导入路径调整
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientResponse {
    private Long patientId;
    private String identifier;
    private PatientType patientType;
    private String fullName;
    private String phoneNumber;
    private PatientStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
