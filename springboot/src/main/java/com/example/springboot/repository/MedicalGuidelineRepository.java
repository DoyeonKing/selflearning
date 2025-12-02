package com.example.springboot.repository;

import com.example.springboot.entity.MedicalGuideline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalGuidelineRepository extends JpaRepository<MedicalGuideline, Integer> {
    // 带条件的分页查询
    Page<MedicalGuideline> findByTitleContainingAndCategoryAndStatus(
            String keyword, String category, MedicalGuideline.GuidelineStatus status, Pageable pageable);

    Page<MedicalGuideline> findByTitleContainingAndCategory(
            String keyword, String category, Pageable pageable);

    Page<MedicalGuideline> findByTitleContainingAndStatus(
            String keyword, MedicalGuideline.GuidelineStatus status, Pageable pageable);

    Page<MedicalGuideline> findByTitleContaining(
            String keyword, Pageable pageable);
}