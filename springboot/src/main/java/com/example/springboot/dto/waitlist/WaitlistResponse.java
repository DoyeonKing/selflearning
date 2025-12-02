package com.example.springboot.dto.waitlist;

import com.example.springboot.dto.patient.PatientResponse; // 导入患者响应DTO
import com.example.springboot.dto.schedule.ScheduleResponse; // 导入排班响应DTO
import com.example.springboot.entity.enums.WaitlistStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaitlistResponse {
    private Integer waitlistId;
    private PatientResponse patient; // 患者信息
    private ScheduleResponse schedule; // 排班信息
    private WaitlistStatus status;
    private LocalDateTime notificationSentAt;
    private LocalDateTime createdAt;
    private Integer queuePosition; // 排队位置（仅 waiting 状态有效）
}
