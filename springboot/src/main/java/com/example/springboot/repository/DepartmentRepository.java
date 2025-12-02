package com.example.springboot.repository;

import com.example.springboot.entity.Department;
import com.example.springboot.entity.ParentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer>, JpaSpecificationExecutor<Department> {

    /**
     * 根据科室名称查找科室
     * @param name 科室名称
     * @return 匹配的科室 Optional
     */
    Optional<Department> findByName(String name);
    
    /**
     * 根据父科室查找所有子科室
     * @param parentDepartment 父科室
     * @return 子科室列表
     */
    List<Department> findByParentDepartment(ParentDepartment parentDepartment);
    
    /**
     * 根据父科室ID查找所有子科室
     * @param parentId 父科室ID
     * @return 子科室列表
     */
    @Query("SELECT d FROM Department d WHERE d.parentDepartment.parentDepartmentId = :parentId")
    List<Department> findByParentDepartmentId(@Param("parentId") Integer parentId);
    
    /**
     * 根据名称模糊查询科室
     */
    @Query("SELECT d FROM Department d WHERE d.name LIKE %:name%")
    List<Department> findByNameContaining(@Param("name") String name);
    
    /**
     * 根据描述模糊查询科室
     */
    @Query("SELECT d FROM Department d WHERE d.description LIKE %:description%")
    List<Department> findByDescriptionContaining(@Param("description") String description);
    
    /**
     * 更新症状科室映射表中的科室ID
     * 将指定科室的症状映射转移到未分配科室
     */
    @Modifying
    @Query(value = "UPDATE symptom_department_mapping SET department_id = :targetDepartmentId WHERE department_id = :sourceDepartmentId", nativeQuery = true)
    int updateSymptomDepartmentMappings(@Param("sourceDepartmentId") Integer sourceDepartmentId, 
                                       @Param("targetDepartmentId") Integer targetDepartmentId);
    
    /**
     * 检查科室名称是否存在
     */
    boolean existsByName(String name);
    
    // 注释掉不存在的实体查询
    // /**
    //  * 更新症状科室映射表中的科室ID
    //  * @param oldDepartmentId 原科室ID
    //  * @param newDepartmentId 新科室ID
    //  * @return 更新的记录数
    //  */
    // @Modifying
    // @Query("UPDATE SymptomDepartmentMapping sdm SET sdm.departmentId = :newDepartmentId WHERE sdm.departmentId = :oldDepartmentId")
    // int updateSymptomDepartmentMappings(@Param("oldDepartmentId") Integer oldDepartmentId, @Param("newDepartmentId") Integer newDepartmentId);
    
    /**
     * 更新诊室表中的科室ID
     * @param oldDepartmentId 原科室ID
     * @param newDepartmentId 新科室ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Location l SET l.department.departmentId = :newDepartmentId WHERE l.department.departmentId = :oldDepartmentId")
    int updateLocationDepartments(@Param("oldDepartmentId") Integer oldDepartmentId, @Param("newDepartmentId") Integer newDepartmentId);
}