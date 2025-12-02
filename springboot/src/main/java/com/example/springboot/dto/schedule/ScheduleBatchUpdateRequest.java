package com.example.springboot.dto.schedule;

import lombok.Data;
import java.util.List;

/**
 * 排班批量更新请求
 */
@Data
public class ScheduleBatchUpdateRequest {
    private List<ScheduleUpdateItem> updates;
    
    @Data
    public static class ScheduleUpdateItem {
        private Integer scheduleId;
        private Integer totalSlots;
        private java.math.BigDecimal fee;
    }
}
