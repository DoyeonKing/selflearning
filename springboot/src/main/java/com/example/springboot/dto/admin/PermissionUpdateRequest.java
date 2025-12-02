package com.example.springboot.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionUpdateRequest {
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String permissionName;

    private String description;
}
