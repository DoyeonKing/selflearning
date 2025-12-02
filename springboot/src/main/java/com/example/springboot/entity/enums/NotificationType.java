package com.example.springboot.entity.enums;

public enum NotificationType {
    payment_success,        // 支付成功
    appointment_reminder,    // 预约提醒
    appointment_success,     // 预约成功（加号等）
    cancellation,           // 取消通知
    waitlist_available,      // 候补可用
    schedule_change,         // 排班变更
    schedule_cancelled,      // 排班取消
    system_notice,          // 系统通知
    leave_approved,         // 请假批准
    leave_rejected          // 请假拒绝
}

