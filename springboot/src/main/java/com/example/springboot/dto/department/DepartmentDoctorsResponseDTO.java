package com.example.springboot.dto.department;

import com.example.springboot.dto.doctor.DoctorResponse;
import lombok.Data;

import java.util.List;

/**
 * 科室医生列表响应DTO
 * 用于返回指定科室下的所有医生信息
 */
@Data
public class DepartmentDoctorsResponseDTO {

    /**
     * 科室名称
     */
    private String departmentName;

    /**
     * 科室ID
     */
    private Integer departmentId;

    /**
     * 该科室下的医生列表
     */
    private List<DoctorResponse> doctors;

    /**
     * 医生总数
     */
    private Integer totalCount;

    public DepartmentDoctorsResponseDTO() {
    }

    public DepartmentDoctorsResponseDTO(String departmentName, Integer departmentId, List<DoctorResponse> doctors) {
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.doctors = doctors;
        this.totalCount = doctors != null ? doctors.size() : 0;
    }
}
