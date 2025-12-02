package com.example.springboot.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DoctorHoursResponse {
    private Integer doctorId;
    private String doctorName;
    private LocalDate date; // 当按 doctor 分组时可为空
    private Integer sessions;
    private BigDecimal hours; // 保留两位小数
    private String locations; // 去重后的地点用“、”连接
}


