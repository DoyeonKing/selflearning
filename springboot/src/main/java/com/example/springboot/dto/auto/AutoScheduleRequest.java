package com.example.springboot.dto.auto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    private ScheduleRules rules = new ScheduleRules();

    private Boolean overwriteExisting = false;

    private Boolean previewOnly = false;

    /**
     * 可选：限定参与生成的时间段ID集合（不传则使用全部 time_slots）
     */
    private List<Integer> timeSlotIds;

    /**
     * 可选：限定参与生成的就诊地点ID集合（不传则使用科室全部 locations）
     */
    private List<Integer> locationIds;

    /**
     * 可选：直接映射 schedules 表的列，若提供优先于 rules 默认值
     */
    private Integer totalSlots;        // schedules.total_slots
    private BigDecimal fee;            // schedules.fee
    private String status;             // schedules.status (available/full/cancelled)
    private String remarks;            // schedules.remarks
}


