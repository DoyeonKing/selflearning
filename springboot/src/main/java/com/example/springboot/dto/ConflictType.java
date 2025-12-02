package com.example.springboot.dto;

/**
 * 排班冲突类型枚举
 */
public enum ConflictType {
    /**
     * 时间冲突 - 同一医生在同一时间有多个排班
     */
    TIME_CONFLICT,
    
    /**
     * 请假冲突 - 排班时间与请假时间冲突
     */
    LEAVE_CONFLICT,
    
    /**
     * 重复排班 - 已存在相同的排班记录
     */
    DUPLICATE_SCHEDULE,
    
    /**
     * 诊室冲突 - 诊室资源不可用
     */
    LOCATION_CONFLICT,
    
    /**
     * 工作量超限 - 超过医生工作量上限
     */
    WORKLOAD_EXCEEDED
}


