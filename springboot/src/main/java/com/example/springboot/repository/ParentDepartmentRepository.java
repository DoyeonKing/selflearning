package com.example.springboot.repository;

import com.example.springboot.entity.ParentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentDepartmentRepository extends JpaRepository<ParentDepartment, Integer> {
    
    /**
     * 根据名称查找父科室
     */
    Optional<ParentDepartment> findByName(String name);
    
    /**
     * 检查父科室名称是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 根据名称模糊查询父科室
     */
    @Query("SELECT p FROM ParentDepartment p WHERE p.name LIKE %:name%")
    List<ParentDepartment> findByNameContaining(@Param("name") String name);
    
    /**
     * 根据描述模糊查询父科室
     */
    @Query("SELECT p FROM ParentDepartment p WHERE p.description LIKE %:description%")
    List<ParentDepartment> findByDescriptionContaining(@Param("description") String description);
}
