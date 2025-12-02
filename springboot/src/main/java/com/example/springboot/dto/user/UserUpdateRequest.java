// 创建通用的用户更新请求DTO
package com.example.springboot.dto.user;

import com.example.springboot.dto.doctor.DoctorUpdateRequest;
import com.example.springboot.dto.patient.PatientUpdateRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "用户角色不能为空")
    private String role; // 支持 "PATIENT", "DOCTOR"

    private PatientUpdateRequest patientUpdateRequest;
    private DoctorUpdateRequest doctorUpdateRequest;
}