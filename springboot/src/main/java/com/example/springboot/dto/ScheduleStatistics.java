package com.example.springboot.dto;

import lombok.Data;

/**
 * 排班统计信息
 */
@Data
public class ScheduleStatistics {
    /**
     * 总排班数
     */
    private Integer totalSchedules;
    
    /**
     * 覆盖天数
     */
    private Integer coveredDays;
    
    /**
     * 参与医生数
     */
    private Integer doctorsInvolved;
    
    /**
     * 平均工作量（每位医生平均班次）
     */
    private Double averageWorkload;
    
    /**
     * 最大工作量
     */
    private Integer maxWorkload;
    
    /**
     * 最小工作量
     */
    private Integer minWorkload;
    
    /**
     * 排班覆盖率（实际分配数/总需求数）
     */
    private Double coverageRate;
    
    /**
     * 冲突数量
     */
    private Integer conflictCount;
    
    /**
     * 生成耗时（毫秒）
     */
    private Long executionTime;
}


