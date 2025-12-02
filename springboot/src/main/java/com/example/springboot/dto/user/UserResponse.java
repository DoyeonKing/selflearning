package com.example.springboot.dto.user;

import com.example.springboot.dto.admin.AdminResponse;
import com.example.springboot.dto.doctor.DoctorResponse;
import com.example.springboot.dto.patient.PatientResponse;
import lombok.Data;

@Data
public class UserResponse {
    private String role;
    private Object userDetails; // 存储具体角色的响应DTO（PatientResponse/DoctorResponse/AdminResponse）
}