package com.example.springboot.service.impl;

import com.example.springboot.dto.doctor.DoctorUpdateRequest;
import com.example.springboot.dto.patient.*;
import com.example.springboot.dto.user.UserUpdateRequest;
import com.example.springboot.util.PasswordEncoderUtil;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.example.springboot.dto.admin.AdminResponse;
import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.user.UserCreateRequest;
import com.example.springboot.dto.user.UserImportResponse;
import com.example.springboot.dto.user.UserResponse;
import com.example.springboot.entity.*;
import com.example.springboot.entity.enums.BlacklistStatus;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.*;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.DoctorService;
import com.example.springboot.service.PatientService;
import com.example.springboot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    // private final ClinicRepository clinicRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ScheduleRepository scheduleRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AdminService adminService;
    private final PasswordEncoderUtil passwordEncoderUtil;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 统一账号唯一性校验
        validateIdentifierUniqueness(request.getId());

        // 根据角色创建不同用户
        return switch (request.getRole().toUpperCase()) {
            case "PATIENT" -> createPatientUser(request);
            case "DOCTOR" -> createDoctorUser(request);
            //case "ADMIN" -> createAdminUser(request);
            default -> throw new BadRequestException("不支持的用户角色: " + request.getRole());
        };
    }

    @Override
    @Transactional
    public UserImportResponse importUsers(List<UserCreateRequest> requests) {
        UserImportResponse response = new UserImportResponse();
        response.setTotal(requests.size());

        for (UserCreateRequest request : requests) {
            try {
                // 复用单个创建逻辑
                createUser(request);
                response.setSuccess(response.getSuccess() + 1);
            } catch (Exception e) {
                response.setFailed(response.getFailed() + 1);
                response.getErrorMessages().add(
                        String.format("账号[%s]创建失败: %s", request.getId(), e.getMessage())
                );
            }
        }

        return response;
    }

    private void validateIdentifierUniqueness(String identifier) {
        if (patientRepository.existsByIdentifier(identifier) ||
                doctorRepository.existsByIdentifier(identifier) ||
                adminRepository.existsByUsername(identifier)) {
            throw new BadRequestException("账号已存在: " + identifier);
        }
    }

    private UserResponse createPatientUser(UserCreateRequest request) {
        // 身份证号唯一性校验
        if (request.getId_card() != null &&
                patientProfileRepository.existsByIdCardNumber(request.getId_card())) {
            throw new BadRequestException("身份证号已存在: " + request.getId_card());
        }

        // 创建患者
        Patient patient = new Patient();
        patient.setIdentifier(request.getId());
        patient.setPasswordHash(request.getPassword());
        patient.setFullName(request.getName());
        patient.setPhoneNumber(request.getPhone());
        patient.setPatientType(request.getPatientType());
        patient.setStatus(request.getPatientStatus());

        Patient savedPatient = patientService.createPatient(patient);

        // 创建患者档案
        if (request.getId_card() != null) {
            PatientProfile profile = new PatientProfile();
            profile.setPatient(savedPatient);
            profile.setIdCardNumber(request.getId_card());
            profile.setAllergies(request.getAllergy_history());
            profile.setMedicalHistory(request.getPast_medical_history());
            profile.setBlacklistStatus(BlacklistStatus.normal);
            patientProfileRepository.save(profile);
        }

        UserResponse response = new UserResponse();
        response.setRole("PATIENT");
        response.setUserDetails(patientService.convertToResponseDto(savedPatient));
        return response;
    }

    private UserResponse createDoctorUser(UserCreateRequest request) {
        // 医生实体现在只关联 Department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        // 身份证号唯一性校验（如果提供了身份证号）
        if (request.getId_card() != null &&
                doctorRepository.existsByIdCardNumber(request.getId_card())) {
            throw new BadRequestException("身份证号已存在: " + request.getId_card());
        }

        // 创建医生
        Doctor doctor = new Doctor();
        doctor.setDepartment(department);  // 设置科室关联
        doctor.setIdentifier(request.getId());
        doctor.setPasswordHash(request.getPassword());
        doctor.setFullName(request.getName());
        doctor.setIdCardNumber(request.getId_card());  // 设置身份证号
        doctor.setPhoneNumber(request.getPhone());
        doctor.setTitle(request.getTitle());
        doctor.setSpecialty(request.getSpecialty());
        doctor.setBio(request.getBio());
        doctor.setPhotoUrl(request.getPhotoUrl());
        doctor.setStatus(request.getDoctorStatus());

        Doctor savedDoctor = doctorService.createDoctor(doctor);

        UserResponse response = new UserResponse();
        response.setRole("DOCTOR");
        response.setUserDetails(doctorService.convertToResponseDto(savedDoctor));
        return response;
    }

//    private UserResponse createAdminUser(UserCreateRequest request) {
//        AdminCreateRequest adminRequest = new AdminCreateRequest();
//        adminRequest.setUsername(request.getIdentifier());
//        adminRequest.setPassword(request.getPassword());
//        adminRequest.setFullName(request.getFullName());
//        adminRequest.setStatus(request.getAdminStatus());
//        adminRequest.setRoleIds(request.getRoleIds());
//
//        AdminResponse savedAdmin = adminService.createAdmin(adminRequest);
//
//        UserResponse response = new UserResponse();
//        response.setRole("ADMIN");
//        response.setUserDetails(savedAdmin);
//        return response;
//    }

    // 实现搜索方法
    /**
     * 查询医生函数。
     * 依赖 DoctorRepository 继承 JpaSpecificationExecutor 来支持此处的 findAll 方法。
     */
//    @Override
//    @Transactional(readOnly = true)
//    public PageResponse<UserResponse> searchUsers(String id, String name, int page, int pageSize) {
//        // 构建分页参数 (注意页码从0开始，前端传1时需要减1)
//        Pageable pageable = PageRequest.of(page - 1, pageSize);
//
//        // 1. 查询患者
//        Page<Patient> patientPage = patientRepository.findAll((Specification<Patient>) (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            if (id != null && !id.isEmpty()) {
//                predicates.add(cb.like(root.get("identifier"), "%" + id + "%"));
//            }
//            if (name != null && !name.isEmpty()) {
//                predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
//            }
//            return cb.and(predicates.toArray(new Predicate[0]));
//        }, pageable);
//
//        // 2. 查询医生
//        // 查询医生: 使用 JPA Specification 进行动态查询。
//        Page<Doctor> doctorPage = doctorRepository.findAll((Specification<Doctor>) (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            // 按工号 (identifier) 模糊查询
//            if (id != null && !id.isEmpty()) {
//                predicates.add(cb.like(root.get("identifier"), "%" + id + "%"));
//            }
//            // 按姓名 (fullName) 模糊查询
//            if (name != null && !name.isEmpty()) {
//                predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
//            }
//            return cb.and(predicates.toArray(new Predicate[0]));
//        }, pageable);
//
//        // 3. 查询管理员
//        Page<Admin> adminPage = adminRepository.findAll((Specification<Admin>) (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            if (id != null && !id.isEmpty()) {
//                predicates.add(cb.like(root.get("username"), "%" + id + "%"));
//            }
//            if (name != null && !name.isEmpty()) {
//                predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
//            }
//            return cb.and(predicates.toArray(new Predicate[0]));
//        }, pageable);
//
//        // 4. 合并结果并转换为UserResponse
//        List<UserResponse> userResponses = new ArrayList<>();
//
//        // 添加患者
//        userResponses.addAll(patientPage.getContent().stream()
//                .map(patient -> {
//                    UserResponse response = new UserResponse();
//                    response.setRole("PATIENT");
//                    response.setUserDetails(patientService.convertToResponseDto(patient));
//                    return response;
//                })
//                .collect(Collectors.toList()));
//
//        // 添加医生
//        userResponses.addAll(doctorPage.getContent().stream()
//        // 转换为UserResponse列表
//        List<UserResponse> userResponses = doctorPage.getContent().stream()
//                .map(doctor -> {
//                    UserResponse response = new UserResponse();
//                    response.setRole("DOCTOR");
//                    response.setUserDetails(doctorService.convertToResponseDto(doctor));
//                    return response;
//                })
//                .collect(Collectors.toList()));
//
//        // 添加管理员
//        userResponses.addAll(adminPage.getContent().stream()
//                .map(admin -> {
//                    UserResponse response = new UserResponse();
//                    response.setRole("ADMIN");
//                    response.setUserDetails(adminService.convertToResponseDto(admin));
//                    return response;
//                })
//                .collect(Collectors.toList()));
//
//        // 计算总条数
//        long totalElements = patientPage.getTotalElements() + doctorPage.getTotalElements() + adminPage.getTotalElements();
//        // 计算总页数
//        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
//
//        return new PageResponse<>(
//                userResponses,
//                totalElements,
//                totalPages,
//                page,
//                pageSize
//        );
//    }

// 替换原注释的 searchUsers 方法为 searchDoctors 实现
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchDoctors(String id, String name, Integer departmentId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 调试：先尝试查询所有医生
        long totalDoctors = doctorRepository.count();
        System.out.println("数据库中医生总数: " + totalDoctors);

        // 医生查询条件构建
        Page<Doctor> doctorPage = doctorRepository.findAll((Specification<Doctor>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 排除已删除的记录
            predicates.add(cb.notEqual(root.get("status"), DoctorStatus.deleted));

            // 工号模糊查询
            if (id != null && !id.isEmpty()) {
                predicates.add(cb.like(root.get("identifier"), "%" + id + "%"));
            }

            // 姓名模糊查询
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
            }

            // 科室ID精确查询 - 修复：使用JOIN来避免NPE
            if (departmentId != null) {
                Join<Doctor, Department> departmentJoin = root.join("department", JoinType.LEFT);
                predicates.add(cb.equal(departmentJoin.get("departmentId"), departmentId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        // 调试：输出查询结果
        System.out.println("医生查询结果: " + doctorPage.getTotalElements() + " 条");
        System.out.println("当前页医生数量: " + doctorPage.getContent().size());

        // 转换为响应DTO（确保包含所有字段）
        List<UserResponse> userResponses = doctorPage.getContent().stream()
                .map(doctor -> {
                    UserResponse response = new UserResponse();
                    response.setRole("DOCTOR");
                    response.setUserDetails(doctorService.convertToResponseDto(doctor));
                    return response;
                })
                .collect(Collectors.toList());

        // 构建分页响应
        return new PageResponse<>(
                userResponses,
                doctorPage.getTotalElements(),
                doctorPage.getTotalPages(),
                page,
                pageSize
        );
    }


    /**
     * 实现 UserService 接口中的抽象方法 searchPatients。
     * 查询患者函数。
     * 依赖 PatientRepository 继承 JpaSpecificationExecutor 来支持此处的 findAll 方法。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchPatients(String id, String name, int page, int pageSize) {
        // 构建分页参数 (页码从0开始)
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 调试：先尝试查询所有患者
        long totalPatients = patientRepository.count();
        System.out.println("数据库中患者总数: " + totalPatients);

        // 查询患者: 使用 JPA Specification 进行动态查询。
        Page<Patient> patientPage = patientRepository.findAll((Specification<Patient>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 排除已删除的记录
            predicates.add(cb.notEqual(root.get("status"), PatientStatus.deleted));
            
            // 按学号/工号/ID (identifier) 模糊查询
            if (id != null && !id.isEmpty()) {
                predicates.add(cb.like(root.get("identifier"), "%" + id + "%"));
            }
            // 按姓名 (fullName) 模糊查询
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        // 调试：输出查询结果
        System.out.println("患者查询结果: " + patientPage.getTotalElements() + " 条");
        System.out.println("当前页患者数量: " + patientPage.getContent().size());

        // 转换为UserResponse列表
        List<UserResponse> userResponses = patientPage.getContent().stream()
                .map(patient -> {
                    UserResponse response = new UserResponse();
                    response.setRole("PATIENT");
                    // 假设 patientService 提供了 convertToResponseDto(Patient) 方法
                    response.setUserDetails(patientService.convertToResponseDto(patient));
                    return response;
                })
                .collect(Collectors.toList());

        // 构建分页响应
        return new PageResponse<>(
                userResponses,
                patientPage.getTotalElements(),
                patientPage.getTotalPages(),
                page,
                pageSize
        );
    }


    @Override
    @Transactional
    public UserResponse updateUser(Long patient_id, Integer doctor_id, UserUpdateRequest request) {  // 参数类型改为Integer
        // 根据角色更新不同类型的用户
        return switch (request.getRole().toUpperCase()) {
            case "PATIENT" -> updatePatientUser(patient_id, request.getPatientUpdateRequest());
            case "DOCTOR" -> updateDoctorUser(doctor_id, request.getDoctorUpdateRequest());
            default -> throw new BadRequestException("不支持的用户角色: " + request.getRole());
        };
    }

    /**
     * 修复错误 1: 实现 UserService 接口中的抽象方法 getMedicalHistories
     * 将查询任务委托给 PatientService。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MedicalHistoryResponse> getMedicalHistories(Integer page, Integer pageSize) {
        // 委托给 patientService
        return patientService.getMedicalHistories(page, pageSize);
    }

    /**
     * 修复错误 3: 实现 UserService 接口中的抽象方法 updateMedicalHistory
     * 暂时抛出异常，直到 PatientService 接口中也添加了对应的方法，以消除编译错误。
     */
    @Override
    @Transactional
    public MedicalHistoryResponse updateMedicalHistory(Long patientId, MedicalHistoryUpdateRequest request) {
        // 1. 查找患者
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("患者不存在，ID: " + patientId));

        // 2. 查找或创建患者档案
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseGet(() -> {
                    PatientProfile newProfile = new PatientProfile();
                    newProfile.setPatient(patient);
                    return newProfile;
                });

        // 3. 更新病史信息
        if (request.getPastMedicalHistory() != null) {
            profile.setMedicalHistory(request.getPastMedicalHistory());
        }
        if (request.getAllergyHistory() != null) {
            profile.setAllergies(request.getAllergyHistory());
        }
        if (request.getBlacklistStatus() != null) {
            profile.setBlacklistStatus(request.getBlacklistStatus());
        }

        // 4. 保存更新后的档案
        PatientProfile updatedProfile = patientProfileRepository.save(profile);

        // 5. 转换为响应DTO
        MedicalHistoryResponse response = new MedicalHistoryResponse();
        response.setId(patient.getPatientId());
        response.setName(patient.getFullName());
        response.setIdCard(updatedProfile.getIdCardNumber());
        response.setPastMedicalHistory(updatedProfile.getMedicalHistory());
        response.setAllergyHistory(updatedProfile.getAllergies());
        response.setIsBlacklisted(updatedProfile.getBlacklistStatus() == BlacklistStatus.blacklisted);

        return response;
    }

    // 修改患者更新方法
    private UserResponse updatePatientUser(Long patientId, PatientUpdateRequest request) {
        // 查找患者（使用ID而非identifier）
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("患者不存在: " + patientId));

        // 更新患者信息
        if (request.getIdentifier() != null) {
            patient.setIdentifier(request.getIdentifier());
        }
        if (request.getPatientType() != null) {
            patient.setPatientType(request.getPatientType());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            patient.setPasswordHash(passwordEncoderUtil.encodePassword(request.getPassword()));
        }
        if (request.getFullName() != null) {
            patient.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            patient.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            // 确保状态值有效
            try {
                String statusStr = request.getStatus().trim();
                String lowerStatus = statusStr.toLowerCase();
                System.out.println("原始状态值: " + request.getStatus());
                System.out.println("处理后状态值: " + lowerStatus);
                // 直接使用小写，因为枚举定义就是小写
                PatientStatus status = PatientStatus.valueOf(lowerStatus);
                patient.setStatus(status);
                System.out.println("成功设置状态: " + status);
            } catch (Exception e) {
                System.out.println("状态转换失败: " + e.getMessage());
                throw new BadRequestException("无效的状态值: " + request.getStatus() + ", 有效值: active, inactive, locked, deleted");
            }
        }

        Patient updatedPatient = patientRepository.save(patient);

        // 更新患者档案
        if (request.getIdCardNumber() != null || request.getAllergies() != null || request.getMedicalHistory() != null) {
            PatientProfile profile = patientProfileRepository.findById(patientId)
                    .orElse(new PatientProfile());

            profile.setPatient(updatedPatient);
            if (request.getIdCardNumber() != null) {
                profile.setIdCardNumber(request.getIdCardNumber());
            }
            if (request.getAllergies() != null) {
                profile.setAllergies(request.getAllergies());
            }
            if (request.getMedicalHistory() != null) {
                profile.setMedicalHistory(request.getMedicalHistory());
            }

            patientProfileRepository.save(profile);
        }

        UserResponse response = new UserResponse();
        response.setRole("PATIENT");
        response.setUserDetails(patientService.convertToResponseDto(updatedPatient));
        return response;
    }

    // 修改医生更新方法
    private UserResponse updateDoctorUser(Integer doctorId, DoctorUpdateRequest request) {
        // 查找医生（使用ID而非identifier）
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("医生不存在: " + doctorId));

        // 关键：确保这里没有任何对 clinic 的引用
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("科室不存在: " + request.getDepartmentId()));
            doctor.setDepartment(department);
        }

        // 更新医生基本信息
        if (request.getIdentifier() != null) {
            doctor.setIdentifier(request.getIdentifier());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            doctor.setPasswordHash(passwordEncoderUtil.encodePassword(request.getPassword()));
        }
        if (request.getFullName() != null) {
            doctor.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            doctor.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getTitle() != null) {
            doctor.setTitle(request.getTitle());
        }
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(request.getSpecialty());
        }
        if (request.getBio() != null) {
            doctor.setBio(request.getBio());
        }
        if (request.getPhotoUrl() != null) {
            doctor.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getStatus() != null) {
            doctor.setStatus(request.getStatus());
        }


        Doctor updatedDoctor = doctorRepository.save(doctor);

        UserResponse response = new UserResponse();
        response.setRole("DOCTOR");
        response.setUserDetails(doctorService.convertToResponseDto(updatedDoctor));
        return response;
    }

    @Override
    @Transactional
    public void softDeleteUser(Long userId, String role) {
        if ("DOCTOR".equalsIgnoreCase(role)) {
            Doctor doctor = doctorRepository.findById(userId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("医生不存在"));
            
            // 检查是否已经删除
            if (doctor.getStatus() == DoctorStatus.deleted) {
                throw new BadRequestException("该医生已被删除");
            }
            
            // 检查是否有未来的排班
            long futureScheduleCount = scheduleRepository.countFutureSchedulesByDoctor(
                doctor,
                java.time.LocalDate.now()
            );
            if (futureScheduleCount > 0) {
                throw new BadRequestException("该医生有 " + futureScheduleCount + " 个未来的排班，无法删除。请先删除或调整相关排班。");
            }
            
            // 软删除
            doctor.setStatus(DoctorStatus.deleted);
            doctorRepository.save(doctor);
            
        } else if ("PATIENT".equalsIgnoreCase(role)) {
            Patient patient = patientRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("患者不存在"));
            
            // 检查是否已经删除
            if (patient.getStatus() == PatientStatus.deleted) {
                throw new BadRequestException("该患者已被删除");
            }
            
            // 检查是否有未完成的预约（可选的前置检查）
            // 这里可以添加更复杂的业务逻辑检查
            
            // 软删除
            patient.setStatus(PatientStatus.deleted);
            patientRepository.save(patient);
            
        } else {
            throw new BadRequestException("不支持的用户角色: " + role);
        }
    }
}