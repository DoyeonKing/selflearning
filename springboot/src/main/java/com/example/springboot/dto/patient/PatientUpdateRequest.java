// 完善PatientUpdateRequest
package com.example.springboot.dto.patient;

import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.entity.enums.PatientType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientUpdateRequest {
    @Size(max = 100, message = "学号或工号长度不能超过100个字符")
    private String identifier;

    private PatientType patientType;

    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phoneNumber;

    private String status;

    @Size(max = 18, message = "身份证号长度不能超过18个字符")
    private String idCardNumber;

    private String allergies;
    private String medicalHistory;
}