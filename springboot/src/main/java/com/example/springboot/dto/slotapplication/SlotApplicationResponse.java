package com.example.springboot.dto.slotapplication;

import com.example.springboot.entity.enums.SlotApplicationStatus;
import com.example.springboot.entity.enums.UrgencyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 加号申请响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotApplicationResponse {
    private Integer applicationId;
    private Integer doctorId;
    private String doctorName;
    private Integer scheduleId;
    private String scheduleDate;
    private String timeSlot;  // MORNING 或 AFTERNOON
    private String startTime;
    private String endTime;
    private String location;
    private Integer addedSlots;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private UrgencyLevel urgencyLevel;
    private String reason;
    private SlotApplicationStatus status;
    private Integer approverId;
    private String approverName;
    private String approverComments;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
