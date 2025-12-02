package com.example.springboot.service;

import com.example.springboot.dto.symptom.SymptomMappingRequest;
import com.example.springboot.dto.symptom.SymptomMappingResponse;
import com.example.springboot.entity.Department;
import com.example.springboot.entity.SymptomDepartmentMapping;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.SymptomDepartmentMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SymptomMappingService {
    
    @Autowired
    private SymptomDepartmentMappingRepository mappingRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    /**
     * 获取所有症状映射
     */
    public List<SymptomMappingResponse> getAllMappings() {
        return mappingRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据科室ID获取症状映射
     */
    public List<SymptomMappingResponse> getMappingsByDepartmentId(Integer departmentId) {
        return mappingRepository.findByDepartmentDepartmentId(departmentId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建症状映射
     */
    @Transactional
    public SymptomMappingResponse createMapping(SymptomMappingRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("科室不存在，ID: " + request.getDepartmentId()));
        
        SymptomDepartmentMapping mapping = new SymptomDepartmentMapping();
        mapping.setSymptomKeywords(request.getSymptomKeywords());
        mapping.setDepartment(department);
        mapping.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        
        SymptomDepartmentMapping saved = mappingRepository.save(mapping);
        return convertToResponse(saved);
    }
    
    /**
     * 更新症状映射
     */
    @Transactional
    public SymptomMappingResponse updateMapping(Integer mappingId, SymptomMappingRequest request) {
        SymptomDepartmentMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("症状映射不存在，ID: " + mappingId));
        
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("科室不存在，ID: " + request.getDepartmentId()));
            mapping.setDepartment(department);
        }
        
        if (request.getSymptomKeywords() != null) {
            mapping.setSymptomKeywords(request.getSymptomKeywords());
        }
        
        if (request.getPriority() != null) {
            mapping.setPriority(request.getPriority());
        }
        
        SymptomDepartmentMapping updated = mappingRepository.save(mapping);
        return convertToResponse(updated);
    }
    
    /**
     * 删除症状映射
     */
    @Transactional
    public void deleteMapping(Integer mappingId) {
        if (!mappingRepository.existsById(mappingId)) {
            throw new ResourceNotFoundException("症状映射不存在，ID: " + mappingId);
        }
        mappingRepository.deleteById(mappingId);
    }
    
    /**
     * 转换为响应DTO
     */
    private SymptomMappingResponse convertToResponse(SymptomDepartmentMapping mapping) {
        SymptomMappingResponse response = new SymptomMappingResponse();
        response.setMappingId(mapping.getMappingId());
        response.setSymptomKeywords(mapping.getSymptomKeywords());
        response.setDepartmentId(mapping.getDepartment().getDepartmentId());
        response.setDepartmentName(mapping.getDepartment().getName());
        response.setPriority(mapping.getPriority());
        response.setCreatedAt(mapping.getCreatedAt());
        response.setUpdatedAt(mapping.getUpdatedAt());
        return response;
    }
}

