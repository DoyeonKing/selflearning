package com.example.springboot.service;

import com.example.springboot.dto.admin.AdminCreateRequest;
import com.example.springboot.dto.admin.AdminResponse;
import com.example.springboot.dto.admin.AdminUpdateRequest;
import com.example.springboot.dto.admin.PermissionResponse;
import com.example.springboot.dto.admin.RoleResponse;
import com.example.springboot.dto.auth.LoginResponse;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Permission;
import com.example.springboot.entity.Role;
import com.example.springboot.entity.enums.AdminStatus;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AdminRepository;
import com.example.springboot.repository.RoleRepository;
import com.example.springboot.util.PasswordEncoderUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;

    @Autowired
    public AdminService(AdminRepository adminRepository, RoleRepository roleRepository, PasswordEncoderUtil passwordEncoderUtil) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoderUtil = passwordEncoderUtil;
    }

    // =========================================================================
    // 【管理员登录】
    // =========================================================================

    /**
     * 管理员登录
     * @param username 管理员用户名
     * @param password 密码
     * @return 登录响应
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        // 1. 查找管理员
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // 2. 验证密码
        if (!passwordEncoderUtil.matches(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 3. 检查账户状态
        if (admin.getStatus() == AdminStatus.inactive) {
            throw new IllegalArgumentException("账户未激活，请联系系统管理员");
        }
        
        if (admin.getStatus() == AdminStatus.locked) {
            throw new IllegalArgumentException("账户已被锁定，请联系系统管理员");
        }

        // 4. 获取角色列表
        List<String> roles = admin.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        // 5. 构建用户信息
        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("adminId", admin.getAdminId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("fullName", admin.getFullName());
        adminInfo.put("status", admin.getStatus().name());
        adminInfo.put("roles", roles);

        // 6. 返回登录响应
        return LoginResponse.builder()
                .token(null) // 暂不使用token
                .userType("admin")
                .userInfo(adminInfo)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminResponse findAdminById(Integer id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id " + id));
        return convertToResponseDto(admin);
    }

    @Transactional(readOnly = true)
    public AdminResponse findAdminByUsername(String username) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username " + username));
        return convertToResponseDto(admin);
    }

    @Transactional
    public AdminResponse createAdmin(AdminCreateRequest request) {
        Admin admin = new Admin();
        BeanUtils.copyProperties(request, admin, "password", "roleIds"); // 忽略密码和角色ID，单独处理
        admin.setPasswordHash(passwordEncoderUtil.encodePassword(request.getPassword()));

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            admin.setRoles(roles);
        }

        return convertToResponseDto(adminRepository.save(admin));
    }

    @Transactional
    public AdminResponse updateAdmin(Integer id, AdminUpdateRequest request) {
        Admin existingAdmin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id " + id));

        // Update fields
        if (request.getUsername() != null) existingAdmin.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            existingAdmin.setPasswordHash(passwordEncoderUtil.encodePassword(request.getPassword()));
        }
        if (request.getFullName() != null) existingAdmin.setFullName(request.getFullName());
        if (request.getStatus() != null) existingAdmin.setStatus(request.getStatus());

        if (request.getRoleIds() != null) { // 如果提供了角色ID列表，则更新
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            existingAdmin.setRoles(roles);
        }

        return convertToResponseDto(adminRepository.save(existingAdmin));
    }

    @Transactional
    public void deleteAdmin(Integer id) {
        if (!adminRepository.existsById(id)) {
            throw new ResourceNotFoundException("Admin not found with id " + id);
        }
        adminRepository.deleteById(id);
    }

    public AdminResponse convertToResponseDto(Admin admin) {
        AdminResponse response = new AdminResponse();
        BeanUtils.copyProperties(admin, response);
        if (admin.getRoles() != null) {
            response.setRoles(admin.getRoles().stream()
                    .map(this::convertRoleToResponseDto)
                    .collect(Collectors.toSet()));
        } else {
            response.setRoles(Collections.emptySet());
        }
        return response;
    }

    private RoleResponse convertRoleToResponseDto(Role role) {
        RoleResponse response = new RoleResponse();
        BeanUtils.copyProperties(role, response);
        if (role.getPermissions() != null) {
            response.setPermissions(role.getPermissions().stream()
                    .map(this::convertPermissionToResponseDto)
                    .collect(Collectors.toSet()));
        } else {
            response.setPermissions(Collections.emptySet());
        }
        return response;
    }

    private PermissionResponse convertPermissionToResponseDto(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        BeanUtils.copyProperties(permission, response);
        return response;
    }
}
