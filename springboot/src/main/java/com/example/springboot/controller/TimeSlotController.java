package com.example.springboot.controller;

import com.example.springboot.common.Result; // 假设你有通用的 Result 类
import com.example.springboot.dto.timeslot.TimeSlotResponse;
import com.example.springboot.service.TimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timeslots") // 定义基础路径
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @Autowired
    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    /**
     * 获取所有时间段列表
     * URL: GET /api/timeslots
     */
    @GetMapping
    public ResponseEntity<Result> getAllTimeSlots() {
        try {
            // 调用 Service 层的方法获取所有时间段 DTO 列表
            List<TimeSlotResponse> timeSlots = timeSlotService.findAllTimeSlots();
            // 使用 Result 类包装成功响应
            return ResponseEntity.ok(Result.success(timeSlots));
        } catch (Exception e) {
            // 处理可能发生的异常，并返回错误信息
            return ResponseEntity.ok(Result.error("500", "获取时间段列表失败: " + e.getMessage()));
        }
    }

    // 你可以根据需要在这里添加其他与 TimeSlot 相关的 Controller 方法
    // 例如：获取单个 TimeSlot, 创建, 更新, 删除等
}