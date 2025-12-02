package com.example.springboot.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医生工作量信息
 */
@Data
public class DoctorWorkload {
    /**
     * 医生ID
     */
    private Integer doctorId;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * 职称
     */
    private String title;
    
    /**
     * 总班次数
     */
    private Integer totalShifts;
    
    /**
     * 工作天数
     */
    private Integer workDays;
    
    /**
     * 最长连续工作天数
     */
    private Integer maxConsecutiveDays;
    
    /**
     * 排班详情（日期->时段列表）
     */
    private Map<LocalDate, List<String>> scheduleDetails;
}


