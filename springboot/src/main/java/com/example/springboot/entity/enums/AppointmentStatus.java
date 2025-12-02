package com.example.springboot.entity.enums;

public enum AppointmentStatus {
    PENDING_PAYMENT,  // 待支付
    scheduled,        // 已预约（未签到）
    CHECKED_IN,       // 已签到（未就诊）
    completed,        // 已完成
    cancelled,        // 已取消
    NO_SHOW          // 爽约
}