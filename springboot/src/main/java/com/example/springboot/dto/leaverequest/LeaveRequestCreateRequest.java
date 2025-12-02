package com.example.springboot.dto.leaverequest;

import com.example.springboot.entity.enums.RequestType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaveRequestCreateRequest {
    @NotNull(message = "医生ID不能为空")
    private Integer doctorId;

    @NotNull(message = "申请类型不能为空")
    private RequestType requestType;

    @NotNull(message = "开始时间不能为空")
    @FutureOrPresent(message = "开始时间不能早于当前时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须在未来")
    private LocalDateTime endTime;

    @NotBlank(message = "申请事由不能为空")
    private String reason;

    // 请假证明文件URL（可选）
    private String proofDocumentUrl;
}
