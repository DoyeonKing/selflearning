package com.example.springboot.dto.notification;

import com.example.springboot.entity.enums.NotificationPriority;
import com.example.springboot.entity.enums.NotificationStatus;
import com.example.springboot.entity.enums.NotificationType;
import com.example.springboot.entity.enums.UserType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long notificationId;
    private Integer userId;
    private UserType userType;
    private NotificationType type;
    private String title;
    private String content;
    private String relatedEntity;
    private Integer relatedId;
    private NotificationStatus status;
    private NotificationPriority priority;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}

