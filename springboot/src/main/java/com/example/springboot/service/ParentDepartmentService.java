package com.example.springboot.service;

import com.example.springboot.entity.ParentDepartment;
import com.example.springboot.repository.ParentDepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ParentDepartmentService {

    @Autowired
    private ParentDepartmentRepository parentDepartmentRepository;

    /**
     * 创建父科室
     */
    public ParentDepartment createParentDepartment(String name, String description) {
        if (parentDepartmentRepository.existsByName(name)) {
            throw new RuntimeException("父科室名称已存在: " + name);
        }
        
        ParentDepartment parentDepartment = new ParentDepartment(name, description);
        return parentDepartmentRepository.save(parentDepartment);
    }

    /**
     * 根据ID查找父科室
     */
    public Optional<ParentDepartment> findById(Integer id) {
        return parentDepartmentRepository.findById(id);
    }

    /**
     * 根据名称查找父科室
     */
    public Optional<ParentDepartment> findByName(String name) {
        return parentDepartmentRepository.findByName(name);
    }

    /**
     * 获取所有父科室
     */
    public List<ParentDepartment> findAll() {
        return parentDepartmentRepository.findAll();
    }

    /**
     * 更新父科室
     */
    public ParentDepartment updateParentDepartment(Integer id, String name, String description) {
        ParentDepartment parentDepartment = parentDepartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("父科室不存在: " + id));
        
        // 检查名称是否与其他父科室冲突
        if (!parentDepartment.getName().equals(name) && parentDepartmentRepository.existsByName(name)) {
            throw new RuntimeException("父科室名称已存在: " + name);
        }
        
        parentDepartment.setName(name);
        parentDepartment.setDescription(description);
        return parentDepartmentRepository.save(parentDepartment);
    }

    /**
     * 删除父科室
     */
    public void deleteParentDepartment(Integer id) {
        ParentDepartment parentDepartment = parentDepartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("父科室不存在: " + id));
        
        // 检查是否有子科室
        if (!parentDepartment.getDepartments().isEmpty()) {
            throw new RuntimeException("无法删除有子科室的父科室");
        }
        
        parentDepartmentRepository.delete(parentDepartment);
    }

    /**
     * 根据名称模糊查询父科室
     */
    public List<ParentDepartment> findByNameContaining(String name) {
        return parentDepartmentRepository.findByNameContaining(name);
    }

    /**
     * 根据描述模糊查询父科室
     */
    public List<ParentDepartment> findByDescriptionContaining(String description) {
        return parentDepartmentRepository.findByDescriptionContaining(description);
    }
}
