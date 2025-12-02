package com.example.springboot.dto.appointment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扫码签到响应DTO
 */
@Data
public class CheckInResponse {
    private Integer appointmentId;
    private Integer scheduleId; // 排班ID，用于前端跳转到叫号队列
    private String patientName;
    private String departmentName;
    private String doctorName;
    private LocalDateTime checkInTime;
    private Integer appointmentNumber;
}



