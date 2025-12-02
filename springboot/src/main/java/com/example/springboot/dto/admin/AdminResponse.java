package com.example.springboot.dto.admin;

import com.example.springboot.entity.enums.AdminStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AdminResponse {
    private Integer adminId;
    private String username;
    private String fullName;
    private AdminStatus status;
    private LocalDateTime createdAt;
    private Set<RoleResponse> roles; // 包含角色响应DTO
}
