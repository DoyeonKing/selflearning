package com.example.springboot.dto.auto;

import lombok.Data;

@Data
public class ScheduleStatistics {
    private Integer totalSchedules;
    private Integer coveredDays;
    private Integer doctorsInvolved;
    private Double averageWorkload;
    private Integer maxWorkload;
    private Integer minWorkload;
    private Double coverageRate;
    private Integer conflictCount;
    private Long executionTime;
}


