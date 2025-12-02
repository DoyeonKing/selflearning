package com.example.springboot.dto.department;

import lombok.Data;
import java.util.List;

/**
 * 科室树形结构DTO
 */
@Data
public class DepartmentTreeDTO {
    private Integer id;
    private String name;
    private String type; // "parent" 或 "department"
    private String description;
    private Integer parentId; // 仅子科室有此字段
    private List<DepartmentTreeDTO> children; // 仅父科室有此字段
}
