package com.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TF-IDF 计算工具类
 */
public class TFIDFCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(TFIDFCalculator.class);
    
    /**
     * 计算词频 TF (Term Frequency)
     * @param term 词
     * @param document 文档（词列表）
     * @return TF值
     */
    public static double tf(String term, List<String> document) {
        if (document == null || document.isEmpty()) {
            return 0.0;
        }
        
        long termCount = document.stream()
                .filter(word -> word.equals(term))
                .count();
        
        return (double) termCount / document.size();
    }
    
    /**
     * 计算逆文档频率 IDF (Inverse Document Frequency)
     * @param term 词
     * @param documents 文档集合
     * @return IDF值
     */
    public static double idf(String term, List<List<String>> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }
        
        long docCount = documents.stream()
                .filter(doc -> doc != null && doc.contains(term))
                .count();
        
        if (docCount == 0) {
            return 0.0;
        }
        
        return Math.log((double) documents.size() / docCount);
    }
    
    /**
     * 计算 TF-IDF 值
     * @param term 词
     * @param document 文档
     * @param documents 文档集合
     * @return TF-IDF值
     */
    public static double tfidf(String term, List<String> document, List<List<String>> documents) {
        return tf(term, document) * idf(term, documents);
    }
    
    /**
     * 计算文档的TF-IDF向量
     * @param document 文档（词列表）
     * @param documents 文档集合（用于计算IDF）
     * @param vocabulary 词汇表（所有可能的词）
     * @return TF-IDF向量
     */
    public static Map<String, Double> calculateTFIDFVector(
            List<String> document, 
            List<List<String>> documents, 
            Set<String> vocabulary) {
        
        Map<String, Double> vector = new HashMap<>();
        
        for (String term : vocabulary) {
            double tfidfValue = tfidf(term, document, documents);
            vector.put(term, tfidfValue);
        }
        
        return vector;
    }
    
    /**
     * 从文档集合中提取词汇表
     * @param documents 文档集合
     * @return 词汇表（去重后的所有词）
     */
    public static Set<String> extractVocabulary(List<List<String>> documents) {
        return documents.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}