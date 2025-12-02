package com.example.springboot.dto.auth;

import lombok.Data;

/**
 * 通用登录请求 DTO
 */
@Data
public class LoginRequest {
    private String identifier; // 用户名/工号/学号
    private String password;   // 密码
}

