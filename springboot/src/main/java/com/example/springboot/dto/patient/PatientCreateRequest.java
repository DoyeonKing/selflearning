package com.example.springboot.dto.patient;

import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.entity.enums.PatientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientCreateRequest {
    @NotBlank(message = "学号或工号不能为空")
    @Size(max = 100, message = "学号或工号长度不能超过100个字符")
    private String identifier;

    @NotNull(message = "患者类型不能为空")
    private PatientType patientType;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phoneNumber;

    @NotNull(message = "账户状态不能为空")
    private PatientStatus status;

    // 患者个人信息扩展
    @Size(max = 18, message = "身份证号长度不能超过18个字符")
    private String idCardNumber;
    private String allergies;
    private String medicalHistory;
}
