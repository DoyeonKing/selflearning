package com.example.springboot.dto.doctor;

import com.example.springboot.entity.enums.DoctorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorCreateRequest {
    @NotNull(message = "所属诊所ID不能为空")
    private Integer clinicId;

    @NotNull(message = "所属科室ID不能为空")
    private Integer departmentId;

    @NotBlank(message = "工号不能为空")
    @Size(max = 100, message = "工号长度不能超过100个字符")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phoneNumber;

    private String title;
    private String specialty;
    private String bio;
    private String photoUrl;

    @NotNull(message = "账户状态不能为空")
    private DoctorStatus status;
}
