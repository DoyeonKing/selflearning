package com.example.springboot.dto.patient;

// 用于 /api/auth/verify-patient 接口
public class VerifyRequest {
    private String identifier;
    private String initialPassword; // 新增字段

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public String getInitialPassword() {
        return initialPassword;
    }
    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}