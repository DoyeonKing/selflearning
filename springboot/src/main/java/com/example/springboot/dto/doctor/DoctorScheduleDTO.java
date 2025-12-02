package com.example.springboot.dto.doctor;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

@Data
public class DoctorScheduleDTO {
    // 排班ID
    private Integer scheduleId;
    // 日期
    private LocalDate scheduleDate;
    // 星期几
    private String dayOfWeek;
    // 时段ID
    private Integer slotId;
    // 时段名称
    private String slotName;
    // 开始时间
    private LocalTime startTime;
    // 结束时间
    private LocalTime endTime;
    // 诊室名称
    private String locationName;
    // 总号源
    private Integer totalSlots;
    // 已预约
    private Integer bookedSlots;
    // 剩余号源
    private Integer remainingSlots;
    // 挂号费用
    private BigDecimal fee;
    // 排班状态
    private String status;
    // 备注
    private String remarks;
}