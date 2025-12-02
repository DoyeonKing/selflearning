package com.example.springboot.controller;

import com.example.springboot.dto.appointment.AppointmentResponse;
import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.payment.PaymentRequest;
import com.example.springboot.dto.waitlist.*;
import com.example.springboot.entity.enums.WaitlistStatus;
import com.example.springboot.service.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@CrossOrigin(origins = "*")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @Autowired
    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /**
     * 获取患者的所有候补记录
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<WaitlistResponse>> getPatientWaitlist(@PathVariable Long patientId) {
        return ResponseEntity.ok(waitlistService.findByPatientId(patientId));
    }

    /**
     * 获取候补详情
     */
    @GetMapping("/{waitlistId}")
    public ResponseEntity<WaitlistResponse> getWaitlistDetail(@PathVariable Integer waitlistId) {
        return ResponseEntity.ok(waitlistService.findWaitlistById(waitlistId));
    }

    /**
     * 创建候补申请
     */
    @PostMapping
    public ResponseEntity<WaitlistResponse> createWaitlist(@RequestBody WaitlistCreateRequest request) {
        return ResponseEntity.ok(waitlistService.createWaitlist(request));
    }

    /**
     * 取消候补
     */
    @PutMapping("/{waitlistId}/cancel")
    public ResponseEntity<WaitlistResponse> cancelWaitlist(@PathVariable Integer waitlistId) {
        return ResponseEntity.ok(waitlistService.cancelWaitlist(waitlistId));
    }

    /**
     * 支付候补费用（候补转正式预约）
     */
    @PostMapping("/{waitlistId}/pay")
    public ResponseEntity<AppointmentResponse> payForWaitlist(
            @PathVariable Integer waitlistId,
            @RequestBody PaymentRequest paymentData) {
        return ResponseEntity.ok(waitlistService.processWaitlistPayment(waitlistId, paymentData));
    }

    /**
     * 查询排队位置
     */
    @GetMapping("/{waitlistId}/position")
    public ResponseEntity<WaitlistPositionResponse> getWaitlistPosition(@PathVariable Integer waitlistId) {
        return ResponseEntity.ok(waitlistService.getWaitlistPosition(waitlistId));
    }

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<PageResponse<WaitlistManagementResponse>> getWaitlistsBySchedule(
            @PathVariable Integer scheduleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return ResponseEntity.ok(
                waitlistService.getWaitlistsByScheduleForManagement(scheduleId, status, page, size)
        );
    }

}