package com.example.springboot.service;

import com.example.springboot.dto.notification.NotificationCreateRequest;
import com.example.springboot.dto.notification.NotificationResponse;
import com.example.springboot.entity.Notification;
import com.example.springboot.entity.enums.NotificationPriority;
import com.example.springboot.entity.enums.NotificationStatus;
import com.example.springboot.entity.enums.NotificationType;
import com.example.springboot.entity.enums.UserType;
import com.example.springboot.repository.NotificationRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 创建通知
     */
    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setUserType(request.getUserType());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setRelatedEntity(request.getRelatedEntity());
        notification.setRelatedId(request.getRelatedId());
        notification.setPriority(request.getPriority() != null ? request.getPriority() : NotificationPriority.normal);
        notification.setStatus(NotificationStatus.unread);

        Notification saved = notificationRepository.save(notification);
        return convertToResponse(saved);
    }

    /**
     * 发送预约成功通知
     */
    @Transactional
    public NotificationResponse sendAppointmentNotification(Integer patientId, Integer appointmentId,
                                                           String departmentName, String doctorName,
                                                           String scheduleDate, String slotName,
                                                           String locationName, Integer appointmentNumber) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.appointment_success);
        request.setTitle("预约成功");
        String content = String.format("您的预约已成功创建！\n科室：%s\n医生：%s\n就诊时间：%s %s\n就诊序号：%d\n", 
                departmentName, doctorName, scheduleDate, slotName, appointmentNumber);
        if (locationName != null && !locationName.trim().isEmpty()) {
            content += String.format("就诊地点：%s\n", locationName);
        }
        content += "请及时完成支付，祝您早日康复！";
        request.setContent(content);
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 发送支付成功通知
     */
    @Transactional
    public NotificationResponse sendPaymentSuccessNotification(Integer patientId, Integer appointmentId, 
                                                               String departmentName, String doctorName, 
                                                               String scheduleDate, String slotName, 
                                                               Double fee) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.payment_success);
        request.setTitle("支付成功");
        request.setContent(String.format("您的挂号费用已支付成功！\n科室：%s\n医生：%s\n就诊时间：%s %s\n费用：¥%.2f\n请按时就诊，祝您早日康复！", 
                departmentName, doctorName, scheduleDate, slotName, fee));
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 发送候补可用通知
     */
    @Transactional
    public NotificationResponse sendWaitlistAvailableNotification(Integer patientId, Integer waitlistId,
                                                                  String departmentName, String doctorName,
                                                                  String scheduleDate, String slotName) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.waitlist_available);
        request.setTitle("候补号源可用");
        request.setContent(String.format("您候补的号源现在可以预约了！\n科室：%s\n医生：%s\n就诊时间：%s %s\n请在15分钟内完成支付，超时将自动取消。", 
                departmentName, doctorName, scheduleDate, slotName));
        request.setRelatedEntity("waitlist");
        request.setRelatedId(waitlistId);
        request.setPriority(NotificationPriority.urgent);

        return createNotification(request);
    }

    /**
     * 发送取消预约通知
     */
    @Transactional
    public NotificationResponse sendCancellationNotification(Integer patientId, Integer appointmentId,
                                                             String departmentName, String doctorName,
                                                             String scheduleDate, String slotName) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.cancellation);
        request.setTitle("预约已取消");
        request.setContent(String.format("您的预约已取消\n科室：%s\n医生：%s\n就诊时间：%s %s\n如有疑问，请联系医院。", 
                departmentName, doctorName, scheduleDate, slotName));
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.normal);

        return createNotification(request);
    }

    /**
     * 发送请假批准通知
     */
    @Transactional
    public NotificationResponse sendLeaveApprovedNotification(Integer doctorId, Integer leaveRequestId,
                                                              String startTime, String endTime,
                                                              String approverComments) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(doctorId);
        request.setUserType(UserType.doctor);
        request.setType(NotificationType.leave_approved);
        request.setTitle("请假申请已批准");
        
        String content = String.format("您的请假申请已批准！\n请假时间：%s 至 %s", startTime, endTime);
        if (approverComments != null && !approverComments.trim().isEmpty()) {
            content += "\n审批意见：" + approverComments;
        }
        
        request.setContent(content);
        request.setRelatedEntity("leave_request");
        request.setRelatedId(leaveRequestId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 发送请假拒绝通知
     */
    @Transactional
    public NotificationResponse sendLeaveRejectedNotification(Integer doctorId, Integer leaveRequestId,
                                                              String startTime, String endTime,
                                                              String approverComments) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(doctorId);
        request.setUserType(UserType.doctor);
        request.setType(NotificationType.leave_rejected);
        request.setTitle("请假申请已拒绝");
        
        String content = String.format("您的请假申请已被拒绝\n请假时间：%s 至 %s", startTime, endTime);
        if (approverComments != null && !approverComments.trim().isEmpty()) {
            content += "\n拒绝理由：" + approverComments;
        }
        
        request.setContent(content);
        request.setRelatedEntity("leave_request");
        request.setRelatedId(leaveRequestId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 发送医生替换通知（给患者）
     */
    @Transactional
    public NotificationResponse sendDoctorChangeNotification(Integer patientId, Integer appointmentId,
                                                             String originalDoctorName, String newDoctorName,
                                                             String departmentName, String scheduleDate, 
                                                             String slotName, String locationName) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.schedule_change);
        request.setTitle("就诊医生变更通知");
        
        String content = String.format("由于原医生 %s 请假，您的预约医生已变更为 %s\n" +
                "科室：%s\n" +
                "就诊时间：%s %s\n" +
                "就诊地点：%s\n" +
                "如有疑问，请联系医院。给您带来不便，敬请谅解！", 
                originalDoctorName, newDoctorName, departmentName, scheduleDate, slotName, locationName);
        
        request.setContent(content);
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 发送排班取消通知（给患者）
     */
    @Transactional
    public NotificationResponse sendScheduleCancelledNotification(Integer patientId, Integer appointmentId,
                                                                  String doctorName, String departmentName,
                                                                  String scheduleDate, String slotName) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.schedule_cancelled);
        request.setTitle("预约已取消");
        
        String content = String.format("由于医生 %s 请假且无法安排替班，您的预约已被取消\n" +
                "科室：%s\n" +
                "原就诊时间：%s %s\n" +
                "您的挂号费用将原路退回。\n" +
                "如需重新预约，请联系医院。给您带来不便，敬请谅解！", 
                doctorName, departmentName, scheduleDate, slotName);
        
        request.setContent(content);
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.urgent);

        return createNotification(request);
    }

    /**
     * 发送医生降级退款通知（给患者）
     */
    @Transactional
    public NotificationResponse sendDoctorDowngradeRefundNotification(Integer patientId, Integer appointmentId,
                                                                       String originalDoctorName, String originalTitle,
                                                                       String newDoctorName, String newTitle,
                                                                       String departmentName, String scheduleDate,
                                                                       String slotName, Double refundAmount) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setUserId(patientId);
        request.setUserType(UserType.patient);
        request.setType(NotificationType.schedule_change);
        request.setTitle("医生变更及退款通知");
        
        String content = String.format("由于原医生 %s（%s）请假，您的预约医生已变更为 %s（%s）\n" +
                "科室：%s\n" +
                "就诊时间：%s %s\n" +
                "由于替班医生职称降级，差额挂号费 %.2f 元将原路退回。\n" +
                "如有疑问，请联系医院。给您带来不便，敬请谅解！", 
                originalDoctorName, originalTitle, newDoctorName, newTitle,
                departmentName, scheduleDate, slotName, refundAmount);
        
        request.setContent(content);
        request.setRelatedEntity("appointment");
        request.setRelatedId(appointmentId);
        request.setPriority(NotificationPriority.high);

        return createNotification(request);
    }

    /**
     * 获取用户通知列表
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Integer userId, UserType userType) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndUserTypeOrderBySentAtDesc(userId, userType);
        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取未读通知列表
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Integer userId, UserType userType) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndUserTypeAndStatusOrderBySentAtDesc(userId, userType, NotificationStatus.unread);
        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取未读通知数量
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId, UserType userType) {
        return notificationRepository.countByUserIdAndUserTypeAndStatus(
                userId, userType, NotificationStatus.unread);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id " + notificationId));
        
        notification.setStatus(NotificationStatus.read);
        notification.setReadAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        return convertToResponse(saved);
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public void markAllAsRead(Integer userId, UserType userType) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndUserTypeAndStatusOrderBySentAtDesc(userId, userType, NotificationStatus.unread);
        
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            notification.setStatus(NotificationStatus.read);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(notifications);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id " + notificationId));
        
        notification.setStatus(NotificationStatus.deleted);
        notificationRepository.save(notification);
    }

    /**
     * 转换为响应DTO
     */
    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        BeanUtils.copyProperties(notification, response);
        return response;
    }
}

