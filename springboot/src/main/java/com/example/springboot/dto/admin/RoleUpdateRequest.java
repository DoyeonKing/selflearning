package com.example.springboot.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RoleUpdateRequest {
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;

    private String description;

    private Set<Integer> permissionIds; // 关联的权限ID
}
