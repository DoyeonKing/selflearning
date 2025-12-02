package com.example.springboot.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班冲突信息
 */
@Data
public class ScheduleConflict {
    /**
     * 冲突类型
     */
    private ConflictType type;
    
    /**
     * 冲突描述
     */
    private String description;
    
    /**
     * 涉及的排班ID列表
     */
    private List<Integer> scheduleIds;
    
    /**
     * 涉及的医生姓名
     */
    private String doctorName;
    
    /**
     * 冲突日期
     */
    private LocalDate conflictDate;
    
    /**
     * 冲突时间段
     */
    private String timeSlot;
    
    /**
     * 建议解决方案
     */
    private String suggestion;
}


