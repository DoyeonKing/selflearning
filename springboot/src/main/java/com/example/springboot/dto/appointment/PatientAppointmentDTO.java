package com.example.springboot.dto.appointment;

import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 患者预约信息DTO - 用于医生查看患者列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAppointmentDTO {
    
    // 预约信息
    private Integer appointmentId;
    private Integer appointmentNumber;  // 就诊序号
    private AppointmentStatus status;   // 预约状态
    private LocalDateTime checkInTime;  // 现场签到时间
    
    // 排班时间信息
    private LocalTime startTime;        // 时段开始时间
    private LocalTime endTime;          // 时段结束时间
    private String slotName;            // 时段名称
    
    // 患者基本信息
    private PatientBasicInfo patient;
    
    /**
     * 患者基本信息内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientBasicInfo {
        private Long patientId;
        private String fullName;        // 姓名
        private String patientType;     // 患者类型
        private String phoneNumber;     // 手机号
        
        // 患者档案信息
        private PatientProfileInfo patientProfile;
    }
    
    /**
     * 患者档案信息内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientProfileInfo {
        private String allergies;       // 过敏史
        private String medicalHistory;  // 基础病史
    }
    
    /**
     * 从Appointment实体转换为DTO
     */
    public static PatientAppointmentDTO fromEntity(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        
        // 构建患者档案信息
        PatientProfileInfo profileInfo = null;
        if (appointment.getPatient() != null && appointment.getPatient().getPatientProfile() != null) {
            profileInfo = PatientProfileInfo.builder()
                    .allergies(appointment.getPatient().getPatientProfile().getAllergies())
                    .medicalHistory(appointment.getPatient().getPatientProfile().getMedicalHistory())
                    .build();
        }
        
        // 构建患者基本信息
        PatientBasicInfo patientInfo = null;
        if (appointment.getPatient() != null) {
            patientInfo = PatientBasicInfo.builder()
                    .patientId(appointment.getPatient().getPatientId())
                    .fullName(appointment.getPatient().getFullName())
                    .patientType(appointment.getPatient().getPatientType() != null 
                            ? appointment.getPatient().getPatientType().name() 
                            : null)
                    .phoneNumber(appointment.getPatient().getPhoneNumber())
                    .patientProfile(profileInfo)
                    .build();
        }
        
        // 获取时间段信息
        LocalTime startTime = null;
        LocalTime endTime = null;
        String slotName = null;
        if (appointment.getSchedule() != null && appointment.getSchedule().getSlot() != null) {
            startTime = appointment.getSchedule().getSlot().getStartTime();
            endTime = appointment.getSchedule().getSlot().getEndTime();
            slotName = appointment.getSchedule().getSlot().getSlotName();
        }
        
        // 构建预约信息
        return PatientAppointmentDTO.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .status(appointment.getStatus())
                .checkInTime(appointment.getCheckInTime())
                .startTime(startTime)
                .endTime(endTime)
                .slotName(slotName)
                .patient(patientInfo)
                .build();
    }
}
