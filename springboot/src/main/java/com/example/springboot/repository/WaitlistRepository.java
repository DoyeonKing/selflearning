package com.example.springboot.repository;

import com.example.springboot.entity.Patient;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.Waitlist;
import com.example.springboot.entity.enums.WaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Integer> {
    List<Waitlist> findByScheduleAndStatusOrderByCreatedAtAsc(Schedule schedule, WaitlistStatus status);
    Optional<Waitlist> findByPatientAndScheduleAndStatus(Patient patient, Schedule schedule, WaitlistStatus status);
    boolean existsByPatientAndScheduleAndStatus(Patient patient, Schedule schedule, WaitlistStatus status);
    List<Waitlist> findByCreatedAtBeforeAndStatus(LocalDateTime time, WaitlistStatus status);
    List<Waitlist> findByPatient(Patient patient);
    Page<Waitlist> findByScheduleAndStatusOrderByCreatedAtAsc(Schedule schedule, WaitlistStatus status, Pageable pageable);
    Page<Waitlist> findByScheduleOrderByCreatedAtAsc(Schedule schedule, Pageable pageable);
    long countByScheduleAndStatusAndCreatedAtBefore(Schedule schedule, WaitlistStatus status, LocalDateTime createdAt);
    
    /**
     * 查询超时的候补记录（状态为 notified，且通知发送时间超过指定时间）
     * @param status 候补状态（应为 notified）
     * @param expireTime 过期时间点（当前时间减去15分钟）
     * @return 超时的候补记录列表
     */
    @Query("SELECT w FROM Waitlist w WHERE w.status = :status AND w.notificationSentAt IS NOT NULL AND w.notificationSentAt < :expireTime")
    List<Waitlist> findExpiredNotifiedWaitlists(@Param("status") WaitlistStatus status, @Param("expireTime") LocalDateTime expireTime);
}
