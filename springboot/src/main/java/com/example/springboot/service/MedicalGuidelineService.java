package com.example.springboot.service;

import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.guideline.MedicalGuidelineRequest;
import com.example.springboot.dto.guideline.MedicalGuidelineResponse;

public interface MedicalGuidelineService {
    // 获取规范列表（分页）
    PageResponse<MedicalGuidelineResponse> getGuidelines(
            Integer page, Integer pageSize, String keyword, String category, String status);

    // 获取规范详情
    MedicalGuidelineResponse getGuidelineById(Integer id);

    // 创建规范
    MedicalGuidelineResponse createGuideline(MedicalGuidelineRequest request);

    // 更新规范
    MedicalGuidelineResponse updateGuideline(Integer id, MedicalGuidelineRequest request);

    // 删除规范
    void deleteGuideline(Integer id);
}