package com.example.springboot.dto.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 排班创建请求DTO
 */
@Data
public class ScheduleCreateRequest {
    @NotNull(message = "医生ID不能为空")
    private Integer doctorId;

    @NotNull(message = "出诊日期不能为空")
    private LocalDate scheduleDate;

    @NotNull(message = "时间段ID不能为空")
    private Integer slotId;

    @NotNull(message = "就诊地点ID不能为空")
    private Integer locationId;

    // 可选参数，使用默认值时可不传
    private Integer totalSlots = 10; // 默认总号源数
    private BigDecimal fee = new BigDecimal("5.00"); // 默认挂号费
    private String remarks; // 备注信息
}