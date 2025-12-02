package com.example.springboot.repository;

import com.example.springboot.entity.Department;
import com.example.springboot.entity.SymptomDepartmentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomDepartmentMappingRepository extends JpaRepository<SymptomDepartmentMapping, Integer> {
    
    /**
     * 根据科室查找所有症状映射
     */
    List<SymptomDepartmentMapping> findByDepartment(Department department);
    
    /**
     * 根据科室ID查找所有症状映射
     */
    List<SymptomDepartmentMapping> findByDepartmentDepartmentId(Integer departmentId);
    
    /**
     * 根据症状关键词模糊查询
     */
    List<SymptomDepartmentMapping> findBySymptomKeywordsContaining(String keyword);
}

