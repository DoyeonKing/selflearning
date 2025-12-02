package com.example.springboot.dto;

import lombok.Data;

/**
 * 排班预览 - 时段数据
 */
@Data
public class SchedulePreviewSlot {
    /**
     * 时段ID
     */
    private Integer slotId;
    
    /**
     * 时段名称
     */
    private String slotName;
    
    /**
     * 时间范围（如：08:00-12:00）
     */
    private String timeRange;
    
    /**
     * 医生ID
     */
    private Integer doctorId;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * 诊室名称
     */
    private String locationName;
    
    /**
     * 是否使用了放宽限制
     */
    private Boolean isRelaxed;
    
    /**
     * 该医生当前连续工作天数
     */
    private Integer consecutiveDays;
}


