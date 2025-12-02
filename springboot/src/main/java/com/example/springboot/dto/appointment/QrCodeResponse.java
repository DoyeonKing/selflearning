package com.example.springboot.dto.appointment;

import lombok.Data;

/**
 * 二维码响应DTO
 */
@Data
public class QrCodeResponse {
    private Integer appointmentId;
    private String qrToken;
    private Integer expiresIn;  // 过期时间（秒）
    private Integer refreshInterval;  // 建议刷新间隔（秒）
}





























