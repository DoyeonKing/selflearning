package com.example.springboot.repository;

import com.example.springboot.entity.SlotApplication;
import com.example.springboot.entity.enums.SlotApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 加号申请Repository
 */
@Repository
public interface SlotApplicationRepository extends JpaRepository<SlotApplication, Integer> {

    /**
     * 根据医生ID查询加号申请
     */
    List<SlotApplication> findByDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    /**
     * 根据医生ID和状态查询加号申请
     */
    List<SlotApplication> findByDoctorIdAndStatusOrderByCreatedAtDesc(Integer doctorId, SlotApplicationStatus status);

    /**
     * 根据状态查询加号申请
     */
    List<SlotApplication> findByStatusOrderByCreatedAtDesc(SlotApplicationStatus status);

    /**
     * 根据患者ID查询加号申请
     */
    List<SlotApplication> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    /**
     * 根据排班ID查询加号申请
     */
    List<SlotApplication> findByScheduleIdOrderByCreatedAtDesc(Integer scheduleId);
}
