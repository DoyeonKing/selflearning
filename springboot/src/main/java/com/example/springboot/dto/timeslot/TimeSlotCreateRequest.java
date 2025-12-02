package com.example.springboot.dto.timeslot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlotCreateRequest {
    @Size(max = 100, message = "时段名称长度不能超过100个字符")
    private String slotName;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;
}
