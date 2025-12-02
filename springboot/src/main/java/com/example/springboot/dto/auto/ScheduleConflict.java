package com.example.springboot.dto.auto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleConflict {
    private ConflictType type;
    private String description;
    private List<Integer> scheduleIds;
    private String doctorName;
    private LocalDate conflictDate;
    private String timeSlot;
    private String suggestion;

    public enum ConflictType {
        TIME_CONFLICT,
        LEAVE_CONFLICT,
        DUPLICATE_SCHEDULE,
        LOCATION_CONFLICT,
        WORKLOAD_EXCEEDED
    }
}


