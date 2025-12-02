package com.example.springboot.service;

import com.example.springboot.dto.slotapplication.SlotApplicationCreateRequest;
import com.example.springboot.dto.slotapplication.SlotApplicationResponse;
import com.example.springboot.dto.slotapplication.SlotApplicationUpdateRequest;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.*;
import com.example.springboot.entity.enums.SlotApplicationStatus;
import com.example.springboot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加号申请服务类
 */
@Service
public class SlotApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SlotApplicationService.class);

    private final SlotApplicationRepository slotApplicationRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final AdminRepository adminRepository;
    private AddOnSlotService addOnSlotService; // 延迟注入，避免循环依赖

    @Autowired
    public SlotApplicationService(
            SlotApplicationRepository slotApplicationRepository,
            DoctorRepository doctorRepository,
            ScheduleRepository scheduleRepository,
            PatientRepository patientRepository,
            AdminRepository adminRepository) {
        this.slotApplicationRepository = slotApplicationRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.patientRepository = patientRepository;
        this.adminRepository = adminRepository;
    }
    
    @Autowired
    public void setAddOnSlotService(AddOnSlotService addOnSlotService) {
        this.addOnSlotService = addOnSlotService;
    }

    /**
     * 创建加号申请
     */
    @Transactional
    public SlotApplicationResponse createSlotApplication(SlotApplicationCreateRequest request) {
        // 验证医生是否存在
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("医生不存在"));

        // 验证排班是否存在且属于该医生
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("排班不存在"));

        if (!schedule.getDoctor().getDoctorId().equals(request.getDoctorId())) {
            throw new RuntimeException("该排班不属于当前医生");
        }

        // 验证患者是否存在
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("患者不存在"));

        // 创建申请
        SlotApplication application = new SlotApplication();
        application.setDoctorId(request.getDoctorId());
        application.setScheduleId(request.getScheduleId());
        application.setAddedSlots(request.getAddedSlots());
        application.setPatientId(request.getPatientId());
        application.setUrgencyLevel(request.getUrgencyLevel());
        application.setReason(request.getReason());
        application.setStatus(SlotApplicationStatus.PENDING);

        SlotApplication saved = slotApplicationRepository.save(application);
        return convertToResponse(saved);
    }

    /**
     * 获取医生的所有加号申请
     */
    public List<SlotApplicationResponse> getApplicationsByDoctor(Integer doctorId) {
        List<SlotApplication> applications = slotApplicationRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取医生的指定状态的加号申请
     */
    public List<SlotApplicationResponse> getApplicationsByDoctorAndStatus(Integer doctorId, SlotApplicationStatus status) {
        List<SlotApplication> applications = slotApplicationRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, status);
        return applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取加号申请
     */
    public SlotApplicationResponse getApplicationById(Integer id) {
        SlotApplication application = slotApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("加号申请不存在"));
        return convertToResponse(application);
    }

    /**
     * 更新加号申请（审批）
     * 如果批准，则调用AddOnSlotService处理完整流程
     */
    @Transactional
    public SlotApplicationResponse updateSlotApplication(Integer id, SlotApplicationUpdateRequest request) {
        SlotApplication application = slotApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("加号申请不存在"));

        logger.info("更新加号申请 - applicationId: {}, 新状态: {}", id, request.getStatus());

        // 如果是批准操作，调用AddOnSlotService处理完整流程
        if (request.getStatus() == SlotApplicationStatus.APPROVED) {
            logger.info("批准加号申请，开始处理完整流程");
            
            // 调用AddOnSlotService处理：创建虚拟号源、生成预约、发送通知
            Appointment appointment = addOnSlotService.processApprovedAddOn(
                    application, 
                    request.getApproverId()
            );
            
            logger.info("加号审批通过处理完成 - appointmentId: {}", appointment.getAppointmentId());
            
            // 重新从数据库加载更新后的申请信息
            SlotApplication updated = slotApplicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("加号申请不存在"));
            return convertToResponse(updated);
        }

        // 其他状态（拒绝等）的常规处理
        application.setStatus(request.getStatus());
        application.setApproverId(request.getApproverId());
        application.setApproverComments(request.getApproverComments());

        if (request.getStatus() == SlotApplicationStatus.REJECTED) {
            application.setApprovedAt(LocalDateTime.now());
        }

        SlotApplication updated = slotApplicationRepository.save(application);
        return convertToResponse(updated);
    }

    /**
     * 取消加号申请
     */
    @Transactional
    public SlotApplicationResponse cancelSlotApplication(Integer id, Integer doctorId) {
        SlotApplication application = slotApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("加号申请不存在"));

        // 验证是否是申请人本人
        if (!application.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("无权取消此申请");
        }

        // 只有待审批状态才能取消
        if (application.getStatus() != SlotApplicationStatus.PENDING) {
            throw new RuntimeException("只有待审批的申请才能取消");
        }

        application.setStatus(SlotApplicationStatus.CANCELLED);
        SlotApplication updated = slotApplicationRepository.save(application);
        return convertToResponse(updated);
    }

    /**
     * 获取所有加号申请（管理员用）
     */
    public List<SlotApplicationResponse> getAllApplications() {
        List<SlotApplication> applications = slotApplicationRepository.findAll();
        return applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取加号申请（管理员用）
     */
    public List<SlotApplicationResponse> getApplicationsByStatus(SlotApplicationStatus status) {
        List<SlotApplication> applications = slotApplicationRepository.findByStatusOrderByCreatedAtDesc(status);
        return applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private SlotApplicationResponse convertToResponse(SlotApplication application) {
        SlotApplicationResponse response = new SlotApplicationResponse();
        response.setApplicationId(application.getApplicationId());
        response.setDoctorId(application.getDoctorId());
        response.setScheduleId(application.getScheduleId());
        response.setAddedSlots(application.getAddedSlots());
        response.setPatientId(application.getPatientId());
        response.setUrgencyLevel(application.getUrgencyLevel());
        response.setReason(application.getReason());
        response.setStatus(application.getStatus());
        response.setApproverId(application.getApproverId());
        response.setApproverComments(application.getApproverComments());
        response.setApprovedAt(application.getApprovedAt());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        // 获取医生信息
        doctorRepository.findById(application.getDoctorId()).ifPresent(doctor -> {
            response.setDoctorName(doctor.getFullName());
        });

        // 获取排班信息
        scheduleRepository.findById(application.getScheduleId()).ifPresent(schedule -> {
            response.setScheduleDate(schedule.getScheduleDate().toString());
            
            // 从TimeSlot获取时间信息
            if (schedule.getSlot() != null) {
                response.setStartTime(schedule.getSlot().getStartTime().toString());
                response.setEndTime(schedule.getSlot().getEndTime().toString());
                
                // 判断时段
                LocalTime startTime = schedule.getSlot().getStartTime();
                response.setTimeSlot(startTime.getHour() < 12 ? "MORNING" : "AFTERNOON");
            }
            
            // 从Location获取地点信息
            if (schedule.getLocation() != null) {
                response.setLocation(schedule.getLocation().getLocationName());
            }
        });

        // 获取患者信息
        patientRepository.findById(application.getPatientId()).ifPresent(patient -> {
            response.setPatientName(patient.getFullName());
            response.setPatientPhone(patient.getPhoneNumber());
        });

        // 获取审批人信息
        if (application.getApproverId() != null) {
            adminRepository.findById(application.getApproverId()).ifPresent(admin -> {
                response.setApproverName(admin.getFullName());
            });
        }

        return response;
    }
}
