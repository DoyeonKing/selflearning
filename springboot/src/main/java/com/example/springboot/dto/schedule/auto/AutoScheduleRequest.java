package com.example.springboot.dto.schedule.auto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AutoScheduleRequest {
    @NotNull(message = "科室ID不能为空")
    private Integer departmentId;

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private ScheduleRules rules;

    private Boolean overwriteExisting = false;

    private Boolean previewOnly = false;
}


