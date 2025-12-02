package com.example.springboot.dto.appointment;

import com.example.springboot.dto.patient.PatientResponse; // 导入患者响应DTO
import com.example.springboot.dto.schedule.ScheduleResponse; // 导入排班响应DTO
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentResponse {
    private Integer appointmentId;
    private PatientResponse patient; // 患者信息
    private ScheduleResponse schedule; // 排班信息
    private Integer appointmentNumber;
    private AppointmentStatus status;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime checkInTime;
    private LocalDateTime calledAt; // 叫号时间
    private Boolean isOnTime; // 是否按时签到
    private Integer missedCallCount; // 过号次数
    private LocalDateTime recheckInTime; // 过号后重新签到时间
    private LocalDateTime createdAt;
}
