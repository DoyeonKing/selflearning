package com.example.springboot.dto.admin;

import lombok.Data;

import java.util.Set;

@Data
public class RoleResponse {
    private Integer roleId;
    private String roleName;
    private String description;
    private Set<PermissionResponse> permissions;
}
