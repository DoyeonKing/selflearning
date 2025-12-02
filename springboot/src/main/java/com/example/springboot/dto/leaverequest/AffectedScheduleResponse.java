package com.example.springboot.dto.leaverequest;

import com.example.springboot.entity.enums.ScheduleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 受影响的排班响应DTO
 */
@Data
public class AffectedScheduleResponse {
    private Integer scheduleId;
    private Integer doctorId;
    private String doctorName;
    private Integer departmentId;
    private String departmentName;
    private LocalDate scheduleDate;
    private Integer slotId;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer locationId;
    private String locationName;
    private Integer totalSlots;
    private Integer bookedSlots;
    private BigDecimal fee;
    private ScheduleStatus status;
    private String remarks;
}
