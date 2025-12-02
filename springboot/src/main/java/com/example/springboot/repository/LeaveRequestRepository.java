package com.example.springboot.repository;

import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.LeaveRequest;
import com.example.springboot.entity.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    List<LeaveRequest> findByDoctor(Doctor doctor);
    List<LeaveRequest> findByStatus(LeaveRequestStatus status);
    List<LeaveRequest> findByStartTimeBeforeAndEndTimeAfterAndStatus(LocalDateTime end, LocalDateTime start, LeaveRequestStatus status);
    
    /**
     * 查询指定医生列表在指定日期范围内的已批准请假记录
     * @param doctorIds 医生ID列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请假记录列表
     */
    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.doctor.doctorId IN :doctorIds " +
           "AND lr.status = 'approved' " +
           "AND lr.endTime >= :startTime " +
           "AND lr.startTime <= :endTime")
    List<LeaveRequest> findApprovedLeavesByDoctorIdsAndDateRange(
        @Param("doctorIds") List<Integer> doctorIds,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
