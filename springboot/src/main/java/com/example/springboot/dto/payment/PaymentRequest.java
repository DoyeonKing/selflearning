// PaymentRequest.java (用于接收支付信息)
package com.example.springboot.dto.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private String paymentMethod;
    private String transactionId;
}