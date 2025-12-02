package com.example.springboot.service;

import com.example.springboot.dto.auth.LoginResponse;
import com.example.springboot.dto.department.DepartmentDTO;
import com.example.springboot.dto.doctor.DoctorActivateRequest;
import com.example.springboot.dto.doctor.DoctorChangePasswordRequest;
import com.example.springboot.dto.doctor.DoctorResponse;
import com.example.springboot.dto.doctor.DoctorUpdateInfoRequest;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.LeaveRequest;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.DoctorRepository;
import com.example.springboot.repository.LeaveRequestRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.util.PasswordEncoderUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import com.example.springboot.entity.Department;
import com.example.springboot.repository.DepartmentRepository;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;
    private final DepartmentRepository departmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DoctorService(DoctorRepository doctorRepository, PasswordEncoderUtil passwordEncoderUtil,
            DepartmentRepository departmentRepository, ScheduleRepository scheduleRepository,
            LeaveRequestRepository leaveRequestRepository) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoderUtil = passwordEncoderUtil;
        this.departmentRepository = departmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }


    // =========================================================================
    // 【医生登录】
    // =========================================================================

    /**
     * 医生登录
     * @param identifier 医生工号
     * @param password 密码
     * @return 登录响应
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String identifier, String password) {
        // 1. 查找医生
        Doctor doctor = doctorRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("工号或密码错误"));

        // 2. 验证密码
        if (!passwordEncoderUtil.matches(password, doctor.getPasswordHash())) {
            throw new IllegalArgumentException("工号或密码错误");
        }

        // 3. 检查账户状态
        if (doctor.getStatus() == DoctorStatus.inactive) {
            throw new IllegalArgumentException("账户未激活，请先激活账户");
        }
        
        if (doctor.getStatus() == DoctorStatus.locked) {
            throw new IllegalArgumentException("账户已被锁定，请联系管理员");
        }
        
        if (doctor.getStatus() == DoctorStatus.deleted) {
            throw new IllegalArgumentException("账户已删除，无法登录");
        }

        // 4. 构建用户信息
        Map<String, Object> doctorInfo = new HashMap<>();
        doctorInfo.put("doctorId", doctor.getDoctorId());
        doctorInfo.put("identifier", doctor.getIdentifier());
        doctorInfo.put("fullName", doctor.getFullName());
        doctorInfo.put("title", doctor.getTitle());
        doctorInfo.put("phoneNumber", doctor.getPhoneNumber());
        doctorInfo.put("departmentName", doctor.getDepartment() != null ? doctor.getDepartment().getName() : null);
        doctorInfo.put("status", doctor.getStatus().name());

        // 5. 返回登录响应
        return LoginResponse.builder()
                .token(null) // 暂不使用token
                .userType("doctor")
                .userInfo(doctorInfo)
                .build();
    }

    // =========================================================================
    // 【账号激活 - 接口 1】验证初始信息和密码
    // =========================================================================

    /**
     * Verifies initial account information: identifier existence, inactive status,
     * and initial password.
     * 
     * @param identifier      Doctor's identifier (work ID).
     * @param initialPassword Initial password provided by the school/hospital.
     */
    @Transactional(readOnly = true)
    public void verifyInitialActivation(String identifier, String initialPassword) {

        // 1. Find doctor record
        Doctor doctor = doctorRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Identifier not found or doctor record not system-registered."));

        // 2. Check: Is the account already active?
        // 假设 DoctorStatus.ACTIVE 表示已激活
        if (doctor.getStatus() == DoctorStatus.active) {
            throw new IllegalArgumentException("Account is already active. Please proceed to login.");
        }

        // 3. Check: Does the initial password match? (Hashing Comparison)
        String storedInitialPasswordHash = doctor.getPasswordHash();

        if (storedInitialPasswordHash == null ||
                !passwordEncoderUtil.matches(initialPassword, storedInitialPasswordHash)) {
            throw new IllegalArgumentException("Initial password verification failed.");
        }
    }

    // =========================================================================
    // 【账号激活 - 接口 2】身份验证与激活 (存储新密码哈希)
    // =========================================================================

    /**
     * Activates the account: performs identity verification (ID card ending), sets
     * the new password, and updates status to ACTIVE.
     * 
     * @param request Contains identifier, ID card ending (last 6 digits), and new
     *                password details.
     */
    @Transactional
    public void activateAccount(DoctorActivateRequest request) {

        // 1. Basic password validation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6
                || request.getNewPassword().length() > 20) {
            throw new IllegalArgumentException("Password length must be between 6 and 20 characters.");
        }

        // 2. Find doctor record
        Doctor doctor = doctorRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor information not found."));

        // 3. Identity Verification (暂时跳过身份证验证，因为数据库中没有该字段)
        // TODO: 如果需要身份证验证，请先在数据库中添加id_card_number字段
        // String storedIdCard = doctor.getIdCardNumber();
        // String requestedEnding = request.getIdCardEnding();
        // ... 身份证验证逻辑

        // 4. Update password and status
        String hashedPassword = passwordEncoderUtil.encodePassword(request.getNewPassword());

        doctor.setPasswordHash(hashedPassword);
        doctor.setStatus(DoctorStatus.active); // Set to active status
        doctorRepository.save(doctor);
    }
    // =========================================================================
    // 【新增核心业务方法】将医生分配到指定科室
    // =========================================================================

    /**
     * 【核心业务方法】将医生分配到指定科室（更新 Doctor.department 字段）。
     * 业务逻辑：根据工号查找医生，如果不存在则创建，无论如何都更新其所属科室和基本信息。
     * 
     * @param departmentId 目标科室的ID
     * @param identifier   医生的工号/ID
     * @param fullName     医生姓名
     * @param title        职称
     * @return 更新后的医生响应 DTO DoctorResponse
     */
    @Transactional
    public DoctorResponse assignDoctorToDepartment(
            Integer departmentId,
            String identifier,
            String fullName,
            String title) {

        // 1. 验证目标科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("科室ID " + departmentId + " 不存在"));

        // 2. 根据工号查找医生
        // 假设 DoctorRepository 中存在 findByIdentifier(String identifier) 方法
        Optional<Doctor> existingDoctor = doctorRepository.findByIdentifier(identifier);

        Doctor doctor;

        if (existingDoctor.isPresent()) {
            // 3a. 医生已存在：更新其信息和科室
            doctor = existingDoctor.get();

            // 业务校验：检查医生是否已经属于该科室
            if (doctor.getDepartment() != null && doctor.getDepartment().getDepartmentId().equals(departmentId)) {
                // 如果已存在于该科室，返回冲突
                throw new RuntimeException("医生 " + identifier + " 已经属于该科室");
            }

            // 更新医生基本信息和所属科室
            doctor.setFullName(fullName);
            doctor.setTitle(title);
            doctor.setDepartment(department); // 【核心操作】

        } else {
            // 3b. 医生不存在：创建新医生实体
            doctor = new Doctor();

            // 赋值前端提供的信息
            doctor.setIdentifier(identifier);
            doctor.setFullName(fullName);
            doctor.setTitle(title);
            doctor.setDepartment(department); // 设置所属科室

            // 处理非空约束字段 (来自 Doctor.java)
            String tempPassword = "DefaultTempPassword";
            doctor.setPasswordHash(passwordEncoderUtil.encodePassword(tempPassword));
            doctor.setStatus(DoctorStatus.inactive); // 默认设置为未激活状态
        }

        // 4. 保存到数据库
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 5. 转换为 DTO 返回
        return convertToResponseDto(savedDoctor);
    }
    // =========================================================================
    // 【新增核心业务方法】删除科室成员
    // =========================================================================

    /**
     * 【核心业务方法】根据工号删除医生。
     * 
     * @param identifier 医生的工号/ID
     */
    @Transactional
    public void deleteDoctorByIdentifier(String identifier) {
        // 1. 查找医生
        Optional<Doctor> doctorOptional = doctorRepository.findByIdentifier(identifier);

        if (doctorOptional.isEmpty()) {
            throw new ResourceNotFoundException("工号为 " + identifier + " 的医生不存在");
        }
        
        Doctor doctor = doctorOptional.get();
        
        // 2. 检查医生是否有未来的排班
        long futureScheduleCount = scheduleRepository.countFutureSchedulesByDoctor(
            doctor,
            LocalDate.now()
        );
        if (futureScheduleCount > 0) {
            throw new RuntimeException("该医生有 " + futureScheduleCount + " 个未来的排班，无法删除。请先删除或调整相关排班。");
        }

        // 3. 执行删除
        doctorRepository.delete(doctor);
    }

    // =========================================================================
    // 原有 CRUD 方法 (更新时增加 idCardNumber 字段的更新)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Doctor> findAllDoctors() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Doctor> findDoctorById(Integer id) {
        return doctorRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Doctor> findDoctorByIdentifier(String identifier) {
        return doctorRepository.findByIdentifier(identifier);
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        // Ensure password is hashed upon creation
        doctor.setPasswordHash(passwordEncoderUtil.encodePassword(doctor.getPasswordHash()));
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor updateDoctor(Integer id, Doctor doctorDetails) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + id));

        // Update fields
        existingDoctor.setDepartment(doctorDetails.getDepartment());
        existingDoctor.setIdentifier(doctorDetails.getIdentifier());

        // TODO: 如果需要身份证号字段，请先在数据库中添加id_card_number字段
        // existingDoctor.setIdCardNumber(doctorDetails.getIdCardNumber());

        if (doctorDetails.getPasswordHash() != null && !doctorDetails.getPasswordHash().isEmpty()) {
            existingDoctor.setPasswordHash(passwordEncoderUtil.encodePassword(doctorDetails.getPasswordHash()));
        }
        existingDoctor.setFullName(doctorDetails.getFullName());
        existingDoctor.setPhoneNumber(doctorDetails.getPhoneNumber());
        existingDoctor.setTitle(doctorDetails.getTitle());
        existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        existingDoctor.setBio(doctorDetails.getBio());
        existingDoctor.setPhotoUrl(doctorDetails.getPhotoUrl());
        existingDoctor.setStatus(doctorDetails.getStatus());

        return doctorRepository.save(existingDoctor);
    }

    @Transactional
    public void deleteDoctor(Integer id) {
        // 1. 查找医生
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + id));
        
        // 2. 检查医生是否有未来的排班
        long futureScheduleCount = scheduleRepository.countFutureSchedulesByDoctor(
            doctor,
            LocalDate.now()
        );
        if (futureScheduleCount > 0) {
            throw new RuntimeException("该医生有 " + futureScheduleCount + " 个未来的排班，无法删除。请先删除或调整相关排班。");
        }
        
        // 3. 执行删除
        doctorRepository.delete(doctor);
    }

    // =========================================================================
    // 【新增核心业务方法】获取指定科室下的所有医生
    // =========================================================================

    /**
     * 【核心业务方法】根据科室ID获取该科室下的所有医生列表
     * 
     * @param departmentId 科室ID
     * @return 该科室下的所有医生列表
     */
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsByDepartmentId(Integer departmentId) {
        // 1. 验证科室是否存在
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("科室ID " + departmentId + " 不存在"));

        // 2. 查询该科室下的所有医生
        List<Doctor> doctors = doctorRepository.findByDepartmentDepartmentId(departmentId);

        // 3. 转换为响应DTO
        return doctors.stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    public DoctorResponse convertToResponseDto(Doctor doctor) {
        DoctorResponse dto = new DoctorResponse();
        BeanUtils.copyProperties(doctor, dto);

        // 处理科室信息转换
        if (doctor.getDepartment() != null) {
            DepartmentDTO deptDto = new DepartmentDTO();
            deptDto.setDepartmentId(doctor.getDepartment().getDepartmentId());
            deptDto.setName(doctor.getDepartment().getName());
            dto.setDepartment(deptDto);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Optional<Doctor> findByIdentifier(String identifier) {
        return doctorRepository.findByIdentifier(identifier);
    }

    @Transactional
    public DoctorResponse updateDoctorInfo(DoctorUpdateInfoRequest request) throws IOException {
        // 1. 根据工号查找医生
        Doctor doctor = doctorRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("医生不存在"));

        // 2. 更新基本信息
        if (request.getPhoneNumber() != null) {
            // 检查手机号是否被其他医生使用
            if (doctorRepository.existsByPhoneNumberAndIdentifierNot(request.getPhoneNumber(), request.getIdentifier())) {
                throw new IllegalArgumentException("手机号已被其他医生使用");
            }
            doctor.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(request.getSpecialty());
        }
        if (request.getBio() != null) {
            doctor.setBio(request.getBio());
        }

        // 3. 处理头像上传
        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            String photoUrl = saveAvatarFile(request.getAvatarFile());
            doctor.setPhotoUrl(photoUrl);
        }

        // 4. 保存更新并返回结果
        Doctor updatedDoctor = doctorRepository.save(doctor);
        return convertToResponseDto(updatedDoctor);
    }

        // 使用绝对路径，避免临时目录问题
        @Value("${file.upload-dir:${user.dir}/images/doctors/}")
        private String uploadDir;

        // 或者明确指定一个固定路径
        // @Value("${file.upload-dir:C:/uploads/images/doctors/}")
        // private String uploadDir;

        private String saveAvatarFile(MultipartFile avatarFile) throws IOException {
            // 1. 验证文件类型和大小
            validateAvatarFile(avatarFile);

            // 2. 生成唯一文件名
            String fileExtension = getFileExtension(avatarFile.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // 3. 创建存储目录（使用绝对路径）
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new IOException("无法创建头像存储目录: " + dir.getAbsolutePath());
                }
                System.out.println("目录创建成功: " + dir.getAbsolutePath());
            }

            // 4. 构建完整的文件路径
            File dest = new File(dir, uniqueFileName);

            // 5. 保存文件
            try {
                avatarFile.transferTo(dest);
                System.out.println("文件保存成功: " + dest.getAbsolutePath());
            } catch (IOException e) {
                throw new IOException("文件保存失败: " + dest.getAbsolutePath() + ", 错误: " + e.getMessage());
            }

            // 6. 返回前端可访问的路径
            return "/images/doctors/" + uniqueFileName;
        }

        private void validateAvatarFile(MultipartFile avatarFile) {
            if (avatarFile == null || avatarFile.isEmpty()) {
                throw new IllegalArgumentException("头像文件不能为空");
            }

            // 验证文件类型
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("只能上传图片文件");
            }

            // 验证文件大小（5MB）
            if (avatarFile.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("头像文件大小不能超过 5MB");
            }

            // 验证文件扩展名
            String originalFilename = avatarFile.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("文件缺少扩展名");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                throw new IllegalArgumentException("不支持的文件格式，请上传 jpg, jpeg, png, gif, bmp 或 webp 格式的图片");
            }
        }

        private String getFileExtension(String filename) {
            if (filename == null || !filename.contains(".")) {
                return ".jpg"; // 默认扩展名
            }
            return filename.substring(filename.lastIndexOf("."));
        }

    // 在DoctorService.java中添加以下方法
    /**
     * 根据医生工号修改密码
     * @param request 包含工号、旧密码、新密码和确认密码的请求对象
     */
    @Transactional
    public void changePassword(DoctorChangePasswordRequest request) {
        // 1. 校验新密码与确认密码一致性
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("新密码与确认密码不一致");
        }

        // 2. 根据工号查询医生信息
        Doctor doctor = doctorRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("医生工号不存在"));

        // 3. 验证旧密码是否正确
        if (!passwordEncoderUtil.matches(request.getOldPassword(), doctor.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码验证失败");
        }

        // 4. 验证新密码与旧密码是否相同（可选，增强安全性）
        if (passwordEncoderUtil.matches(request.getNewPassword(), doctor.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        // 5. 加密新密码并更新
        String newPasswordHash = passwordEncoderUtil.encodePassword(request.getNewPassword());
        doctor.setPasswordHash(newPasswordHash);
        doctorRepository.save(doctor);
    }

    /**
     * 获取医生在指定时间段的排班和请假信息
     * 用于替班医生选择时的悬停显示
     * 
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含排班和请假信息的Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDoctorScheduleAndLeave(Integer doctorId, LocalDate startDate, LocalDate endDate) {
        // 1. 验证医生是否存在
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("医生不存在，ID: " + doctorId));
        
        // 2. 获取排班信息
        List<Schedule> schedules = scheduleRepository.findByDoctorAndScheduleDateBetween(
                doctor, startDate, endDate);
        
        // 3. 获取请假信息
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByDoctor(doctor);
        
        // 4. 过滤请假记录（只保留与查询时间段有交集的记录）
        List<LeaveRequest> filteredLeaves = leaveRequests.stream()
                .filter(leave -> {
                    LocalDate leaveStart = leave.getStartTime().toLocalDate();
                    LocalDate leaveEnd = leave.getEndTime().toLocalDate();
                    // 检查是否有时间交集
                    return !(leaveEnd.isBefore(startDate) || leaveStart.isAfter(endDate));
                })
                .toList();
        
        // 5. 转换为简化的DTO格式
        List<Map<String, Object>> scheduleDTOs = schedules.stream()
                .map(schedule -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("scheduleId", schedule.getScheduleId());
                    dto.put("scheduleDate", schedule.getScheduleDate().toString());
                    dto.put("slotName", schedule.getSlot() != null ? schedule.getSlot().getSlotName() : "");
                    dto.put("startTime", schedule.getSlot() != null ? schedule.getSlot().getStartTime().toString() : "");
                    dto.put("endTime", schedule.getSlot() != null ? schedule.getSlot().getEndTime().toString() : "");
                    dto.put("status", schedule.getStatus().toString());
                    dto.put("bookedSlots", schedule.getBookedSlots());
                    dto.put("totalSlots", schedule.getTotalSlots());
                    return dto;
                })
                .toList();
        
        List<Map<String, Object>> leaveDTOs = filteredLeaves.stream()
                .map(leave -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("requestId", leave.getRequestId());
                    dto.put("startTime", leave.getStartTime().toString());
                    dto.put("endTime", leave.getEndTime().toString());
                    dto.put("status", leave.getStatus().toString());
                    dto.put("reason", leave.getReason());
                    return dto;
                })
                .toList();
        
        // 6. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("schedules", scheduleDTOs);
        result.put("leaves", leaveDTOs);
        result.put("doctorName", doctor.getFullName());
        result.put("doctorTitle", doctor.getTitle());
        
        return result;
    }
}
