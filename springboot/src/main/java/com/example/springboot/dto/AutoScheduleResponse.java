package com.example.springboot.dto;

import com.example.springboot.dto.schedule.ScheduleResponse;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 自动排班响应结果
 */
@Data
public class AutoScheduleResponse {
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 提示消息
     */
    private String message;
    
    /**
     * 生成的排班列表
     */
    private List<ScheduleResponse> schedules;
    
    /**
     * 统计信息
     */
    private ScheduleStatistics statistics;
    
    /**
     * 冲突列表
     */
    private List<ScheduleConflict> conflicts;
    
    /**
     * 未分配的时间段
     */
    private List<UnassignedSlot> unassignedSlots;
    
    /**
     * 工作量分布（key: doctorId）
     */
    private Map<Integer, DoctorWorkload> workloadDistribution;
    
    /**
     * 软约束违反情况（警告信息）
     */
    private List<String> warnings;
    
    /**
     * 排班预览表（按日期-时段组织）
     */
    private List<SchedulePreviewDay> schedulePreview;
}

