package com.example.springboot.task;

import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.AppointmentType;
import com.example.springboot.entity.enums.ScheduleStatus;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 加号支付超时处理定时任务
 * 参考候补超时处理（WaitlistExpirationTask）的实现方式
 * 每1分钟执行一次，检查payment_deadline < NOW()的加号预约，自动取消并释放虚拟号源
 */
@Component
public class AddOnPaymentExpirationTask {

    private static final Logger logger = LoggerFactory.getLogger(AddOnPaymentExpirationTask.class);

    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    @Autowired
    public AddOnPaymentExpirationTask(
            AppointmentRepository appointmentRepository,
            ScheduleRepository scheduleRepository,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
    }

    /**
     * 加号支付超时处理任务
     * 每60秒执行一次（参考候补的fixedRate = 60000）
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredAddOnPayments() {
        try {
            logger.debug("开始执行加号支付超时处理任务");
            
            LocalDateTime now = LocalDateTime.now();
            
            // 查询超时未支付的加号预约（参考候补的expireNotifiedWaitlists）
            List<Appointment> expiredAppointments = appointmentRepository.findExpiredAddOnPayments(
                    AppointmentType.ADD_ON,
                    AppointmentStatus.PENDING_PAYMENT,
                    now
            );
            
            if (expiredAppointments.isEmpty()) {
                logger.debug("没有超时的加号预约");
                return;
            }
            
            logger.info("找到 {} 个超时的加号预约", expiredAppointments.size());
            
            for (Appointment appointment : expiredAppointments) {
                try {
                    processExpiredAppointment(appointment);
                } catch (Exception e) {
                    logger.error("处理超时预约失败 - appointmentId: {}", 
                            appointment.getAppointmentId(), e);
                    // 单个失败不影响其他记录的处理
                }
            }
            
            logger.debug("加号支付超时处理任务执行完成");
            
        } catch (Exception e) {
            logger.error("加号支付超时处理任务执行失败", e);
        }
    }

    /**
     * 处理单个超时预约
     */
    private void processExpiredAppointment(Appointment appointment) {
        logger.info("处理超时加号预约 - appointmentId: {}, patientId: {}, deadline: {}", 
                appointment.getAppointmentId(),
                appointment.getPatient().getPatientId(),
                appointment.getPaymentDeadline());

        // 1. 取消预约
        appointment.setStatus(AppointmentStatus.cancelled);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
        logger.info("预约已取消 - appointmentId: {}", appointment.getAppointmentId());

        // 2. 释放加号号源
        // 注意：加号采用预留机制，支付前不占用bookedSlots，因此超时时无需修改号源数量
        // 只需取消预约即可，号源会自动释放
        logger.info("加号预约已取消，号源自动释放（预留机制）- scheduleId: {}", 
                appointment.getSchedule().getScheduleId());

        // 3. 通知患者
        try {
            com.example.springboot.dto.notification.NotificationCreateRequest notification = 
                new com.example.springboot.dto.notification.NotificationCreateRequest();
            notification.setUserId(appointment.getPatient().getPatientId().intValue());
            notification.setUserType(com.example.springboot.entity.enums.UserType.patient);
            notification.setType(com.example.springboot.entity.enums.NotificationType.cancellation);
            notification.setTitle("加号已失效");
            notification.setContent(String.format("由于未在规定时间内完成支付，您的加号预约（预约号: %d）已自动取消。如需就诊，请重新申请。",
                    appointment.getAppointmentId()));
            notification.setRelatedEntity("appointment");
            notification.setRelatedId(appointment.getAppointmentId());
            notification.setPriority(com.example.springboot.entity.enums.NotificationPriority.high);
            notificationService.createNotification(notification);
            
            logger.info("已发送超时通知给患者 - patientId: {}", 
                    appointment.getPatient().getPatientId());
        } catch (Exception e) {
            logger.error("发送超时通知失败 - patientId: {}", 
                    appointment.getPatient().getPatientId(), e);
            // 通知失败不影响主流程
        }
    }
}
