package com.example.springboot.controller;

import com.example.springboot.dto.appointment.AppointmentCreateRequest;
import com.example.springboot.dto.appointment.AppointmentResponse;
import com.example.springboot.dto.appointment.AppointmentUpdateRequest;
import com.example.springboot.dto.appointment.CheckInRequest;
import com.example.springboot.dto.appointment.CheckInResponse;
import com.example.springboot.dto.appointment.QrCodeResponse;
import com.example.springboot.service.AppointmentService;
import com.example.springboot.service.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final WaitlistService waitlistService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, WaitlistService waitlistService) {
        this.appointmentService = appointmentService;
        this.waitlistService = waitlistService;
    }

    /**
     * 获取患者的所有预约
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.findByPatientId(patientId));
    }

    /**
     * 获取指定医生的预约患者列表
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(@PathVariable Integer doctorId) {
        return ResponseEntity.ok(appointmentService.findByDoctorId(doctorId));
    }

    /**
     * 获取患者即将就诊的预约
     */
    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.findUpcomingByPatientId(patientId));
    }

    /**
     * 创建预约
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody AppointmentCreateRequest request) {
        return ResponseEntity.ok(appointmentService.createAppointment(request));
    }

    /**
     * 现场挂号（分诊台辅助患者挂号）
     */
    @PostMapping("/walk-in")
    public ResponseEntity<AppointmentResponse> createWalkInAppointment(@RequestBody AppointmentCreateRequest request) {
        return ResponseEntity.ok(appointmentService.createWalkInAppointment(request));
    }

    /**
     * 取消预约
     */
    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    /**
     * 获取预约详情
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentDetail(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.findAppointmentById(appointmentId));
    }

    /**
     * 更新预约支付状态
     */
    @PutMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> updateAppointmentPayment(
            @PathVariable Integer appointmentId,
            @RequestBody AppointmentUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointment(appointmentId, request));
    }

    /**
     * 支付挂号费用
     */
    @PostMapping("/{appointmentId}/pay")
    public ResponseEntity<AppointmentResponse> payForAppointment(
            @PathVariable Integer appointmentId,
            @RequestBody AppointmentUpdateRequest paymentData) {
        return ResponseEntity.ok(appointmentService.processPayment(appointmentId, paymentData));
    }

    /**
     * 测试Redis连接
     */
    @GetMapping("/test/redis")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试写入
            redisTemplate.opsForValue().set("test:key", "test:value", 60, TimeUnit.SECONDS);
            
            // 测试读取
            String value = redisTemplate.opsForValue().get("test:key");
            
            result.put("status", "success");
            result.put("message", "Redis连接成功");
            result.put("value", value);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis连接失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 生成预约二维码Token
     */
    @GetMapping("/{appointmentId}/qr-code")
    public ResponseEntity<QrCodeResponse> generateQrCode(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.generateQrCode(appointmentId));
    }

    /**
     * 扫码签到
     */
    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(@RequestBody CheckInRequest request) {
        return ResponseEntity.ok(appointmentService.checkIn(request));
    }

    /**
     * 清除预约签到时间（管理员功能，用于测试或误操作）
     */
    @DeleteMapping("/{appointmentId}/check-in")
    public ResponseEntity<AppointmentResponse> clearCheckIn(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.clearCheckIn(appointmentId));
    }

    /**
     * 获取叫号队列（已签到但未就诊的预约列表）
     */
    @GetMapping("/schedule/{scheduleId}/call-queue")
    public ResponseEntity<List<AppointmentResponse>> getCallQueue(@PathVariable Integer scheduleId) {
        return ResponseEntity.ok(appointmentService.getCallQueue(scheduleId));
    }

    /**
     * 获取下一个应该叫号的预约
     */
    @GetMapping("/schedule/{scheduleId}/next-to-call")
    public ResponseEntity<AppointmentResponse> getNextAppointmentToCall(@PathVariable Integer scheduleId) {
        AppointmentResponse response = appointmentService.getNextAppointmentToCall(scheduleId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 执行叫号
     */
    @PostMapping("/{appointmentId}/call")
    public ResponseEntity<AppointmentResponse> callAppointment(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.callAppointment(appointmentId));
    }

    /**
     * 标记过号（仅标记，不重新排队，需要患者重新扫码才能重新排队）
     */
    @PostMapping("/{appointmentId}/mark-missed")
    public ResponseEntity<AppointmentResponse> markMissedCall(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.markMissedCall(appointmentId));
    }

    /**
     * 过号后重新签到（序号排到后面）
     * 规则：患者重新扫码后调用此接口，系统将其放在当前时段最后一位
     */
    @PostMapping("/{appointmentId}/recheck-in")
    public ResponseEntity<AppointmentResponse> recheckInAfterMissedCall(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.recheckInAfterMissedCall(appointmentId));
    }

    /**
     * 标记就诊完成（医生完成就诊后调用，会自动叫号下一位）
     */
    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
    }
}