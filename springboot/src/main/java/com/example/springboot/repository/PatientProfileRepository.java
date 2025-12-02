package com.example.springboot.repository;

import com.example.springboot.entity.Patient;
import com.example.springboot.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


// 将 Long 改为 Integer，并删除不必要的 find/exists 方法
@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    // 无需添加自定义方法，JpaRepository.findById(patientId) 已足够
    boolean existsByIdCardNumber(String idCardNumber);
    Optional<PatientProfile> findByPatient(Patient patient);
}