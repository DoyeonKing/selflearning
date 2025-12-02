package com.example.springboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 自动排班请求参数
 */
@Data
public class AutoScheduleRequest {
    /**
     * 科室ID
     */
    @NotNull(message = "科室ID不能为空")
    private Integer departmentId;
    
    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    /**
     * 排班规则配置
     */
    private ScheduleRules rules;
    
    /**
     * 是否覆盖已有排班（默认false，不覆盖）
     */
    private Boolean overwriteExisting = false;
    
    /**
     * 是否仅预览（不保存到数据库）
     */
    private Boolean previewOnly = false;
}

