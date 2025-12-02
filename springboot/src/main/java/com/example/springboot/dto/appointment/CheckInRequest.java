package com.example.springboot.dto.appointment;

import lombok.Data;

/**
 * 扫码签到请求DTO
 */
@Data
public class CheckInRequest {
    private String qrToken;  // 二维码Token
    private Integer doctorId;  // 医生ID（可选，用于验证）
}





























