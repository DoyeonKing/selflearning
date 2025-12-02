package com.example.springboot.service;

import com.example.springboot.dto.admin.PermissionCreateRequest;
import com.example.springboot.dto.admin.PermissionResponse;
import com.example.springboot.dto.admin.PermissionUpdateRequest;
import com.example.springboot.entity.Permission;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.PermissionRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PermissionResponse findPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));
        return convertToResponseDto(permission);
    }

    @Transactional
    public PermissionResponse createPermission(PermissionCreateRequest request) {
        if (permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new BadRequestException("Permission with name '" + request.getPermissionName() + "' already exists.");
        }
        Permission permission = new Permission();
        BeanUtils.copyProperties(request, permission);
        return convertToResponseDto(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse updatePermission(Integer id, PermissionUpdateRequest request) {
        Permission existingPermission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));

        if (request.getPermissionName() != null && !request.getPermissionName().equals(existingPermission.getPermissionName())) {
            if (permissionRepository.existsByPermissionName(request.getPermissionName())) {
                throw new BadRequestException("Permission with name '" + request.getPermissionName() + "' already exists.");
            }
            existingPermission.setPermissionName(request.getPermissionName());
        }
        if (request.getDescription() != null) existingPermission.setDescription(request.getDescription());

        return convertToResponseDto(permissionRepository.save(existingPermission));
    }

    @Transactional
    public void deletePermission(Integer id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission not found with id " + id);
        }
        // TODO: Consider business logic for deleting permissions that are linked to roles.
        permissionRepository.deleteById(id);
    }

    public PermissionResponse convertToResponseDto(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        BeanUtils.copyProperties(permission, response);
        return response;
    }
}
