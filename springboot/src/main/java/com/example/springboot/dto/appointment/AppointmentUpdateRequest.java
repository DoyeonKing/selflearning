package com.example.springboot.dto.appointment;

import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentUpdateRequest {
    @Min(value = 1, message = "就诊序号必须大于0")
    private Integer appointmentNumber;

    private AppointmentStatus status;

    private PaymentStatus paymentStatus;

    @Size(max = 50, message = "支付方式长度不能超过50个字符")
    private String paymentMethod;

    private String transactionId;

    private LocalDateTime checkInTime;
}
