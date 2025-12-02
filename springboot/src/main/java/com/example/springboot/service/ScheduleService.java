package com.example.springboot.service;

import com.example.springboot.dto.schedule.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 排班服务接口
 */
public interface ScheduleService {
    
    /**
     * 获取排班列表
     */
    Page<ScheduleResponse> getSchedules(ScheduleListRequest request);
    
    /**
     * 根据ID获取排班详情
     */
    ScheduleResponse getScheduleById(Integer scheduleId);
    
    /**
     * 更新排班
     */
    ScheduleResponse updateSchedule(Integer scheduleId, ScheduleUpdateRequest request);
    
    /**
     * 批量更新排班
     */
    List<ScheduleResponse> batchUpdateSchedules(ScheduleBatchUpdateRequest request);
    
    /**
     * 创建排班
     */
    //ScheduleResponse createSchedule(ScheduleResponse request);

    /**
     * 创建排班
     */
    ScheduleResponse createSchedule(ScheduleCreateRequest request);

    
    /**
     * 删除排班
     */
    void deleteSchedule(Integer scheduleId);

    /**
     * 根据医生、时段、门诊室和日期删除排班
     */
    void deleteScheduleByParams(ScheduleDeleteRequest request);

    // 在原有方法基础上新增带分页参数的重载方法
    Page<ScheduleResponse> getSchedules(ScheduleListRequest request, Pageable pageable);

    /**
     * 根据医生ID和日期范围获取排班列表
     */
    Page<ScheduleResponse> getSchedulesByDoctorId(Integer doctorId, String startDate, String endDate, Pageable pageable);
}