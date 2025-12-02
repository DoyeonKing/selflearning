package com.example.springboot.dto.notification;

import com.example.springboot.entity.enums.NotificationPriority;
import com.example.springboot.entity.enums.NotificationType;
import com.example.springboot.entity.enums.UserType;
import lombok.Data;

@Data
public class NotificationCreateRequest {
    private Integer userId;
    private UserType userType;
    private NotificationType type;
    private String title;
    private String content;
    private String relatedEntity;
    private Integer relatedId;
    private NotificationPriority priority;
}

