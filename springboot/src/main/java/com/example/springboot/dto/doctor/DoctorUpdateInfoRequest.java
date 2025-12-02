// DoctorUpdateInfoRequest.java
package com.example.springboot.dto.doctor;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

// DoctorUpdateInfoRequest.java
@Data
public class DoctorUpdateInfoRequest {
    private String identifier;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phoneNumber;

    private String specialty;
    private String bio;
    private MultipartFile avatarFile;
}