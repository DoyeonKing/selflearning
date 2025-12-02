package com.example.springboot.service.impl;

import com.example.springboot.dto.*;
import com.example.springboot.dto.schedule.ScheduleResponse;
import com.example.springboot.entity.*;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.entity.enums.ScheduleStatus;
import com.example.springboot.repository.*;
import com.example.springboot.service.AutoScheduleService;
import com.example.springboot.util.ConflictDetector;
import com.example.springboot.util.ScheduleConstraintValidator;
import com.example.springboot.util.WorkloadCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自动排班服务实现类
 * 实现CSP+贪心算法的自动排班逻辑
 */
@Slf4j
@Service
public class AutoScheduleServiceImpl implements AutoScheduleService {
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private ScheduleConstraintValidator constraintValidator;
    
    @Autowired
    private WorkloadCalculator workloadCalculator;
    
    @Autowired
    private ConflictDetector conflictDetector;
    
    @Override
    @Transactional
    public AutoScheduleResponse autoGenerateSchedule(AutoScheduleRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始自动排班: departmentId={}, startDate={}, endDate={}", 
                request.getDepartmentId(), request.getStartDate(), request.getEndDate());
            
            // 1. 参数校验
            validateRequest(request);
            
            // 确保rules不为空
            if (request.getRules() == null) {
                request.setRules(new ScheduleRules());
            }
            
            // 2. 数据准备
            List<Doctor> doctors = prepareActiveDoctors(request.getDepartmentId());
            List<TimeSlot> allTimeSlots = timeSlotRepository.findAll();
            
            // 🔥 新增：上午/下午各选择一个时段
            List<TimeSlot> timeSlots = selectRepresentativeSlots(allTimeSlots);
            
            List<Location> locations = prepareLocations(request.getDepartmentId());
            List<Schedule> existingSchedules = loadExistingSchedules(request);
            Map<Integer, List<LeaveRequest>> leaveMap = buildLeaveRequestMap(
                doctors, request.getStartDate(), request.getEndDate()
            );
            
            log.info("数据准备完成: doctors={}, timeSlots={} (从{}个筛选), locations={}", 
                doctors.size(), timeSlots.size(), allTimeSlots.size(), locations.size());
            
            // 3. 初始化
            List<Schedule> generatedSchedules = new ArrayList<>();
            Map<Integer, Integer> workloadMap = new HashMap<>();
            List<UnassignedSlot> unassignedSlots = new ArrayList<>();
            int relaxedCount = 0; // 统计使用放宽限制的次数
            
            // 🔥 新增：为每个医生预分配固定诊室（避免诊室冲突）
            Map<Integer, Location> doctorLocationMap = assignDoctorLocations(doctors, locations);
            
            // 4. 主算法循环 - CSP + 贪心策略
            LocalDate currentDate = request.getStartDate();
            while (!currentDate.isAfter(request.getEndDate())) {
                for (TimeSlot slot : timeSlots) {
                    // 🔥 获取该时段需要的医生人数
                    int minDoctors = Math.max(1, request.getRules().getMinDoctorsPerSlot());
                    int maxDoctors = Math.max(minDoctors, request.getRules().getMaxDoctorsPerSlot());
                    
                    // 🔥 记录该时段已分配的医生数量和已使用的诊室
                    Set<Integer> slotDoctorsAssigned = new HashSet<>();
                    Set<Integer> slotLocationsUsed = new HashSet<>();
                    
                    // 🔥 为该时段分配多个医生（minDoctors ~ maxDoctors）
                    int assignedCount = 0;
                    int attemptCount = 0;
                    int maxAttempts = doctors.size(); // 防止无限循环
                    
                    while (assignedCount < minDoctors && attemptCount < maxAttempts) {
                        attemptCount++;
                        
                        // 4.1 筛选可用医生（只排除该时段已排班的医生）
                        List<Doctor> availableDoctors = filterAvailableDoctors(
                            doctors, currentDate, slot, workloadMap, 
                            leaveMap, generatedSchedules, existingSchedules, request.getRules()
                        );
                        
                        // 🔥 排除该时段已排班的医生（允许医生在同一天的不同时段工作）
                        availableDoctors = availableDoctors.stream()
                            .filter(d -> !slotDoctorsAssigned.contains(d.getDoctorId()))
                            .collect(Collectors.toList());
                        
                        if (availableDoctors.isEmpty()) {
                            // 检查是否启用严格模式
                            if (request.getRules().getStrictMode() != null && request.getRules().getStrictMode()) {
                                // 严格模式：不放宽限制
                                log.warn("⚠️ 严格模式：{}的时段{}已分配{}个医生，需要{}个但无更多可用医生", 
                                    currentDate, slot.getSlotName(), assignedCount, minDoctors);
                                break;
                            } else {
                                // 非严格模式：尝试放宽连续工作天数限制
                                log.warn("{}的时段{}已分配{}个医生，需要{}个，尝试放宽连续工作限制", 
                                    currentDate, slot.getSlotName(), assignedCount, minDoctors);
                                availableDoctors = filterAvailableDoctorsRelaxed(
                                    doctors, currentDate, slot, workloadMap, 
                                    leaveMap, generatedSchedules, existingSchedules, request.getRules()
                                );
                                
                                // 🔥 排除该时段已排班的医生（允许医生在同一天的不同时段工作）
                                availableDoctors = availableDoctors.stream()
                                    .filter(d -> !slotDoctorsAssigned.contains(d.getDoctorId()))
                                    .collect(Collectors.toList());
                                
                                if (availableDoctors.isEmpty()) {
                                    log.warn("{}的时段{}已分配{}个医生，需要{}个但无更多可用医生（已放宽限制）", 
                                        currentDate, slot.getSlotName(), assignedCount, minDoctors);
                                    break;
                                } else {
                                    relaxedCount++;
                                    log.warn("⚠️ 已为{}的时段{}放宽连续工作限制（第{}个医生）", 
                                        currentDate, slot.getSlotName(), assignedCount + 1);
                                }
                            }
                        }
                        
                        // 4.2 智能选择医生（综合考虑工作量和连续工作天数）
                        Doctor selectedDoctor = selectBestDoctor(
                            availableDoctors, workloadMap, generatedSchedules, 
                            currentDate, request.getRules()
                        );
                        
                        // 4.4 获取医生的固定诊室
                        Location assignedLocation = doctorLocationMap.get(selectedDoctor.getDoctorId());
                        
                        if (assignedLocation == null) {
                            log.warn("医生{}未分配诊室，跳过", selectedDoctor.getFullName());
                            continue;
                        }
                        
                        // 🔥 检查诊室是否已被该时段其他医生使用
                        if (slotLocationsUsed.contains(assignedLocation.getLocationId())) {
                            log.warn("诊室{}在{}的时段{}已被占用，跳过医生{}", 
                                assignedLocation.getLocationName(), currentDate, slot.getSlotName(), 
                                selectedDoctor.getFullName());
                            continue;
                        }
                        
                        // 4.5 创建排班记录
                        Schedule schedule = buildSchedule(
                            selectedDoctor, currentDate, slot, 
                            assignedLocation, request.getRules()
                        );
                        
                        generatedSchedules.add(schedule);
                        
                        // 4.6 更新工作量
                        workloadMap.merge(selectedDoctor.getDoctorId(), 1, Integer::sum);
                        
                        // 🔥 记录该医生已在该时段排班（允许同一天不同时段工作）
                        slotDoctorsAssigned.add(selectedDoctor.getDoctorId());
                        slotLocationsUsed.add(assignedLocation.getLocationId());
                        assignedCount++;
                        
                        // 如果达到最大医生数，停止为该时段分配
                        if (assignedCount >= maxDoctors) {
                            break;
                        }
                    }
                    
                    // 如果未达到最小医生数，记录为未完全分配
                    if (assignedCount < minDoctors) {
                        UnassignedSlot unassigned = new UnassignedSlot();
                        unassigned.setDate(currentDate);
                        unassigned.setSlotId(slot.getSlotId());
                        unassigned.setSlotName(slot.getSlotName());
                        unassigned.setReason(String.format("仅分配了%d个医生，未达到最小要求%d个", 
                            assignedCount, minDoctors));
                        unassigned.setSuggestions(Arrays.asList(
                            "增加医生数量", 
                            "减少每时段最小医生数", 
                            "调整请假安排", 
                            "关闭严格模式"));
                        unassignedSlots.add(unassigned);
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
            
            log.info("排班生成完成: 共生成{}条排班记录，其中{}条使用了放宽限制", 
                generatedSchedules.size(), relaxedCount);
            
            if (relaxedCount > 0) {
                log.warn("⚠️ 警告：有{}个时段因无可用医生而放宽了连续工作天数限制", relaxedCount);
            }
            
            // 5. 冲突检测
            List<ScheduleConflict> conflicts = conflictDetector.detectConflicts(
                generatedSchedules, existingSchedules
            );
            
            // 6. 保存到数据库（如果不是预览模式且无冲突）
            if (!request.getPreviewOnly() && conflicts.isEmpty()) {
                scheduleRepository.saveAll(generatedSchedules);
                log.info("排班已保存到数据库");
            } else if (request.getPreviewOnly()) {
                log.info("预览模式，排班未保存");
            } else {
                log.warn("检测到{}个冲突，排班未保存", conflicts.size());
            }
            
            // 7. 生成统计和响应
            long executionTime = System.currentTimeMillis() - startTime;
            return buildResponse(
                generatedSchedules, workloadMap, 
                conflicts, unassignedSlots, doctors, 
                request, executionTime
            );
            
        } catch (Exception e) {
            log.error("自动排班生成失败", e);
            return buildErrorResponse(e.getMessage());
        }
    }
    
    /**
     * 参数校验
     */
    private void validateRequest(AutoScheduleRequest request) {
        if (request.getDepartmentId() == null) {
            throw new IllegalArgumentException("科室ID不能为空");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("结束日期不能为空");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("开始日期不能早于今天");
        }
        
        // 检查时间跨度
        Period period = Period.between(request.getStartDate(), request.getEndDate());
        if (period.getDays() > 90) {
            throw new IllegalArgumentException("时间跨度不能超过90天");
        }
    }
    
    /**
     * 准备科室的活跃医生列表
     */
    private List<Doctor> prepareActiveDoctors(Integer departmentId) {
        List<Doctor> doctors = doctorRepository.findByDepartmentDepartmentIdAndStatus(
            departmentId, DoctorStatus.active
        );
        
        if (doctors.isEmpty()) {
            throw new IllegalArgumentException("该科室没有可用的医生");
        }
        
        return doctors;
    }
    
    /**
     * 准备科室的诊室列表
     */
    private List<Location> prepareLocations(Integer departmentId) {
        List<Location> locations = locationRepository.findByDepartmentDepartmentId(departmentId);
        
        if (locations.isEmpty()) {
            throw new IllegalArgumentException("该科室没有可用的诊室");
        }
        
        return locations;
    }
    
    /**
     * 为每个医生分配固定诊室
     * 策略：轮流分配，确保每个医生有自己的诊室
     */
    private Map<Integer, Location> assignDoctorLocations(List<Doctor> doctors, List<Location> locations) {
        Map<Integer, Location> doctorLocationMap = new HashMap<>();
        
        if (locations.isEmpty()) {
            log.warn("⚠️ 没有可用的诊室");
            return doctorLocationMap;
        }
        
        // 轮流分配诊室
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get(i);
            Location location = locations.get(i % locations.size());  // 轮换使用诊室
            doctorLocationMap.put(doctor.getDoctorId(), location);
            log.info("✅ 医生 {} 分配到诊室 {}", doctor.getFullName(), location.getLocationName());
        }
        
        return doctorLocationMap;
    }
    
    /**
     * 筛选代表性时段：上午和下午各选择一个时段
     * 规则：上午选择最早的时段，下午选择最早的时段
     */
    private List<TimeSlot> selectRepresentativeSlots(List<TimeSlot> allSlots) {
        List<TimeSlot> result = new ArrayList<>();
        
        // 上午时段：12:00之前
        Optional<TimeSlot> morningSlot = allSlots.stream()
            .filter(slot -> slot.getStartTime().isBefore(java.time.LocalTime.NOON))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .findFirst();
        
        // 下午时段：12:00及之后
        Optional<TimeSlot> afternoonSlot = allSlots.stream()
            .filter(slot -> !slot.getStartTime().isBefore(java.time.LocalTime.NOON))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .findFirst();
        
        morningSlot.ifPresent(result::add);
        afternoonSlot.ifPresent(result::add);
        
        log.info("筛选时段完成：上午={}, 下午={}", 
            morningSlot.map(TimeSlot::getSlotName).orElse("无"),
            afternoonSlot.map(TimeSlot::getSlotName).orElse("无"));
        
        return result;
    }
    
    /**
     * 加载已有排班
     */
    private List<Schedule> loadExistingSchedules(AutoScheduleRequest request) {
        return scheduleRepository.findByScheduleDateBetween(
            request.getStartDate(), 
            request.getEndDate()
        );
    }
    
    /**
     * 构建请假记录映射
     */
    private Map<Integer, List<LeaveRequest>> buildLeaveRequestMap(
            List<Doctor> doctors,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<Integer> doctorIds = doctors.stream()
            .map(Doctor::getDoctorId)
            .collect(Collectors.toList());
        
        List<LeaveRequest> leaveRequests = leaveRequestRepository
            .findApprovedLeavesByDoctorIdsAndDateRange(
                doctorIds,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            );
        
        // 按医生ID分组
        return leaveRequests.stream()
            .collect(Collectors.groupingBy(lr -> lr.getDoctor().getDoctorId()));
    }
    
    /**
     * 筛选可用医生
     */
    private List<Doctor> filterAvailableDoctors(
            List<Doctor> doctors,
            LocalDate date,
            TimeSlot slot,
            Map<Integer, Integer> workloadMap,
            Map<Integer, List<LeaveRequest>> leaveMap,
            List<Schedule> generatedSchedules,
            List<Schedule> existingSchedules,
            ScheduleRules rules) {
        
        // 合并已有排班和新生成的排班
        List<Schedule> allSchedules = new ArrayList<>();
        allSchedules.addAll(existingSchedules);
        allSchedules.addAll(generatedSchedules);
        
        return doctors.stream()
            .filter(doctor -> constraintValidator.checkHardConstraints(
                doctor, date, slot, leaveMap, allSchedules, workloadMap, rules
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 筛选可用医生（放宽连续工作天数限制）
     * 用于紧急情况，确保每天至少有医生可用
     */
    private List<Doctor> filterAvailableDoctorsRelaxed(
            List<Doctor> doctors,
            LocalDate date,
            TimeSlot slot,
            Map<Integer, Integer> workloadMap,
            Map<Integer, List<LeaveRequest>> leaveMap,
            List<Schedule> generatedSchedules,
            List<Schedule> existingSchedules,
            ScheduleRules rules) {
        
        // 合并已有排班和新生成的排班
        List<Schedule> allSchedules = new ArrayList<>();
        allSchedules.addAll(existingSchedules);
        allSchedules.addAll(generatedSchedules);
        
        return doctors.stream()
            .filter(doctor -> constraintValidator.checkHardConstraintsRelaxed(
                doctor, date, slot, leaveMap, allSchedules, workloadMap, rules
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 智能选择最佳医生
     * 综合考虑工作量和连续工作天数，选择最合适的医生
     */
    private Doctor selectBestDoctor(
            List<Doctor> availableDoctors,
            Map<Integer, Integer> workloadMap,
            List<Schedule> generatedSchedules,
            LocalDate date,
            ScheduleRules rules) {
        
        if (availableDoctors.isEmpty()) {
            throw new IllegalStateException("无可用医生");
        }
        
        if (availableDoctors.size() == 1) {
            return availableDoctors.get(0);
        }
        
        // 计算每个医生的评分（越低越好）
        Doctor bestDoctor = availableDoctors.get(0);
        double bestScore = Double.MAX_VALUE;
        
        for (Doctor doctor : availableDoctors) {
            // 获取当前工作量
            int workload = workloadMap.getOrDefault(doctor.getDoctorId(), 0);
            
            // 计算连续工作天数
            int consecutiveDays = constraintValidator.getConsecutiveWorkDays(
                doctor, date, generatedSchedules
            );
            
            // 综合评分：工作量权重0.6 + 连续天数权重0.4
            // 连续天数越多，越需要休息，分数越高
            double score = workload * 0.6 + consecutiveDays * 0.4;
            
            if (score < bestScore) {
                bestScore = score;
                bestDoctor = doctor;
            }
        }
        
        return bestDoctor;
    }
    
    /**
     * 按工作量排序（升序）
     */
    private void sortByWorkload(List<Doctor> doctors, Map<Integer, Integer> workloadMap) {
        doctors.sort((d1, d2) -> {
            int workload1 = workloadMap.getOrDefault(d1.getDoctorId(), 0);
            int workload2 = workloadMap.getOrDefault(d2.getDoctorId(), 0);
            return Integer.compare(workload1, workload2);
        });
    }
    
    /**
     * 构建排班记录
     */
    private Schedule buildSchedule(
            Doctor doctor,
            LocalDate date,
            TimeSlot slot,
            Location location,
            ScheduleRules rules) {
        
        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setScheduleDate(date);
        schedule.setSlot(slot);
        schedule.setLocation(location);
        schedule.setTotalSlots(rules.getDefaultTotalSlots());
        schedule.setBookedSlots(0);
        schedule.setFee(rules.getDefaultFee());
        schedule.setStatus(ScheduleStatus.available);
        schedule.setRemarks("自动排班生成");
        
        return schedule;
    }
    
    /**
     * 构建成功响应
     */
    private AutoScheduleResponse buildResponse(
            List<Schedule> generatedSchedules,
            Map<Integer, Integer> workloadMap,
            List<ScheduleConflict> conflicts,
            List<UnassignedSlot> unassignedSlots,
            List<Doctor> doctors,
            AutoScheduleRequest request,
            long executionTime) {
        
        AutoScheduleResponse response = new AutoScheduleResponse();
        
        // 基本信息
        boolean success = conflicts.isEmpty();
        response.setSuccess(success);
        response.setMessage(success 
            ? String.format("自动排班生成成功，共生成%d条排班记录", generatedSchedules.size())
            : String.format("检测到%d个冲突，请处理后重试", conflicts.size())
        );
        
        // 转换为ScheduleResponse列表
        List<ScheduleResponse> scheduleResponses = generatedSchedules.stream()
            .map(this::convertToScheduleResponse)
            .collect(Collectors.toList());
        response.setSchedules(scheduleResponses);
        
        // 统计信息
        ScheduleStatistics statistics = buildStatistics(
            generatedSchedules, workloadMap, unassignedSlots, request, executionTime
        );
        response.setStatistics(statistics);
        
        // 冲突列表
        response.setConflicts(conflicts);
        
        // 未分配时间段
        response.setUnassignedSlots(unassignedSlots);
        
        // 工作量分布
        Map<Integer, DoctorWorkload> workloadDistribution = 
            workloadCalculator.calculateWorkloadDistribution(generatedSchedules, doctors);
        response.setWorkloadDistribution(workloadDistribution);
        
        // 警告信息
        List<String> warnings = generateWarnings(workloadDistribution, unassignedSlots, request.getRules());
        response.setWarnings(warnings);
        
        // 排班预览表
        List<SchedulePreviewDay> schedulePreview = buildSchedulePreview(
            generatedSchedules, request.getStartDate(), request.getEndDate()
        );
        response.setSchedulePreview(schedulePreview);
        
        return response;
    }
    
    /**
     * 转换为ScheduleResponse
     */
    private ScheduleResponse convertToScheduleResponse(Schedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setScheduleId(schedule.getScheduleId());
        response.setDoctorId(schedule.getDoctor().getDoctorId());
        response.setDoctorName(schedule.getDoctor().getFullName());
        response.setDoctorTitle(schedule.getDoctor().getTitle());
        response.setDepartmentId(schedule.getDoctor().getDepartment().getDepartmentId());
        response.setDepartmentName(schedule.getDoctor().getDepartment().getName());
        response.setScheduleDate(schedule.getScheduleDate());
        response.setSlotId(schedule.getSlot().getSlotId());
        response.setSlotName(schedule.getSlot().getSlotName());
        response.setStartTime(schedule.getSlot().getStartTime());
        response.setEndTime(schedule.getSlot().getEndTime());
        response.setLocationId(schedule.getLocation().getLocationId());
        response.setLocation(schedule.getLocation().getLocationName());
        response.setTotalSlots(schedule.getTotalSlots());
        response.setBookedSlots(schedule.getBookedSlots());
        response.setFee(schedule.getFee());
        response.setStatus(schedule.getStatus().name());
        response.setRemarks(schedule.getRemarks());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());
        return response;
    }
    
    /**
     * 构建统计信息
     */
    private ScheduleStatistics buildStatistics(
            List<Schedule> schedules,
            Map<Integer, Integer> workloadMap,
            List<UnassignedSlot> unassignedSlots,
            AutoScheduleRequest request,
            long executionTime) {
        
        ScheduleStatistics statistics = new ScheduleStatistics();
        
        // 总排班数
        statistics.setTotalSchedules(schedules.size());
        
        // 覆盖天数
        Period period = Period.between(request.getStartDate(), request.getEndDate());
        statistics.setCoveredDays(period.getDays() + 1);
        
        // 参与医生数
        long doctorsInvolved = schedules.stream()
            .map(s -> s.getDoctor().getDoctorId())
            .distinct()
            .count();
        statistics.setDoctorsInvolved((int) doctorsInvolved);
        
        // 工作量统计
        Map<String, Object> workloadSummary = 
            workloadCalculator.calculateWorkloadSummary(
                workloadMap.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            DoctorWorkload w = new DoctorWorkload();
                            w.setTotalShifts(e.getValue());
                            return w;
                        }
                    ))
            );
        
        statistics.setAverageWorkload((Double) workloadSummary.get("avg"));
        statistics.setMaxWorkload((Integer) workloadSummary.get("max"));
        statistics.setMinWorkload((Integer) workloadSummary.get("min"));
        
        // 覆盖率计算
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();
        int totalSlots = statistics.getCoveredDays() * timeSlots.size();
        double coverageRate = totalSlots > 0 
            ? (double) schedules.size() / totalSlots 
            : 0.0;
        statistics.setCoverageRate(coverageRate);
        
        // 冲突数量
        statistics.setConflictCount(0);
        
        // 执行时间
        statistics.setExecutionTime(executionTime);
        
        return statistics;
    }
    
    /**
     * 生成警告信息
     */
    private List<String> generateWarnings(
            Map<Integer, DoctorWorkload> workloadDistribution,
            List<UnassignedSlot> unassignedSlots,
            ScheduleRules rules) {
        
        List<String> warnings = new ArrayList<>();
        
        // 检查连续工作天数（接近上限时提醒，达到上限会被硬约束阻止）
        Integer consecutiveLimit = rules.getConsecutiveWorkDaysLimit();
        if (consecutiveLimit != null && consecutiveLimit > 0) {
            int warningThreshold = Math.max(consecutiveLimit - 1, 1); // 接近上限的阈值
            
            for (DoctorWorkload workload : workloadDistribution.values()) {
                if (workload.getMaxConsecutiveDays() >= warningThreshold) {
                    warnings.add(String.format(
                        "医生%d(%s)连续工作%d天，接近上限(%d天)",
                        workload.getDoctorId(),
                        workload.getDoctorName(),
                        workload.getMaxConsecutiveDays(),
                        consecutiveLimit
                    ));
                }
            }
        }
        
        // 检查未分配时间段
        if (!unassignedSlots.isEmpty()) {
            Map<LocalDate, Long> unassignedByDate = unassignedSlots.stream()
                .collect(Collectors.groupingBy(
                    UnassignedSlot::getDate,
                    Collectors.counting()
                ));
            
            for (Map.Entry<LocalDate, Long> entry : unassignedByDate.entrySet()) {
                warnings.add(String.format(
                    "%s有%d个时段未分配",
                    entry.getKey(),
                    entry.getValue()
                ));
            }
        }
        
        return warnings;
    }
    
    /**
     * 构建排班预览表
     */
    private List<SchedulePreviewDay> buildSchedulePreview(
            List<Schedule> generatedSchedules,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<SchedulePreviewDay> previewDays = new ArrayList<>();
        String[] weekDayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        
        // 按日期组织排班
        Map<LocalDate, List<Schedule>> schedulesByDate = generatedSchedules.stream()
            .collect(Collectors.groupingBy(Schedule::getScheduleDate));
        
        // 遍历每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            SchedulePreviewDay previewDay = new SchedulePreviewDay();
            previewDay.setDate(currentDate);
            previewDay.setDayOfWeek(currentDate.getDayOfWeek().getValue());
            previewDay.setDayOfWeekName(weekDayNames[currentDate.getDayOfWeek().getValue() % 7]);
            
            List<Schedule> daySchedules = schedulesByDate.getOrDefault(currentDate, new ArrayList<>());
            
            // 按时段组织
            Map<Integer, Schedule> schedulesBySlot = daySchedules.stream()
                .collect(Collectors.toMap(
                    s -> s.getSlot().getSlotId(),
                    s -> s,
                    (s1, s2) -> s1 // 如果有重复，取第一个
                ));
            
            // 获取所有时段
            List<SchedulePreviewSlot> previewSlots = new ArrayList<>();
            for (Schedule schedule : daySchedules) {
                SchedulePreviewSlot previewSlot = new SchedulePreviewSlot();
                previewSlot.setSlotId(schedule.getSlot().getSlotId());
                previewSlot.setSlotName(schedule.getSlot().getSlotName());
                previewSlot.setTimeRange(schedule.getSlot().getStartTime() + "-" + schedule.getSlot().getEndTime());
                previewSlot.setDoctorId(schedule.getDoctor().getDoctorId());
                previewSlot.setDoctorName(schedule.getDoctor().getFullName());
                previewSlot.setLocationName(schedule.getLocation() != null ? schedule.getLocation().getLocationName() : "未分配");
                
                // 计算连续工作天数
                int consecutiveDays = constraintValidator.getConsecutiveWorkDays(
                    schedule.getDoctor(), currentDate, generatedSchedules
                );
                previewSlot.setConsecutiveDays(consecutiveDays + 1); // +1 包括当天
                previewSlot.setIsRelaxed(false); // 这里简化处理，实际可以在生成时标记
                
                previewSlots.add(previewSlot);
            }
            
            // 按时段ID排序
            previewSlots.sort(Comparator.comparing(SchedulePreviewSlot::getSlotId));
            
            previewDay.setSlots(previewSlots);
            previewDays.add(previewDay);
            
            currentDate = currentDate.plusDays(1);
        }
        
        return previewDays;
    }
    
    /**
     * 构建错误响应
     */
    private AutoScheduleResponse buildErrorResponse(String errorMessage) {
        AutoScheduleResponse response = new AutoScheduleResponse();
        response.setSuccess(false);
        response.setMessage("自动排班失败: " + errorMessage);
        response.setSchedules(new ArrayList<>());
        response.setConflicts(new ArrayList<>());
        response.setUnassignedSlots(new ArrayList<>());
        response.setWorkloadDistribution(new HashMap<>());
        response.setWarnings(new ArrayList<>());
        response.setSchedulePreview(new ArrayList<>());
        return response;
    }
}

