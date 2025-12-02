package com.example.springboot.dto.leaverequest;

import lombok.Data;

import java.util.List;

/**
 * 替班确认响应DTO
 */
@Data
public class SubstituteConfirmResponse {
    private Integer leaveRequestId;
    private Integer successCount;
    private Integer failedCount;
    private Integer cancelledCount;
    private List<String> messages;
}
