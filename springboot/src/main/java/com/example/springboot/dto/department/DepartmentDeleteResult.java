package com.example.springboot.dto.department;

import lombok.Data;
import java.util.List;

/**
 * 科室删除结果DTO
 */
@Data
public class DepartmentDeleteResult {
    private Integer departmentId;
    private String departmentName;
    private Integer doctorCount;
    private List<String> movedDoctors;
    private Integer locationCount;
    private List<String> movedLocations;
    private Boolean success;
    private String message;
}
