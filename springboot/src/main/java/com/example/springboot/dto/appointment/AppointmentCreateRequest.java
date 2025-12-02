package com.example.springboot.dto.appointment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentCreateRequest {
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "排班ID不能为空")
    private Integer scheduleId;

    @Min(value = 1, message = "就诊序号必须大于0")
    private Integer appointmentNumber; // 可以由系统生成，也可以在预约时指定
}
