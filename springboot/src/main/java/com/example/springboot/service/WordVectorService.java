package com.example.springboot.service;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AI 词向量服务引擎
 * 负责加载 Word2Vec 模型并提供向量计算能力
 */
@Service
public class WordVectorService {

    private static final Logger logger = LoggerFactory.getLogger(WordVectorService.class);
    
    private Word2Vec word2VecModel;
    
    @Value("${ai.model.path:models/medical_word2vec.bin}")
    private String modelPath;
    
    @PostConstruct
    public void init() {
        try {
            File modelFile = new File(modelPath);
            
            // 如果配置的路径不存在，尝试查找 .txt 格式
            if (!modelFile.exists()) {
                String txtPath = modelPath.replace(".bin", ".txt");
                File txtFile = new File(txtPath);
                if (txtFile.exists()) {
                    logger.info("📝 找到 .txt 格式模型，使用: {}", txtPath);
                    modelFile = txtFile;
                    modelPath = txtPath;
                }
            }
            
            if (modelFile.exists()) {
                logger.info("⏳ 正在加载 AI 语义模型: {} (预计耗时 2-5 秒)...", modelPath);
                
                // DL4J 的 readWord2VecModel 方法可以自动识别 .bin 和 .txt 格式
                // 对于 .txt 格式，它会自动检测并加载
                try {
                    this.word2VecModel = WordVectorSerializer.readWord2VecModel(modelFile);
                    logger.info("✅ AI 模型加载成功！词汇量: {}", word2VecModel.getVocab().numWords());
                } catch (Exception e) {
                    // 如果 readWord2VecModel 失败，尝试使用 readWord2Vec（专门用于文本格式）
                    if (modelPath.toLowerCase().endsWith(".txt")) {
                        logger.info("尝试使用文本格式加载方法...");
                        this.word2VecModel = WordVectorSerializer.readWord2Vec(modelFile);
                        logger.info("✅ AI 模型加载成功（文本格式）！词汇量: {}", word2VecModel.getVocab().numWords());
                    } else {
                        throw e; // 重新抛出异常
                    }
                }
            } else {
                logger.warn("⚠️ 未找到模型文件: {} (也尝试了 .txt 格式)。系统将自动降级为【纯关键词匹配模式】。", modelPath);
            }
        } catch (Exception e) {
            logger.error("❌ 模型加载失败 (请检查文件格式或依赖版本)", e);
        }
    }

    /**
     * 将分词列表转换为句向量 (Sentence Vector)
     * 算法：平均词向量法 (Averaging Word Vectors)
     */
    public INDArray encodeText(List<String> words) {
        if (word2VecModel == null || words == null || words.isEmpty()) {
            return null;
        }
        
        INDArray sumVector = null;
        int count = 0;
        
        for (String word : words) {
            if (word2VecModel.hasWord(word)) {
                INDArray vector = word2VecModel.getWordVectorMatrix(word);
                if (sumVector == null) {
                    sumVector = vector.dup(); // 复制向量，防止修改模型内部数据
                } else {
                    sumVector.addi(vector);   // 向量累加
                }
                count++;
            }
        }
        
        // 求平均值，生成归一化的句向量
        if (sumVector != null && count > 0) {
            return sumVector.divi(count);
        }
        return null;
    }

    /**
     * 计算两个向量的余弦相似度 (Cosine Similarity)
     * 返回值: 0.0 (不相关) ~ 1.0 (完全一致)
     */
    public double calculateSimilarity(INDArray vec1, INDArray vec2) {
        if (vec1 == null || vec2 == null) return 0.0;
        return Transforms.cosineSim(vec1, vec2);
    }

    /**
     * AI 联想：查找最相似的 N 个词 (用于同义词扩展)
     */
    public Collection<String> findNearestWords(String word, int topN) {
        if (word2VecModel == null || !word2VecModel.hasWord(word)) {
            return Collections.emptyList();
        }
        
        return word2VecModel.wordsNearest(word, topN);
    }
    
    // 服务健康检查
    public boolean isReady() {
        return word2VecModel != null;
    }
}

