package com.example.springboot.controller;

import com.example.springboot.dto.slotapplication.SlotApplicationCreateRequest;
import com.example.springboot.dto.slotapplication.SlotApplicationResponse;
import com.example.springboot.dto.slotapplication.SlotApplicationUpdateRequest;
import com.example.springboot.entity.enums.SlotApplicationStatus;
import com.example.springboot.service.SlotApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 加号申请控制器
 */
@RestController
@RequestMapping("/api/slot-applications")
@CrossOrigin(origins = "*")
public class SlotApplicationController {

    private final SlotApplicationService slotApplicationService;

    @Autowired
    public SlotApplicationController(SlotApplicationService slotApplicationService) {
        this.slotApplicationService = slotApplicationService;
    }

    /**
     * 创建加号申请
     */
    @PostMapping
    public ResponseEntity<SlotApplicationResponse> createSlotApplication(
            @Valid @RequestBody SlotApplicationCreateRequest request) {
        SlotApplicationResponse response = slotApplicationService.createSlotApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取医生的所有加号申请
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<SlotApplicationResponse>> getApplicationsByDoctor(
            @PathVariable Integer doctorId) {
        List<SlotApplicationResponse> applications = slotApplicationService.getApplicationsByDoctor(doctorId);
        return ResponseEntity.ok(applications);
    }

    /**
     * 获取医生的指定状态的加号申请
     */
    @GetMapping("/doctor/{doctorId}/status/{status}")
    public ResponseEntity<List<SlotApplicationResponse>> getApplicationsByDoctorAndStatus(
            @PathVariable Integer doctorId,
            @PathVariable SlotApplicationStatus status) {
        List<SlotApplicationResponse> applications = 
            slotApplicationService.getApplicationsByDoctorAndStatus(doctorId, status);
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据ID获取加号申请
     */
    @GetMapping("/{id}")
    public ResponseEntity<SlotApplicationResponse> getApplicationById(@PathVariable Integer id) {
        SlotApplicationResponse application = slotApplicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    /**
     * 更新加号申请（审批）
     */
    @PutMapping("/{id}")
    public ResponseEntity<SlotApplicationResponse> updateSlotApplication(
            @PathVariable Integer id,
            @Valid @RequestBody SlotApplicationUpdateRequest request) {
        SlotApplicationResponse response = slotApplicationService.updateSlotApplication(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消加号申请
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<SlotApplicationResponse> cancelSlotApplication(
            @PathVariable Integer id,
            @RequestParam Integer doctorId) {
        SlotApplicationResponse response = slotApplicationService.cancelSlotApplication(id, doctorId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有加号申请（管理员用）
     */
    @GetMapping
    public ResponseEntity<List<SlotApplicationResponse>> getAllApplications() {
        List<SlotApplicationResponse> applications = slotApplicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据状态获取加号申请（管理员用）
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SlotApplicationResponse>> getApplicationsByStatus(
            @PathVariable SlotApplicationStatus status) {
        List<SlotApplicationResponse> applications = slotApplicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }
}
