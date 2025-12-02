package com.example.springboot.controller;

import com.example.springboot.dto.*;
import com.example.springboot.dto.AutoScheduleRequest;
import com.example.springboot.dto.AutoScheduleResponse;
import com.example.springboot.dto.ScheduleRules;
import com.example.springboot.dto.schedule.*;
import com.example.springboot.dto.auto.*;
import com.example.springboot.service.AutoScheduleService;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.service.AutoScheduleService;
import com.example.springboot.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排班管理控制器
 */
@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AutoScheduleService autoScheduleService;
    /**
     * 获取排班列表
     */
    @GetMapping
    public ResponseEntity<Page<ScheduleResponse>> getSchedules(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            System.out.println("=== 后端接收到的请求参数 ===");
            System.out.println("departmentId: " + departmentId + " (类型: "
                    + (departmentId != null ? departmentId.getClass().getSimpleName() : "null") + ")");
            System.out.println("startDate: " + startDate + " (类型: "
                    + (startDate != null ? startDate.getClass().getSimpleName() : "null") + ")");
            System.out.println("endDate: " + endDate + " (类型: "
                    + (endDate != null ? endDate.getClass().getSimpleName() : "null") + ")");
            System.out.println("=============================");

            ScheduleListRequest request = new ScheduleListRequest();
            request.setDepartmentId(departmentId);
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    java.time.LocalDate parsedStartDate = java.time.LocalDate.parse(startDate);
                    request.setStartDate(parsedStartDate);
                    System.out.println("成功解析开始日期: " + parsedStartDate);
                } catch (Exception e) {
                    System.err.println("解析开始日期失败: " + startDate + ", 错误: " + e.getMessage());
                }
            }
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    java.time.LocalDate parsedEndDate = java.time.LocalDate.parse(endDate);
                    request.setEndDate(parsedEndDate);
                    System.out.println("成功解析结束日期: " + parsedEndDate);
                } catch (Exception e) {
                    System.err.println("解析结束日期失败: " + endDate + ", 错误: " + e.getMessage());
                }
            }

            System.out.println("准备调用服务层...");
            Page<ScheduleResponse> schedules = scheduleService.getSchedules(request);
            System.out.println("查询结果: " + schedules.getTotalElements() + " 条记录");
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("查询排班数据时发生错误:");
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 自动生成排班
     */
//    @PostMapping("/auto-generate")
//    public ResponseEntity<AutoScheduleResponse> autoGenerate(@Valid @RequestBody AutoScheduleRequest request) {
//        AutoScheduleResponse resp = autoScheduleService.autoGenerateSchedule(request);
//        return ResponseEntity.ok(resp);
//    }

    /**
     * 获取默认排班规则
     */
//    @GetMapping("/auto-generate/rules")
//    public ResponseEntity<ScheduleRules> getDefaultRules() {
//        return ResponseEntity.ok(new ScheduleRules());
//    }

    /**
     * 根据参数删除排班
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteScheduleByParams(@Valid @RequestBody ScheduleDeleteRequest request) {
        try {
            System.out.println("=== 后端接收到的删除请求 ===");
            System.out.println("doctorId: " + request.getDoctorId());
            System.out.println("slotId: " + request.getSlotId());
            System.out.println("locationId: " + request.getLocationId());
            System.out.println("scheduleDate: " + request.getScheduleDate() + " (类型: "
                    + request.getScheduleDate().getClass().getSimpleName() + ")");
            System.out.println("=============================");

            scheduleService.deleteScheduleByParams(request);
            System.out.println("✅ 排班删除成功");
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            System.err.println("❌ 资源未找到: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            System.err.println("❌ 请求错误: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("❌ 删除排班时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 查询所有排班记录（支持分页参数）
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ScheduleResponse>> searchAllSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            // 构建空的查询条件，查询所有记录
            ScheduleListRequest request = new ScheduleListRequest();
            Page<ScheduleResponse> schedules = scheduleService.getSchedules(request, pageable);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("查询所有排班记录时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    

    /**
     * 创建排班
     */
    @PostMapping("/create")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        try {
            ScheduleResponse schedule = scheduleService.createSchedule(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("创建排班时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取单个排班详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getScheduleById(@PathVariable Integer id) {
        try {
            ScheduleResponse schedule = scheduleService.getScheduleById(id);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            System.err.println("获取排班详情时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 更新单个排班
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable Integer id,
            @RequestBody ScheduleUpdateRequest request) {
        try {
            ScheduleResponse schedule = scheduleService.updateSchedule(id, request);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            System.err.println("更新排班时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 批量更新排班
     */
    @PutMapping("/batch-update")
    public ResponseEntity<List<ScheduleResponse>> batchUpdateSchedules(
            @RequestBody ScheduleBatchUpdateRequest request) {
        try {
            System.out.println("收到批量更新请求: " + request.getUpdates().size() + " 条记录");
            List<ScheduleResponse> schedules = scheduleService.batchUpdateSchedules(request);
            System.out.println("批量更新完成: " + schedules.size() + " 条记录");
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("批量更新排班时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // /**
    // * 创建排班
    // */
    // @PostMapping
    // public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody
    // ScheduleResponse request) {
    // try {
    // ScheduleResponse schedule = scheduleService.createSchedule(request);
    // return ResponseEntity.ok(schedule);
    // } catch (Exception e) {
    // System.err.println("创建排班时发生错误: " + e.getMessage());
    // return ResponseEntity.status(500).build();
    // }
    // }

    /**
     * 删除排班
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer id) {
        try {
            scheduleService.deleteSchedule(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("删除排班时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 自动生成排班
     */
    @PostMapping("/auto-generate")
    public ResponseEntity<AutoScheduleResponse> autoGenerateSchedule(
            @Valid @RequestBody AutoScheduleRequest request) {
        try {
            System.out.println("=== 收到自动排班请求 ===");
            System.out.println("departmentId: " + request.getDepartmentId());
            System.out.println("startDate: " + request.getStartDate());
            System.out.println("endDate: " + request.getEndDate());
            System.out.println("previewOnly: " + request.getPreviewOnly());
            System.out.println("=============================");
            
            AutoScheduleResponse response = autoScheduleService.autoGenerateSchedule(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 参数错误: " + e.getMessage());
            AutoScheduleResponse errorResponse = new AutoScheduleResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.err.println("❌ 自动排班时发生错误: " + e.getMessage());
            e.printStackTrace();
            AutoScheduleResponse errorResponse = new AutoScheduleResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("自动排班失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取默认排班规则
     */
    @GetMapping("/auto-generate/rules")
    public ResponseEntity<ScheduleRules> getDefaultRules() {
        try {
            ScheduleRules rules = new ScheduleRules();
            // 默认值已在ScheduleRules类中设置
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            System.err.println("获取默认规则时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 根据医生ID获取排班列表
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Page<ScheduleResponse>> getSchedulesByDoctorId(
            @PathVariable Integer doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            System.out.println("=== 查询医生排班 ===");
            System.out.println("doctorId: " + doctorId);
            System.out.println("startDate: " + startDate);
            System.out.println("endDate: " + endDate);
            System.out.println("page: " + page + ", size: " + size);
            System.out.println("====================");

            Pageable pageable = PageRequest.of(page, size);
            Page<ScheduleResponse> schedules = scheduleService.getSchedulesByDoctorId(
                    doctorId, startDate, endDate, pageable);
            
            System.out.println("查询结果: " + schedules.getTotalElements() + " 条记录");
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("查询医生排班时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
