package com.example.springboot.util;

import com.example.springboot.dto.ScheduleRules;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.LeaveRequest;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.TimeSlot;
import com.example.springboot.entity.enums.DoctorStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 排班约束验证器
 * 负责检查所有硬约束条件
 */
@Component
public class ScheduleConstraintValidator {
    
    /**
     * 检查所有硬约束
     * 
     * @param doctor 医生
     * @param date 日期
     * @param slot 时间段
     * @param leaveMap 请假记录映射
     * @param existingSchedules 已有排班列表
     * @param workloadMap 当前工作量映射
     * @param rules 排班规则
     * @return true表示通过所有硬约束，false表示违反约束
     */
    public boolean checkHardConstraints(
            Doctor doctor, 
            LocalDate date, 
            TimeSlot slot,
            Map<Integer, List<LeaveRequest>> leaveMap,
            List<Schedule> existingSchedules,
            Map<Integer, Integer> workloadMap,
            ScheduleRules rules) {
        
        // HC-1: 时空唯一性检查
        if (hasTimeConflict(doctor, date, slot, existingSchedules)) {
            return false;
        }
        
        // HC-2: 请假约束检查
        if (isOnLeave(doctor, date, leaveMap)) {
            return false;
        }
        
        // HC-3: 医生状态检查
        if (doctor.getStatus() != DoctorStatus.active) {
            return false;
        }
        
        // HC-4: 工作量上限检查
        Integer currentWorkload = workloadMap.getOrDefault(doctor.getDoctorId(), 0);
        if (currentWorkload >= rules.getMaxShiftsPerDoctor()) {
            return false;
        }
        
        // HC-5: 连续工作天数限制检查
        if (rules.getConsecutiveWorkDaysLimit() != null && rules.getConsecutiveWorkDaysLimit() > 0) {
            int consecutiveDays = calculateConsecutiveWorkDays(doctor, date, existingSchedules);
            if (consecutiveDays >= rules.getConsecutiveWorkDaysLimit()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查时间冲突
     * 同一医生不能在同一时间有多个排班
     */
    private boolean hasTimeConflict(
            Doctor doctor, 
            LocalDate date, 
            TimeSlot slot,
            List<Schedule> schedules) {
        
        return schedules.stream()
            .anyMatch(s -> 
                s.getDoctor().getDoctorId().equals(doctor.getDoctorId())
                && s.getScheduleDate().equals(date)
                && s.getSlot().getSlotId().equals(slot.getSlotId())
            );
    }
    
    /**
     * 检查医生是否在请假
     */
    private boolean isOnLeave(
            Doctor doctor, 
            LocalDate date,
            Map<Integer, List<LeaveRequest>> leaveMap) {
        
        List<LeaveRequest> leaves = leaveMap.get(doctor.getDoctorId());
        if (leaves == null || leaves.isEmpty()) {
            return false;
        }
        
        LocalDateTime checkDateTime = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        return leaves.stream()
            .filter(lr -> "approved".equals(lr.getStatus()))
            .anyMatch(lr -> {
                // 检查日期是否在请假时间范围内
                LocalDateTime leaveStart = lr.getStartTime();
                LocalDateTime leaveEnd = lr.getEndTime();
                
                // 如果请假开始时间 <= 当天结束 且 请假结束时间 >= 当天开始，则有冲突
                return !leaveStart.isAfter(endOfDay) && !leaveEnd.isBefore(checkDateTime);
            });
    }
    
    /**
     * 检查硬约束（放宽版本：跳过连续工作天数限制）
     * 用于紧急情况，确保每天至少有医生可用
     */
    public boolean checkHardConstraintsRelaxed(
            Doctor doctor, 
            LocalDate date, 
            TimeSlot slot,
            Map<Integer, List<LeaveRequest>> leaveMap,
            List<Schedule> existingSchedules,
            Map<Integer, Integer> workloadMap,
            ScheduleRules rules) {
        
        // HC-1: 时空唯一性检查
        if (hasTimeConflict(doctor, date, slot, existingSchedules)) {
            return false;
        }
        
        // HC-2: 请假约束检查
        if (isOnLeave(doctor, date, leaveMap)) {
            return false;
        }
        
        // HC-3: 医生状态检查
        if (doctor.getStatus() != DoctorStatus.active) {
            return false;
        }
        
        // HC-4: 工作量上限检查
        Integer currentWorkload = workloadMap.getOrDefault(doctor.getDoctorId(), 0);
        if (currentWorkload >= rules.getMaxShiftsPerDoctor()) {
            return false;
        }
        
        // HC-5: 跳过连续工作天数限制检查（放宽版本）
        
        return true;
    }
    
    /**
     * 获取医生在指定日期前的连续工作天数（公共方法）
     * 
     * @param doctor 医生
     * @param date 当前要分配的日期
     * @param existingSchedules 已有排班列表
     * @return 连续工作天数
     */
    public int getConsecutiveWorkDays(
            Doctor doctor, 
            LocalDate date, 
            List<Schedule> existingSchedules) {
        return calculateConsecutiveWorkDays(doctor, date, existingSchedules);
    }
    
    /**
     * 计算医生在指定日期前的连续工作天数
     * 
     * @param doctor 医生
     * @param date 当前要分配的日期
     * @param existingSchedules 已有排班列表
     * @return 连续工作天数
     */
    private int calculateConsecutiveWorkDays(
            Doctor doctor, 
            LocalDate date, 
            List<Schedule> existingSchedules) {
        
        int consecutiveDays = 0;
        LocalDate checkDate = date.minusDays(1); // 从前一天开始往回查
        
        // 往前查找连续工作日
        while (true) {
            final LocalDate currentCheckDate = checkDate;
            
            // 检查该医生在checkDate这天是否有排班
            boolean hasSchedule = existingSchedules.stream()
                .anyMatch(s -> 
                    s.getDoctor().getDoctorId().equals(doctor.getDoctorId())
                    && s.getScheduleDate().equals(currentCheckDate)
                );
            
            if (hasSchedule) {
                consecutiveDays++;
                checkDate = checkDate.minusDays(1);
            } else {
                // 遇到没有排班的日期，中断连续性
                break;
            }
            
            // 防止无限循环，最多查30天
            if (consecutiveDays > 30) {
                break;
            }
        }
        
        return consecutiveDays;
    }
    
    /**
     * 检查软约束违反情况
     * 
     * @param schedules 排班列表
     * @param rules 排班规则
     * @return 警告信息列表
     */
    public List<String> checkSoftConstraints(List<Schedule> schedules, ScheduleRules rules) {
        // TODO: 实现软约束检查，如连续工作天数等
        // 这部分可以在后续版本实现
        return List.of();
    }
}

