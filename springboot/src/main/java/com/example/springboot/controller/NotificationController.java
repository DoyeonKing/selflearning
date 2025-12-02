package com.example.springboot.controller;

import com.example.springboot.dto.notification.NotificationResponse;
import com.example.springboot.entity.enums.UserType;
import com.example.springboot.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 获取用户通知列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @PathVariable Integer userId,
            @RequestParam UserType userType) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId, userType);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取未读通知列表
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @PathVariable Integer userId,
            @RequestParam UserType userType) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId, userType);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable Integer userId,
            @RequestParam UserType userType) {
        long count = notificationService.getUnreadCount(userId, userType);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("userType", userType);
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        NotificationResponse notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @PathVariable Integer userId,
            @RequestParam UserType userType) {
        notificationService.markAllAsRead(userId, userType);
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted");
        return ResponseEntity.ok(response);
    }
}

