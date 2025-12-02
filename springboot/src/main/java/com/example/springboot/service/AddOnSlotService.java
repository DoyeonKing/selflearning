package com.example.springboot.service;

import com.example.springboot.entity.*;
import com.example.springboot.entity.enums.*;
import com.example.springboot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 加号专用服务
 * 处理加号审批通过后的完整流程
 * 参考候补（Waitlist）的实现方式
 */
@Service
public class AddOnSlotService {

    private static final Logger logger = LoggerFactory.getLogger(AddOnSlotService.class);
    
    // 支付期限配置（小时）
    private static final int PAYMENT_DEADLINE_HOURS = 24;

    private final SlotApplicationRepository slotApplicationRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    @Autowired
    public AddOnSlotService(
            SlotApplicationRepository slotApplicationRepository,
            ScheduleRepository scheduleRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            NotificationService notificationService) {
        this.slotApplicationRepository = slotApplicationRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
    }

    /**
     * 处理加号审批通过的完整流程
     * 参考候补的notification_sent_at + 15分钟模式
     * 
     * @param application 加号申请
     * @param approverId 审批人ID
     * @return 生成的预约记录
     */
    @Transactional
    public Appointment processApprovedAddOn(SlotApplication application, Integer approverId) {
        logger.info("开始处理加号审批通过流程 - applicationId: {}, patientId: {}, scheduleId: {}", 
                application.getApplicationId(), application.getPatientId(), application.getScheduleId());

        try {
            // 验证申请状态
            if (application.getStatus() == SlotApplicationStatus.APPROVED) {
                logger.warn("加号申请已经被批准过了 - applicationId: {}", application.getApplicationId());
                throw new RuntimeException("该加号申请已经被批准，请勿重复操作");
            }
            
            // 1. 更新申请状态
            logger.info("步骤1: 更新申请状态");
            updateApplicationStatus(application, approverId);
            
            // 2. 获取原始排班（不修改号源数量，使用预留机制）
            logger.info("步骤2: 获取原始排班");
            Schedule schedule = scheduleRepository.findById(application.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("原排班不存在 - scheduleId: " + application.getScheduleId()));
            
            logger.info("原始排班信息 - scheduleId: {}, 总号源数: {}, 已预约数: {}",
                    schedule.getScheduleId(), schedule.getTotalSlots(), schedule.getBookedSlots());
            
            // 只在备注中记录加号信息，不修改totalSlots（加号采用预留机制）
            schedule.setRemarks((schedule.getRemarks() != null ? schedule.getRemarks() + "; " : "") + 
                    "加号+" + application.getAddedSlots() + "(申请ID:" + application.getApplicationId() + ")");
            scheduleRepository.save(schedule);
            logger.info("记录加号信息到排班备注");
            
            // 3. 自动生成预约记录
            logger.info("步骤3: 生成预约记录");
            Appointment appointment = createAddOnAppointment(application, schedule);
            logger.info("生成加号预约成功 - appointmentId: {}", appointment.getAppointmentId());
            
            // 4. 关联预约ID到申请记录
            logger.info("步骤4: 关联预约ID到申请记录");
            application.setAppointmentId(appointment.getAppointmentId());
            slotApplicationRepository.save(application);
            
            // 5. 发送通知
            logger.info("步骤5: 发送通知");
            sendNotifications(application, appointment);
            logger.info("发送通知成功");
            
            logger.info("加号审批通过流程完成 - applicationId: {}, appointmentId: {}", 
                    application.getApplicationId(), appointment.getAppointmentId());
            
            return appointment;
            
        } catch (Exception e) {
            logger.error("处理加号审批通过流程失败 - applicationId: {}", application.getApplicationId(), e);
            logger.error("错误详情: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("处理加号审批失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新申请状态
     */
    private void updateApplicationStatus(SlotApplication application, Integer approverId) {
        application.setStatus(SlotApplicationStatus.APPROVED);
        application.setApproverId(approverId);
        application.setApprovedAt(LocalDateTime.now());
        slotApplicationRepository.save(application);
    }

    // 注意：不再创建虚拟号源，而是直接在原排班上增加号源数量
    // 这样可以避免违反 schedules 表的唯一约束 (doctor_id, schedule_date, slot_id)

    /**
     * 自动生成预约记录
     * 参考候补的notification_sent_at，这里使用payment_deadline
     */
    private Appointment createAddOnAppointment(SlotApplication application, Schedule schedule) {
        logger.info("创建加号预约 - patientId: {}, scheduleId: {}",
                application.getPatientId(), schedule.getScheduleId());
        
        // 获取患者信息
        Patient patient = patientRepository.findById(application.getPatientId())
                .orElseThrow(() -> new RuntimeException("患者不存在 - patientId: " + application.getPatientId()));

        logger.info("患者信息 - name: {}, phone: {}", patient.getFullName(), patient.getPhoneNumber());

        // 获取下一个就诊序号
        Integer nextNumber = getNextAppointmentNumber(schedule);
        logger.info("分配就诊序号: {}", nextNumber);

        // 创建预约记录
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSchedule(schedule);
        appointment.setAppointmentNumber(nextNumber);
        appointment.setAppointmentType(AppointmentType.ADD_ON); // 加号类型
        appointment.setIsAddOn(true);
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT); // 待支付状态
        appointment.setPaymentStatus(PaymentStatus.unpaid);
        appointment.setIsWalkIn(false);
        
        // ===== 关键：设置支付截止时间（参考候补的notification_sent_at） =====
        LocalDateTime deadline = LocalDateTime.now().plusHours(PAYMENT_DEADLINE_HOURS);
        appointment.setPaymentDeadline(deadline);
        
        logger.info("设置支付截止时间: {} ({}小时后)", deadline, PAYMENT_DEADLINE_HOURS);

        logger.info("保存预约记录到数据库");
        try {
            Appointment saved = appointmentRepository.save(appointment);
            logger.info("预约记录保存成功 - appointmentId: {}", saved.getAppointmentId());
            return saved;
        } catch (Exception e) {
            logger.error("保存预约记录失败", e);
            throw new RuntimeException("创建预约记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取下一个就诊序号
     * 加号患者使用独立的序号范围（从101开始），避免与普通预约冲突
     */
    private Integer getNextAppointmentNumber(Schedule schedule) {
        // 加号预约从 101 开始编号
        Integer maxAddOnNumber = appointmentRepository.findMaxAppointmentNumberByScheduleAndType(
                schedule, AppointmentType.ADD_ON);
        
        Integer nextNumber;
        if (maxAddOnNumber == null || maxAddOnNumber < 101) {
            nextNumber = 101; // 第一个加号从101开始
        } else {
            nextNumber = maxAddOnNumber + 1;
        }
        
        logger.info("分配加号序号 - 当前最大加号序号: {}, 新序号: {}", maxAddOnNumber, nextNumber);
        return nextNumber;
    }

    /**
     * 发送通知
     */
    private void sendNotifications(SlotApplication application, Appointment appointment) {
        try {
            // 通知医生
            com.example.springboot.dto.notification.NotificationCreateRequest doctorNotification = 
                new com.example.springboot.dto.notification.NotificationCreateRequest();
            doctorNotification.setUserId(application.getDoctorId());
            doctorNotification.setUserType(com.example.springboot.entity.enums.UserType.doctor);
            doctorNotification.setType(com.example.springboot.entity.enums.NotificationType.system_notice);
            doctorNotification.setTitle("加号申请已批准");
            doctorNotification.setContent(String.format("您的加号申请已通过审批，患者将收到通知。申请ID: %d", 
                    application.getApplicationId()));
            doctorNotification.setRelatedEntity("slot_application");
            doctorNotification.setRelatedId(application.getApplicationId());
            doctorNotification.setPriority(com.example.springboot.entity.enums.NotificationPriority.normal);
            notificationService.createNotification(doctorNotification);

            // 通知患者
            String patientMessage = String.format(
                    "医生已为您申请加号，请在%s前完成支付。预约号: %d，挂号费: %.2f元。支付后凭证将自动激活。",
                    appointment.getPaymentDeadline().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    appointment.getAppointmentId(),
                    appointment.getSchedule().getFee()
            );
            
            com.example.springboot.dto.notification.NotificationCreateRequest patientNotification = 
                new com.example.springboot.dto.notification.NotificationCreateRequest();
            patientNotification.setUserId(application.getPatientId().intValue());
            patientNotification.setUserType(com.example.springboot.entity.enums.UserType.patient);
            patientNotification.setType(com.example.springboot.entity.enums.NotificationType.appointment_success);
            patientNotification.setTitle("加号成功，请尽快支付");
            patientNotification.setContent(patientMessage);
            patientNotification.setRelatedEntity("appointment");
            patientNotification.setRelatedId(appointment.getAppointmentId());
            patientNotification.setPriority(com.example.springboot.entity.enums.NotificationPriority.urgent);
            notificationService.createNotification(patientNotification);
            
        } catch (Exception e) {
            logger.error("发送通知失败", e);
            // 通知失败不影响主流程
        }
    }

    /**
     * 处理支付成功
     */
    @Transactional
    public void handlePaymentSuccess(Integer appointmentId, String paymentMethod, String transactionId) {
        logger.info("处理加号支付成功 - appointmentId: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

        // 验证是否为加号预约
        if (appointment.getAppointmentType() != AppointmentType.ADD_ON) {
            throw new RuntimeException("不是加号预约");
        }

        // 验证支付期限
        if (LocalDateTime.now().isAfter(appointment.getPaymentDeadline())) {
            throw new RuntimeException("支付已超时，请重新申请");
        }

        // 更新预约状态
        appointment.setStatus(AppointmentStatus.scheduled);
        appointment.setPaymentStatus(PaymentStatus.paid);
        appointment.setPaymentMethod(paymentMethod);
        appointment.setTransactionId(transactionId);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // 更新排班的已预约数（加号支付成功后才占用号源）
        Schedule schedule = appointment.getSchedule();
        Integer currentBooked = schedule.getBookedSlots();
        schedule.setBookedSlots(currentBooked + 1);
        scheduleRepository.save(schedule);
        
        logger.info("更新排班已预约数 - scheduleId: {}, 原已预约: {}, 新已预约: {}",
                schedule.getScheduleId(), currentBooked, schedule.getBookedSlots());

        logger.info("加号支付处理完成 - appointmentId: {}, scheduleId: {}", 
                appointmentId, schedule.getScheduleId());

        // 发送支付成功通知
        try {
            com.example.springboot.dto.notification.NotificationCreateRequest notification = 
                new com.example.springboot.dto.notification.NotificationCreateRequest();
            notification.setUserId(appointment.getPatient().getPatientId().intValue());
            notification.setUserType(com.example.springboot.entity.enums.UserType.patient);
            notification.setType(com.example.springboot.entity.enums.NotificationType.payment_success);
            notification.setTitle("支付成功");
            notification.setContent(String.format("您的加号预约已支付成功。预约号: %d，请按时就诊。", appointmentId));
            notification.setRelatedEntity("appointment");
            notification.setRelatedId(appointmentId);
            notification.setPriority(com.example.springboot.entity.enums.NotificationPriority.high);
            notificationService.createNotification(notification);
        } catch (Exception e) {
            logger.error("发送支付成功通知失败", e);
        }
    }
}
