package com.example.springboot.dto.symptom;

import lombok.Data;

@Data
public class SymptomMappingRequest {
    private String symptomKeywords;
    private Integer departmentId;
    private Integer priority;
}

