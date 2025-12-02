package com.example.springboot.service;

import com.example.springboot.dto.AutoScheduleRequest;
import com.example.springboot.dto.AutoScheduleResponse;

/**
 * 自动排班服务接口
 */
public interface AutoScheduleService {
    
    /**
     * 自动生成排班
     * 
     * @param request 排班请求参数
     * @return 排班结果
     */
    AutoScheduleResponse autoGenerateSchedule(AutoScheduleRequest request);
}


