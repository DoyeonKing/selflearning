package com.example.springboot.service;

import com.example.springboot.dto.admin.PermissionResponse;
import com.example.springboot.dto.admin.RoleCreateRequest;
import com.example.springboot.dto.admin.RoleResponse;
import com.example.springboot.dto.admin.RoleUpdateRequest;
import com.example.springboot.entity.Permission;
import com.example.springboot.entity.Role;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.PermissionRepository;
import com.example.springboot.repository.RoleRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse findRoleById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));
        return convertToResponseDto(role);
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new BadRequestException("Role with name '" + request.getRoleName() + "' already exists.");
        }
        Role role = new Role();
        BeanUtils.copyProperties(request, role, "permissionIds");

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(permissions);
        }

        return convertToResponseDto(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Integer id, RoleUpdateRequest request) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));

        if (request.getRoleName() != null && !request.getRoleName().equals(existingRole.getRoleName())) {
            if (roleRepository.existsByRoleName(request.getRoleName())) {
                throw new BadRequestException("Role with name '" + request.getRoleName() + "' already exists.");
            }
            existingRole.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) existingRole.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) { // 如果提供了权限ID列表，则更新
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            existingRole.setPermissions(permissions);
        }

        return convertToResponseDto(roleRepository.save(existingRole));
    }

    @Transactional
    public void deleteRole(Integer id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found with id " + id);
        }
        // TODO: Consider business logic for deleting roles that are assigned to admins.
        roleRepository.deleteById(id);
    }

    public RoleResponse convertToResponseDto(Role role) {
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
