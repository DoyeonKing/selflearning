package com.example.springboot.dto.schedule.auto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScheduleRules {
    private Integer minDoctorsPerSlot = 1;
    private Integer maxDoctorsPerSlot = 3;
    private Integer maxShiftsPerDoctor = 999;
    private Integer defaultTotalSlots = 20;
    private BigDecimal defaultFee = new BigDecimal("5.00");
    private Integer consecutiveWorkDaysLimit = 6;
    private Integer minRestDays = 1;
    private Boolean balanceWorkload = true;
    private Boolean considerPreferences = false;
}


