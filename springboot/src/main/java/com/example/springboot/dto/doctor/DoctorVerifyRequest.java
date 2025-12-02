package com.example.springboot.dto.doctor;

/**
 * DTO for the first step of doctor account activation (verification of work ID and initial password).
 */
public class DoctorVerifyRequest {
    private String identifier; // Work ID (工号)
    private String initialPassword; // Initial password (初始密码)

    // --- Getters ---

    public String getIdentifier() {
        return identifier;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    // --- Setters ---

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
