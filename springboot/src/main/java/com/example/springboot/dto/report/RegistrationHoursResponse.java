package com.example.springboot.dto.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RegistrationHoursResponse {
    private Integer doctorId;
    private String doctorName;
    private Integer departmentId;
    private String departmentName;
    private Integer parentDepartmentId;
    private String parentDepartmentName;
    private LocalDate workDate;
    private Integer segmentIndex;
    private String segmentLabel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstCallTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastEndTime;
    private BigDecimal rawHours;
    private BigDecimal regHours;
    private Integer visitCount;
    private boolean nightFlag;
    private String locations;
    private BigDecimal departmentCoefficient;
    private BigDecimal performancePoints;
}

