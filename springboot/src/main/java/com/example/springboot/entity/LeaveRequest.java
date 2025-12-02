package com.example.springboot.entity; // 包名调整

import com.example.springboot.entity.enums.LeaveRequestStatus; // 导入路径调整
import com.example.springboot.entity.enums.RequestType;         // 导入路径调整
import com.example.springboot.converter.LeaveRequestStatusConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 调班/休假申请表
 */
@Entity
@Table(name = "leave_requests")
@Data
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor; // 申请人医生ID

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType; // 申请类型

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // 申请开始时间

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime; // 申请结束时间

    private String reason; // 申请事由

    @Column(name = "proof_document_url", length = 500)
    private String proofDocumentUrl; // 请假证明文件URL（图片或PDF）

    @Convert(converter = LeaveRequestStatusConverter.class)
    @Column(nullable = false)
    private LeaveRequestStatus status; // 审批状态

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Admin approver; // 审批人管理员ID

    @Column(name = "approver_comments")
    private String approverComments; // 审批人员的意见或备注

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 申请创建时间

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 申请更新时间
}
