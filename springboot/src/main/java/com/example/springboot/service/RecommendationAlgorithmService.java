package com.example.springboot.service;

import com.example.springboot.entity.Department;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.SymptomDepartmentMapping;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.DoctorRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.repository.SymptomDepartmentMappingRepository;
import com.example.springboot.util.CosineSimilarityCalculator;
import com.example.springboot.util.TFIDFCalculator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.springboot.repository.PatientProfileRepository;
import com.example.springboot.repository.PatientRepository;
import com.example.springboot.entity.PatientProfile;
import com.example.springboot.entity.Schedule;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 推荐算法核心服务
 * 实现基于症状的医生推荐算法
 */
@Service
public class RecommendationAlgorithmService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationAlgorithmService.class);
    
    @Autowired
    private NLPService nlpService;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private SymptomDepartmentMappingRepository mappingRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private WordVectorService wordVectorService;
    
    // 缓存：医生ID -> 特征向量 (避免每次重复计算医生向量)
    private final Map<Integer, INDArray> doctorVectorCache = new ConcurrentHashMap<>();
    
    // 权重配置（可根据实际效果调整）
    private static final double W1_SYMPTOM_MATCH = 0.4;      // 症状匹配度权重
    private static final double W2_DEPARTMENT_MATCH = 0.3;  // 科室匹配度权重
    private static final double W3_TITLE_SCORE = 0.15;       // 职称分数权重
    private static final double W4_POPULARITY = 0.1;         // 热度分数权重
    private static final double W5_AVAILABILITY = 0.05;     // 可预约性权重
    
    /**
     * 系统启动时，预计算所有医生的特征向量
     */
    @PostConstruct
    public void initDoctorVectors() {
        if (wordVectorService == null || !wordVectorService.isReady()) {
            logger.info("⚠️ AI 模型未就绪，跳过医生向量预计算（将使用普通匹配）。");
            return;
        }
        
        logger.info("🚀 开始预计算医生特征向量...");
        List<Doctor> doctors = doctorRepository.findAll();
        refreshDoctorVectorCache(doctors);
    }
    
    /**
     * 刷新缓存的方法 (当医生信息更新时也可调用此方法)
     */
    public void refreshDoctorVectorCache(List<Doctor> doctors) {
        if (wordVectorService == null || !wordVectorService.isReady()) {
            return;
        }
        
        int count = 0;
        for (Doctor doc : doctors) {
            if (doc.getSpecialty() != null && !doc.getSpecialty().isEmpty()) {
                // A. 调用 NLP 服务进行分词
                List<String> keywords = nlpService.segmentText(doc.getSpecialty());
                // B. 调用 AI 服务转为向量
                INDArray vector = wordVectorService.encodeText(keywords);
                
                if (vector != null) {
                    doctorVectorCache.put(doc.getDoctorId(), vector);
                    count++;
                }
            }
        }
        logger.info("✅ 已构建 {} 位医生的 AI 语义索引。", count);
    }
    
    /**
     * 混合推荐策略
     * @param symptomKeywords 症状关键词列表
     * @param topN 返回Top-N个推荐
     * @return 医生推荐列表
     */
    @Transactional(readOnly = true)
    public List<DoctorRecommendation> hybridRecommend(List<String> symptomKeywords, int topN) {
        logger.info("开始推荐，症状关键词: {}, Top-N: {}", symptomKeywords, topN);
        
        if (symptomKeywords == null || symptomKeywords.isEmpty()) {
            logger.warn("症状关键词为空，返回空列表");
            return new ArrayList<>();
        }
        
        // 1. 通过symptom_department_mapping匹配科室
        List<Department> matchedDepartments = matchDepartmentsBySymptoms(symptomKeywords);
        logger.info("匹配到的科室数量: {}", matchedDepartments.size());
        
        if (matchedDepartments.isEmpty()) {
            logger.warn("未匹配到相关科室，返回空列表");
            return new ArrayList<>();
        }
        
        // 2. 从匹配的科室中获取医生列表
        List<Doctor> candidateDoctors = new ArrayList<>();
        for (Department department : matchedDepartments) {
            List<Doctor> doctors = doctorRepository.findByDepartmentDepartmentIdAndStatus(
                    department.getDepartmentId(), DoctorStatus.active);
            candidateDoctors.addAll(doctors);
        }
        
        // 去重
        candidateDoctors = candidateDoctors.stream()
                .distinct()
                .collect(Collectors.toList());
        
        logger.info("候选医生数量: {}", candidateDoctors.size());
        
        if (candidateDoctors.isEmpty()) {
            logger.warn("候选医生为空，返回空列表");
            return new ArrayList<>();
        }
        
        // 3. 对每个医生计算综合评分
        List<DoctorRecommendation> recommendations = new ArrayList<>();
        for (Doctor doctor : candidateDoctors) {
            double score = calculateScore(doctor, symptomKeywords, matchedDepartments);
            String reason = generateRecommendationReason(doctor, symptomKeywords);
            
            DoctorRecommendation recommendation = new DoctorRecommendation();
            recommendation.setDoctorId(doctor.getDoctorId());
            recommendation.setDoctorName(doctor.getFullName());
            recommendation.setDepartmentId(doctor.getDepartment().getDepartmentId());
            recommendation.setDepartmentName(doctor.getDepartment().getName());
            recommendation.setTitle(doctor.getTitle());
            recommendation.setTitleLevel(doctor.getTitleLevel());
            recommendation.setSpecialty(doctor.getSpecialty());
            recommendation.setPhotoUrl(doctor.getPhotoUrl());
            recommendation.setScore(score);
            recommendation.setReason(reason);
            
            recommendations.add(recommendation);
        }
        
        // 4. 按评分排序，返回Top-N
        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        List<DoctorRecommendation> result = recommendations.stream()
                .limit(topN)
                .collect(Collectors.toList());
        
        logger.info("推荐完成，返回 {} 个推荐结果", result.size());
        return result;
    }
    
    /**
     * 重写相似度计算 (优先使用缓存)
     */
    public double calculateSimilarity(List<String> symptoms, Doctor doctor) {
        if (doctor.getSpecialty() == null || doctor.getSpecialty().trim().isEmpty()) {
            return 0.0;
        }
        
        // 降级判断：模型未加载 OR 该医生无缓存 -> 回退到普通匹配
        if (wordVectorService == null || !wordVectorService.isReady() || 
            !doctorVectorCache.containsKey(doctor.getDoctorId())) {
            return nlpService.calculateSymptomMatch(symptoms, doctor.getSpecialty());
        }

        // A. 实时计算患者输入的向量
        INDArray userVector = wordVectorService.encodeText(symptoms);
        if (userVector == null) return 0.0;

        // B. 从缓存直接获取医生向量 (纳秒级)
        INDArray doctorVector = doctorVectorCache.get(doctor.getDoctorId());

        // C. 计算并返回相似度
        return wordVectorService.calculateSimilarity(userVector, doctorVector);
    }
    
    /**
     * 多因素加权评分
     * @param doctor 医生
     * @param symptoms 症状关键词列表
     * @param matchedDepartments 匹配的科室列表
     * @return 综合评分（0-1之间）
     */
    private double calculateScore(Doctor doctor, List<String> symptoms, List<Department> matchedDepartments) {
        // 1. 症状匹配度（基于specialty字段的TF-IDF相似度）
        double symptomMatchScore = calculateSimilarity(symptoms, doctor);
        
        // 2. 科室匹配度
        double departmentMatchScore = calculateDepartmentMatchScore(doctor, matchedDepartments);
        
        // 3. 职称分数
        double titleScore = calculateTitleScore(doctor.getTitleLevel());
        
        // 4. 热度分数（基于预约数量）
        double popularityScore = calculatePopularityScore(doctor);
        
        // 5. 可预约性（基于当前可预约号源）
        double availabilityScore = calculateAvailabilityScore(doctor);
        
        // 加权求和
        double finalScore = W1_SYMPTOM_MATCH * symptomMatchScore +
                           W2_DEPARTMENT_MATCH * departmentMatchScore +
                           W3_TITLE_SCORE * titleScore +
                           W4_POPULARITY * popularityScore +
                           W5_AVAILABILITY * availabilityScore;
        
        logger.debug("医生 {} 评分详情: 症状={}, 科室={}, 职称={}, 热度={}, 可预约={}, 总分={}",
                doctor.getFullName(), symptomMatchScore, departmentMatchScore, 
                titleScore, popularityScore, availabilityScore, finalScore);
        
        return finalScore;
    }
    
    /**
     * 计算科室匹配度
     */
    private double calculateDepartmentMatchScore(Doctor doctor, List<Department> matchedDepartments) {
        if (matchedDepartments == null || matchedDepartments.isEmpty()) {
            return 0.0;
        }
        
        Integer doctorDepartmentId = doctor.getDepartment().getDepartmentId();
        boolean isMatched = matchedDepartments.stream()
                .anyMatch(dept -> dept.getDepartmentId().equals(doctorDepartmentId));
        
        return isMatched ? 1.0 : 0.0;
    }
    
    /**
     * 计算职称分数
     */
    private double calculateTitleScore(Integer titleLevel) {
        if (titleLevel == null) {
            return 0.5; // 默认值
        }
        
        switch (titleLevel) {
            case 0: return 1.0;  // 主任医师
            case 1: return 0.8;   // 副主任医师
            case 2: return 0.6;   // 主治医师
            default: return 0.5;
        }
    }
    
    /**
     * 计算热度分数（基于预约数量）
     */
    private double calculatePopularityScore(Doctor doctor) {
        try {
            // 统计该医生的总预约数（排除已取消的）
            long appointmentCount = appointmentRepository.findByScheduleDoctorDoctorId(doctor.getDoctorId())
                    .stream()
                    .filter(apt -> apt.getStatus() != null && 
                            !apt.getStatus().name().equals("cancelled"))
                    .count();
            
            // 归一化到0-1（假设最大预约数为1000，可根据实际情况调整）
            double maxAppointments = 1000.0;
            return Math.min(1.0, appointmentCount / maxAppointments);
        } catch (Exception e) {
            logger.warn("计算热度分数失败: {}", e.getMessage());
            return 0.5; // 默认值
        }
    }
    
    /**
     * 计算可预约性分数
     */
    private double calculateAvailabilityScore(Doctor doctor) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(30); // 未来30天
            
            // 查询未来30天的排班
            List<com.example.springboot.entity.Schedule> schedules = 
                    scheduleRepository.findByScheduleDateBetweenAndDoctorIn(
                            today, futureDate, Collections.singletonList(doctor));
            
            if (schedules.isEmpty()) {
                return 0.0;
            }
            
            // 计算平均可预约率
            double totalAvailability = 0.0;
            int count = 0;
            
            for (com.example.springboot.entity.Schedule schedule : schedules) {
                int totalSlots = schedule.getTotalSlots() != null ? schedule.getTotalSlots() : 0;
                int bookedSlots = schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0;
                
                if (totalSlots > 0) {
                    double availability = 1.0 - ((double) bookedSlots / totalSlots);
                    totalAvailability += availability;
                    count++;
                }
            }
            
            return count > 0 ? totalAvailability / count : 0.0;
        } catch (Exception e) {
            logger.warn("计算可预约性分数失败: {}", e.getMessage());
            return 0.5; // 默认值
        }
    }
    
    /**
     * 生成推荐理由
     */
    private String generateRecommendationReason(Doctor doctor, List<String> symptoms) {
        StringBuilder reason = new StringBuilder();
        
        // 症状描述
        String symptomStr = String.join("、", symptoms);
        reason.append("根据您的症状'").append(symptomStr).append("'，");
        
        // 科室信息
        String departmentName = doctor.getDepartment().getName();
        reason.append("推荐").append(doctor.getFullName()).append("医生，");
        reason.append("擅长").append(departmentName).append("相关疾病，");
        
        // 职称信息
        if (doctor.getTitleLevel() != null) {
            switch (doctor.getTitleLevel()) {
                case 0:
                    reason.append("主任医师，经验丰富。");
                    break;
                case 1:
                    reason.append("副主任医师，临床经验丰富。");
                    break;
                case 2:
                    reason.append("主治医师，专业可靠。");
                    break;
                default:
                    reason.append("建议预约。");
            }
        } else {
            reason.append("建议预约。");
        }
        
        return reason.toString();
    }
    
    /**
     * 通过症状匹配科室
     */
    @Transactional(readOnly = true)
    private List<Department> matchDepartmentsBySymptoms(List<String> symptoms) {
        Set<Integer> departmentIds = new HashSet<>();
        
        // 扩展同义词
        List<String> expandedSymptoms = nlpService.expandSynonyms(symptoms);
        
        // 查询所有症状映射
        List<SymptomDepartmentMapping> allMappings = mappingRepository.findAll();
        
        for (SymptomDepartmentMapping mapping : allMappings) {
            String symptomKeywords = mapping.getSymptomKeywords();
            if (symptomKeywords == null || symptomKeywords.trim().isEmpty()) {
                continue;
            }
            
            // 检查是否有匹配的症状
            String[] keywords = symptomKeywords.split("[,，、]");
            for (String keyword : keywords) {
                String trimmedKeyword = keyword.trim();
                if (expandedSymptoms.contains(trimmedKeyword)) {
                    departmentIds.add(mapping.getDepartment().getDepartmentId());
                    break;
                }
            }
        }
        
        // 根据优先级排序科室
        List<Department> departments = new ArrayList<>();
        for (Integer deptId : departmentIds) {
            departmentRepository.findById(deptId).ifPresent(departments::add);
        }
        
        return departments;
    }
    
    /**
     * 内部类：医生推荐结果DTO
     */
    public static class DoctorRecommendation {
        private Integer doctorId;
        private String doctorName;
        private Integer departmentId;
        private String departmentName;
        private String title;
        private Integer titleLevel;
        private String specialty;
        private String photoUrl;
        private Double score;
        private String reason;
        
        // Getters and Setters
        public Integer getDoctorId() { return doctorId; }
        public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }
        
        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
        
        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
        
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Integer getTitleLevel() { return titleLevel; }
        public void setTitleLevel(Integer titleLevel) { this.titleLevel = titleLevel; }
        
        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }
        
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}