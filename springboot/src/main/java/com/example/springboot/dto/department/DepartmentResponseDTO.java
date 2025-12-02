package com.example.springboot.dto.department; // 请在您的项目中创建此文件

import lombok.Data;

@Data
public class DepartmentResponseDTO {
    private Integer departmentId;
    private String name;
    private String description;
    // 只包含上级科室的ID和名称，不包含完整的Department实体
    private Integer parentDepartmentId;
    private String parentDepartmentName;
}