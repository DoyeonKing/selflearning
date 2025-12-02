package com.example.springboot.entity.enums;

/**
 * 预约类型枚举
 */
public enum AppointmentType {
    APPOINTMENT,        // 预约挂号
    WALK_IN,            // 现场挂号
    SAME_DAY_FOLLOW_UP, // 当日复诊号
    NEXT_DAY_FOLLOW_UP, // 隔日复诊号（第二天、第三天）
    ADD_ON,             // 加号
    PRIORITY            // 优先号（军人、老年人等）
}

