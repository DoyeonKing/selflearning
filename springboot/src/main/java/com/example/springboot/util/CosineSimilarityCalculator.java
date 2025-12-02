package com.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 余弦相似度计算工具类
 */
public class CosineSimilarityCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(CosineSimilarityCalculator.class);
    
    /**
     * 计算两个向量的余弦相似度
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 余弦相似度值（0-1之间）
     */
    public static double cosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }
        
        // 获取所有键的并集
        Set<String> allKeys = new HashSet<>(vector1.keySet());
        allKeys.addAll(vector2.keySet());
        
        if (allKeys.isEmpty()) {
            return 0.0;
        }
        
        // 计算点积
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String key : allKeys) {
            double v1 = vector1.getOrDefault(key, 0.0);
            double v2 = vector2.getOrDefault(key, 0.0);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        // 计算余弦相似度
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0.0) {
            return 0.0;
        }
        
        return dotProduct / denominator;
    }
    
    /**
     * 计算两个词列表的余弦相似度（基于词频）
     * @param list1 词列表1
     * @param list2 词列表2
     * @return 余弦相似度值
     */
    public static double cosineSimilarity(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null || list1.isEmpty() || list2.isEmpty()) {
            return 0.0;
        }
        
        // 构建词频向量
        Map<String, Double> vector1 = buildTermFrequencyVector(list1);
        Map<String, Double> vector2 = buildTermFrequencyVector(list2);
        
        return cosineSimilarity(vector1, vector2);
    }
    
    /**
     * 构建词频向量
     * @param terms 词列表
     * @return 词频向量
     */
    private static Map<String, Double> buildTermFrequencyVector(List<String> terms) {
        Map<String, Double> vector = new HashMap<>();
        
        if (terms == null || terms.isEmpty()) {
            return vector;
        }
        
        int totalTerms = terms.size();
        Map<String, Long> termCount = new HashMap<>();
        
        // 统计词频
        for (String term : terms) {
            termCount.put(term, termCount.getOrDefault(term, 0L) + 1);
        }
        
        // 计算TF（词频）
        for (Map.Entry<String, Long> entry : termCount.entrySet()) {
            vector.put(entry.getKey(), (double) entry.getValue() / totalTerms);
        }
        
        return vector;
    }
}