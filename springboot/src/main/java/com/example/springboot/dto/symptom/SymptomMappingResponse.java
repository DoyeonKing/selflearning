package com.example.springboot.dto.symptom;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SymptomMappingResponse {
    private Integer mappingId;
    private String symptomKeywords;
    private Integer departmentId;
    private String departmentName;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

