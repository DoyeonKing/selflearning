package com.example.springboot.util;

import com.example.springboot.dto.DoctorWorkload;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.Schedule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * 工作量计算器
 * 负责计算医生工作量分布和统计信息
 */
@Component
public class WorkloadCalculator {
    
    /**
     * 计算医生工作量分布
     * 
     * @param schedules 排班列表
     * @param doctors 医生列表
     * @return 工作量分布映射（key: doctorId）
     */
    public Map<Integer, DoctorWorkload> calculateWorkloadDistribution(
            List<Schedule> schedules,
            List<Doctor> doctors) {
        
        Map<Integer, DoctorWorkload> distribution = new HashMap<>();
        
        // 初始化所有医生的工作量信息
        for (Doctor doctor : doctors) {
            DoctorWorkload workload = new DoctorWorkload();
            workload.setDoctorId(doctor.getDoctorId());
            workload.setDoctorName(doctor.getFullName());
            workload.setTitle(doctor.getTitle());
            workload.setTotalShifts(0);
            workload.setWorkDays(0);
            workload.setMaxConsecutiveDays(0);
            workload.setScheduleDetails(new HashMap<>());
            distribution.put(doctor.getDoctorId(), workload);
        }
        
        // 统计工作日期
        Map<Integer, Set<LocalDate>> workDaysMap = new HashMap<>();
        
        // 遍历排班记录进行统计
        for (Schedule schedule : schedules) {
            Integer doctorId = schedule.getDoctor().getDoctorId();
            DoctorWorkload workload = distribution.get(doctorId);
            
            if (workload == null) {
                // 如果医生不在初始列表中，创建新的工作量记录
                workload = new DoctorWorkload();
                workload.setDoctorId(doctorId);
                workload.setDoctorName(schedule.getDoctor().getFullName());
                workload.setTitle(schedule.getDoctor().getTitle());
                workload.setTotalShifts(0);
                workload.setWorkDays(0);
                workload.setMaxConsecutiveDays(0);
                workload.setScheduleDetails(new HashMap<>());
                distribution.put(doctorId, workload);
            }
            
            // 增加班次数
            workload.setTotalShifts(workload.getTotalShifts() + 1);
            
            // 记录工作日期
            workDaysMap.computeIfAbsent(doctorId, k -> new HashSet<>())
                .add(schedule.getScheduleDate());
            
            // 记录排班详情
            workload.getScheduleDetails()
                .computeIfAbsent(schedule.getScheduleDate(), k -> new ArrayList<>())
                .add(schedule.getSlot().getSlotName());
        }
        
        // 计算工作天数和最长连续工作天数
        for (Map.Entry<Integer, Set<LocalDate>> entry : workDaysMap.entrySet()) {
            DoctorWorkload workload = distribution.get(entry.getKey());
            workload.setWorkDays(entry.getValue().size());
            workload.setMaxConsecutiveDays(
                calculateMaxConsecutiveDays(entry.getValue())
            );
        }
        
        return distribution;
    }
    
    /**
     * 计算最长连续工作天数
     * 
     * @param workDays 工作日期集合
     * @return 最长连续工作天数
     */
    private Integer calculateMaxConsecutiveDays(Set<LocalDate> workDays) {
        if (workDays.isEmpty()) {
            return 0;
        }
        
        // 将日期排序
        List<LocalDate> sortedDays = new ArrayList<>(workDays);
        Collections.sort(sortedDays);
        
        int maxConsecutive = 1;
        int currentConsecutive = 1;
        
        // 遍历计算连续天数
        for (int i = 1; i < sortedDays.size(); i++) {
            LocalDate prev = sortedDays.get(i - 1);
            LocalDate curr = sortedDays.get(i);
            
            // 如果是连续的日期
            if (curr.equals(prev.plusDays(1))) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 1;
            }
        }
        
        return maxConsecutive;
    }
    
    /**
     * 计算工作量统计摘要
     * 
     * @param workloadDistribution 工作量分布
     * @return 包含min, max, avg的映射
     */
    public Map<String, Object> calculateWorkloadSummary(
            Map<Integer, DoctorWorkload> workloadDistribution) {
        
        Map<String, Object> summary = new HashMap<>();
        
        if (workloadDistribution.isEmpty()) {
            summary.put("min", 0);
            summary.put("max", 0);
            summary.put("avg", 0.0);
            return summary;
        }
        
        int minWorkload = Integer.MAX_VALUE;
        int maxWorkload = 0;
        int totalShifts = 0;
        
        for (DoctorWorkload workload : workloadDistribution.values()) {
            int shifts = workload.getTotalShifts();
            minWorkload = Math.min(minWorkload, shifts);
            maxWorkload = Math.max(maxWorkload, shifts);
            totalShifts += shifts;
        }
        
        double avgWorkload = (double) totalShifts / workloadDistribution.size();
        
        summary.put("min", minWorkload == Integer.MAX_VALUE ? 0 : minWorkload);
        summary.put("max", maxWorkload);
        summary.put("avg", avgWorkload);
        
        return summary;
    }
}


