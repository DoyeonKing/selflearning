package com.example.springboot.dto.leaverequest;

import com.example.springboot.entity.enums.LeaveRequestStatus;
import com.example.springboot.entity.enums.RequestType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaveRequestUpdateRequest {
    private RequestType requestType;

    @FutureOrPresent(message = "开始时间不能早于当前时间")
    private LocalDateTime startTime;

    @Future(message = "结束时间必须在未来")
    private LocalDateTime endTime;

    private String reason;
    private LeaveRequestStatus status;
    private Integer approverId; // 审批人ID
    private String approverComments;
}
