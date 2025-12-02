// DoctorUpdateRequest已存在，补充必要的验证注解
package com.example.springboot.dto.doctor;

import com.example.springboot.entity.enums.DoctorStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorUpdateRequest {
    private Integer clinicId;
    private Integer departmentId;

    @Size(max = 100, message = "工号长度不能超过100个字符")
    private String identifier;

    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phoneNumber;

    private String title;
    private String specialty;
    private String bio;
    private String photoUrl;
    private DoctorStatus status;
}