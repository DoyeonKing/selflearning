package com.example.springboot.dto.doctor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求体 DTO: 将医生添加到科室 (对应前端“添加新成员”)
 */
@Data
public class AddDoctorRequest {

    // 医生ID / 请输入医生工号
    @NotBlank(message = "医生ID (工号) 不能为空")
    private String identifier;

    // 医生姓名 / 请输入医生姓名
    @NotBlank(message = "医生姓名不能为空")
    private String fullName;

    // 职称 / 请输入医生职称
    @NotBlank(message = "职称不能为空")
    private String title;
}