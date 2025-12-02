package com.example.springboot.dto.leaverequest;

import lombok.Data;

import java.util.Map;

/**
 * 替班确认请求DTO
 */
@Data
public class SubstituteConfirmRequest {
    /**
     * 请假申请ID
     */
    private Integer leaveRequestId;
    
    /**
     * 替班安排
     * Key: scheduleId (排班ID)
     * Value: substituteDoctorId (替班医生ID，null表示取消排班)
     */
    private Map<Integer, Integer> substitutions;
}
