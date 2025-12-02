package com.example.springboot.util;

import com.example.springboot.dto.ConflictType;
import com.example.springboot.dto.ScheduleConflict;
import com.example.springboot.entity.Schedule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 冲突检测器
 * 负责检测排班之间的各种冲突
 */
@Component
public class ConflictDetector {
    
    /**
     * 检测排班冲突
     * 
     * @param newSchedules 新生成的排班列表
     * @param existingSchedules 已存在的排班列表
     * @return 冲突列表
     */
    public List<ScheduleConflict> detectConflicts(
            List<Schedule> newSchedules,
            List<Schedule> existingSchedules) {
        
        List<ScheduleConflict> conflicts = new ArrayList<>();
        
        // 合并所有排班进行检查
        List<Schedule> allSchedules = new ArrayList<>();
        allSchedules.addAll(newSchedules);
        allSchedules.addAll(existingSchedules);
        
        // 检查时空冲突（同一医生在同一时间有多个排班）
        for (int i = 0; i < allSchedules.size(); i++) {
            for (int j = i + 1; j < allSchedules.size(); j++) {
                Schedule s1 = allSchedules.get(i);
                Schedule s2 = allSchedules.get(j);
                
                if (isTimeConflict(s1, s2)) {
                    conflicts.add(buildConflict(
                        ConflictType.TIME_CONFLICT,
                        "医生在同一时间有多个排班",
                        s1, s2
                    ));
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * 判断两个排班是否存在时间冲突
     * 
     * @param s1 排班1
     * @param s2 排班2
     * @return true表示有冲突
     */
    private boolean isTimeConflict(Schedule s1, Schedule s2) {
        // 必须是同一个医生
        if (!s1.getDoctor().getDoctorId().equals(s2.getDoctor().getDoctorId())) {
            return false;
        }
        
        // 必须是同一天
        if (!s1.getScheduleDate().equals(s2.getScheduleDate())) {
            return false;
        }
        
        // 必须是同一个时间段
        if (!s1.getSlot().getSlotId().equals(s2.getSlot().getSlotId())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 构建冲突对象
     * 
     * @param type 冲突类型
     * @param description 描述
     * @param s1 排班1
     * @param s2 排班2
     * @return 冲突对象
     */
    private ScheduleConflict buildConflict(
            ConflictType type, 
            String description,
            Schedule s1, 
            Schedule s2) {
        
        ScheduleConflict conflict = new ScheduleConflict();
        conflict.setType(type);
        conflict.setDescription(description);
        conflict.setDoctorName(s1.getDoctor().getFullName());
        conflict.setConflictDate(s1.getScheduleDate());
        conflict.setTimeSlot(s1.getSlot().getSlotName());
        
        // 设置涉及的排班ID
        List<Integer> scheduleIds = new ArrayList<>();
        if (s1.getScheduleId() != null) {
            scheduleIds.add(s1.getScheduleId());
        }
        if (s2.getScheduleId() != null) {
            scheduleIds.add(s2.getScheduleId());
        }
        conflict.setScheduleIds(scheduleIds);
        
        // 设置建议
        switch (type) {
            case TIME_CONFLICT:
                conflict.setSuggestion("请检查并删除重复排班");
                break;
            case LEAVE_CONFLICT:
                conflict.setSuggestion("请调整排班日期或取消请假");
                break;
            case DUPLICATE_SCHEDULE:
                conflict.setSuggestion("该排班已存在，无需重复创建");
                break;
            case LOCATION_CONFLICT:
                conflict.setSuggestion("请为该排班分配其他诊室");
                break;
            case WORKLOAD_EXCEEDED:
                conflict.setSuggestion("该医生工作量已达上限，建议分配给其他医生");
                break;
            default:
                conflict.setSuggestion("请检查排班信息");
        }
        
        return conflict;
    }
    
    /**
     * 检测单个排班列表内部的冲突
     * 
     * @param schedules 排班列表
     * @return 冲突列表
     */
    public List<ScheduleConflict> detectInternalConflicts(List<Schedule> schedules) {
        return detectConflicts(schedules, new ArrayList<>());
    }
}


