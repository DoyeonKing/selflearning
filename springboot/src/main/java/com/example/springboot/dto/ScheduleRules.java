package com.example.springboot.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 排班规则配置
 */
@Data
public class ScheduleRules {
    /**
     * 每个时段最少医生数（软约束）
     */
    private Integer minDoctorsPerSlot = 1;
    
    /**
     * 每个时段最多医生数（软约束）
     */
    private Integer maxDoctorsPerSlot = 3;
    
    /**
     * 每位医生最大班次数（整个周期）
     */
    private Integer maxShiftsPerDoctor = 999;
    
    /**
     * 默认号源数量
     */
    private Integer defaultTotalSlots = 20;
    
    /**
     * 默认挂号费
     */
    private BigDecimal defaultFee = new BigDecimal("5.00");
    
    /**
     * 连续工作天数限制
     */
    private Integer consecutiveWorkDaysLimit = 6;
    
    /**
     * 最少休息天数
     */
    private Integer minRestDays = 1;
    
    /**
     * 是否启用工作量均衡
     */
    private Boolean balanceWorkload = true;
    
    /**
     * 是否考虑医生时间偏好（预留，后续实现）
     */
    private Boolean considerPreferences = false;
    
    /**
     * 严格模式：是否严格遵守连续工作天数限制
     * true: 绝不违反限制，即使某些时段无人排班
     * false: 优先保证覆盖，必要时放宽限制
     */
    private Boolean strictMode = false;
}

