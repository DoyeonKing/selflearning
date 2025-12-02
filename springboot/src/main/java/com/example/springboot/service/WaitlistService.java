package com.example.springboot.service;

import com.example.springboot.dto.appointment.AppointmentResponse;
import com.example.springboot.dto.appointment.AppointmentUpdateRequest;
import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.patient.PatientSimpleResponse;
import com.example.springboot.dto.payment.PaymentRequest;
import com.example.springboot.dto.waitlist.*;
import com.example.springboot.dto.schedule.ScheduleResponse;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Patient;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.Waitlist;
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.BlacklistStatus;
import com.example.springboot.entity.enums.WaitlistStatus;
import com.example.springboot.entity.enums.PaymentStatus;
import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.PatientRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.repository.WaitlistRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final PatientRepository patientRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository; // For checking existing appointments
    private final PatientService patientService; // For converting patient entity to DTO
    private final ScheduleService scheduleService;
    private final AppointmentService appointmentService; // For converting schedule entity to DTO
    private final NotificationService notificationService;

    @Autowired
    public WaitlistService(WaitlistRepository waitlistRepository,
                           PatientRepository patientRepository,
                           ScheduleRepository scheduleRepository,
                           AppointmentRepository appointmentRepository,
                           PatientService patientService,
                           ScheduleService scheduleService,
                           AppointmentService appointmentService,
                           NotificationService notificationService) {
        this.waitlistRepository = waitlistRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientService = patientService;
        this.scheduleService = scheduleService;
        this.appointmentService = appointmentService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> findAllWaitlists() {
        return waitlistRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WaitlistResponse findWaitlistById(Integer id) {
        Waitlist waitlist = waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id " + id));
        return convertToResponseDto(waitlist);
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> findWaitlistsBySchedule(Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));
        return waitlistRepository.findByScheduleAndStatusOrderByCreatedAtAsc(schedule, WaitlistStatus.waiting).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WaitlistResponse createWaitlist(WaitlistCreateRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + request.getPatientId()));
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + request.getScheduleId()));

        // 检查患者状态
        if (patient.getStatus() == PatientStatus.deleted) {
            throw new BadRequestException("患者已删除，无法加入候补队列");
        }

        // 检查患者是否已在黑名单
        if (patient.getPatientProfile() != null && patient.getPatientProfile().getBlacklistStatus() == BlacklistStatus.blacklisted) {
            throw new BadRequestException("Patient is blacklisted and cannot join waitlist.");
        }
        // 检查患者是否已有该排班的有效预约（排除已取消的预约）
        if (appointmentRepository.existsByPatientAndScheduleAndStatusNotCancelled(patient, schedule)) {
            throw new BadRequestException("Patient already has an appointment for this schedule.");
        }
        // 检查患者是否已在该排班的候补队列中（只检查 waiting 状态）
        if (waitlistRepository.existsByPatientAndScheduleAndStatus(patient, schedule, WaitlistStatus.waiting)) {
            throw new BadRequestException("Patient is already on the waitlist for this schedule.");
        }

        Waitlist waitlist = new Waitlist();
        waitlist.setPatient(patient);
        waitlist.setSchedule(schedule);
        waitlist.setStatus(WaitlistStatus.waiting);  // 新候补记录状态为 waiting
        waitlist.setCreatedAt(LocalDateTime.now());

        return convertToResponseDto(waitlistRepository.save(waitlist));
    }

    @Transactional
    public WaitlistResponse updateWaitlist(Integer id, WaitlistUpdateRequest request) {
        Waitlist existingWaitlist = waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id " + id));

        if (request.getStatus() != null) existingWaitlist.setStatus(request.getStatus());
        if (request.getNotificationSentAt() != null) existingWaitlist.setNotificationSentAt(request.getNotificationSentAt());

        return convertToResponseDto(waitlistRepository.save(existingWaitlist));
    }

    @Transactional
    public void deleteWaitlist(Integer id) {
        if (!waitlistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Waitlist entry not found with id " + id);
        }
        waitlistRepository.deleteById(id);
    }

    /**
     * Attempts to fill an empty slot from the waitlist.
     * This method would typically be called by a scheduled task or when an appointment is canceled.
     *
     * @param scheduleId The ID of the schedule with an empty slot.
     * @return The created appointment response if a slot was filled, or null if no waitlist entry could fill it.
     */
    @Transactional
    public Appointment createAppointmentFromWaitlist(Integer scheduleId) {
        System.out.println("createAppointmentFromWaitlist 被调用，scheduleId: " + scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));

        // 检查是否有空余号源
        System.out.println("候补填充检查号源 - bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
        
        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
            System.out.println("号源已满（bookedSlots: " + schedule.getBookedSlots() + "），无法填充候补");
            return null; // 没有空余号源
        }

        // 获取最早的等待中的候补条目
        List<Waitlist> pendingWaitlists = waitlistRepository.findByScheduleAndStatusOrderByCreatedAtAsc(schedule, WaitlistStatus.waiting);
        System.out.println("找到 " + pendingWaitlists.size() + " 个等待中的候补记录");
        if (pendingWaitlists.isEmpty()) {
            System.out.println("没有等待中的候补人员");
            return null; // 没有等待中的候补人员
        }

        for (Waitlist waitlist : pendingWaitlists) {
            Patient patient = waitlist.getPatient();
            // 再次检查患者状态和是否已预约
            if (patient.getPatientProfile() != null && patient.getPatientProfile().getBlacklistStatus() == BlacklistStatus.blacklisted) {
                waitlist.setStatus(WaitlistStatus.expired); // 标记为过期（拒绝）
                waitlistRepository.save(waitlist);
                continue;
            }
            // 检查患者是否已有该排班的有效预约（排除已取消的预约）
            if (appointmentRepository.existsByPatientAndScheduleAndStatusNotCancelled(patient, schedule)) {
                System.out.println("候补患者 " + patient.getPatientId() + " 已有该排班的有效预约，跳过");
                waitlist.setStatus(WaitlistStatus.expired); // 标记为过期（已有预约）
                waitlistRepository.save(waitlist);
                continue;
            }

            // 找到合适的候补人员，通知患者支付
            // 重要：此时需要锁定号源，防止其他人预约
            // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
            schedule = scheduleRepository.findById(schedule.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
            
            // 再次检查号源是否还有空位（防止并发问题）
            if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
                System.out.println("候补填充时号源已被占用，跳过此候补");
                continue;
            }
            
            // 锁定号源：bookedSlots + 1（为候补患者预留）
            schedule.setBookedSlots(schedule.getBookedSlots() + 1);
            scheduleRepository.save(schedule);
            System.out.println("候补号源已锁定，bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
            
            waitlist.setStatus(WaitlistStatus.notified); // 标记为已通知（等待支付）
            waitlist.setNotificationSentAt(LocalDateTime.now()); // 记录通知发送时间
            waitlistRepository.save(waitlist); // 保存候补
            
            System.out.println("候补通知已发送，号源已锁定，等待患者支付，waitlistId: " + waitlist.getWaitlistId());
            
            // 发送候补可用通知
            try {
                String departmentName = "未知科室";
                String doctorName = "未知医生";
                String scheduleDate = "";
                String slotName = "";
                
                if (schedule != null) {
                    if (schedule.getDoctor() != null && schedule.getDoctor().getDepartment() != null) {
                        departmentName = schedule.getDoctor().getDepartment().getName();
                    }
                    if (schedule.getDoctor() != null) {
                        doctorName = schedule.getDoctor().getFullName();
                    }
                    if (schedule.getScheduleDate() != null) {
                        scheduleDate = schedule.getScheduleDate().toString();
                    }
                    if (schedule.getSlot() != null) {
                        slotName = schedule.getSlot().getSlotName();
                    }
                }
                
                notificationService.sendWaitlistAvailableNotification(
                        patient.getPatientId().intValue(),
                        waitlist.getWaitlistId(),
                        departmentName,
                        doctorName,
                        scheduleDate,
                        slotName
                );
            } catch (Exception e) {
                // 通知发送失败不影响流程，只记录日志
                System.err.println("Failed to send waitlist available notification: " + e.getMessage());
            }
            
            // 返回 null，因为此时还没有创建预约，等待患者支付时再创建
            System.out.println("候补填充完成，等待患者支付，waitlistId: " + waitlist.getWaitlistId());
            return null;
        }
        return null; // 没有找到可以成功转换的候补人员
    }


    public WaitlistResponse convertToResponseDto(Waitlist waitlist) {
        WaitlistResponse response = new WaitlistResponse();
        BeanUtils.copyProperties(waitlist, response, "patient", "schedule");
        response.setPatient(patientService.convertToResponseDto(waitlist.getPatient()));
        response.setSchedule(ScheduleResponse.fromEntity(waitlist.getSchedule()));
        
        // 计算排队位置（仅对 waiting 状态）
        if (waitlist.getStatus() == WaitlistStatus.waiting) {
            long position = waitlistRepository.countByScheduleAndStatusAndCreatedAtBefore(
                waitlist.getSchedule(), 
                WaitlistStatus.waiting, 
                waitlist.getCreatedAt()
            ) + 1;
            response.setQueuePosition((int) position);
        } else {
            response.setQueuePosition(null);
        }
        
        return response;
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> findByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + patientId));
        return waitlistRepository.findByPatient(patient).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WaitlistResponse cancelWaitlist(Integer waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist not found with id " + waitlistId));

        // 允许取消 waiting 或 notified 状态的候补
        if (waitlist.getStatus() != WaitlistStatus.waiting && waitlist.getStatus() != WaitlistStatus.notified) {
            throw new BadRequestException("Only waiting or notified waitlist entries can be canceled");
        }

        // 如果是 notified 状态，需要释放锁定的号源
        if (waitlist.getStatus() == WaitlistStatus.notified) {
            Schedule schedule = waitlist.getSchedule();
            if (schedule != null) {
                // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
                schedule = scheduleRepository.findById(schedule.getScheduleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                
                // 释放号源：bookedSlots - 1（因为通知时已经锁定了）
                if (schedule.getBookedSlots() > 0) {
                    schedule.setBookedSlots(schedule.getBookedSlots() - 1);
                    scheduleRepository.save(schedule);
                    System.out.println("候补取消 - 释放号源，bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
                }
                
                // 触发自动填充（通知下一个候补）
                if (schedule.getBookedSlots() < schedule.getTotalSlots()) {
                    try {
                        createAppointmentFromWaitlist(schedule.getScheduleId());
                    } catch (Exception e) {
                        System.err.println("候补取消 - 自动填充失败: " + e.getMessage());
                    }
                }
            }
        }

        WaitlistUpdateRequest request = new WaitlistUpdateRequest();
        request.setStatus(WaitlistStatus.expired);
        return updateWaitlist(waitlistId, request);
    }

    @Transactional
    public AppointmentResponse processWaitlistPayment(Integer waitlistId, PaymentRequest paymentData) {
        // 1. 查找候补记录
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist not found with id " + waitlistId));

        // 2. 检查状态必须是 notified（已通知）
        if (waitlist.getStatus() != WaitlistStatus.notified) {
            throw new BadRequestException("Only notified waitlist entries can be paid");
        }

        Schedule schedule = waitlist.getSchedule();
        Patient patient = waitlist.getPatient();
        
        // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
        schedule = scheduleRepository.findById(schedule.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        // 3. 检查号源是否已被占用（号源在通知时已锁定，这里只需要检查是否超出）
        // 注意：bookedSlots 在通知时已经 +1（锁定），所以这里应该 <= totalSlots
        System.out.println("候补支付检查号源 - bookedSlots(已锁定): " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
        
        // 如果 bookedSlots 超过 totalSlots，说明号源已被其他人占用（不应该发生，但需要检查）
        if (schedule.getBookedSlots() > schedule.getTotalSlots()) {
            System.out.println("候补支付时号源异常（bookedSlots: " + schedule.getBookedSlots() + " > totalSlots: " + schedule.getTotalSlots() + "），无法完成支付");
            throw new BadRequestException("No available slot, the schedule is already fully booked");
        }

        // 4. 检查患者是否已经有这个排班的有效预约了（排除已取消的预约）
        if (appointmentRepository.existsByPatientAndScheduleAndStatusNotCancelled(patient, schedule)) {
            System.out.println("候补患者已有该排班的有效预约");
            throw new BadRequestException("Patient already has an appointment for this schedule");
        }

        // 5. 为当前患者创建正式预约
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSchedule(schedule);
        appointment.setAppointmentNumber(getNextAppointmentNumber(schedule, patient));
        appointment.setStatus(AppointmentStatus.scheduled);  // 已预约状态
        appointment.setPaymentStatus(PaymentStatus.paid);    // 已支付
        appointment.setPaymentMethod(paymentData.getPaymentMethod());
        appointment.setTransactionId(paymentData.getTransactionId());
        appointment.setCreatedAt(LocalDateTime.now());

        // 6. 号源已经在通知时锁定了（bookedSlots + 1），所以这里不需要再增加
        // 只需要确保 bookedSlots 正确（如果之前同步更新过，这里不需要再改）
        System.out.println("候补支付成功，创建预约，bookedSlots: " + schedule.getBookedSlots() + "（号源已在通知时锁定）");

        // 7. 更新候补状态为已预约
        waitlist.setStatus(WaitlistStatus.booked);
        waitlistRepository.save(waitlist);

        // 8. 保存预约
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        // 9. 发送支付成功通知
        try {
            String departmentName = "未知科室";
            String doctorName = "未知医生";
            String scheduleDate = "";
            String slotName = "";
            Double fee = 0.0;
            
            if (schedule != null) {
                if (schedule.getDoctor() != null && schedule.getDoctor().getDepartment() != null) {
                    departmentName = schedule.getDoctor().getDepartment().getName();
                }
                if (schedule.getDoctor() != null) {
                    doctorName = schedule.getDoctor().getFullName();
                }
                if (schedule.getScheduleDate() != null) {
                    scheduleDate = schedule.getScheduleDate().toString();
                }
                if (schedule.getSlot() != null) {
                    slotName = schedule.getSlot().getSlotName();
                }
                if (schedule.getFee() != null) {
                    fee = schedule.getFee().doubleValue();
                }
            }
            
            notificationService.sendPaymentSuccessNotification(
                    patient.getPatientId().intValue(),
                    savedAppointment.getAppointmentId(),
                    departmentName,
                    doctorName,
                    scheduleDate,
                    slotName,
                    fee
            );
        } catch (Exception e) {
            // 通知发送失败不影响支付流程，只记录日志
            System.err.println("Failed to send payment success notification for waitlist: " + e.getMessage());
        }
        
        return appointmentService.findAppointmentById(savedAppointment.getAppointmentId());
    }

    private int getNextAppointmentNumber(Schedule schedule, Patient patient) {
        Appointment lastAppointment = appointmentRepository.findTopByScheduleOrderByAppointmentNumberDesc(schedule);
        int baseNumber = (lastAppointment == null || lastAppointment.getAppointmentNumber() == null)
                ? 0
                : lastAppointment.getAppointmentNumber();

        if (lastAppointment != null && lastAppointment.getPatient() != null
                && patient != null
                && Objects.equals(lastAppointment.getPatient().getPatientId(), patient.getPatientId())) {
            return baseNumber + 1;
        }

        return baseNumber + 1;
    }

    @Transactional(readOnly = true)
    public WaitlistPositionResponse getWaitlistPosition(Integer waitlistId) {
        // 获取候补记录
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id " + waitlistId));

        // 获取同一排班下状态为waiting的所有候补记录（按创建时间升序）
        List<Waitlist> waitingList = waitlistRepository
                .findByScheduleAndStatusOrderByCreatedAtAsc(waitlist.getSchedule(), WaitlistStatus.waiting);

        // 计算当前位置
        int position = 0;
        for (int i = 0; i < waitingList.size(); i++) {
            if (waitingList.get(i).getWaitlistId().equals(waitlistId)) {
                position = i + 1; // 位置从1开始
                break;
            }
        }

        // 构建响应对象
        WaitlistPositionResponse response = new WaitlistPositionResponse();
        response.setWaitlistId(waitlistId);
        response.setScheduleId(waitlist.getSchedule().getScheduleId());
        response.setStatus(waitlist.getStatus());
        response.setPosition(position);
        response.setTotalWaiting(waitingList.size());

        // 简单估算等待时间（实际项目中可根据历史数据进行更精确的估算）
        if (position > 0) {
            response.setEstimatedTime("预计还需等待约" + (position * 10) + "分钟");
        } else {
            response.setEstimatedTime(null);
        }

        return response;
    }

    // 在WaitlistService中修改方法返回类型
    @Transactional(readOnly = true)
    public PageResponse<WaitlistManagementResponse> getWaitlistsByScheduleForManagement(
            Integer scheduleId, String status, Integer page, Integer size) {

        // 验证排班是否存在
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));

        // 处理分页参数（1基页码转0基索引）
        int pageNum = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        // 根据状态筛选查询
        Page<Waitlist> waitlistPage;
        if (status != null && !status.isEmpty()) {
            try {
                WaitlistStatus waitlistStatus = WaitlistStatus.valueOf(status);
                waitlistPage = waitlistRepository.findByScheduleAndStatusOrderByCreatedAtAsc(schedule, waitlistStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid waitlist status: " + status);
            }
        } else {
            waitlistPage = waitlistRepository.findByScheduleOrderByCreatedAtAsc(schedule, pageable);
        }

        // 转换为响应DTO并计算位置
        List<WaitlistManagementResponse> responses = waitlistPage.getContent().stream()
                .map(waitlist -> {
                    WaitlistManagementResponse response = new WaitlistManagementResponse();
                    response.setWaitlistId(waitlist.getWaitlistId());
                    response.setPatientId(waitlist.getPatient().getPatientId());
                    response.setScheduleId(waitlist.getSchedule().getScheduleId());
                    response.setStatus(waitlist.getStatus());
                    response.setCreatedAt(waitlist.getCreatedAt());

                    // 设置患者信息
                    PatientSimpleResponse patientResp = new PatientSimpleResponse();
                    patientResp.setPatientId(waitlist.getPatient().getPatientId());
                    patientResp.setName(waitlist.getPatient().getFullName()); // 注意字段名与实体类匹配
                    patientResp.setPhone(waitlist.getPatient().getPhoneNumber());
                    response.setPatient(patientResp);

                    // 计算候补位置（仅waiting状态有效）
                    if (waitlist.getStatus() == WaitlistStatus.waiting) {
                        long position = waitlistRepository.countByScheduleAndStatusAndCreatedAtBefore(
                                schedule, WaitlistStatus.waiting, waitlist.getCreatedAt()) + 1;
                        response.setPosition((int) position);
                    } else {
                        response.setPosition(null);
                    }
                    return response;
                })
                .collect(Collectors.toList());

        // 构建符合PageResponse结构的返回对象
        return new PageResponse<>(
                responses,
                waitlistPage.getTotalElements(),
                waitlistPage.getTotalPages(),
                pageNum + 1, // 转回1基页码
                pageSize
        );
    }

    /**
     * 处理超时的候补记录
     * 将状态为 notified 且通知发送时间超过15分钟的候补记录更新为 expired
     * 如果该候补对应的排班还有空余号源，触发自动填充流程
     */
    @Transactional
    public void expireNotifiedWaitlists() {
        // 计算过期时间点（当前时间减去15分钟）
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(15);
        
        // 查询超时的候补记录
        List<Waitlist> expiredWaitlists = waitlistRepository.findExpiredNotifiedWaitlists(
                WaitlistStatus.notified, expireTime);
        
        System.out.println("候补超时处理 - 找到 " + expiredWaitlists.size() + " 个超时的候补记录");
        
        for (Waitlist waitlist : expiredWaitlists) {
            System.out.println("处理超时候补 - waitlistId: " + waitlist.getWaitlistId() + 
                    ", notificationSentAt: " + waitlist.getNotificationSentAt());
            
            // 更新状态为 expired
            waitlist.setStatus(WaitlistStatus.expired);
            waitlistRepository.save(waitlist);
            
            // 释放锁定的号源（候补超时未支付）
            Schedule schedule = waitlist.getSchedule();
            if (schedule != null) {
                // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
                schedule = scheduleRepository.findById(schedule.getScheduleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                
                // 释放号源：bookedSlots - 1（因为通知时已经锁定了）
                if (schedule.getBookedSlots() > 0) {
                    schedule.setBookedSlots(schedule.getBookedSlots() - 1);
                    scheduleRepository.save(schedule);
                    System.out.println("候补超时处理 - 释放号源，bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
                }
                
                // 检查是否还有空余号源，触发自动填充（通知下一个候补）
                if (schedule.getBookedSlots() < schedule.getTotalSlots()) {
                    System.out.println("候补超时处理 - 触发自动填充，scheduleId: " + schedule.getScheduleId());
                    try {
                        createAppointmentFromWaitlist(schedule.getScheduleId());
                    } catch (Exception e) {
                        // 自动填充失败不影响超时处理流程，只记录日志
                        System.err.println("候补超时处理 - 自动填充失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("候补超时处理 - 号源已满，无需自动填充");
                }
            }
        }
        
        System.out.println("候补超时处理完成 - 共处理 " + expiredWaitlists.size() + " 个超时的候补记录");
    }

}
