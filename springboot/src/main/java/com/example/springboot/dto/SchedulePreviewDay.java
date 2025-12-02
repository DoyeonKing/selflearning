package com.example.springboot.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班预览 - 单日数据
 */
@Data
public class SchedulePreviewDay {
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 星期（1-7）
     */
    private Integer dayOfWeek;
    
    /**
     * 星期显示（周一、周二...）
     */
    private String dayOfWeekName;
    
    /**
     * 各时段的排班信息
     */
    private List<SchedulePreviewSlot> slots;
}


