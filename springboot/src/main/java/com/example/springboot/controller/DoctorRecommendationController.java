package com.example.springboot.controller;

import com.example.springboot.dto.recommendation.DoctorRecommendationDTO;
import com.example.springboot.dto.recommendation.DoctorRecommendationRequest;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.service.NLPService;
import com.example.springboot.service.RecommendationAlgorithmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 医生推荐控制器
 */
@RestController
@RequestMapping("/api/doctor-recommendations")
public class DoctorRecommendationController {
    
    private static final Logger logger = LoggerFactory.getLogger(DoctorRecommendationController.class);
    
    @Autowired
    private NLPService nlpService;
    
    @Autowired
    private RecommendationAlgorithmService recommendationService;

    @Autowired
private PatientRepository patientRepository;

/**
 * 科室推荐（第一个流程图）
 * POST /api/doctor-recommendations/departments
 */
@PostMapping("/departments")
public ResponseEntity<?> recommendDepartments(@RequestBody DepartmentRecommendationRequest request) {
    try {
        if (request == null || request.getSymptomDescription() == null || 
            request.getSymptomDescription().trim().isEmpty()) {
            throw new BadRequestException("症状描述不能为空");
        }
        
        List<NLPService.DepartmentRecommendation> recommendations = 
                nlpService.recommendDepartments(
                        request.getSymptomDescription(), request.getPatientId());
        
        // 转换为DTO
        List<DepartmentRecommendationDTO> result = recommendations.stream()
                .map(this::convertDepartmentToDTO)
                .collect(Collectors.toList());
        
        return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (Exception e) {
        logger.error("科室推荐失败", e);
        return new ResponseEntity<>("推荐失败: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

/**
 * 按科室推荐医生（第二个流程图，带排班信息）
 * POST /api/doctor-recommendations/by-department
 */
@PostMapping("/by-department")
public ResponseEntity<?> recommendDoctorsByDepartment(
        @RequestBody DoctorRecommendationByDepartmentRequest request) {
    try {
        if (request.getDepartmentId() == null) {
            throw new BadRequestException("科室ID不能为空");
        }
        
        List<RecommendationAlgorithmService.DoctorRecommendationWithSchedule> recommendations = 
                recommendationService.recommendDoctorsByDepartment(
                        request.getDepartmentId(),
                        request.getPatientId(),
                        request.getSymptomKeywords(),
                        request.getTopN() != null ? request.getTopN() : 10);
        
        // 转换为DTO
        List<DoctorRecommendationWithScheduleDTO> result = recommendations.stream()
                .map(this::convertWithScheduleToDTO)
                .collect(Collectors.toList());
        
        return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (Exception e) {
        logger.error("按科室推荐医生失败", e);
        return new ResponseEntity<>("推荐失败: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// 转换方法
private DepartmentRecommendationDTO convertDepartmentToDTO(
        NLPService.DepartmentRecommendation rec) {
    DepartmentRecommendationDTO dto = new DepartmentRecommendationDTO();
    dto.setDepartmentId(rec.getDepartmentId());
    dto.setDepartmentName(rec.getDepartmentName());
    dto.setDescription(rec.getDescription());
    dto.setScore(rec.getScore());
    dto.setReason(rec.getReason());
    dto.setPriority(rec.getPriority());
    return dto;
}

private DoctorRecommendationWithScheduleDTO convertWithScheduleToDTO(
        RecommendationAlgorithmService.DoctorRecommendationWithSchedule rec) {
    DoctorRecommendationWithScheduleDTO dto = new DoctorRecommendationWithScheduleDTO();
    dto.setDoctorId(rec.getDoctorId());
    dto.setDoctorName(rec.getDoctorName());
    dto.setDepartmentId(rec.getDepartmentId());
    dto.setDepartmentName(rec.getDepartmentName());
    dto.setTitle(rec.getTitle());
    dto.setTitleLevel(rec.getTitleLevel());
    dto.setSpecialty(rec.getSpecialty());
    dto.setPhotoUrl(rec.getPhotoUrl());
    dto.setScore(rec.getScore());
    dto.setReason(rec.getReason());
    
    // 转换排班信息
    if (rec.getAvailableSchedules() != null) {
        List<DoctorRecommendationWithScheduleDTO.ScheduleInfo> scheduleInfos = 
                rec.getAvailableSchedules().stream()
                        .map(s -> {
                            DoctorRecommendationWithScheduleDTO.ScheduleInfo info = 
                                    new DoctorRecommendationWithScheduleDTO.ScheduleInfo();
                            info.setScheduleId(s.getScheduleId());
                            info.setScheduleDate(s.getScheduleDate());
                            info.setStartTime(s.getStartTime());
                            info.setEndTime(s.getEndTime());
                            info.setTotalSlots(s.getTotalSlots());
                            info.setBookedSlots(s.getBookedSlots());
                            info.setAvailableSlots(s.getAvailableSlots());
                            info.setLocationName(s.getLocationName());
                            return info;
                        })
                        .collect(Collectors.toList());
        dto.setAvailableSchedules(scheduleInfos);
    }
    
    return dto;
}
    
    /**
     * 根据症状描述推荐医生
     * POST /api/doctor-recommendations
     */
    @PostMapping
    public ResponseEntity<?> recommendDoctors(@RequestBody DoctorRecommendationRequest request) {
        try {
            // 参数验证
            if (request == null || request.getSymptomDescription() == null || 
                request.getSymptomDescription().trim().isEmpty()) {
                throw new BadRequestException("症状描述不能为空");
            }
            
            if (request.getTopN() == null || request.getTopN() <= 0) {
                request.setTopN(5); // 默认值
            }
            
            logger.info("收到推荐请求: {}", request.getSymptomDescription());
            
            // 1. 提取症状关键词
            List<String> symptomKeywords = nlpService.extractSymptomKeywords(
                    request.getSymptomDescription());
            
            if (symptomKeywords.isEmpty()) {
                return new ResponseEntity<>("未能从描述中提取到有效的症状关键词", HttpStatus.OK);
            }
            
            // 2. 调用推荐算法
            List<RecommendationAlgorithmService.DoctorRecommendation> recommendations = 
                    recommendationService.hybridRecommend(symptomKeywords, request.getTopN());
            
            // 3. 转换为DTO
            List<DoctorRecommendationDTO> result = recommendations.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return new ResponseEntity<>(result, HttpStatus.OK);
            
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("推荐失败", e);
            return new ResponseEntity<>("推荐失败: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 转换为DTO
     */
    private DoctorRecommendationDTO convertToDTO(
            RecommendationAlgorithmService.DoctorRecommendation recommendation) {
        DoctorRecommendationDTO dto = new DoctorRecommendationDTO();
        dto.setDoctorId(recommendation.getDoctorId());
        dto.setDoctorName(recommendation.getDoctorName());
        dto.setDepartmentId(recommendation.getDepartmentId());
        dto.setDepartmentName(recommendation.getDepartmentName());
        dto.setTitle(recommendation.getTitle());
        dto.setTitleLevel(recommendation.getTitleLevel());
        dto.setSpecialty(recommendation.getSpecialty());
        dto.setPhotoUrl(recommendation.getPhotoUrl());
        dto.setScore(recommendation.getScore());
        dto.setReason(recommendation.getReason());
        return dto;
    }
}