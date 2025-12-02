package com.example.springboot.dto.schedule;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 排班更新请求
 */
@Data
public class ScheduleUpdateRequest {
    private Integer totalSlots;
    private BigDecimal fee;
}
