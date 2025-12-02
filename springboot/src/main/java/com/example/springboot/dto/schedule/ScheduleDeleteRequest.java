package com.example.springboot.dto.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 排班删除请求DTO
 */
@Data
public class ScheduleDeleteRequest {
    @NotNull(message = "医生ID不能为空")
    private Integer doctorId;

    @NotNull(message = "时段ID不能为空")
    private Integer slotId;

    @NotNull(message = "门诊室ID不能为空")
    private Integer locationId;

    @NotNull(message = "日期不能为空")
    private LocalDate scheduleDate;
}