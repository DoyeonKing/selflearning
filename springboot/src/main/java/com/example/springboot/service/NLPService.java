package com.example.springboot.service;

import com.example.springboot.entity.SymptomDepartmentMapping;
import com.example.springboot.entity.SymptomSynonym;
import com.example.springboot.entity.Department;
import com.example.springboot.entity.PatientProfile;
import com.example.springboot.repository.SymptomDepartmentMappingRepository;
import com.example.springboot.repository.SymptomSynonymRepository;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.repository.PatientProfileRepository;
import com.example.springboot.repository.AppointmentRepository;
import com.example.springboot.repository.DoctorRepository;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.util.CosineSimilarityCalculator;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NLP处理服务
 * 负责中文分词、症状提取、同义词扩展等
 */
@Service
public class NLPService {
    
    private static final Logger logger = LoggerFactory.getLogger(NLPService.class);
    
    @Autowired
    private SymptomDepartmentMappingRepository mappingRepository;
    
    @Autowired
    private SymptomSynonymRepository synonymRepository;

    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private PatientProfileRepository patientProfileRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private WordVectorService wordVectorService;
    
    private final JiebaSegmenter segmenter = new JiebaSegmenter();
    
    // 停用词集合（可根据需要扩展）
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", 
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", 
            "自己", "这", "最近", "经常", "还", "伴有", "等", "什么", "怎么", "如何"
    ));
    
    /**
     * 从患者描述中提取症状关键词
     * @param description 患者自然语言描述
     * @return 症状关键词列表
     */
    @Transactional(readOnly = true)
    public List<String> extractSymptomKeywords(String description) {
        if (description == null || description.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.debug("开始提取症状关键词，描述: {}", description);
        
        // 1. 文本标准化
        String normalizedText = normalizeText(description);
        
        // 2. 中文分词
        List<SegToken> tokens = segmenter.sentenceProcess(normalizedText);
        List<String> words = tokens.stream()
                .map(SegToken::word)
                .filter(word -> word.length() > 1) // 过滤单字
                .filter(word -> !STOP_WORDS.contains(word)) // 过滤停用词
                .collect(Collectors.toList());
        
        logger.debug("分词结果: {}", words);
        
        // 3. 从symptom_department_mapping表学习症状关键词
        Set<String> symptomKeywords = learnSymptomKeywordsFromMapping();
        
        // 4. 匹配提取症状关键词
        List<String> extractedSymptoms = new ArrayList<>();
        for (String word : words) {
            // 直接匹配
            if (symptomKeywords.contains(word)) {
                extractedSymptoms.add(word);
            } else {
                // 模糊匹配（包含关系）
                for (String symptom : symptomKeywords) {
                    if (symptom.contains(word) || word.contains(symptom)) {
                        extractedSymptoms.add(symptom);
                        break;
                    }
                }
            }
        }
        
        // 去重
        List<String> result = extractedSymptoms.stream()
                .distinct()
                .collect(Collectors.toList());
        
        logger.info("提取的症状关键词: {}", result);
        return result;
    }
    
    /**
     * 升级版同义词扩展：数据库同义词 + AI 联想
     * @param keywords 原始关键词列表
     * @return 扩展后的关键词列表（包含同义词）
     */
    @Transactional(readOnly = true)
    public List<String> expandSynonyms(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> expandedKeywords = new HashSet<>(keywords);
        
        for (String keyword : keywords) {
            // A. 查数据库 (原有逻辑 - 精确映射)
            List<SymptomSynonym> synonyms = synonymRepository.findBySymptomKeyword(keyword);
            synonyms.forEach(s -> expandedKeywords.add(s.getSynonym()));
            
            List<SymptomSynonym> reverseSynonyms = synonymRepository.findBySynonym(keyword);
            reverseSynonyms.forEach(s -> expandedKeywords.add(s.getSymptomKeyword()));
            
            // B. AI 语义联想 (新增逻辑 - 模糊召回)
            if (wordVectorService != null && wordVectorService.isReady()) {
                // 如果是没见过的词，让 AI 找 3 个最相似的词
                Collection<String> aiSynonyms = wordVectorService.findNearestWords(keyword, 3);
                if (!aiSynonyms.isEmpty()) {
                    expandedKeywords.addAll(aiSynonyms);
                    logger.debug("🤖 AI 为 '{}' 联想了: {}", keyword, aiSynonyms);
                }
            }
        }
        
        List<String> result = new ArrayList<>(expandedKeywords);
        logger.debug("同义词扩展(混合模式): {} -> {}", keywords, result);
        return result;
    }
    
    /**
     * 辅助方法：将文本分词并转为列表 (供外部调用)
     */
    public List<String> segmentText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalized = normalizeText(text);
        return segmenter.sentenceProcess(normalized).stream()
                .map(SegToken::word)
                .filter(w -> w.length() > 1 && !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }
    
    /**
     * 计算症状与文本的匹配度 (AI + 规则双轨制)
     * @param symptoms 症状关键词列表
     * @param text 文本（如医生的specialty字段）
     * @return 匹配度（0-1之间）
     */
    public double calculateSymptomMatch(List<String> symptoms, String text) {
        if (symptoms == null || symptoms.isEmpty() || text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        // 场景 A: AI 模型未就绪 -> 降级为原有词频匹配
        if (wordVectorService == null || !wordVectorService.isReady()) {
            List<String> textWords = segmentText(text);
            return CosineSimilarityCalculator.cosineSimilarity(symptoms, textWords);
        }

        // 场景 B: AI 向量匹配
        INDArray symptomVector = wordVectorService.encodeText(symptoms);
        INDArray textVector = wordVectorService.encodeText(segmentText(text));
        
        // 如果向量计算失败，降级到原有方法
        if (symptomVector == null || textVector == null) {
            List<String> textWords = segmentText(text);
            return CosineSimilarityCalculator.cosineSimilarity(symptoms, textWords);
        }
        
        // 计算语义相似度
        double similarity = wordVectorService.calculateSimilarity(symptomVector, textVector);
        
        // 场景 C: 保底策略
        // 如果 AI 判定相似度低 (<0.5)，但存在完全相同的关键词，强制给一个及格分
        List<String> textWords = segmentText(text);
        boolean hasExactMatch = textWords.stream().anyMatch(symptoms::contains);
        if (hasExactMatch && similarity < 0.5) {
            return 0.5; 
        }

        return Math.max(0.0, similarity);
    }
    
    /**
     * 文本标准化
     * @param text 原始文本
     * @return 标准化后的文本
     */
    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        
        // 去除多余空格
        String normalized = text.trim()
                .replaceAll("\\s+", "")
                .replaceAll("，", ",")
                .replaceAll("。", ".")
                .replaceAll("、", ",");
        
        return normalized;
    }
    
    /**
     * 从symptom_department_mapping表学习症状关键词
     * @return 症状关键词集合
     */
    @Transactional(readOnly = true)
    private Set<String> learnSymptomKeywordsFromMapping() {
        List<SymptomDepartmentMapping> mappings = mappingRepository.findAll();
        Set<String> keywords = new HashSet<>();
        
        for (SymptomDepartmentMapping mapping : mappings) {
            String symptomKeywords = mapping.getSymptomKeywords();
            if (symptomKeywords != null && !symptomKeywords.trim().isEmpty()) {
                // 按逗号分割
                String[] parts = symptomKeywords.split("[,，、]");
                for (String part : parts) {
                    String keyword = part.trim();
                    if (!keyword.isEmpty()) {
                        keywords.add(keyword);
                    }
                }
            }
        }
        
        logger.debug("从映射表学习的症状关键词数量: {}", keywords.size());
        return keywords;
    }


/**
 * 科室推荐（第一个流程图）
 * @param symptomDescription 症状描述
 * @param patientId 患者ID（可选）
 * @return 科室推荐列表
 */
@Transactional(readOnly = true)
public List<DepartmentRecommendation> recommendDepartments(String symptomDescription, Long patientId) {
    logger.info("开始科室推荐，症状描述: {}, 患者ID: {}", symptomDescription, patientId);
    
    // 1. 分词与实体识别、症状标准化、生成症状特征
    List<String> symptomKeywords = this.extractSymptomKeywords(symptomDescription);
    
    if (symptomKeywords.isEmpty()) {
        logger.warn("未能提取症状关键词");
        return new ArrayList<>();
    }
    
    // 2. 查询症状-科室映射、匹配多个科室
    List<Department> matchedDepartments = matchDepartmentsBySymptoms(symptomKeywords);
    
    // 3. 计算综合得分
    List<DepartmentRecommendation> recommendations = new ArrayList<>();
    for (Department dept : matchedDepartments) {
        double score = calculateDepartmentScore(dept, symptomKeywords);
        String reason = generateDepartmentReason(dept, symptomKeywords);
        
        DepartmentRecommendation rec = new DepartmentRecommendation();
        rec.setDepartmentId(dept.getDepartmentId());
        rec.setDepartmentName(dept.getName());
        rec.setDescription(dept.getDescription());
        rec.setScore(score);
        rec.setReason(reason);
        
        recommendations.add(rec);
    }
    
    // 4. 读取患者档案
    if (patientId != null) {
        Optional<PatientProfile> profileOpt = patientProfileRepository.findById(patientId);
        if (profileOpt.isPresent()) {
            PatientProfile profile = profileOpt.get();
            String medicalHistory = profile.getMedicalHistory();
            
            // 5. 判断是否有基础病史
            if (medicalHistory != null && !medicalHistory.trim().isEmpty()) {
                // 6. 病史关联分析
                adjustByMedicalHistory(recommendations, medicalHistory, symptomKeywords);
            }
        }
    }
    
    // 按分数排序
    recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
    
    return recommendations;
}

/**
 * 按科室推荐医生（第二个流程图）
 * @param departmentId 科室ID
 * @param patientId 患者ID
 * @param symptomKeywords 症状关键词（可选）
 * @param topN Top-N
 * @return 带排班信息的医生推荐列表
 */
@Transactional(readOnly = true)
public List<DoctorRecommendationWithSchedule> recommendDoctorsByDepartment(
        Integer departmentId, Long patientId, List<String> symptomKeywords, int topN) {
    
    logger.info("开始按科室推荐医生，科室ID: {}, 患者ID: {}", departmentId, patientId);
    
    // 1. 查询科室下活跃医生
    List<Doctor> doctors = doctorRepository.findByDepartmentDepartmentIdAndStatus(
            departmentId, DoctorStatus.active);
    
    if (doctors.isEmpty()) {
        return new ArrayList<>();
    }
    
    // 2. 读取患者数据
    Optional<PatientProfile> profileOpt = patientId != null ? 
            patientProfileRepository.findById(patientId) : Optional.empty();
    
    // 3. 计算医生匹配度
    List<DoctorRecommendationWithSchedule> recommendations = new ArrayList<>();
    for (Doctor doctor : doctors) {
        double matchScore = symptomKeywords != null && !symptomKeywords.isEmpty() ?
                calculateSimilarity(symptomKeywords, doctor) : 0.5;
        
        double titleScore = calculateTitleScore(doctor.getTitleLevel());
        double popularityScore = calculatePopularityScore(doctor);
        
        double initialScore = 0.4 * matchScore + 0.3 * titleScore + 0.3 * popularityScore;
        
        DoctorRecommendationWithSchedule rec = new DoctorRecommendationWithSchedule();
        rec.setDoctorId(doctor.getDoctorId());
        rec.setDoctorName(doctor.getFullName());
        rec.setDepartmentId(doctor.getDepartment().getDepartmentId());
        rec.setDepartmentName(doctor.getDepartment().getName());
        rec.setTitle(doctor.getTitle());
        rec.setTitleLevel(doctor.getTitleLevel());
        rec.setSpecialty(doctor.getSpecialty());
        rec.setPhotoUrl(doctor.getPhotoUrl());
        rec.setScore(initialScore);
        
        recommendations.add(rec);
    }
    
    // 4. 检测有无基础病史
    if (profileOpt.isPresent()) {
        PatientProfile profile = profileOpt.get();
        String medicalHistory = profile.getMedicalHistory();
        
        if (medicalHistory != null && !medicalHistory.trim().isEmpty()) {
            // 5. 按基础病史调整排序
            adjustDoctorRankingByHistory(recommendations, medicalHistory);
        }
    }
    
    // 6. 生成初版推荐列表（已排序）
    recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
    
    // 7. 查询医生排班、关联时段+诊室、计算剩余号源、生成可用排班列表
    enrichWithScheduleInfo(recommendations);
    
    // 8. 生成终版医生推荐列表
    List<DoctorRecommendationWithSchedule> result = recommendations.stream()
            .limit(topN)
            .collect(Collectors.toList());
    
    return result;
}

/**
 * 病史关联分析（调整科室推荐）
 */
private void adjustByMedicalHistory(List<DepartmentRecommendation> recommendations, 
                                    String medicalHistory, List<String> symptoms) {
    // 从病史中提取关键词，如果与当前症状相关，提升相关科室的分数
    List<String> historyKeywords = this.extractSymptomKeywords(medicalHistory);
    
    for (DepartmentRecommendation rec : recommendations) {
        // 检查该科室是否与病史相关
        Department dept = departmentRepository.findById(rec.getDepartmentId()).orElse(null);
        if (dept != null) {
            // 如果病史关键词与症状关键词有重叠，提升分数
            long overlap = historyKeywords.stream()
                    .filter(symptoms::contains)
                    .count();
            
            if (overlap > 0) {
                rec.setScore(rec.getScore() * 1.2); // 提升20%
                rec.setReason(rec.getReason() + "（根据您的病史，该科室更适合）");
            }
        }
    }
}

/**
 * 按基础病史调整医生排序
 */
private void adjustDoctorRankingByHistory(List<DoctorRecommendationWithSchedule> recommendations, 
                                          String medicalHistory) {
    List<String> historyKeywords = this.extractSymptomKeywords(medicalHistory);
    
    for (DoctorRecommendationWithSchedule rec : recommendations) {
        Doctor doctor = doctorRepository.findById(rec.getDoctorId()).orElse(null);
        if (doctor != null && doctor.getSpecialty() != null) {
            // 检查医生专长是否与病史相关
            double historyMatch = this.calculateSymptomMatch(historyKeywords, doctor.getSpecialty());
            if (historyMatch > 0.3) {
                rec.setScore(rec.getScore() * 1.15); // 提升15%
            }
        }
    }
}

/**
 * 丰富排班信息
 */
private void enrichWithScheduleInfo(List<DoctorRecommendationWithSchedule> recommendations) {
    LocalDate today = LocalDate.now();
    LocalDate futureDate = today.plusDays(30);
    
    for (DoctorRecommendationWithSchedule rec : recommendations) {
        Doctor doctor = doctorRepository.findById(rec.getDoctorId()).orElse(null);
        if (doctor == null) continue;
        
        // 查询未来30天的排班
        List<Schedule> schedules = scheduleRepository.findByScheduleDateBetweenAndDoctorIn(
                today, futureDate, Collections.singletonList(doctor));
        
        List<DoctorRecommendationWithSchedule.ScheduleInfo> scheduleInfos = new ArrayList<>();
        
        for (Schedule schedule : schedules) {
            // 计算剩余号源
            int totalSlots = schedule.getTotalSlots() != null ? schedule.getTotalSlots() : 0;
            int bookedSlots = schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0;
            int availableSlots = Math.max(0, totalSlots - bookedSlots);
            
            if (availableSlots > 0) { // 只返回有号源的排班
                DoctorRecommendationWithSchedule.ScheduleInfo info = 
                        new DoctorRecommendationWithSchedule.ScheduleInfo();
                info.setScheduleId(schedule.getScheduleId());
                info.setScheduleDate(schedule.getScheduleDate());
                if (schedule.getSlot() != null) {
                    info.setStartTime(schedule.getSlot().getStartTime());
                    info.setEndTime(schedule.getSlot().getEndTime());
                }
                info.setTotalSlots(totalSlots);
                info.setBookedSlots(bookedSlots);
                info.setAvailableSlots(availableSlots);
                if (schedule.getLocation() != null) {
                    info.setLocationName(schedule.getLocation().getName());
                }
                
                scheduleInfos.add(info);
            }
        }
        
        rec.setAvailableSchedules(scheduleInfos);
    }
}

/**
 * 计算科室得分
 */
private double calculateDepartmentScore(Department dept, List<String> symptoms) {
    // 基于症状-科室映射的优先级
    List<SymptomDepartmentMapping> mappings = mappingRepository
            .findByDepartmentDepartmentId(dept.getDepartmentId());
    
    double maxPriority = 0.0;
    for (SymptomDepartmentMapping mapping : mappings) {
        String keywords = mapping.getSymptomKeywords();
        if (keywords != null) {
            String[] parts = keywords.split("[,，、]");
            for (String part : parts) {
                if (symptoms.contains(part.trim())) {
                    maxPriority = Math.max(maxPriority, mapping.getPriority() != null ? 
                            mapping.getPriority() : 1.0);
                }
            }
        }
    }
    
    return maxPriority / 10.0; // 归一化到0-1
}

/**
 * 生成科室推荐理由
 */
private String generateDepartmentReason(Department dept, List<String> symptoms) {
    String symptomStr = String.join("、", symptoms);
    return String.format("根据您的症状'%s'，建议挂%s", symptomStr, dept.getName());
}

/**
 * 通过症状匹配科室
 */
@Transactional(readOnly = true)
private List<Department> matchDepartmentsBySymptoms(List<String> symptoms) {
    Set<Integer> departmentIds = new HashSet<>();
    
    // 扩展同义词
    List<String> expandedSymptoms = expandSynonyms(symptoms);
    
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
 * 计算相似度（症状与医生专长）
 */
public double calculateSimilarity(List<String> symptoms, Doctor doctor) {
    if (doctor.getSpecialty() == null || doctor.getSpecialty().trim().isEmpty()) {
        return 0.0;
    }
    
    // 使用NLP服务计算症状与specialty的匹配度
    return calculateSymptomMatch(symptoms, doctor.getSpecialty());
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

// 内部类：科室推荐结果
public static class DepartmentRecommendation {
    private Integer departmentId;
    private String departmentName;
    private String description;
    private Double score;
    private String reason;
    private Integer priority;
    
    // Getters and Setters
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}

// 内部类：带排班信息的医生推荐
public static class DoctorRecommendationWithSchedule {
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
    private List<ScheduleInfo> availableSchedules;
    

}
}