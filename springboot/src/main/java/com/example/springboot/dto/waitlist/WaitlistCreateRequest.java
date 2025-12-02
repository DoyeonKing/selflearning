package com.example.springboot.dto.waitlist;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaitlistCreateRequest {
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "排班ID不能为空")
    private Integer scheduleId;
}
