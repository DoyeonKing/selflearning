package com.example.springboot.dto.leaverequest;

import lombok.Data;

import java.util.List;

/**
 * 请假批准详情响应DTO（包含受影响的排班和可用替班医生）
 */
@Data
public class LeaveApprovalDetailResponse {
    private LeaveRequestResponse leaveRequest;
    private List<AffectedScheduleWithSubstitutes> affectedSchedules;
    
    @Data
    public static class AffectedScheduleWithSubstitutes {
        private AffectedScheduleResponse schedule;
        private List<SubstituteDoctorResponse> substituteDoctors;
    }
}
