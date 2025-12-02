package com.example.springboot.dto.doctor;

import com.example.springboot.dto.department.DepartmentDTO; // 导入部门响应DTO
import com.example.springboot.entity.enums.DoctorStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DoctorResponse {
    private Integer doctorId;
    private DepartmentDTO department; // 科室信息
    private String identifier;
    private String fullName;
    private String phoneNumber;
    private String title;
    private Integer titleLevel; // 职称等级：0-主任医师，1-副主任医师，2-主治医师
    private String specialty;
    private String bio;
    private String photoUrl;
    private DoctorStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
