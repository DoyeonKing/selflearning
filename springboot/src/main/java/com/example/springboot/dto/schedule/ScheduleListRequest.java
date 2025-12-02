package com.example.springboot.dto.schedule;

import lombok.Data;
import java.time.LocalDate;

/**
 * 排班列表查询请求
 */
@Data
public class ScheduleListRequest {
    private Integer departmentId;
    private LocalDate startDate;
    private LocalDate endDate;
}
