package com.example.springboot.dto.leaverequest;

import lombok.Data;

/**
 * 替班医生响应DTO
 */
@Data
public class SubstituteDoctorResponse {
    private Integer doctorId;
    private String identifier;
    private String fullName;
    private String title;
    private Integer titleLevel;
    private String specialty;
    private String photoUrl;
    private Integer departmentId;
    private String departmentName;
    
    /**
     * 匹配等级：high-高（平级）、medium-平（升级）、low-低（降级）
     */
    private String matchLevel;
    
    /**
     * 匹配原因说明
     */
    private String matchReason;
    
    /**
     * 是否有冲突（该时间段已有排班）
     */
    private Boolean hasConflict;
    
    /**
     * 是否在请假中（该时间段有已批准的请假）
     */
    private Boolean isOnLeave;
}
