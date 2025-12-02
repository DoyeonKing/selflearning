package com.example.springboot.dto.patient;

import lombok.Data;

@Data
public class ActivateRequest {
    private String identifier;       // 学号或工号
    private String idCardEnding;     // 身份证后6位
    private String newPassword;
    private String confirmPassword;  // 确认密码，用于 Service 层校验
}