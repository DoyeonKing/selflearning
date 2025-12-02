package com.example.springboot.controller;

import com.example.springboot.dto.appointment.AppointmentResponse;
import com.example.springboot.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员预约管理控制器
 * 提供退款等管理员专用功能
 */
@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')") // 需要管理员权限
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * 退款接口
     * 只有已支付的预约才能退款
     */
    @PostMapping("/{appointmentId}/refund")
    public ResponseEntity<AppointmentResponse> refundAppointment(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.refundAppointment(appointmentId));
    }
}

