package com.example.springboot.dto.auto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UnassignedSlot {
    private LocalDate date;
    private Integer slotId;
    private String slotName;
    private String reason;
    private List<String> suggestions;
}


