package com.example.springboot.entity.enums;

/**
 * 候补状态枚举
 * 必须与数据库中的 ENUM 定义完全一致
 */
public enum WaitlistStatus {
    waiting,    // 等待中 - 用户加入候补队列，等待号源
    notified,   // 已通知 - 有号源可用，已通知患者
    expired,    // 已过期 - 超时未预约或被拒绝
    booked      // 已预约 - 候补成功，已转为正式预约
}