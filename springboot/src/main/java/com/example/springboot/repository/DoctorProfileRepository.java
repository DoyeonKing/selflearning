package com.example.springboot.repository;

import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Integer> {
    
    /**
     * 根据医生查找画像
     */
    Optional<DoctorProfile> findByDoctor(Doctor doctor);
    
    /**
     * 根据医生ID查找画像
     */
    Optional<DoctorProfile> findByDoctorDoctorId(Integer doctorId);
}