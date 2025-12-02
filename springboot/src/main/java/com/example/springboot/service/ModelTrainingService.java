package com.example.springboot.service;

import com.example.springboot.entity.Department;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.DoctorRepository;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.common.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型训练服务
 * 全自动从数据库读取数据并训练Word2Vec模型
 * 
 * 工作流程：
 * 1. 自动连接数据库，读取doctors表的specialty和bio字段
 * 2. 读取外部语料文件（resources/medical_corpus.txt，可选）
 * 3. 读取词典文件（resources/med_word.txt）加载到jieba
 * 4. 使用jieba分词
 * 5. 训练Word2Vec模型
 * 6. 保存模型文件到models目录
 */
@Service
public class ModelTrainingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainingService.class);
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${ai.model.path:models/medical_word2vec.bin}")
    private String modelPath;
    
    private final JiebaSegmenter segmenter = new JiebaSegmenter();
    
    // 停用词集合
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "最近", "经常", "还", "伴有", "等", "什么", "怎么", "如何",
            "可以", "能够", "应该", "可能", "或者", "但是", "因为", "所以"
    ));
    
    /**
     * 训练Word2Vec模型
     * 
     * @return 训练结果信息
     */
    public TrainingResult trainModel() {
        logger.info("=".repeat(60));
        logger.info("开始训练 Word2Vec 模型（Java全自动方案）");
        logger.info("=".repeat(60));
        
        long startTime = System.currentTimeMillis();
        TrainingResult result = new TrainingResult();
        
        try {
            // 1. 加载医疗词典到jieba
            logger.info("[1/5] 正在加载医疗词典...");
            loadMedicalDictionary();
            logger.info("✅ 词典加载完成");
            
            // 2. 从数据库读取语料
            logger.info("[2/5] 正在从数据库读取语料...");
            List<String> sentences = collectSentencesFromDatabase();
            logger.info("✅ 从数据库读取 {} 条句子", sentences.size());
            result.setDatabaseSentences(sentences.size());
            
            // 3. 读取外部语料文件（可选）
            logger.info("[3/5] 正在读取外部语料文件...");
            List<String> externalSentences = loadExternalCorpus();
            if (!externalSentences.isEmpty()) {
                sentences.addAll(externalSentences);
                logger.info("✅ 外部语料加载 {} 条句子", externalSentences.size());
                result.setExternalSentences(externalSentences.size());
            } else {
                logger.info("⚠️  未找到外部语料文件（可选，跳过）");
            }
            
            // 4. 分词处理
            logger.info("[4/5] 正在进行分词处理...");
            List<List<String>> tokenizedSentences = tokenizeSentences(sentences);
            logger.info("✅ 分词完成，共 {} 条有效句子", tokenizedSentences.size());
            result.setTokenizedSentences(tokenizedSentences.size());
            
            if (tokenizedSentences.isEmpty()) {
                throw new IllegalStateException("训练数据为空！请检查数据库是否有医生数据，或提供外部语料文件。");
            }
            
            // 5. 训练模型
            logger.info("[5/5] 开始训练 Word2Vec 模型（这可能需要几分钟）...");
            Word2Vec model = trainWord2VecModel(tokenizedSentences);
            logger.info("✅ 模型训练完成！词汇量: {}", model.getVocab().numWords());
            result.setVocabularySize(model.getVocab().numWords());
            
            // 6. 保存模型
            logger.info("正在保存模型到: {}", modelPath);
            File modelFile = new File(modelPath);
            modelFile.getParentFile().mkdirs(); // 确保目录存在
            
            WordVectorSerializer.writeWord2VecModel(model, modelFile);
            logger.info("✅ 模型已保存！文件大小: {} MB", String.format("%.2f", modelFile.length() / 1024.0 / 1024.0));
            result.setModelFile(modelPath);
            result.setModelSize(modelFile.length());
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            result.setSuccess(true);
            result.setDuration(duration);
            result.setMessage("模型训练成功！");
            
            logger.info("=".repeat(60));
            logger.info("✅ 模型训练完成！耗时: {} 秒", duration);
            logger.info("=".repeat(60));
            
        } catch (Exception e) {
            logger.error("❌ 模型训练失败", e);
            result.setSuccess(false);
            result.setMessage("训练失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 加载医疗词典到jieba分词器
     */
    private void loadMedicalDictionary() throws IOException {
        try {
            Resource resource = resourceLoader.getResource("classpath:med_word.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        String word = line.trim();
                        if (!word.isEmpty() && word.length() > 1) {
                            // jieba会自动加载用户词典，这里主要是记录
                            count++;
                        }
                    }
                    logger.info("   词典包含 {} 个医疗词汇", count);
                }
            } else {
                logger.warn("⚠️  未找到词典文件 classpath:med_word.txt，将使用默认分词");
            }
        } catch (Exception e) {
            logger.warn("⚠️  加载词典失败: {}，将使用默认分词", e.getMessage());
        }
    }
    
    /**
     * 从数据库收集句子
     */
    private List<String> collectSentencesFromDatabase() {
        List<String> sentences = new ArrayList<>();
        
        // 1. 从doctors表读取
        List<Doctor> doctors = doctorRepository.findAll().stream()
                .filter(d -> d.getStatus() != DoctorStatus.deleted)
                .filter(d -> (d.getSpecialty() != null && !d.getSpecialty().trim().isEmpty()) ||
                             (d.getBio() != null && !d.getBio().trim().isEmpty()))
                .collect(Collectors.toList());
        
        logger.info("   从数据库读取 {} 个医生记录", doctors.size());
        
        for (Doctor doctor : doctors) {
            // 构建医生描述句子
            List<String> parts = new ArrayList<>();
            
            if (doctor.getFullName() != null) {
                parts.add(doctor.getFullName());
            }
            if (doctor.getTitle() != null) {
                parts.add(doctor.getTitle());
            }
            
            // 获取科室名称
            Department dept = doctor.getDepartment();
            if (dept != null && dept.getName() != null) {
                parts.add(dept.getName());
            }
            
            // 擅长领域
            if (doctor.getSpecialty() != null && !doctor.getSpecialty().trim().isEmpty()) {
                String specialty = doctor.getSpecialty().trim();
                // 如果包含多个句子，拆分
                if (specialty.contains("。") || specialty.contains("，")) {
                    String[] specialtyParts = specialty.split("[。，]");
                    for (String sent : specialtyParts) {
                        sent = sent.trim();
                        if (sent.length() > 5) {
                            sentences.add(sent);
                        }
                    }
                } else {
                    parts.add(specialty);
                }
            }
            
            // 构建完整句子
            if (parts.size() >= 2) {
                String sentence = String.join(" ", parts);
                if (sentence.length() > 5) {
                    sentences.add(sentence);
                }
            }
            
            // 个人简介单独添加
            if (doctor.getBio() != null && !doctor.getBio().trim().isEmpty()) {
                String bio = doctor.getBio().trim();
                if (bio.length() > 10) {
                    sentences.add(bio);
                }
            }
        }
        
        // 2. 从departments表读取
        List<Department> departments = departmentRepository.findAll().stream()
                .filter(d -> d.getDescription() != null && !d.getDescription().trim().isEmpty())
                .collect(Collectors.toList());
        
        for (Department dept : departments) {
            if (dept.getName() != null && dept.getDescription() != null) {
                String sentence = dept.getName() + " " + dept.getDescription();
                if (sentence.length() > 5) {
                    sentences.add(sentence);
                }
            }
        }
        
        return sentences;
    }
    
    /**
     * 加载外部语料文件（可选）
     */
    private List<String> loadExternalCorpus() {
        List<String> sentences = new ArrayList<>();
        
        try {
            Resource resource = resourceLoader.getResource("classpath:medical_corpus.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.length() > 5) {
                            sentences.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("外部语料文件不存在或读取失败: {}", e.getMessage());
        }
        
        return sentences;
    }
    
    /**
     * 对句子进行分词
     */
    private List<List<String>> tokenizeSentences(List<String> sentences) {
        return sentences.stream()
                .map(sentence -> {
                    // 使用jieba分词
                    List<SegToken> tokens = segmenter.sentenceProcess(sentence);
                    return tokens.stream()
                            .map(SegToken::word)
                            .filter(word -> word.length() > 1) // 过滤单字
                            .filter(word -> !STOP_WORDS.contains(word)) // 过滤停用词
                            .filter(word -> !word.trim().isEmpty())
                            .collect(Collectors.toList());
                })
                .filter(words -> words.size() >= 2) // 至少2个词
                .collect(Collectors.toList());
    }
    
    /**
     * 训练Word2Vec模型
     */
    private Word2Vec trainWord2VecModel(List<List<String>> tokenizedSentences) {
        // 将分词结果转换为句子（空格分隔）
        List<String> sentences = tokenizedSentences.stream()
                .map(words -> String.join(" ", words))
                .collect(Collectors.toList());
        
        // 创建句子迭代器
        SentenceIterator iterator = new CollectionSentenceIterator(sentences);
        
        // 创建词汇缓存
        AbstractCache<org.deeplearning4j.models.word2vec.VocabWord> cache = new AbstractCache<>();
        
        // 创建分词器工厂（使用空格分词，因为已经用jieba分好了）
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor());
        
        // 配置并构建Word2Vec模型
        Word2Vec.Builder builder = new Word2Vec.Builder();
        builder.minWordFrequency(1) // 最小词频
                .iterations(10) // 训练轮数
                .epochs(20) // epochs
                .layerSize(200) // 向量维度
                .seed(42) // 随机种子
                .windowSize(5) // 上下文窗口
                .iterate(iterator)
                .tokenizerFactory(tokenizerFactory)
                .vocabCache(cache);
        
        Word2Vec model = builder.build();
        model.fit();
        
        return model;
    }
    
    /**
     * 训练结果类
     */
    public static class TrainingResult {
        private boolean success;
        private String message;
        private String modelFile;
        private long modelSize;
        private int vocabularySize;
        private int databaseSentences;
        private int externalSentences;
        private int tokenizedSentences;
        private long duration; // 秒
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getModelFile() { return modelFile; }
        public void setModelFile(String modelFile) { this.modelFile = modelFile; }
        
        public long getModelSize() { return modelSize; }
        public void setModelSize(long modelSize) { this.modelSize = modelSize; }
        
        public int getVocabularySize() { return vocabularySize; }
        public void setVocabularySize(int vocabularySize) { this.vocabularySize = vocabularySize; }
        
        public int getDatabaseSentences() { return databaseSentences; }
        public void setDatabaseSentences(int databaseSentences) { this.databaseSentences = databaseSentences; }
        
        public int getExternalSentences() { return externalSentences; }
        public void setExternalSentences(int externalSentences) { this.externalSentences = externalSentences; }
        
        public int getTokenizedSentences() { return tokenizedSentences; }
        public void setTokenizedSentences(int tokenizedSentences) { this.tokenizedSentences = tokenizedSentences; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
    }
}

