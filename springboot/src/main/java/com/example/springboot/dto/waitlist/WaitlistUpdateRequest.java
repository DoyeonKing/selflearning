package com.example.springboot.dto.waitlist;

import com.example.springboot.entity.enums.WaitlistStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaitlistUpdateRequest {
    private WaitlistStatus status;
    private LocalDateTime notificationSentAt;
}
