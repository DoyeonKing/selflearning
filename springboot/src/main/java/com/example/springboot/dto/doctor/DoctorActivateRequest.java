package com.example.springboot.dto.doctor;

/**
 * DTO for the second step of doctor account activation (identity verification and setting new password).
 */
public class DoctorActivateRequest {
    private String identifier; // Work ID (工号), carried over from step 1
    private String idCardEnding; // Last 6 digits of ID card (身份证后6位) for identity verification
    private String newPassword; // New password
    private String confirmPassword; // Confirmation of the new password

    // --- Getters ---

    public String getIdentifier() {
        return identifier;
    }

    public String getIdCardEnding() {
        return idCardEnding;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    // --- Setters ---

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setIdCardEnding(String idCardEnding) {
        this.idCardEnding = idCardEnding;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
