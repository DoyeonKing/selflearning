package com.example.springboot.dto.user;

import com.example.springboot.entity.enums.AdminStatus;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.entity.enums.PatientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户角色不能为空")
    private String role; // 支持 "PATIENT", "DOCTOR", "ADMIN"

    // 公共字段 - 修改参数名以匹配要求
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过100个字符")
    private String id; // 原identifier改为id，对应学号/工号

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name; // 原fullName改为name

    @NotBlank(message = "电话号码不能为空") // 修改为必需项
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phone; // 原phoneNumber改为phone

    // 患者特有字段
    private PatientType patientType;
    private PatientStatus patientStatus;

    @NotBlank(message = "身份证号不能为空") // 修改为必需项
    @Size(max = 18, message = "身份证号长度不能超过18个字符")
    private String id_card; // 原idCardNumber改为id_card

    private String allergy_history; // 原allergies改为allergy_history
    private String past_medical_history; // 原medicalHistory改为past_medical_history

    // 医生特有字段
    private Integer clinicId;
    private Integer departmentId;
    private String title;
    private String specialty;
    private String bio;
    private String photoUrl;
    private DoctorStatus doctorStatus;

    // 管理员特有字段
    private AdminStatus adminStatus;
    private Set<Integer> roleIds;
}