package com.example.springboot.entity.enums;

/**
 * 加号申请状态枚举
 */
public enum SlotApplicationStatus {
    PENDING,    // 待审批
    APPROVED,   // 已通过
    REJECTED,   // 已拒绝
    CANCELLED   // 已取消
}
