package com.example.springboot.dto.waitlist;

import com.example.springboot.entity.enums.WaitlistStatus;
import lombok.Data;

@Data
public class WaitlistPositionResponse {
    private Integer waitlistId;
    private Integer scheduleId;
    private WaitlistStatus status;
    private Integer position;
    private Integer totalWaiting;
    private String estimatedTime;
}