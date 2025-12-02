package com.example.springboot.repository;

import com.example.springboot.entity.Department;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.enums.DoctorStatus;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 关键：新增导入
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
// 继承 JpaSpecificationExecutor 以支持 findAll(Specification, Pageable) 方法，解决编译错误 2
public interface DoctorRepository extends JpaRepository<Doctor, Integer>, JpaSpecificationExecutor<Doctor> {
    // 通过工号查找医生
    Optional<Doctor> findByIdentifier(String identifier);

    // 检查工号是否存在
    boolean existsByIdentifier(String identifier);

    // 检查身份证号是否存在
    boolean existsByIdCardNumber(String idCardNumber);

    @Query("SELECT d FROM Doctor d JOIN FETCH d.department WHERE d.identifier LIKE %:id% OR d.fullName LIKE %:name%")
    Page<Doctor> findByKeywordWithDepartment(
            @Param("id") String id,
            @Param("name") String name,
            Pageable pageable);
    // 根据科室ID查找该科室下的所有医生
    List<Doctor> findByDepartmentDepartmentId(Integer departmentId);
    
    // 根据科室ID和状态查找医生
    List<Doctor> findByDepartmentDepartmentIdAndStatus(Integer departmentId, DoctorStatus status);
    
    // 根据科室和状态查找医生
    List<Doctor> findByDepartmentAndStatus(Department department, DoctorStatus status);

    boolean existsByPhoneNumberAndIdentifierNot(String phoneNumber, String identifier);
}
