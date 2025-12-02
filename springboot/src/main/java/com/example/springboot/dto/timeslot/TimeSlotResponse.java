package com.example.springboot.dto.timeslot;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlotResponse {
    private Integer slotId;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
}
