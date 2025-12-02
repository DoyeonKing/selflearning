package com.example.springboot.dto.leaverequest;

import com.example.springboot.dto.admin.AdminResponse; // 导入管理员响应DTO
import com.example.springboot.dto.doctor.DoctorResponse; // 导入医生响应DTO
import com.example.springboot.entity.enums.LeaveRequestStatus;
import com.example.springboot.entity.enums.RequestType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaveRequestResponse {
    private Integer requestId;
    private DoctorResponse doctor; // 申请医生信息
    private RequestType requestType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String proofDocumentUrl; // 请假证明文件URL
    private LeaveRequestStatus status;
    private AdminResponse approver; // 审批管理员信息
    private String approverComments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
