package com.example.springboot.controller;

import com.example.springboot.dto.leaverequest.LeaveRequestCreateRequest;
import com.example.springboot.dto.leaverequest.LeaveRequestResponse;
import com.example.springboot.dto.leaverequest.LeaveRequestUpdateRequest;
import com.example.springboot.entity.enums.LeaveRequestStatus;
import com.example.springboot.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 请假申请控制器
 */
@RestController
@RequestMapping("/api/leave-requests")
@CrossOrigin(origins = "*")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @Autowired
    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    /**
     * 获取所有请假申请
     */
    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> getAllLeaveRequests() {
        List<LeaveRequestResponse> requests = leaveRequestService.findAllLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * 根据ID获取请假申请
     */
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequestById(@PathVariable Integer id) {
        LeaveRequestResponse request = leaveRequestService.findLeaveRequestById(id);
        return ResponseEntity.ok(request);
    }

    /**
     * 获取指定医生的请假申请
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequestsByDoctor(@PathVariable Integer doctorId) {
        List<LeaveRequestResponse> requests = leaveRequestService.findLeaveRequestsByDoctor(doctorId);
        return ResponseEntity.ok(requests);
    }

    /**
     * 根据状态获取请假申请
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequestsByStatus(@PathVariable LeaveRequestStatus status) {
        List<LeaveRequestResponse> requests = leaveRequestService.findLeaveRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }

    /**
     * 创建请假申请
     */
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody LeaveRequestCreateRequest request) {
        LeaveRequestResponse createdRequest = leaveRequestService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    /**
     * 更新请假申请（主要用于审批）
     */
    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> updateLeaveRequest(
            @PathVariable Integer id,
            @Valid @RequestBody LeaveRequestUpdateRequest request) {
        LeaveRequestResponse updatedRequest = leaveRequestService.updateLeaveRequest(id, request);
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * 删除请假申请
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveRequest(@PathVariable Integer id) {
        leaveRequestService.deleteLeaveRequest(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 审批请假申请
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Integer id,
            @RequestParam Integer approverId,
            @RequestParam(required = false) String comments) {
        
        System.out.println("=== 批准请假申请 ===");
        System.out.println("请假申请ID: " + id);
        System.out.println("审批人ID: " + approverId);
        System.out.println("审批意见: " + comments);
        
        LeaveRequestUpdateRequest updateRequest = new LeaveRequestUpdateRequest();
        updateRequest.setStatus(LeaveRequestStatus.APPROVED);
        updateRequest.setApproverId(approverId);
        updateRequest.setApproverComments(comments);
        
        LeaveRequestResponse updatedRequest = leaveRequestService.updateLeaveRequest(id, updateRequest);
        System.out.println("更新后的状态: " + updatedRequest.getStatus());
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * 拒绝请假申请
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Integer id,
            @RequestParam Integer approverId,
            @RequestParam(required = false) String comments) {
        
        System.out.println("=== 拒绝请假申请 ===");
        System.out.println("请假申请ID: " + id);
        System.out.println("审批人ID: " + approverId);
        System.out.println("拒绝理由: " + comments);
        
        LeaveRequestUpdateRequest updateRequest = new LeaveRequestUpdateRequest();
        updateRequest.setStatus(LeaveRequestStatus.REJECTED);
        updateRequest.setApproverId(approverId);
        updateRequest.setApproverComments(comments);
        
        LeaveRequestResponse updatedRequest = leaveRequestService.updateLeaveRequest(id, updateRequest);
        System.out.println("更新后的状态: " + updatedRequest.getStatus());
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * 获取请假批准详情（包含受影响的排班和可用替班医生）
     */
    @GetMapping("/{id}/approval-detail")
    public ResponseEntity<com.example.springboot.dto.leaverequest.LeaveApprovalDetailResponse> getLeaveApprovalDetail(
            @PathVariable Integer id) {
        com.example.springboot.dto.leaverequest.LeaveApprovalDetailResponse detail = 
                leaveRequestService.getLeaveApprovalDetail(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * 确认替班安排
     */
    @PostMapping("/confirm-substitution")
    public ResponseEntity<com.example.springboot.dto.leaverequest.SubstituteConfirmResponse> confirmSubstitution(
            @Valid @RequestBody com.example.springboot.dto.leaverequest.SubstituteConfirmRequest request) {
        com.example.springboot.dto.leaverequest.SubstituteConfirmResponse response = 
                leaveRequestService.confirmSubstitution(request);
        return ResponseEntity.ok(response);
    }
}
