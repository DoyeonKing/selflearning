package com.example.springboot.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 未分配时间段信息
 */
@Data
public class UnassignedSlot {
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 时间段ID
     */
    private Integer slotId;
    
    /**
     * 时间段名称
     */
    private String slotName;
    
    /**
     * 未分配原因
     */
    private String reason;
    
    /**
     * 可能的解决方案
     */
    private List<String> suggestions;
}


