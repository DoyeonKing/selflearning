package com.example.springboot.dto.payment;

import jakarta.validation.constraints.Size;
import lombok.Data;
import com.example.springboot.entity.enums.PaymentStatus;

@Data
public class AppointmentPaymentRequest {
    // 仅包含支付相关字段
    private PaymentStatus paymentStatus;

    @Size(max = 50, message = "支付方式长度不能超过50个字符")
    private String paymentMethod;

    private String transactionId;
}