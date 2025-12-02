package com.example.springboot.repository;

import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Patient;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.AppointmentType;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByPatient(Patient patient);
    List<Appointment> findBySchedule(Schedule schedule);
    boolean existsByPatientAndSchedule(Patient patient, Schedule schedule);
    
    /**
     * 检查患者是否有该排班的有效预约（排除已取消的预约）
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient = :patient AND a.schedule = :schedule AND a.status != com.example.springboot.entity.enums.AppointmentStatus.cancelled")
    boolean existsByPatientAndScheduleAndStatusNotCancelled(@Param("patient") Patient patient, @Param("schedule") Schedule schedule);
    
    /**
     * 统计排班的有效预约数（排除已取消的预约）
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.schedule = :schedule AND a.status != com.example.springboot.entity.enums.AppointmentStatus.cancelled")
    long countByScheduleAndStatusNotCancelled(@Param("schedule") Schedule schedule);
    long countBySchedule(Schedule schedule);
    List<Appointment> findByScheduleScheduleDateAndScheduleSlotStartTimeBeforeAndStatus(
            java.time.LocalDate scheduleDate, java.time.LocalTime checkInTime, com.example.springboot.entity.enums.AppointmentStatus status);
    // 添加复杂查询方法 - 查询患者所有未完成、未取消的预约（排班结束时间还没到的）
    // 判断逻辑：排班日期在今天之后，或者排班日期是今天但结束时间在今天之后
    @Query("SELECT a FROM Appointment a WHERE a.patient = :patient " +
            "AND a.status != com.example.springboot.entity.enums.AppointmentStatus.cancelled " +
            "AND a.status != com.example.springboot.entity.enums.AppointmentStatus.completed " +
            "AND (a.schedule.scheduleDate > :today " +
            "OR (a.schedule.scheduleDate = :today2 AND a.schedule.slot.endTime > :now)) " +
            "ORDER BY a.schedule.scheduleDate ASC, a.schedule.slot.startTime ASC")
    List<Appointment> findByPatientAndScheduleScheduleDateAfterOrScheduleScheduleDateEqualAndScheduleSlotStartTimeAfter(
            @Param("patient") Patient patient,
            @Param("today") LocalDate today,
            @Param("today") LocalDate today2,
            @Param("now") LocalTime now);

    List<Appointment> findByScheduleDoctorDoctorId(Integer doctorId);
    
    /**
     * 根据医生ID和日期查询预约列表（包含患者和患者档案信息）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @return 预约列表
     */
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p " +
           "LEFT JOIN FETCH p.patientProfile " +
           "LEFT JOIN FETCH a.schedule s " +
           "LEFT JOIN FETCH s.doctor " +
           "LEFT JOIN FETCH s.slot " +
           "WHERE s.doctor.doctorId = :doctorId " +
           "AND s.scheduleDate = :scheduleDate " +
           "ORDER BY s.slot.startTime ASC, a.appointmentNumber ASC")
    List<Appointment> findByDoctorIdAndDate(
            @Param("doctorId") Integer doctorId,
            @Param("scheduleDate") LocalDate scheduleDate);

    @Query("SELECT MAX(a.appointmentNumber) FROM Appointment a WHERE a.schedule = :schedule")
    Integer findMaxAppointmentNumberBySchedule(@Param("schedule") Schedule schedule);

    Appointment findTopByScheduleOrderByAppointmentNumberDesc(Schedule schedule);

    @Query(""" 
            SELECT a FROM Appointment a
            JOIN FETCH a.schedule s
            JOIN FETCH s.doctor d
            JOIN FETCH d.department dept
            LEFT JOIN FETCH dept.parentDepartment parent
            LEFT JOIN FETCH s.slot slot
            LEFT JOIN FETCH s.location loc
            WHERE s.scheduleDate BETWEEN :startDate AND :endDate
              AND (:departmentId IS NULL OR dept.departmentId = :departmentId)
              AND (:doctorId IS NULL OR d.doctorId = :doctorId)
              AND a.status = :status
              AND a.checkInTime IS NOT NULL
            """)
    List<Appointment> findAppointmentsForRegistrationHours(
            @Param("departmentId") Integer departmentId,
            @Param("doctorId") Integer doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") com.example.springboot.entity.enums.AppointmentStatus status);
    
    /**
     * 查询某个排班下指定状态的预约（用于叫号队列）
     */
    @Query("SELECT a FROM Appointment a WHERE a.schedule = :schedule AND a.status = :status")
    List<Appointment> findByScheduleAndStatus(
            @Param("schedule") Schedule schedule, 
            @Param("status") com.example.springboot.entity.enums.AppointmentStatus status);
    
    // ===== 加号功能相关查询方法 =====
    
    /**
     * 查询超时未支付的加号预约（参考候补的expireNotifiedWaitlists实现）
     * 用于定时任务检查并取消超时的加号预约
     */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentType = :appointmentType " +
           "AND a.status = :status " +
           "AND a.paymentDeadline IS NOT NULL " +
           "AND a.paymentDeadline < :now")
    List<Appointment> findExpiredAddOnPayments(
            @Param("appointmentType") AppointmentType appointmentType,
            @Param("status") AppointmentStatus status,
            @Param("now") LocalDateTime now);
    
    /**
     * 根据预约类型和状态查询预约列表
     */
    List<Appointment> findByAppointmentTypeAndStatus(
            AppointmentType appointmentType, 
            AppointmentStatus status);
    
    /**
     * 查询指定排班和类型的最大预约序号（用于加号序号分配）
     */
    @Query("SELECT MAX(a.appointmentNumber) FROM Appointment a " +
           "WHERE a.schedule = :schedule AND a.appointmentType = :appointmentType")
    Integer findMaxAppointmentNumberByScheduleAndType(
            @Param("schedule") Schedule schedule,
            @Param("appointmentType") AppointmentType appointmentType);
    
    /**
     * 统计指定排班的待支付加号数量（用于计算实际可用号源）
     */
    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.schedule = :schedule " +
           "AND a.appointmentType = :appointmentType " +
           "AND a.status = :status")
    Integer countByScheduleAndTypeAndStatus(
            @Param("schedule") Schedule schedule,
            @Param("appointmentType") AppointmentType appointmentType,
            @Param("status") AppointmentStatus status);
}
