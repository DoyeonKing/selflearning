package com.example.springboot.dto.admin;

import com.example.springboot.entity.enums.AdminStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class AdminUpdateRequest {
    @Size(max = 100, message = "用户名长度不能超过100个字符")
    private String username;

    @Size(min = 6, max = 255, message = "密码长度至少为6个字符")
    private String password;

    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;

    private AdminStatus status;

    private Set<Integer> roleIds; // 关联的角色ID
}
