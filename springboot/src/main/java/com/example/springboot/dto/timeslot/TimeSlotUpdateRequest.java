package com.example.springboot.dto.timeslot;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlotUpdateRequest {
    @Size(max = 100, message = "时段名称长度不能超过100个字符")
    private String slotName;

    private LocalTime startTime;
    private LocalTime endTime;
}
