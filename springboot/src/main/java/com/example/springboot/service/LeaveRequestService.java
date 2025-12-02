package com.example.springboot.service;

import com.example.springboot.dto.leaverequest.*;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.LeaveRequest;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.LeaveRequestStatus;
import com.example.springboot.entity.enums.ScheduleStatus;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AdminRepository;
import com.example.springboot.repository.DoctorRepository;
import com.example.springboot.repository.LeaveRequestRepository;
import com.example.springboot.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    private final ScheduleRepository scheduleRepository;
    private final DoctorService doctorService; // For converting doctor entity to DTO
    private final AdminService adminService; // For converting admin entity to DTO
    private final NotificationService notificationService; // For sending notifications
    private final com.example.springboot.repository.AppointmentRepository appointmentRepository;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               DoctorRepository doctorRepository,
                               AdminRepository adminRepository,
                               ScheduleRepository scheduleRepository,
                               DoctorService doctorService,
                               AdminService adminService,
                               NotificationService notificationService,
                               com.example.springboot.repository.AppointmentRepository appointmentRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
        this.scheduleRepository = scheduleRepository;
        this.doctorService = doctorService;
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findAllLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestRepository.findAll();
        // 强制加载医生信息以避免LazyInitializationException
        requests.forEach(request -> {
            if (request.getDoctor() != null) {
                request.getDoctor().getFullName(); // 触发lazy loading
                if (request.getDoctor().getDepartment() != null) {
                    request.getDoctor().getDepartment().getName(); // 触发department lazy loading
                }
            }
            if (request.getApprover() != null) {
                request.getApprover().getFullName(); // 触发approver lazy loading
            }
        });
        return requests.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse findLeaveRequestById(Integer id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest not found with id " + id));
        return convertToResponseDto(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findLeaveRequestsByDoctor(Integer doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));
        
        // 检查医生是否已删除
        if (doctor.getStatus() == DoctorStatus.deleted) {
            throw new BadRequestException("医生已删除，无法查询请假记录");
        }
        
        List<LeaveRequest> requests = leaveRequestRepository.findByDoctor(doctor);
        // 强制加载医生和审批人信息以避免LazyInitializationException
        requests.forEach(request -> {
            if (request.getDoctor() != null) {
                request.getDoctor().getFullName(); // 触发lazy loading
                if (request.getDoctor().getDepartment() != null) {
                    request.getDoctor().getDepartment().getName(); // 触发department lazy loading
                }
            }
            if (request.getApprover() != null) {
                request.getApprover().getFullName(); // 触发approver lazy loading
            }
        });
        
        return requests.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findLeaveRequestsByStatus(LeaveRequestStatus status) {
        List<LeaveRequest> requests = leaveRequestRepository.findByStatus(status);
        // 强制加载医生信息以避免LazyInitializationException
        requests.forEach(request -> {
            if (request.getDoctor() != null) {
                request.getDoctor().getFullName(); // 触发lazy loading
                if (request.getDoctor().getDepartment() != null) {
                    request.getDoctor().getDepartment().getName(); // 触发department lazy loading
                }
            }
            if (request.getApprover() != null) {
                request.getApprover().getFullName(); // 触发approver lazy loading
            }
        });
        return requests.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(LeaveRequestCreateRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + request.getDoctorId()));

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time cannot be after end time.");
        }

        // 可以添加逻辑：检查医生在申请时段是否有已安排的排班，如果有，需要特殊处理
        // List<Schedule> conflictingSchedules = scheduleRepository.findByDoctorAndScheduleDateBetween(...)
        // 简单处理：当前仅创建申请

        LeaveRequest leaveRequest = new LeaveRequest();
        BeanUtils.copyProperties(request, leaveRequest, "doctorId");
        leaveRequest.setDoctor(doctor);
        leaveRequest.setStatus(LeaveRequestStatus.PENDING); // 初始状态为待审批
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        return convertToResponseDto(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestResponse updateLeaveRequest(Integer id, LeaveRequestUpdateRequest request) {
        System.out.println("=== Service: updateLeaveRequest ===");
        System.out.println("请假申请ID: " + id);
        System.out.println("请求状态: " + request.getStatus());
        System.out.println("审批人ID: " + request.getApproverId());
        
        LeaveRequest existingRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest not found with id " + id));

        System.out.println("现有请假状态: " + existingRequest.getStatus());
        
        // 只有PENDING状态的申请可以修改或审批
        if (existingRequest.getStatus() != LeaveRequestStatus.PENDING && request.getStatus() != null && request.getStatus() != existingRequest.getStatus()) {
            throw new BadRequestException("Only pending leave requests can be updated or approved/rejected.");
        }

        if (request.getRequestType() != null) existingRequest.setRequestType(request.getRequestType());
        if (request.getStartTime() != null) existingRequest.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) existingRequest.setEndTime(request.getEndTime());
        if (request.getReason() != null) existingRequest.setReason(request.getReason());

        if (request.getStartTime() != null && request.getEndTime() != null && request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time cannot be after end time.");
        }

        if (request.getStatus() != null) {
            System.out.println("设置新状态: " + request.getStatus());
            existingRequest.setStatus(request.getStatus());
            if (request.getStatus() == LeaveRequestStatus.APPROVED) {
                System.out.println("处理批准逻辑...");
                // 如果申请被批准，需要更新相关排班的状态
                updateAffectedSchedules(existingRequest.getDoctor(), existingRequest.getStartTime(), existingRequest.getEndTime(), ScheduleStatus.cancelled);
                
                // 发送批准通知
                try {
                    notificationService.sendLeaveApprovedNotification(
                        existingRequest.getDoctor().getDoctorId(),
                        existingRequest.getRequestId(),
                        existingRequest.getStartTime().toString(),
                        existingRequest.getEndTime().toString(),
                        request.getApproverComments()
                    );
                } catch (Exception e) {
                    // 通知发送失败不影响主流程
                    System.err.println("Failed to send leave approved notification: " + e.getMessage());
                }
            } else if (request.getStatus() == LeaveRequestStatus.REJECTED) {
                // 如果被拒绝，不做排班更改
                
                // 发送拒绝通知
                try {
                    notificationService.sendLeaveRejectedNotification(
                        existingRequest.getDoctor().getDoctorId(),
                        existingRequest.getRequestId(),
                        existingRequest.getStartTime().toString(),
                        existingRequest.getEndTime().toString(),
                        request.getApproverComments()
                    );
                } catch (Exception e) {
                    // 通知发送失败不影响主流程
                    System.err.println("Failed to send leave rejected notification: " + e.getMessage());
                }
            }
        }

        if (request.getApproverId() != null) {
            Admin approver = adminRepository.findById(request.getApproverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Approver Admin not found with id " + request.getApproverId()));
            existingRequest.setApprover(approver);
        }
        if (request.getApproverComments() != null) existingRequest.setApproverComments(request.getApproverComments());

        existingRequest.setUpdatedAt(LocalDateTime.now());
        LeaveRequest savedRequest = leaveRequestRepository.save(existingRequest);
        System.out.println("保存后的状态: " + savedRequest.getStatus());
        System.out.println("保存后的审批人ID: " + (savedRequest.getApprover() != null ? savedRequest.getApprover().getAdminId() : "null"));
        return convertToResponseDto(savedRequest);
    }

    @Transactional
    public void deleteLeaveRequest(Integer id) {
        if (!leaveRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("LeaveRequest not found with id " + id);
        }
        leaveRequestRepository.deleteById(id);
    }

    /**
     * Helper method to update schedules affected by an approved leave request.
     */
    private void updateAffectedSchedules(Doctor doctor, LocalDateTime startTime, LocalDateTime endTime, ScheduleStatus newStatus) {
        java.time.LocalDate startDate = startTime.toLocalDate();
        java.time.LocalDate endDate = endTime.toLocalDate();

        List<Schedule> affectedSchedules = scheduleRepository.findByDoctorAndScheduleDateBetween(doctor, startDate, endDate);

        for (Schedule schedule : affectedSchedules) {
            LocalDateTime scheduleStartTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getStartTime());
            LocalDateTime scheduleEndTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getEndTime());

            // 检查排班时间是否与请假时间段重叠
            boolean overlaps = !scheduleEndTime.isBefore(startTime) && !scheduleStartTime.isAfter(endTime);

            if (overlaps) {
                schedule.setStatus(newStatus);
                scheduleRepository.save(schedule);
                // TODO: Handle existing appointments for these canceled schedules (e.g., notify patients, automatically cancel)
            }
        }
    }


    /**
     * 获取请假批准详情（包含受影响的排班和可用替班医生）
     */
    public LeaveApprovalDetailResponse getLeaveApprovalDetail(Integer requestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest not found with id " + requestId));
        
        LeaveApprovalDetailResponse response = new LeaveApprovalDetailResponse();
        response.setLeaveRequest(convertToResponseDto(leaveRequest));
        
        // 获取受影响的排班
        List<Schedule> affectedSchedules = getAffectedSchedules(
                leaveRequest.getDoctor(), 
                leaveRequest.getStartTime(), 
                leaveRequest.getEndTime()
        );
        
        // 为每个受影响的排班生成替班医生列表
        List<LeaveApprovalDetailResponse.AffectedScheduleWithSubstitutes> scheduleList = new ArrayList<>();
        for (Schedule schedule : affectedSchedules) {
            LeaveApprovalDetailResponse.AffectedScheduleWithSubstitutes item = 
                    new LeaveApprovalDetailResponse.AffectedScheduleWithSubstitutes();
            item.setSchedule(convertToAffectedScheduleResponse(schedule));
            item.setSubstituteDoctors(getSuggestedSubstituteDoctors(
                    schedule, 
                    leaveRequest.getDoctor()
            ));
            scheduleList.add(item);
        }
        
        response.setAffectedSchedules(scheduleList);
        return response;
    }
    
    /**
     * 获取受影响的排班列表
     */
    private List<Schedule> getAffectedSchedules(Doctor doctor, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        
        List<Schedule> affectedSchedules = scheduleRepository.findByDoctorAndScheduleDateBetween(doctor, startDate, endDate);
        
        // 过滤出真正重叠的排班
        return affectedSchedules.stream()
                .filter(schedule -> {
                    LocalDateTime scheduleStartTime = LocalDateTime.of(
                            schedule.getScheduleDate(), 
                            schedule.getSlot().getStartTime()
                    );
                    LocalDateTime scheduleEndTime = LocalDateTime.of(
                            schedule.getScheduleDate(), 
                            schedule.getSlot().getEndTime()
                    );
                    return !scheduleEndTime.isBefore(startTime) && !scheduleStartTime.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取建议的替班医生列表
     */
    private List<SubstituteDoctorResponse> getSuggestedSubstituteDoctors(Schedule schedule, Doctor originalDoctor) {
        // 获取同科室的所有在职医生
        List<Doctor> sameDepartmentDoctors = doctorRepository.findByDepartmentAndStatus(
                originalDoctor.getDepartment(), 
                DoctorStatus.active
        );
        
        // 移除请假医生本人
        sameDepartmentDoctors = sameDepartmentDoctors.stream()
                .filter(d -> !d.getDoctorId().equals(originalDoctor.getDoctorId()))
                .collect(Collectors.toList());
        
        // 检查每个医生在该时间段是否有排班冲突或请假
        Map<Integer, Boolean> conflictMap = new HashMap<>();
        Map<Integer, Boolean> onLeaveMap = new HashMap<>();
        
        LocalDateTime scheduleStartTime = LocalDateTime.of(
                schedule.getScheduleDate(), 
                schedule.getSlot().getStartTime()
        );
        LocalDateTime scheduleEndTime = LocalDateTime.of(
                schedule.getScheduleDate(), 
                schedule.getSlot().getEndTime()
        );
        
        for (Doctor doctor : sameDepartmentDoctors) {
            // 检查排班冲突
            List<Schedule> doctorSchedules = scheduleRepository.findByDoctorAndScheduleDateBetween(
                    doctor, 
                    schedule.getScheduleDate(), 
                    schedule.getScheduleDate()
            );
            boolean hasConflict = doctorSchedules.stream()
                    .anyMatch(s -> s.getSlot().getSlotId().equals(schedule.getSlot().getSlotId()));
            conflictMap.put(doctor.getDoctorId(), hasConflict);
            
            // 检查是否在请假中
            List<LeaveRequest> doctorLeaves = leaveRequestRepository.findByDoctor(doctor);
            boolean isOnLeave = doctorLeaves.stream()
                    .filter(leave -> leave.getStatus() == LeaveRequestStatus.APPROVED)
                    .anyMatch(leave -> {
                        // 检查请假时间是否与排班时间有交集
                        return !leave.getEndTime().isBefore(scheduleStartTime) 
                                && !leave.getStartTime().isAfter(scheduleEndTime);
                    });
            onLeaveMap.put(doctor.getDoctorId(), isOnLeave);
        }
        
        // 转换为响应DTO并设置匹配等级
        List<SubstituteDoctorResponse> substitutes = sameDepartmentDoctors.stream()
                .map(doctor -> {
                    SubstituteDoctorResponse dto = new SubstituteDoctorResponse();
                    dto.setDoctorId(doctor.getDoctorId());
                    dto.setIdentifier(doctor.getIdentifier());
                    dto.setFullName(doctor.getFullName());
                    dto.setTitle(doctor.getTitle());
                    dto.setTitleLevel(doctor.getTitleLevel());
                    dto.setSpecialty(doctor.getSpecialty());
                    dto.setPhotoUrl(doctor.getPhotoUrl());
                    dto.setDepartmentId(doctor.getDepartment().getDepartmentId());
                    dto.setDepartmentName(doctor.getDepartment().getName());
                    dto.setHasConflict(conflictMap.get(doctor.getDoctorId()));
                    dto.setIsOnLeave(onLeaveMap.get(doctor.getDoctorId()));
                    
                    // 设置匹配等级和原因
                    setMatchLevel(dto, doctor, originalDoctor);
                    
                    return dto;
                })
                .sorted((a, b) -> {
                    // 优先级排序：无请假 > 无冲突 > 平级 > 升级 > 降级
                    // 先按请假排序
                    if (!a.getIsOnLeave().equals(b.getIsOnLeave())) {
                        return a.getIsOnLeave() ? 1 : -1;
                    }
                    // 再按排班冲突排序
                    if (!a.getHasConflict().equals(b.getHasConflict())) {
                        return a.getHasConflict() ? 1 : -1;
                    }
                    // 最后按职称匹配度排序
                    int levelCompare = getMatchLevelPriority(a.getMatchLevel()) - getMatchLevelPriority(b.getMatchLevel());
                    return levelCompare;
                })
                .collect(Collectors.toList());
        
        return substitutes;
    }
    
    /**
     * 设置医生的匹配等级
     */
    private void setMatchLevel(SubstituteDoctorResponse dto, Doctor candidate, Doctor original) {
        Integer originalLevel = original.getTitleLevel() != null ? original.getTitleLevel() : 999;
        Integer candidateLevel = candidate.getTitleLevel() != null ? candidate.getTitleLevel() : 999;
        
        if (candidateLevel.equals(originalLevel)) {
            dto.setMatchLevel("high");
            dto.setMatchReason("平级替换 - " + (candidate.getTitle() != null ? candidate.getTitle() : "同等级"));
        } else if (candidateLevel < originalLevel) {
            dto.setMatchLevel("medium");
            dto.setMatchReason("升级替换 - " + (candidate.getTitle() != null ? candidate.getTitle() : "更高级别"));
        } else {
            dto.setMatchLevel("low");
            dto.setMatchReason("降级替换 - " + (candidate.getTitle() != null ? candidate.getTitle() : "较低级别"));
        }
    }
    
    /**
     * 获取匹配等级的优先级数值（越小越优先）
     */
    private int getMatchLevelPriority(String matchLevel) {
        switch (matchLevel) {
            case "high": return 0;
            case "medium": return 1;
            case "low": return 2;
            default: return 999;
        }
    }
    
    /**
     * 转换排班为受影响排班响应DTO
     */
    private AffectedScheduleResponse convertToAffectedScheduleResponse(Schedule schedule) {
        AffectedScheduleResponse response = new AffectedScheduleResponse();
        response.setScheduleId(schedule.getScheduleId());
        response.setDoctorId(schedule.getDoctor().getDoctorId());
        response.setDoctorName(schedule.getDoctor().getFullName());
        response.setDepartmentId(schedule.getDoctor().getDepartment().getDepartmentId());
        response.setDepartmentName(schedule.getDoctor().getDepartment().getName());
        response.setScheduleDate(schedule.getScheduleDate());
        response.setSlotId(schedule.getSlot().getSlotId());
        response.setSlotName(schedule.getSlot().getSlotName());
        response.setStartTime(schedule.getSlot().getStartTime());
        response.setEndTime(schedule.getSlot().getEndTime());
        response.setLocationId(schedule.getLocation().getLocationId());
        response.setLocationName(schedule.getLocation().getLocationName());
        response.setTotalSlots(schedule.getTotalSlots());
        response.setBookedSlots(schedule.getBookedSlots());
        response.setFee(schedule.getFee());
        response.setStatus(schedule.getStatus());
        response.setRemarks(schedule.getRemarks());
        return response;
    }

    /**
     * 确认替班安排
     */
    @Transactional
    public SubstituteConfirmResponse confirmSubstitution(SubstituteConfirmRequest request) {
        logger.info("=== 开始确认替班安排 ===");
        logger.info("请假申请ID: {}", request.getLeaveRequestId());
        logger.info("替班安排数量: {}", request.getSubstitutions().size());
        
        SubstituteConfirmResponse response = new SubstituteConfirmResponse();
        response.setLeaveRequestId(request.getLeaveRequestId());
        
        List<String> messages = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int cancelledCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : request.getSubstitutions().entrySet()) {
            Integer scheduleId = entry.getKey();
            Integer substituteDoctorId = entry.getValue();
            
            logger.info("处理排班 #{}, 替班医生ID: {}", scheduleId, substituteDoctorId);
            
            try {
                Schedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));
                
                // 获取该排班下的所有预约
                List<com.example.springboot.entity.Appointment> appointments = 
                        appointmentRepository.findBySchedule(schedule);
                
                logger.info("排班 #{} 有 {} 个预约", scheduleId, appointments.size());
                
                if (substituteDoctorId == null) {
                    // 取消排班
                    Doctor originalDoctor = schedule.getDoctor();
                    schedule.setStatus(ScheduleStatus.cancelled);
                    scheduleRepository.save(schedule);
                    
                    logger.info("排班 #{} 已取消，开始发送通知给 {} 位患者", scheduleId, appointments.size());
                    
                    // 给所有患者发送排班取消通知
                    int notificationCount = 0;
                    for (com.example.springboot.entity.Appointment appointment : appointments) {
                        try {
                            logger.info("发送取消通知给患者 #{}, 预约 #{}", 
                                appointment.getPatient().getPatientId(), appointment.getAppointmentId());
                            
                            notificationService.sendScheduleCancelledNotification(
                                appointment.getPatient().getPatientId().intValue(),
                                appointment.getAppointmentId(),
                                originalDoctor.getFullName(),
                                originalDoctor.getDepartment().getName(),
                                schedule.getScheduleDate().toString(),
                                schedule.getSlot().getSlotName()
                            );
                            notificationCount++;
                            logger.info("成功发送取消通知给患者 #{}", appointment.getPatient().getPatientId());
                        } catch (Exception notifError) {
                            // 通知发送失败不影响主流程
                            logger.error("发送取消通知失败 - 患者 #{}: {}", 
                                appointment.getPatient().getPatientId(), notifError.getMessage(), notifError);
                        }
                    }
                    
                    cancelledCount++;
                    messages.add("排班 #" + scheduleId + " 已取消，已通知 " + notificationCount + "/" + appointments.size() + " 位患者");
                    logger.info("排班 #{} 取消完成，成功通知 {}/{} 位患者", scheduleId, notificationCount, appointments.size());
                } else {
                    // 替换医生
                    Doctor substituteDoctor = doctorRepository.findById(substituteDoctorId)
                            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + substituteDoctorId));
                    
                    Doctor originalDoctor = schedule.getDoctor();
                    
                    logger.info("替换医生: {} -> {}", originalDoctor.getFullName(), substituteDoctor.getFullName());
                    logger.info("原医生职称: {} (level: {})", originalDoctor.getTitle(), originalDoctor.getTitleLevel());
                    logger.info("替班医生职称: {} (level: {})", substituteDoctor.getTitle(), substituteDoctor.getTitleLevel());
                    
                    // 比较职称等级（0=主任医师，1=副主任医师，2=主治医师，数字越小职称越高）
                    Integer originalLevel = originalDoctor.getTitleLevel() != null ? originalDoctor.getTitleLevel() : 999;
                    Integer substituteLevel = substituteDoctor.getTitleLevel() != null ? substituteDoctor.getTitleLevel() : 999;
                    boolean isDowngrade = substituteLevel > originalLevel; // 替班医生职称更低
                    
                    // 更新排班的医生和状态
                    schedule.setDoctor(substituteDoctor);
                    // 恢复排班状态为可用（如果之前被取消了）
                    if (schedule.getStatus() == ScheduleStatus.cancelled) {
                        // 根据号源情况设置状态
                        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
                            schedule.setStatus(ScheduleStatus.full);
                        } else {
                            schedule.setStatus(ScheduleStatus.available);
                        }
                        logger.info("恢复排班状态: cancelled -> {}", schedule.getStatus());
                    }
                    scheduleRepository.save(schedule);
                    
                    logger.info("排班 #{} 医生已更新，职称变化: {}", scheduleId, 
                        isDowngrade ? "降级" : (substituteLevel < originalLevel ? "升级" : "平级"));
                    logger.info("开始发送通知给 {} 位患者", appointments.size());
                    
                    // 给所有患者发送医生变更通知
                    int notificationCount = 0;
                    int refundNotificationCount = 0;
                    for (com.example.springboot.entity.Appointment appointment : appointments) {
                        try {
                            logger.info("发送医生变更通知给患者 #{}, 预约 #{}", 
                                appointment.getPatient().getPatientId(), appointment.getAppointmentId());
                            
                            if (isDowngrade) {
                                // 职称降级，发送退款通知
                                // 计算退款金额（这里简化处理，实际应该根据职称差异计算）
                                double refundAmount = calculateRefundAmount(originalLevel, substituteLevel, schedule.getFee());
                                
                                logger.info("职称降级，退款金额: {} 元", refundAmount);
                                
                                notificationService.sendDoctorDowngradeRefundNotification(
                                    appointment.getPatient().getPatientId().intValue(),
                                    appointment.getAppointmentId(),
                                    originalDoctor.getFullName(),
                                    originalDoctor.getTitle() != null ? originalDoctor.getTitle() : "医师",
                                    substituteDoctor.getFullName(),
                                    substituteDoctor.getTitle() != null ? substituteDoctor.getTitle() : "医师",
                                    originalDoctor.getDepartment().getName(),
                                    schedule.getScheduleDate().toString(),
                                    schedule.getSlot().getSlotName(),
                                    refundAmount
                                );
                                refundNotificationCount++;
                            } else {
                                // 平级或升级，发送普通变更通知
                                notificationService.sendDoctorChangeNotification(
                                    appointment.getPatient().getPatientId().intValue(),
                                    appointment.getAppointmentId(),
                                    originalDoctor.getFullName(),
                                    substituteDoctor.getFullName(),
                                    originalDoctor.getDepartment().getName(),
                                    schedule.getScheduleDate().toString(),
                                    schedule.getSlot().getSlotName(),
                                    schedule.getLocation().getLocationName()
                                );
                            }
                            notificationCount++;
                            logger.info("成功发送医生变更通知给患者 #{}", appointment.getPatient().getPatientId());
                        } catch (Exception notifError) {
                            // 通知发送失败不影响主流程
                            logger.error("发送医生变更通知失败 - 患者 #{}: {}", 
                                appointment.getPatient().getPatientId(), notifError.getMessage(), notifError);
                        }
                    }
                    
                    int appointmentCount = appointments.size();
                    
                    successCount++;
                    String statusMsg = isDowngrade ? "（职称降级，已退款）" : 
                                      (substituteLevel < originalLevel ? "（职称升级）" : "");
                    messages.add("排班 #" + scheduleId + " 已由 " + originalDoctor.getFullName() + 
                                " 替换为 " + substituteDoctor.getFullName() + statusMsg +
                                "，转移了 " + appointmentCount + " 个预约，已通知 " + notificationCount + "/" + appointmentCount + " 位患者" +
                                (refundNotificationCount > 0 ? "（含 " + refundNotificationCount + " 个退款通知）" : ""));
                    logger.info("排班 #{} 替换完成，成功通知 {}/{} 位患者", scheduleId, notificationCount, appointmentCount);
                }
            } catch (Exception e) {
                failedCount++;
                messages.add("排班 #" + scheduleId + " 处理失败: " + e.getMessage());
                logger.error("处理排班 #{} 失败: {}", scheduleId, e.getMessage(), e);
            }
        }
        
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setCancelledCount(cancelledCount);
        response.setMessages(messages);
        
        logger.info("=== 替班安排确认完成 ===");
        logger.info("成功: {}, 取消: {}, 失败: {}", successCount, cancelledCount, failedCount);
        
        return response;
    }

    /**
     * 计算退款金额
     * 根据职称等级差异计算退款金额
     * @param originalLevel 原医生职称等级 (0=主任医师，1=副主任医师，2=主治医师)
     * @param substituteLevel 替班医生职称等级
     * @param totalFee 总挂号费
     * @return 退款金额
     */
    private double calculateRefundAmount(Integer originalLevel, Integer substituteLevel, java.math.BigDecimal totalFee) {
        if (originalLevel == null || substituteLevel == null || totalFee == null) {
            return 0.0;
        }
        
        // 只有降级才退款
        if (substituteLevel <= originalLevel) {
            return 0.0;
        }
        
        int levelDiff = substituteLevel - originalLevel;
        double fee = totalFee.doubleValue();
        
        // 根据职称差异计算退款比例
        // 每降一级退款 20% 的挂号费
        double refundRate = levelDiff * 0.20;
        if (refundRate > 0.5) {
            refundRate = 0.5; // 最多退款 50%
        }
        
        double refundAmount = fee * refundRate;
        
        // 保留两位小数
        return Math.round(refundAmount * 100.0) / 100.0;
    }

    private LeaveRequestResponse convertToResponseDto(LeaveRequest leaveRequest) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        BeanUtils.copyProperties(leaveRequest, response, "doctor", "approver");

        if (leaveRequest.getDoctor() != null) {
            response.setDoctor(doctorService.convertToResponseDto(leaveRequest.getDoctor()));
        }
        if (leaveRequest.getApprover() != null) {
            response.setApprover(adminService.findAdminById(leaveRequest.getApprover().getAdminId())); // 避免无限递归，这里直接通过ID获取简单AdminResponse
        }
        return response;
    }
}
