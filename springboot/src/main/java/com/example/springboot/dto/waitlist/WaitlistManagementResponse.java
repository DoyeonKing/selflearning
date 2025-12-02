// src/main/java/com/example/springboot/dto/waitlist/WaitlistManagementResponse.java
package com.example.springboot.dto.waitlist;

import com.example.springboot.dto.patient.PatientSimpleResponse;
import com.example.springboot.entity.enums.WaitlistStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaitlistManagementResponse {
    private Integer waitlistId;
    private Long patientId;
    private Integer scheduleId;
    private WaitlistStatus status;
    private LocalDateTime createdAt;
    private PatientSimpleResponse patient;
    private Integer position;
}