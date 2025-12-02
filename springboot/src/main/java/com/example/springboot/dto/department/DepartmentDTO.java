package com.example.springboot.dto.department;

import lombok.Data;

/**
 * 创建科室的数据传输对象
 */
@Data
public class DepartmentDTO {
    private Integer departmentId;
    private String name; // 新科室的名称
    private String parentDepartmentName; // 上级科室的名称 (可能为空)
    private String description; // 科室描述

}