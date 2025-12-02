// filePath: springboot/src/main/java/com/example/springboot/dto/doctor/DoctorChangePasswordRequest.java
package com.example.springboot.dto.doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorChangePasswordRequest {
    @NotBlank(message = "医生工号不能为空")
    private String identifier; // 医生工号

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword; // 旧密码

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
    private String newPassword; // 新密码

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword; // 确认密码
}