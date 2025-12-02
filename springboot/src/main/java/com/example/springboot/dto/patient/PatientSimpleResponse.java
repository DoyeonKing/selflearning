// src/main/java/com/example/springboot/dto/patient/PatientSimpleResponse.java
package com.example.springboot.dto.patient;

import lombok.Data;

@Data
public class PatientSimpleResponse {
    private Long patientId;
    private String name;
    private String phone;
}