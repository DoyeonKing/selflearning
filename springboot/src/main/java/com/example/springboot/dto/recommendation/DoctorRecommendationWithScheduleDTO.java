package com.example.springboot.dto.recommendation;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 带排班信息的医生推荐响应DTO
 */
@Data
public class DoctorRecommendationWithScheduleDTO {
    private Integer doctorId;
    private String doctorName;
    private Integer departmentId;
    private String departmentName;
    private String title;
    private Integer titleLevel;
    private String specialty;
    private String photoUrl;
    private Double score;
    private String reason;
    
    // 排班信息列表
    private List<ScheduleInfo> availableSchedules;
    
    @Data
    public static class ScheduleInfo {
        private Integer scheduleId;
        private LocalDate scheduleDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer totalSlots;
        private Integer bookedSlots;
        private Integer availableSlots; // 剩余号源
        private String locationName; // 诊室名称
    }
}