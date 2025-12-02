package com.example.springboot.dto.department;

import lombok.Data;

/**
 * 科室查询和分页数据传输对象
 * 用于接收前端的搜索条件和分页参数。
 */
@Data
public class DepartmentQueryDTO {

    /**
     * 科室名称，用于模糊搜索 (like '%name%')
     */
    private String name;

    /**
     * 科室描述，用于模糊搜索 (like '%description%')
     */
    private String description;

    /**
     * 父科室ID，用于精确搜索
     */
    private Integer parentDepartmentId;

    /**
     * 当前页码 (从 0 开始)
     * 默认值将在 Controller 层或 Service 层处理 (通常为 0)
     */
    private Integer page;

    /**
     * 每页记录数
     * 默认值将在 Controller 层或 Service 层处理 (通常为 10)
     */
    private Integer size;

    /**
     * 排序字段 (可选，例如: departmentId, name)
     */
    private String sortBy;

    /**
     * 排序方向 (可选，例如: asc, desc)
     */
    private String sortOrder;
}