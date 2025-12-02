package com.example.springboot.repository;

import com.example.springboot.entity.SymptomSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomSynonymRepository extends JpaRepository<SymptomSynonym, Integer> {
    
    /**
     * 根据症状关键词查找所有同义词
     */
    List<SymptomSynonym> findBySymptomKeyword(String symptomKeyword);
    
    /**
     * 根据同义词查找原始关键词
     */
    List<SymptomSynonym> findBySynonym(String synonym);
    
    /**
     * 检查同义词是否存在
     */
    boolean existsBySymptomKeywordAndSynonym(String symptomKeyword, String synonym);
}