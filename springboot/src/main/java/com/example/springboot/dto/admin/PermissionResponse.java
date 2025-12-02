package com.example.springboot.dto.admin;

import lombok.Data;

@Data
public class PermissionResponse {
    private Integer permissionId;
    private String permissionName;
    private String description;
}
