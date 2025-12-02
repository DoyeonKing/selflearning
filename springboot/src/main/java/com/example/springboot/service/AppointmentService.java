package com.example.springboot.service;

import com.example.springboot.common.Constants;
import com.example.springboot.dto.appointment.AppointmentCreateRequest;
import com.example.springboot.dto.appointment.AppointmentResponse;
import com.example.springboot.dto.appointment.AppointmentUpdateRequest;
import com.example.springboot.dto.appointment.CheckInRequest;
import com.example.springboot.dto.appointment.CheckInResponse;
import com.example.springboot.dto.appointment.QrCodeResponse;
import com.example.springboot.dto.schedule.ScheduleResponse;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Patient;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.*;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.PatientRepository;
import com.example.springboot.repository.ScheduleRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ScheduleRepository scheduleRepository;
    private final PatientService patientService; // For updating patient profile (no-show count)
    private final DoctorService doctorService; // To get Doctor details for response
    private final DepartmentService departmentService; // To get Department details for response
    private final TimeSlotService timeSlotService; // To get TimeSlot details for response
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final WaitlistService waitlistService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String QR_TOKEN_PREFIX = "qr:token:";
    private static final int QR_TOKEN_MIN_EXPIRE_SECONDS = 1800; // 最小30分钟过期
    private static final int QR_REFRESH_INTERVAL_SECONDS = 60; // 建议60秒刷新一次
    // 签到时间限制：已改为随到随签，只要在工作时间结束之前都可以签到


    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              ScheduleRepository scheduleRepository,
                              PatientService patientService,
                              DoctorService doctorService,
                              DepartmentService departmentService,
                              TimeSlotService timeSlotService,
                              ScheduleService scheduleService,
                              NotificationService notificationService,
                              @Lazy WaitlistService waitlistService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.timeSlotService = timeSlotService;
        this.scheduleService = scheduleService;
        this.notificationService = notificationService;
        this.waitlistService = waitlistService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findAppointmentById(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));
        return convertToResponseDto(appointment);
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + request.getPatientId()));

        // Check patient status
        if (patient.getStatus() == PatientStatus.deleted) {
            throw new BadRequestException("患者已删除，无法创建预约");
        }

        // Check patient blacklist status
        if (patient.getPatientProfile() != null && patient.getPatientProfile().getBlacklistStatus() == BlacklistStatus.blacklisted) {
            throw new BadRequestException("Patient is blacklisted and cannot make appointments.");
        }

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + request.getScheduleId()));

        System.out.println("创建预约检查 - scheduleId: " + request.getScheduleId() + ", patientId: " + request.getPatientId() + ", bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
        
        if (schedule.getStatus() != ScheduleStatus.available) {
            throw new BadRequestException("Schedule is not active for booking.");
        }
        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
            System.out.println("创建预约失败 - 号源已满: bookedSlots(" + schedule.getBookedSlots() + ") >= totalSlots(" + schedule.getTotalSlots() + ")");
            throw new BadRequestException("No available slots for this schedule.");
        }
        if (schedule.getScheduleDate().isBefore(java.time.LocalDate.now()) ||
                (schedule.getScheduleDate().isEqual(java.time.LocalDate.now()) && schedule.getSlot().getEndTime().isBefore(java.time.LocalTime.now()))) {
            throw new BadRequestException("Cannot book past or ongoing schedules.");
        }

        // Check if patient already has an active appointment for this schedule (excluding cancelled appointments)
        if (appointmentRepository.existsByPatientAndScheduleAndStatusNotCancelled(patient, schedule)) {
            throw new BadRequestException("Patient already has an appointment for this schedule.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSchedule(schedule);
        appointment.setAppointmentNumber(getNextAppointmentNumberForRebooking(schedule, patient)); // 自动分配就诊序号
        appointment.setStatus(AppointmentStatus.scheduled); // 初始状态为待支付
        appointment.setPaymentStatus(PaymentStatus.unpaid);
        appointment.setCreatedAt(LocalDateTime.now());

        // 增加排班的已预约数
        schedule.setBookedSlots(schedule.getBookedSlots() + 1);
        scheduleRepository.save(schedule); // 保存更新后的排班信息

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 发送预约通知
        try {
            String departmentName = "未知科室";
            String doctorName = "未知医生";
            String scheduleDate = "";
            String slotName = "";
            String locationName = "";
            
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
                if (schedule.getLocation() != null) {
                    locationName = schedule.getLocation().getName();
                }
            }
            
            notificationService.sendAppointmentNotification(
                    patient.getPatientId().intValue(),
                    savedAppointment.getAppointmentId(),
                    departmentName,
                    doctorName,
                    scheduleDate,
                    slotName,
                    locationName,
                    savedAppointment.getAppointmentNumber()
            );
        } catch (Exception e) {
            // 通知发送失败不影响预约创建流程，只记录日志
            logger.error("发送预约通知失败 - 预约ID: {}, 错误: {}", savedAppointment.getAppointmentId(), e.getMessage());
        }

        return convertToResponseDto(savedAppointment);
    }

    /**
     * 创建现场挂号预约（分诊台辅助患者挂号）
     * 与普通预约的区别：isWalkIn=true, appointmentType=WALK_IN
     */
    @Transactional
    public AppointmentResponse createWalkInAppointment(AppointmentCreateRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + request.getPatientId()));

        // Check patient status
        if (patient.getStatus() == PatientStatus.deleted) {
            throw new BadRequestException("患者已删除，无法创建预约");
        }

        // Check patient blacklist status
        if (patient.getPatientProfile() != null && patient.getPatientProfile().getBlacklistStatus() == BlacklistStatus.blacklisted) {
            throw new BadRequestException("Patient is blacklisted and cannot make appointments.");
        }

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + request.getScheduleId()));

        if (schedule.getStatus() != ScheduleStatus.available) {
            throw new BadRequestException("Schedule is not active for booking.");
        }
        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
            throw new BadRequestException("No available slots for this schedule.");
        }
        if (schedule.getScheduleDate().isBefore(java.time.LocalDate.now()) ||
                (schedule.getScheduleDate().isEqual(java.time.LocalDate.now()) && schedule.getSlot().getEndTime().isBefore(java.time.LocalTime.now()))) {
            throw new BadRequestException("Cannot book past or ongoing schedules.");
        }

        // Check if patient already has an active appointment for this schedule (excluding cancelled appointments)
        if (appointmentRepository.existsByPatientAndScheduleAndStatusNotCancelled(patient, schedule)) {
            throw new BadRequestException("Patient already has an appointment for this schedule.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSchedule(schedule);
        appointment.setAppointmentNumber(getNextAppointmentNumberForRebooking(schedule, patient));
        appointment.setStatus(AppointmentStatus.scheduled); // 现场挂号创建后为已预约状态
        appointment.setPaymentStatus(PaymentStatus.unpaid); // 现场挂号需要现场缴费，初始为未支付
        appointment.setIsWalkIn(true); // 标记为现场挂号
        appointment.setAppointmentType(AppointmentType.WALK_IN); // 设置预约类型为现场挂号
        appointment.setCreatedAt(LocalDateTime.now());

        // 增加排班的已预约数
        schedule.setBookedSlots(schedule.getBookedSlots() + 1);
        scheduleRepository.save(schedule);

        logger.info("现场挂号创建成功 - 预约ID: {}, 患者: {}, 排班ID: {}", 
                appointment.getAppointmentId(), patient.getFullName(), schedule.getScheduleId());

        return convertToResponseDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateAppointment(Integer id, AppointmentUpdateRequest request) {
        Appointment existingAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        AppointmentStatus originalStatus = existingAppointment.getStatus();
        boolean shouldRestoreSlots = false;
        boolean shouldAssignNewNumber = false;

        // Update fields if provided in request
        if (request.getAppointmentNumber() != null) existingAppointment.setAppointmentNumber(request.getAppointmentNumber());
        if (request.getStatus() != null) {
            AppointmentStatus newStatus = request.getStatus();
            // Handle specific status transitions and logic
            if (newStatus == AppointmentStatus.cancelled && originalStatus != AppointmentStatus.cancelled) {
                // 如果是取消预约，需要减少排班的已预约数
                // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
                Schedule schedule = scheduleRepository.findById(existingAppointment.getSchedule().getScheduleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                
                System.out.println("取消预约 - appointmentId: " + existingAppointment.getAppointmentId() + ", scheduleId: " + schedule.getScheduleId() + ", 取消前 bookedSlots: " + schedule.getBookedSlots());
                
                // 确保 bookedSlots 不会小于 0
                int currentBookedSlots = schedule.getBookedSlots();
                if (currentBookedSlots > 0) {
                    schedule.setBookedSlots(currentBookedSlots - 1);
                    scheduleRepository.save(schedule);
                    System.out.println("取消预约后 - bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
                } else {
                    System.out.println("警告：取消预约时 bookedSlots 已经是 0 或负数(" + currentBookedSlots + ")，无法减少");
                }
                
                // 取消预约后，触发候补自动填充
                // 注意：这里 schedule.getBookedSlots() 已经是减少后的值了
                System.out.println("取消预约后，检查候补填充 - bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
                if (schedule.getBookedSlots() < schedule.getTotalSlots()) {
                    // 有空余号源，尝试从候补队列中填充
                    try {
                        // 刷新 schedule 对象，确保获取最新的 bookedSlots 值
                        schedule = scheduleRepository.findById(schedule.getScheduleId())
                                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                        
                        System.out.println("刷新后检查 - bookedSlots: " + schedule.getBookedSlots() + ", totalSlots: " + schedule.getTotalSlots());
                        // 再次检查是否有空余号源（防止并发问题）
                        if (schedule.getBookedSlots() < schedule.getTotalSlots()) {
                            System.out.println("开始触发候补自动填充，scheduleId: " + schedule.getScheduleId());
                            Appointment filledAppointment = waitlistService.createAppointmentFromWaitlist(schedule.getScheduleId());
                            if (filledAppointment != null) {
                                System.out.println("候补填充成功，创建预约ID: " + filledAppointment.getAppointmentId());
                            } else {
                                System.out.println("候补填充返回null，可能没有可用的候补或候补不符合条件");
                            }
                        } else {
                            System.out.println("刷新后发现号源已满，跳过候补填充");
                        }
                    } catch (Exception e) {
                        // 候补填充失败不影响取消预约流程，只记录日志
                        System.err.println("Failed to fill waitlist after appointment cancellation: " + e.getMessage());
                        e.printStackTrace(); // 打印完整堆栈，便于调试
                    }
                } else {
                    System.out.println("取消预约后号源仍满，无需候补填充");
                }
            } else if (newStatus == AppointmentStatus.NO_SHOW && originalStatus != AppointmentStatus.NO_SHOW) {
                // 如果是爽约，增加患者爽约次数并检查是否加入黑名单
                Patient patient = existingAppointment.getPatient();
                if (patient.getPatientProfile() != null) {
                    patient.getPatientProfile().setNoShowCount(patient.getPatientProfile().getNoShowCount() + 1);
                    if (patient.getPatientProfile().getNoShowCount() >= Constants.MAX_NO_SHOW_COUNT) {
                        patient.getPatientProfile().setBlacklistStatus(BlacklistStatus.blacklisted);
                    }
                    patientService.savePatientProfile(patient.getPatientProfile()); // 更新患者档案
                }
                // 爽约也应减少号源
                Schedule schedule = existingAppointment.getSchedule();
                if (schedule.getBookedSlots() > 0) {
                    schedule.setBookedSlots(schedule.getBookedSlots() - 1);
                    scheduleRepository.save(schedule);
                }
            } else if (newStatus == AppointmentStatus.cancelled && originalStatus == AppointmentStatus.CHECKED_IN) {
                // 已签到的预约不能取消
                throw new BadRequestException("已签到的预约不能取消，如需取消请联系管理员");
            } else if (originalStatus == AppointmentStatus.cancelled && isActiveStatus(newStatus)) {
                shouldRestoreSlots = true;
                shouldAssignNewNumber = true;
            }
            existingAppointment.setStatus(newStatus);
        }
        if (request.getPaymentStatus() != null) existingAppointment.setPaymentStatus(request.getPaymentStatus());
        if (request.getPaymentMethod() != null) existingAppointment.setPaymentMethod(request.getPaymentMethod());
        if (request.getTransactionId() != null) existingAppointment.setTransactionId(request.getTransactionId());
        // 不允许通过 updateAppointment 接口直接设置签到时间，必须通过签到接口
        // 如果请求中包含 checkInTime，忽略它（避免误操作）
        // if (request.getCheckInTime() != null) existingAppointment.setCheckInTime(request.getCheckInTime());

        if (shouldAssignNewNumber) {
            existingAppointment.setAppointmentNumber(
                    getNextAppointmentNumberForRebooking(existingAppointment.getSchedule(), existingAppointment.getPatient()));
        }

        if (shouldRestoreSlots) {
            Schedule schedule = existingAppointment.getSchedule();
            schedule.setBookedSlots(schedule.getBookedSlots() + 1);
            scheduleRepository.save(schedule);
        }

        return convertToResponseDto(appointmentRepository.save(existingAppointment));
    }

    @Transactional
    public void deleteAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        // 删除预约时，应减少对应排班的已预约数
        Schedule schedule = appointment.getSchedule();
        if (schedule.getBookedSlots() > 0) {
            schedule.setBookedSlots(schedule.getBookedSlots() - 1);
            scheduleRepository.save(schedule);
        }
        appointmentRepository.delete(appointment);
    }

    private AppointmentResponse convertToResponseDto(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        BeanUtils.copyProperties(appointment, response);

        response.setPatient(patientService.convertToResponseDto(appointment.getPatient()));

        // ScheduleResponse 包含 Doctor, TimeSlot, Clinic, Department 信息
        response.setSchedule(ScheduleResponse.fromEntity(appointment.getSchedule()));

        return response;
    }

    // 在AppointmentService中添加以下方法
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + patientId));
        return appointmentRepository.findByPatient(patient).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByDoctorId(Integer doctorId) {
        return appointmentRepository.findByScheduleDoctorDoctorId(doctorId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findUpcomingByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + patientId));
        return appointmentRepository.findByPatientAndScheduleScheduleDateAfterOrScheduleScheduleDateEqualAndScheduleSlotStartTimeAfter(
                        patient,
                        LocalDate.now(),
                        LocalDate.now(),
                        LocalTime.now()
                ).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.cancelled) {
            throw new BadRequestException("Appointment is already canceled");
        }

        // 检查预约时间是否已过去
        if (appointment.getSchedule() != null && appointment.getSchedule().getScheduleDate() != null 
                && appointment.getSchedule().getSlot() != null 
                && appointment.getSchedule().getSlot().getStartTime() != null) {
            LocalDate scheduleDate = appointment.getSchedule().getScheduleDate();
            LocalTime startTime = appointment.getSchedule().getSlot().getStartTime();
            LocalDateTime scheduleDateTime = LocalDateTime.of(scheduleDate, startTime);
            LocalDateTime now = LocalDateTime.now();
            
            // 如果预约时间已过去，不允许取消
            if (scheduleDateTime.isBefore(now) || scheduleDateTime.isEqual(now)) {
                throw new BadRequestException("Cannot cancel appointment that has already passed");
            }
        }

        AppointmentUpdateRequest request = new AppointmentUpdateRequest();
        request.setStatus(AppointmentStatus.cancelled);
        AppointmentResponse response = updateAppointment(appointmentId, request);
        
        // 注意：候补自动填充已在 updateAppointment 方法中处理（当状态变为 cancelled 时）
        // 这里只需要发送通知即可
        
        // 取消预约后发送通知
        try {
            Schedule schedule = appointment.getSchedule();
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
            
            notificationService.sendCancellationNotification(
                    appointment.getPatient().getPatientId().intValue(),
                    appointmentId,
                    departmentName,
                    doctorName,
                    scheduleDate,
                    slotName
            );
        } catch (Exception e) {
            // 通知发送失败不影响取消流程，只记录日志
            System.err.println("Failed to send cancellation notification: " + e.getMessage());
        }
        
        return response;
    }

    @Transactional
    public AppointmentResponse processPayment(Integer appointmentId, AppointmentUpdateRequest paymentData) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        if (appointment.getPaymentStatus() == PaymentStatus.paid) {
            throw new BadRequestException("Appointment is already paid");
        }

        paymentData.setPaymentStatus(PaymentStatus.paid);
        // 支付成功后，状态应该保持为 scheduled（已预约），而不是 completed（已完成）
        // completed 状态应该在就诊完成后由医生或系统标记
        if (appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT) {
            paymentData.setStatus(AppointmentStatus.scheduled); // 从待支付改为已预约
        }
        // 如果已经是 scheduled 状态，则保持 scheduled 状态不变
        AppointmentResponse response = updateAppointment(appointmentId, paymentData);
        
        // 支付成功后发送通知
        try {
            Schedule schedule = appointment.getSchedule();
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
                    appointment.getPatient().getPatientId().intValue(),
                    appointmentId,
                    departmentName,
                    doctorName,
                    scheduleDate,
                    slotName,
                    fee
            );
        } catch (Exception e) {
            // 通知发送失败不影响支付流程，只记录日志
            System.err.println("Failed to send payment success notification: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 退款接口
     * 只有已支付的预约才能退款
     * 退款时会同时取消预约，并恢复号源（如果预约未取消）
     */
    @Transactional
    public AppointmentResponse refundAppointment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        // 检查支付状态
        if (appointment.getPaymentStatus() != PaymentStatus.paid) {
            throw new BadRequestException("只有已支付的预约才能退款，当前支付状态：" + appointment.getPaymentStatus());
        }

        // 检查预约状态（已完成的预约不能退款）
        if (appointment.getStatus() == AppointmentStatus.completed) {
            throw new BadRequestException("已完成的预约不能退款");
        }

        // 检查是否已签到（已签到的预约不能退款）
        if (appointment.getCheckInTime() != null) {
            throw new BadRequestException("已签到的预约不能退款");
        }

        // 更新支付状态为已退款
        appointment.setPaymentStatus(PaymentStatus.refunded);
        appointment.setUpdatedAt(LocalDateTime.now());
        
        // 如果预约未取消，则同时取消预约（退款即取消）
        // 取消预约会自动处理号源恢复和候补填充
        if (appointment.getStatus() != AppointmentStatus.cancelled) {
            AppointmentUpdateRequest updateRequest = new AppointmentUpdateRequest();
            updateRequest.setStatus(AppointmentStatus.cancelled);
            updateRequest.setPaymentStatus(PaymentStatus.refunded);
            // 调用 updateAppointment 来取消预约，这会自动处理号源恢复和候补填充
            return updateAppointment(appointmentId, updateRequest);
        }
        
        // 如果预约已经取消，只需要更新支付状态
        appointmentRepository.save(appointment);

        logger.info("退款成功 - 预约ID: {}, 患者: {}, 退款时间: {}", 
                appointmentId, appointment.getPatient().getFullName(), appointment.getUpdatedAt());

        return convertToResponseDto(appointment);
    }

    private int getNextAppointmentNumberForRebooking(Schedule schedule, Patient patient) {
        Appointment lastAppointment = appointmentRepository.findTopByScheduleOrderByAppointmentNumberDesc(schedule);
        int baseNumber = (lastAppointment == null || lastAppointment.getAppointmentNumber() == null)
                ? 0
                : lastAppointment.getAppointmentNumber();

        if (lastAppointment != null && lastAppointment.getPatient() != null
                && Objects.equals(lastAppointment.getPatient().getPatientId(), patient.getPatientId())) {
            return baseNumber + 1;
        }

        return baseNumber + 1;
    }

    private boolean isActiveStatus(AppointmentStatus status) {
        return status == AppointmentStatus.scheduled || 
               status == AppointmentStatus.PENDING_PAYMENT || 
               status == AppointmentStatus.CHECKED_IN;
    }

    /**
     * 生成预约二维码Token
     */
    @Transactional(readOnly = true)
    public QrCodeResponse generateQrCode(Integer appointmentId) {
        logger.info("========== 开始生成二维码Token ==========");
        logger.info("预约ID: {}", appointmentId);
        LocalDateTime requestTime = LocalDateTime.now();
        logger.info("请求时间: {}", requestTime);
        
        // 1. 验证预约是否存在
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    logger.error("预约不存在，ID: {}", appointmentId);
                    return new ResourceNotFoundException("预约不存在，ID: " + appointmentId);
                });
        logger.info("预约信息查询成功 - 预约ID: {}, 状态: {}, 患者ID: {}", 
                appointment.getAppointmentId(), appointment.getStatus(), appointment.getPatient().getPatientId());

        // 2. 验证预约状态（必须是 scheduled）
        // 只有 scheduled 和 CHECKED_IN 状态可以生成二维码（已签到但未就诊的也可以刷新二维码）
        if (appointment.getStatus() != AppointmentStatus.scheduled && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            logger.warn("预约状态不正确，无法生成二维码 - 预约ID: {}, 当前状态: {}, 允许的状态: scheduled, CHECKED_IN", 
                    appointmentId, appointment.getStatus());
            throw new BadRequestException("预约状态不正确，无法生成二维码。当前状态: " + appointment.getStatus());
        }
        logger.info("预约状态验证通过 - 状态: {}", appointment.getStatus());

        // 3. 验证预约时间（只要在排班结束时间之前都可以生成二维码）
        Schedule schedule = appointment.getSchedule();
        if (schedule == null || schedule.getScheduleDate() == null || schedule.getSlot() == null) {
            logger.error("预约排班信息不完整 - 预约ID: {}, schedule: {}, scheduleDate: {}, slot: {}", 
                    appointmentId, schedule != null, 
                    schedule != null ? schedule.getScheduleDate() : null,
                    schedule != null && schedule.getSlot() != null ? schedule.getSlot().getSlotId() : null);
            throw new BadRequestException("预约排班信息不完整");
        }
        logger.info("排班信息查询成功 - 排班ID: {}, 排班日期: {}, 时间段: {} - {}", 
                schedule.getScheduleId(), schedule.getScheduleDate(), 
                schedule.getSlot().getStartTime(), schedule.getSlot().getEndTime());

        LocalDateTime scheduleEndTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getEndTime());
        LocalDateTime now = LocalDateTime.now();
        logger.info("时间验证 - 当前时间: {}, 排班结束时间: {}", now, scheduleEndTime);
        
        // 如果排班结束时间已过去，不允许生成二维码
        // 允许在排班开始时间之前生成二维码（随到随签）
        if (now.isAfter(scheduleEndTime)) {
            Duration timePassed = Duration.between(scheduleEndTime, now);
            logger.warn("排班结束时间已过，无法生成二维码 - 预约ID: {}, 排班结束时间: {}, 当前时间: {}, 已过: {}分钟", 
                    appointmentId, scheduleEndTime, now, timePassed.toMinutes());
            throw new BadRequestException("排班结束时间已过（" + 
                schedule.getScheduleDate() + " " + schedule.getSlot().getEndTime() + "），无法生成二维码");
        }

        // 4. 计算Token过期时间：动态设置为排班结束时间，但不少于最小过期时间
        Duration timeUntilEnd = Duration.between(now, scheduleEndTime);
        long expireSeconds = Math.max(timeUntilEnd.getSeconds(), QR_TOKEN_MIN_EXPIRE_SECONDS);
        long expireMinutes = expireSeconds / 60;
        long timeUntilEndMinutes = timeUntilEnd.toMinutes();
        logger.info("Token过期时间计算 - 距离排班结束: {}分钟 ({}秒), 最小过期时间: {}分钟 ({}秒), 最终过期时间: {}分钟 ({}秒)", 
                timeUntilEndMinutes, timeUntilEnd.getSeconds(),
                QR_TOKEN_MIN_EXPIRE_SECONDS / 60, QR_TOKEN_MIN_EXPIRE_SECONDS,
                expireMinutes, expireSeconds);
        
        // 生成Token
        long timestamp = System.currentTimeMillis();
        String random = generateRandomString(6); // 6位随机字符串
        String qrToken = String.format("APPOINTMENT_%d_%d_%s", appointmentId, timestamp, random);
        logger.info("Token生成成功 - Token: {}, 生成时间戳: {}", qrToken, timestamp);

        // 5. 存储到Redis（动态过期时间：排班结束时间或最小30分钟，取较大值）
        String redisKey = QR_TOKEN_PREFIX + qrToken;
        try {
            redisTemplate.opsForValue().set(redisKey, 
                    String.valueOf(appointmentId), 
                    expireSeconds, 
                    TimeUnit.SECONDS);
            logger.info("Token已存储到Redis - Redis Key: {}, 预约ID: {}, 过期时间: {}秒 ({}分钟)", 
                    redisKey, appointmentId, expireSeconds, expireMinutes);
            
            // 验证Redis存储是否成功
            String storedValue = redisTemplate.opsForValue().get(redisKey);
            if (storedValue != null && storedValue.equals(String.valueOf(appointmentId))) {
                logger.info("Redis存储验证成功 - Key: {}, Value: {}", redisKey, storedValue);
            } else {
                logger.error("Redis存储验证失败 - Key: {}, 期望值: {}, 实际值: {}", 
                        redisKey, appointmentId, storedValue);
            }
        } catch (Exception e) {
            logger.error("Redis存储失败 - Key: {}, 预约ID: {}, 错误信息: {}", 
                    redisKey, appointmentId, e.getMessage(), e);
            throw new BadRequestException("二维码Token存储失败，请重试");
        }

        // 6. 返回响应
        QrCodeResponse response = new QrCodeResponse();
        response.setAppointmentId(appointmentId);
        response.setQrToken(qrToken);
        response.setExpiresIn((int) expireSeconds);
        response.setRefreshInterval(QR_REFRESH_INTERVAL_SECONDS);

        logger.info("二维码Token生成完成 - 预约ID: {}, Token: {}, 过期时间: {}秒, 刷新间隔: {}秒", 
                appointmentId, qrToken, expireSeconds, QR_REFRESH_INTERVAL_SECONDS);
        logger.info("========== 二维码Token生成结束 ==========");
        return response;
    }

    /**
     * 扫码签到
     */
    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {
        logger.info("========== 开始扫码签到 ==========");
        String qrToken = request.getQrToken();
        LocalDateTime checkInRequestTime = LocalDateTime.now();
        logger.info("签到请求时间: {}", checkInRequestTime);
        logger.info("二维码Token: {}", qrToken);
        
        if (qrToken == null || qrToken.isEmpty()) {
            logger.error("二维码Token为空");
            throw new BadRequestException("二维码Token不能为空");
        }

        // 1. 从Redis查询Token对应的预约ID
        String redisKey = QR_TOKEN_PREFIX + qrToken;
        logger.info("查询Redis - Key: {}", redisKey);
        
        String appointmentIdStr = null;
        try {
            appointmentIdStr = redisTemplate.opsForValue().get(redisKey);
            logger.info("Redis查询结果 - Key: {}, Value: {}", redisKey, appointmentIdStr);
        } catch (Exception e) {
            logger.error("Redis查询失败 - Key: {}, 错误信息: {}", redisKey, e.getMessage(), e);
            throw new BadRequestException("二维码验证失败，请重试");
        }

        if (appointmentIdStr == null) {
            logger.warn("二维码Token在Redis中不存在或已过期 - Token: {}, Redis Key: {}", qrToken, redisKey);
            
            // 尝试解析Token获取预约ID（用于调试）
            try {
                String[] tokenParts = qrToken.split("_");
                if (tokenParts.length >= 2) {
                    String possibleAppointmentId = tokenParts[1];
                    logger.info("从Token解析可能的预约ID: {}", possibleAppointmentId);
                }
            } catch (Exception e) {
                logger.debug("Token解析失败: {}", e.getMessage());
            }
            
            throw new BadRequestException("二维码已过期或无效，请患者刷新二维码后重试");
        }

        final Integer appointmentId;
        try {
            appointmentId = Integer.parseInt(appointmentIdStr);
            logger.info("预约ID解析成功: {}", appointmentId);
        } catch (NumberFormatException e) {
            logger.error("预约ID格式错误 - 原始值: {}, 错误信息: {}", appointmentIdStr, e.getMessage());
            throw new BadRequestException("二维码Token格式错误，请刷新二维码后重试");
        }

        // 2. 查询预约
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    logger.error("预约不存在 - 预约ID: {}", appointmentId);
                    return new ResourceNotFoundException("预约不存在，ID: " + appointmentId);
                });
        logger.info("预约信息查询成功 - 预约ID: {}, 状态: {}, 患者: {}, 就诊序号: {}", 
                appointment.getAppointmentId(), appointment.getStatus(), 
                appointment.getPatient().getFullName(), appointment.getAppointmentNumber());

        // 3. 验证预约状态（必须是 scheduled，已签到的不能重复签到）
        if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
            // Token已使用，删除Token
            try {
                redisTemplate.delete(redisKey);
                logger.info("已删除Redis中的Token - Key: {}", redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败 - Key: {}, 错误: {}", redisKey, e.getMessage());
            }

            String errorMessage = String.format(
                "该预约已签到（签到时间：%s），请勿重复操作。预约ID：%d。",
                appointment.getCheckInTime(), appointmentId
            );
            throw new BadRequestException(errorMessage);
        }
        
        if (appointment.getStatus() != AppointmentStatus.scheduled) {
            logger.warn("预约状态不正确，无法签到 - 预约ID: {}, 当前状态: {}, 期望状态: scheduled", 
                    appointmentId, appointment.getStatus());
            // Token已使用，删除Token
            try {
                redisTemplate.delete(redisKey);
                logger.info("已删除Redis中的Token - Key: {}", redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败 - Key: {}, 错误: {}", redisKey, e.getMessage());
            }
            throw new BadRequestException("预约状态不正确，无法签到。当前状态: " + appointment.getStatus() + "，只有已预约（scheduled）状态的预约才能签到。");
        }
        logger.info("预约状态验证通过 - 状态: scheduled");

        // 5. 验证签到时间（随到随签：只要在工作时间结束之前都可以签到）
        Schedule schedule = appointment.getSchedule();
        if (schedule == null || schedule.getScheduleDate() == null || schedule.getSlot() == null) {
            logger.error("预约排班信息不完整 - 预约ID: {}, schedule: {}, scheduleDate: {}, slot: {}", 
                    appointmentId, schedule != null, 
                    schedule != null ? schedule.getScheduleDate() : null,
                    schedule != null && schedule.getSlot() != null ? schedule.getSlot().getSlotId() : null);
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败: {}", e.getMessage());
            }
            throw new BadRequestException("预约排班信息不完整");
        }
        logger.info("排班信息查询成功 - 排班ID: {}, 排班日期: {}, 时间段: {} - {}", 
                schedule.getScheduleId(), schedule.getScheduleDate(), 
                schedule.getSlot().getStartTime(), schedule.getSlot().getEndTime());

        LocalDateTime scheduleStartTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getStartTime());
        LocalDateTime scheduleEndTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getEndTime());
        LocalDateTime now = LocalDateTime.now();
        logger.info("时间验证 - 当前时间: {}, 排班开始时间: {}, 排班结束时间: {}", now, scheduleStartTime, scheduleEndTime);
        
        // 签到时间窗口：时段开始前30分钟至结束前10分钟
        final int CHECK_IN_START_MINUTES = 30; // 开始前30分钟
        final int CHECK_IN_END_MINUTES = 10;   // 结束前10分钟
        LocalDateTime checkInStartTime = scheduleStartTime.minusMinutes(CHECK_IN_START_MINUTES);
        LocalDateTime checkInEndTime = scheduleEndTime.minusMinutes(CHECK_IN_END_MINUTES);
        
        logger.info("签到时间窗口 - 开始时间: {}, 截止时间: {}, 当前时间: {}", 
                checkInStartTime, checkInEndTime, now);
        
        // 判断是否在签到时间窗口内
        if (now.isBefore(checkInStartTime)) {
            // 签到时间未到
            logger.warn("签到时间未到 - 预约ID: {}, 签到开始时间: {}, 当前时间: {}", 
                    appointmentId, checkInStartTime, now);
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败: {}", e.getMessage());
            }
            throw new BadRequestException("签到时间未到，请在时段开始前30分钟开始签到（" + checkInStartTime + "）");
        }
        
        if (now.isAfter(scheduleEndTime)) {
            // 时段已结束，不能签到
            logger.warn("时段已结束，无法签到 - 预约ID: {}, 排班结束时间: {}, 当前时间: {}", 
                    appointmentId, scheduleEndTime, now);
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败: {}", e.getMessage());
            }
            throw new BadRequestException("时段已结束，无法签到。请改约后续时段或退号");
        }
        
        // 判断是否跨场（上午/下午场）
        boolean isCrossSession = isCrossSession(schedule.getScheduleDate(), schedule.getSlot().getStartTime(), schedule.getSlot().getEndTime(), now);
        
        // 判断是否迟到（超过签到截止时间但未到时段结束）
        boolean isLate = now.isAfter(checkInEndTime) && now.isBefore(scheduleEndTime);
        
        // 迟到降档：跨场则直接作废，未跨场但迟到则移到当前时段队尾
        if (isLate && isCrossSession) {
            // 跨场：直接作废预约
            logger.warn("跨场迟到，预约作废 - 预约ID: {}, 排班结束时间: {}, 当前时间: {}", 
                    appointmentId, scheduleEndTime, now);
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                logger.error("删除Redis Token失败: {}", e.getMessage());
            }
            appointment.setStatus(AppointmentStatus.cancelled);
            appointmentRepository.save(appointment);
            throw new BadRequestException("预约已过期（跨场迟到），预约已作废，请重新挂号");
        }
        
        if (isLate && !isCrossSession) {
            // 未跨场但迟到：标记为迟到，后续会移到当前时段队尾
            logger.warn("迟到签到 - 预约ID: {}, 签到截止时间: {}, 当前时间: {}", 
                    appointmentId, checkInEndTime, now);
        }
        
        // 判断是否按时签到（在签到时间窗口内且未超过截止时间）
        boolean isOnTimeCheckIn = !isLate && now.isAfter(checkInStartTime) && now.isBefore(checkInEndTime);
        
        logger.info("签到时间判断 - 是否跨场: {}, 是否迟到: {}, 是否按时: {}, 签到截止时间: {}", 
                isCrossSession, isLate, isOnTimeCheckIn, checkInEndTime);

        // 7. 更新签到时间和状态（先不分配实时候诊序号）
        appointment.setCheckInTime(now);
        appointment.setIsOnTime(isOnTimeCheckIn); // 记录是否按时签到
        appointment.setIsLate(isLate); // 记录是否迟到
        appointment.setStatus(AppointmentStatus.CHECKED_IN); // 设置状态为已签到
        
        // 8. 保存预约
        try {
            appointmentRepository.save(appointment);
            logger.info("签到信息已保存 - 预约ID: {}, 签到时间: {}, 是否按时: {}, 是否迟到: {}, 新状态: CHECKED_IN", 
                    appointmentId, now, isOnTimeCheckIn, isLate, appointment.getStatus());
        } catch (Exception e) {
            logger.error("保存签到信息失败 - 预约ID: {}, 错误信息: {}", appointmentId, e.getMessage(), e);
            throw new BadRequestException("签到失败，请重试");
        }
        
        // 9. 分配实时候诊序号（先签到先就诊）- 在保存后重新计算
        Integer realTimeQueueNumber = assignRealTimeQueueNumber(schedule, appointment, isLate);
        appointment.setRealTimeQueueNumber(realTimeQueueNumber);
        appointmentRepository.save(appointment);
        logger.info("实时候诊序号已分配 - 预约ID: {}, 实时候诊序号: {}", appointmentId, realTimeQueueNumber);

        // 7. 立即删除Token（确保一次性使用）
        try {
            Boolean deleted = redisTemplate.delete(redisKey);
            logger.info("Token已删除 - Key: {}, 删除结果: {}", redisKey, deleted);
        } catch (Exception e) {
            logger.error("删除Token失败 - Key: {}, 错误信息: {}", redisKey, e.getMessage(), e);
            // 即使删除失败也不影响签到结果，只记录日志
        }

        // 8. 返回签到信息
        CheckInResponse response = new CheckInResponse();
        response.setAppointmentId(appointmentId);
        response.setScheduleId(schedule.getScheduleId()); // 添加排班ID
        response.setPatientName(appointment.getPatient().getFullName());
        
        if (schedule.getDoctor() != null) {
            if (schedule.getDoctor().getDepartment() != null) {
                response.setDepartmentName(schedule.getDoctor().getDepartment().getName());
                logger.info("科室信息: {}", schedule.getDoctor().getDepartment().getName());
            }
            response.setDoctorName(schedule.getDoctor().getFullName());
            logger.info("医生信息: {}", schedule.getDoctor().getFullName());
        }
        
        response.setCheckInTime(now);
        response.setAppointmentNumber(appointment.getAppointmentNumber());

        logger.info("签到成功 - 预约ID: {}, 患者: {}, 就诊序号: {}, 签到时间: {}", 
                appointmentId, response.getPatientName(), response.getAppointmentNumber(), now);
        logger.info("========== 扫码签到结束 ==========");
        return response;
    }

    /**
     * 清除预约签到时间（管理员功能）
     */
    @Transactional
    public AppointmentResponse clearCheckIn(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("预约不存在，ID: " + appointmentId));
        
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("该预约状态不是已签到（当前状态：" + appointment.getStatus() + "），无需清除");
        }
        
        // 清除签到时间和状态（改回 scheduled）
        appointment.setCheckInTime(null);
        appointment.setIsOnTime(false); // 清除按时签到标记
        appointment.setCalledAt(null); // 清除叫号时间
        appointment.setRecheckInTime(null); // 清除重新签到时间
        appointment.setStatus(AppointmentStatus.scheduled); // 改回已预约状态
        appointmentRepository.save(appointment);
        
        return convertToResponseDto(appointment);
    }

    /**
     * 获取叫号队列（已签到但未就诊的预约列表）
     * 排序规则（按优先级从高到低）：
     * 1. 预约挂号：提前签到按挂号序号，时段内签到按签到时间
     * 2. 现场挂号：排在预约挂号之后
     * 3. 当日复诊号（同医生）：每两位正常挂号患者之后插入一个复诊号
     * 4. 当日复诊号（不同医生）：排在该医生所有患者的最后面
     * 5. 过号：重新扫码后排当前时段队尾
     * 6. 加号：加在所有号源的最后
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getCallQueue(Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));
        
        // 查询已签到且未就诊的预约
        List<Appointment> appointments = appointmentRepository
                .findByScheduleAndStatus(schedule, AppointmentStatus.CHECKED_IN);
        
        // 分离不同类型的预约
        List<Appointment> normalAppointments = new ArrayList<>();  // 正常预约和现场挂号
        List<Appointment> sameDayFollowUps = new ArrayList<>();     // 当日复诊号（同医生）
        List<Appointment> differentDoctorFollowUps = new ArrayList<>(); // 当日复诊号（不同医生）
        List<Appointment> missedCallAppointments = new ArrayList<>(); // 过号（重新扫码后排队尾）
        List<Appointment> addOnAppointments = new ArrayList<>();    // 加号
        
        for (Appointment appointment : appointments) {
            AppointmentType type = appointment.getAppointmentType();
            if (type == null) {
                // 兼容旧数据，根据isWalkIn判断
                type = Boolean.TRUE.equals(appointment.getIsWalkIn()) 
                    ? AppointmentType.WALK_IN 
                    : AppointmentType.APPOINTMENT;
            }
            
            if (Boolean.TRUE.equals(appointment.getIsAddOn())) {
                addOnAppointments.add(appointment);
            } else if (appointment.getRecheckInTime() != null && appointment.getMissedCallCount() != null && appointment.getMissedCallCount() > 0) {
                // 过号：有重新签到时间且过号次数>0
                missedCallAppointments.add(appointment);
            } else if (type == AppointmentType.SAME_DAY_FOLLOW_UP) {
                // 判断是否同医生（通过originalAppointmentId关联的预约是否在同一排班）
                if (appointment.getOriginalAppointmentId() != null) {
                    Appointment originalAppointment = appointmentRepository.findById(appointment.getOriginalAppointmentId()).orElse(null);
                    if (originalAppointment != null && originalAppointment.getSchedule().getScheduleId().equals(scheduleId)) {
                        sameDayFollowUps.add(appointment);
                    } else {
                        differentDoctorFollowUps.add(appointment);
                    }
                } else {
                    differentDoctorFollowUps.add(appointment);
                }
            } else {
                normalAppointments.add(appointment);
            }
            }
            
        // 对正常预约和现场挂号进行排序
        normalAppointments.sort((a1, a2) -> {
            // 1. 预约优先于现场挂号
            boolean a1WalkIn = a1.getAppointmentType() == AppointmentType.WALK_IN || Boolean.TRUE.equals(a1.getIsWalkIn());
            boolean a2WalkIn = a2.getAppointmentType() == AppointmentType.WALK_IN || Boolean.TRUE.equals(a2.getIsWalkIn());
            
            if (!a1WalkIn && a2WalkIn) {
                return -1; // a1是预约，a2是现场挂号，a1优先
            }
            if (a1WalkIn && !a2WalkIn) {
                return 1; // a1是现场挂号，a2是预约，a2优先
            }
            
            // 2. 迟到降档
            boolean a1Late = Boolean.TRUE.equals(a1.getIsLate());
            boolean a2Late = Boolean.TRUE.equals(a2.getIsLate());
            
            if (!a1Late && a2Late) {
                return -1; // a1按时，a2迟到，a1优先
            }
            if (a1Late && !a2Late) {
                return 1; // a1迟到，a2按时，a2优先
            }
            
            // 3. 按时签到的按实时候诊序号排序
            if (!a1Late && !a2Late) {
                Integer num1 = a1.getRealTimeQueueNumber();
                Integer num2 = a2.getRealTimeQueueNumber();
                if (num1 != null && num2 != null) {
                    return Integer.compare(num1, num2);
            }
                // 如果实时候诊序号为空，按签到时间排序
                if (a1.getCheckInTime() != null && a2.getCheckInTime() != null) {
            return a1.getCheckInTime().compareTo(a2.getCheckInTime());
                }
            }
            
            // 4. 都迟到，按签到时间排序
            if (a1Late && a2Late) {
                if (a1.getCheckInTime() != null && a2.getCheckInTime() != null) {
                    return a1.getCheckInTime().compareTo(a2.getCheckInTime());
                }
            }
            
            // 5. 默认按挂号序号排序
            return Integer.compare(
                    a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                    a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
            );
        });
        
        // 对当日复诊号排序（按签到时间）
        sameDayFollowUps.sort((a1, a2) -> {
            if (a1.getCheckInTime() != null && a2.getCheckInTime() != null) {
                return a1.getCheckInTime().compareTo(a2.getCheckInTime());
            }
            return Integer.compare(
                    a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                    a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
            );
        });
        
        // 对不同医生的复诊号排序（按签到时间）
        differentDoctorFollowUps.sort((a1, a2) -> {
            if (a1.getCheckInTime() != null && a2.getCheckInTime() != null) {
                return a1.getCheckInTime().compareTo(a2.getCheckInTime());
            }
            return Integer.compare(
                    a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                    a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
            );
        });
        
        // 对过号排序（按重新签到时间）
        missedCallAppointments.sort((a1, a2) -> {
            if (a1.getRecheckInTime() != null && a2.getRecheckInTime() != null) {
                return a1.getRecheckInTime().compareTo(a2.getRecheckInTime());
            }
            return Integer.compare(
                    a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                    a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
            );
        });
        
        // 对加号排序（按签到时间）
        addOnAppointments.sort((a1, a2) -> {
            if (a1.getCheckInTime() != null && a2.getCheckInTime() != null) {
                return a1.getCheckInTime().compareTo(a2.getCheckInTime());
            }
            return Integer.compare(
                    a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                    a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
            );
        });
        
        // 构建最终队列
        List<Appointment> finalQueue = new ArrayList<>();
        
        // 1. 先添加正常预约和现场挂号
        finalQueue.addAll(normalAppointments);
        
        // 2. 插入当日复诊号（同医生）：每两位正常挂号患者之后插入一个复诊号
        int followUpIndex = 0;
        for (int i = 2; i < finalQueue.size() && followUpIndex < sameDayFollowUps.size(); i += 3) {
            finalQueue.add(i, sameDayFollowUps.get(followUpIndex));
            followUpIndex++;
        }
        // 剩余的复诊号添加到末尾（如果还有）
        while (followUpIndex < sameDayFollowUps.size()) {
            finalQueue.add(sameDayFollowUps.get(followUpIndex));
            followUpIndex++;
        }
        
        // 3. 添加不同医生的复诊号（排在最后）
        finalQueue.addAll(differentDoctorFollowUps);
        
        // 4. 添加过号（排在最后，按重新签到时间排序）
        finalQueue.addAll(missedCallAppointments);
        
        // 5. 添加加号（排在最后）
        finalQueue.addAll(addOnAppointments);
        
        return finalQueue.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取下一个应该叫号的预约
     * 规则：先叫按时签到的（按序号），再叫迟到的（按签到时间），最后叫过号重新签到的
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getNextAppointmentToCall(Integer scheduleId) {
        List<AppointmentResponse> queue = getCallQueue(scheduleId);
        
        // 找到第一个未叫号的
        for (AppointmentResponse response : queue) {
            Appointment appointment = appointmentRepository.findById(response.getAppointmentId())
                    .orElse(null);
            if (appointment != null && appointment.getCalledAt() == null) {
                return response;
            }
        }
        
        return null; // 没有待叫号的预约
    }

    /**
     * 执行叫号
     */
    @Transactional
    public AppointmentResponse callAppointment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));
        
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("只有已签到的预约才能被叫号，当前状态：" + appointment.getStatus());
        }
        
        // 如果已经叫过号（过号后重新签到），增加过号次数
        if (appointment.getCalledAt() != null) {
            appointment.setMissedCallCount(
                    (appointment.getMissedCallCount() == null ? 0 : appointment.getMissedCallCount()) + 1);
            logger.info("过号患者重新叫号 - 预约ID: {}, 过号次数: {}", appointmentId, appointment.getMissedCallCount());
        }
        
        // 更新叫号时间
        appointment.setCalledAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
        
        logger.info("叫号成功 - 预约ID: {}, 患者: {}, 就诊序号: {}, 是否按时: {}, 叫号时间: {}", 
                appointmentId, appointment.getPatient().getFullName(), 
                appointment.getAppointmentNumber(), appointment.getIsOnTime(), appointment.getCalledAt());
        
        return convertToResponseDto(appointment);
    }

    /**
     * 标记就诊完成（并自动叫号下一位）
     */
    @Transactional
    public AppointmentResponse completeAppointment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("只有已签到的预约才能标记就诊完成，当前状态：" + appointment.getStatus());
        }

        if (appointment.getCalledAt() == null) {
            throw new BadRequestException("该预约还未被叫号，无法标记就诊完成");
        }

        // 标记就诊完成
        appointment.setStatus(AppointmentStatus.completed);
        appointmentRepository.save(appointment);

        logger.info("就诊完成 - 预约ID: {}, 患者: {}, 医生: {}, 完成时间: {}",
                appointmentId, appointment.getPatient().getFullName(),
                appointment.getSchedule().getDoctor().getFullName(), LocalDateTime.now());

        // 自动叫号下一位
        try {
            autoCallNextPatient(appointment.getSchedule().getScheduleId(), appointmentId);
        } catch (Exception e) {
            logger.error("自动叫号下一位失败 - 预约ID: {}, 错误: {}", appointmentId, e.getMessage());
            // 不抛出异常，因为就诊完成已经成功了
        }

        return convertToResponseDto(appointment);
    }

    /**
     * 自动叫号下一位患者
     * @param scheduleId 时段ID
     * @param completedAppointmentId 刚完成就诊的预约ID（用于日志）
     */
    private void autoCallNextPatient(Integer scheduleId, Integer completedAppointmentId) {
        logger.info("开始自动叫号下一位 - 时段ID: {}, 刚完成预约ID: {}", scheduleId, completedAppointmentId);

        // 获取下一个待叫号的预约
        AppointmentResponse nextAppointment = getNextAppointmentToCall(scheduleId);

        if (nextAppointment == null) {
            logger.info("没有待叫号的患者 - 时段ID: {}", scheduleId);
            return;
        }

        try {
            // 执行叫号
            AppointmentResponse calledAppointment = callAppointment(nextAppointment.getAppointmentId());

            logger.info("自动叫号成功 - 时段ID: {}, 叫号预约ID: {}, 患者: {}, 医生: {}",
                    scheduleId, calledAppointment.getAppointmentId(),
                    calledAppointment.getPatient() != null ? calledAppointment.getPatient().getFullName() : "未知",
                    calledAppointment.getSchedule() != null && calledAppointment.getSchedule().getDoctorName() != null ?
                    calledAppointment.getSchedule().getDoctorName() : "未知");

        } catch (Exception e) {
            logger.error("自动叫号失败 - 时段ID: {}, 预约ID: {}, 错误: {}", scheduleId, nextAppointment.getAppointmentId(), e.getMessage());
            throw e; // 重新抛出异常，让调用方处理
        }
    }

    /**
     * 标记过号（状态改回scheduled，允许重新扫码签到）
     * 规则：管理员/医生标记患者过号，增加过号次数，并将预约状态改回scheduled，清除签到记录
     */
    @Transactional
    public AppointmentResponse markMissedCall(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("只有已签到的预约才能标记过号，当前状态：" + appointment.getStatus());
        }

        if (appointment.getCalledAt() == null) {
            throw new BadRequestException("该预约还未被叫号，无法标记过号");
        }

        // 增加过号次数
        appointment.setMissedCallCount(
                (appointment.getMissedCallCount() == null ? 0 : appointment.getMissedCallCount()) + 1);

        // 清除签到记录，状态改回scheduled
        appointment.setStatus(AppointmentStatus.scheduled);
        appointment.setCheckInTime(null);
        appointment.setCalledAt(null);
        appointment.setRecheckInTime(null);
        appointment.setRealTimeQueueNumber(null);
        appointment.setIsOnTime(false);
        appointment.setIsLate(false);

        appointmentRepository.save(appointment);

        logger.info("标记过号成功 - 预约ID: {}, 患者: {}, 过号次数: {}, 状态已改回scheduled",
                appointmentId, appointment.getPatient().getFullName(), appointment.getMissedCallCount());

        return convertToResponseDto(appointment);
    }

    /**
     * 过号后重新签到 - 放在当前时段最后一位
     * 规则：听到叫号未进诊室，需到自助机"再次扫码"，系统把你放在"当前时段最后一位"
     */
    @Transactional
    public AppointmentResponse recheckInAfterMissedCall(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));
        
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("只有已签到的预约才能重新签到，当前状态：" + appointment.getStatus());
        }
        
        // 如果已叫号，说明是过号后重新扫码，允许重新签到
        // 如果已重新签到过但再次被叫号，也允许再次重新签到
        if (appointment.getCalledAt() == null && appointment.getRecheckInTime() == null) {
            throw new BadRequestException("该预约还未被叫号，无需重新签到");
        }
        
        // 如果已重新签到但未再次叫号，不允许重复重新签到
        if (appointment.getRecheckInTime() != null && appointment.getCalledAt() == null) {
            throw new BadRequestException("该预约已重新签到，请等待叫号。如需再次重新签到，请先让医生叫号后再扫码");
        }
        
        Schedule schedule = appointment.getSchedule();
        if (schedule == null) {
            throw new BadRequestException("预约排班信息不完整");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 关键：就诊序号不变！只是清除叫号记录，重新进入队列
        // appointment.setAppointmentNumber(...); // 不修改序号
        
        // 清除叫号时间，允许重新排队
        appointment.setCalledAt(null);
        
        // 记录重新签到时间（用于排序）
        appointment.setRecheckInTime(now);
        
        // 重新签到后，isOnTime设为false（因为已经过号了）
        appointment.setIsOnTime(false);
        
        // 过号重排：放在当前时段最后一位
        // 查询同一排班下已签到的预约，找到最大的实时候诊序号
        List<Appointment> checkedInAppointments = appointmentRepository
                .findByScheduleAndStatus(schedule, AppointmentStatus.CHECKED_IN);
        
        int maxRealTimeQueueNumber = checkedInAppointments.stream()
                .filter(a -> a.getRealTimeQueueNumber() != null && !a.getAppointmentId().equals(appointmentId))
                .mapToInt(Appointment::getRealTimeQueueNumber)
                .max()
                .orElse(0);
        
        // 设置为最大序号+1，确保排在最后
        appointment.setRealTimeQueueNumber(maxRealTimeQueueNumber + 1);
        
        // 增加过号次数
        appointment.setMissedCallCount(
                (appointment.getMissedCallCount() == null ? 0 : appointment.getMissedCallCount()) + 1);
        
        appointmentRepository.save(appointment);
        
        logger.info("过号重新签到成功 - 预约ID: {}, 患者: {}, 就诊序号: {} (不变), 实时候诊序号: {} (最后一位), 重新签到时间: {}", 
                appointmentId, appointment.getPatient().getFullName(), 
                appointment.getAppointmentNumber(), appointment.getRealTimeQueueNumber(), appointment.getRecheckInTime());
        
        return convertToResponseDto(appointment);
    }

    /**
     * 判断是否跨场（上午/下午场）
     * 规则：如果当前时间已经跨过了上午场（12:00）或下午场（18:00），则算跨场
     */
    private boolean isCrossSession(LocalDate scheduleDate, LocalTime slotStartTime, LocalTime slotEndTime, LocalDateTime currentTime) {
        // 定义上午场结束时间（12:00）和下午场结束时间（18:00）
        LocalTime morningEnd = LocalTime.of(12, 0);
        LocalTime afternoonEnd = LocalTime.of(18, 0);
        
        LocalTime currentTimeOnly = currentTime.toLocalTime();
        LocalDate currentDate = currentTime.toLocalDate();
        
        // 如果日期不同，肯定跨场
        if (!currentDate.equals(scheduleDate)) {
            return true;
        }
        
        // 如果排班是上午场（结束时间在12:00之前），但当前时间已过12:00，算跨场
        if (slotEndTime.isBefore(morningEnd) && currentTimeOnly.isAfter(morningEnd)) {
            return true;
        }
        
        // 如果排班是下午场（开始时间在12:00之后，结束时间在18:00之前），但当前时间已过18:00，算跨场
        if (slotStartTime.isAfter(morningEnd) && slotEndTime.isBefore(afternoonEnd) && currentTimeOnly.isAfter(afternoonEnd)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 分配实时候诊序号
     * 规则：
     * 1. 提前签到（时段开始前）：按挂号序号排序
     * 2. 时段内签到：按签到时间排序（先签到先就诊）
     * 3. 迟到签到：排在队尾
     */
    private Integer assignRealTimeQueueNumber(Schedule schedule, Appointment currentAppointment, boolean isLate) {
        // 重新查询当前预约（确保获取最新数据）
        Appointment refreshedAppointment = appointmentRepository.findById(currentAppointment.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        final Integer currentAppointmentId = refreshedAppointment.getAppointmentId();
        final LocalDateTime currentCheckInTime = refreshedAppointment.getCheckInTime();
        
        // 查询同一排班下已签到的预约（包括当前预约）
        List<Appointment> checkedInAppointments = appointmentRepository
                .findByScheduleAndStatus(schedule, AppointmentStatus.CHECKED_IN);
        
        // 计算时段开始时间
        LocalDateTime scheduleStartTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getSlot().getStartTime());
        
        // 如果迟到，排在队尾（获取当前最大序号+1）
        if (isLate) {
            int maxNumber = checkedInAppointments.stream()
                    .filter(a -> a.getRealTimeQueueNumber() != null && 
                               !a.getAppointmentId().equals(currentAppointmentId))
                    .mapToInt(Appointment::getRealTimeQueueNumber)
                    .max()
                    .orElse(0);
            return maxNumber + 1;
        }
        
        // 判断当前预约是提前签到还是时段内签到
        boolean isEarlyCheckIn = currentCheckInTime != null && currentCheckInTime.isBefore(scheduleStartTime);
        
        // 按时签到的预约（包括提前和时段内）
        List<Appointment> onTimeAppointments = checkedInAppointments.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsOnTime()) && a.getCheckInTime() != null)
                .collect(Collectors.toList());
        
        // 分离提前签到和时段内签到
        List<Appointment> earlyCheckIns = onTimeAppointments.stream()
                .filter(a -> a.getCheckInTime().isBefore(scheduleStartTime))
                .sorted((a1, a2) -> Integer.compare(
                        a1.getAppointmentNumber() != null ? a1.getAppointmentNumber() : 0,
                        a2.getAppointmentNumber() != null ? a2.getAppointmentNumber() : 0
                ))
                .collect(Collectors.toList());
        
        List<Appointment> inTimeCheckIns = onTimeAppointments.stream()
                .filter(a -> !a.getCheckInTime().isBefore(scheduleStartTime))
                .sorted((a1, a2) -> a1.getCheckInTime().compareTo(a2.getCheckInTime()))
                .collect(Collectors.toList());
        
        // 合并：提前签到的在前（按挂号序号），时段内签到的在后（按签到时间）
        List<Appointment> sortedAppointments = new ArrayList<>();
        sortedAppointments.addAll(earlyCheckIns);
        sortedAppointments.addAll(inTimeCheckIns);
        
        // 找到当前预约在排序后的位置
        for (int i = 0; i < sortedAppointments.size(); i++) {
            if (sortedAppointments.get(i).getAppointmentId().equals(currentAppointmentId)) {
                return i + 1;
            }
        }
        
        // 如果当前预约不在按时签到列表中（理论上不会发生），返回列表大小+1
        return sortedAppointments.size() + 1;
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}