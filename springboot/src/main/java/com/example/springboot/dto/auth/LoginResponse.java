package com.example.springboot.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;          // Token（暂不使用，返回null）
    private String userType;       // 用户类型: doctor/admin/patient
    private Object userInfo;       // 用户信息（根据类型不同）
}

