package com.example.springboot.dto.auto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class DoctorWorkload {
    private Integer doctorId;
    private String doctorName;
    private String title;
    private Integer totalShifts;
    private Integer workDays;
    private Integer maxConsecutiveDays;
    private Map<LocalDate, List<String>> scheduleDetails;
}


