package com.example.springboot.dto.auto;

import com.example.springboot.dto.schedule.ScheduleResponse;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AutoScheduleResponse {
    private Boolean success;
    private String message;
    private List<ScheduleResponse> schedules;
    private ScheduleStatistics statistics;
    private List<ScheduleConflict> conflicts;
    private List<UnassignedSlot> unassignedSlots;
    private Map<Integer, DoctorWorkload> workloadDistribution;
    private List<String> warnings;
}


