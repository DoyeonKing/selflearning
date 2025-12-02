package com.example.springboot.service;

import com.example.springboot.dto.department.DepartmentDTO;
import com.example.springboot.dto.department.DepartmentResponseDTO;
import com.example.springboot.dto.department.DepartmentQueryDTO;
import com.example.springboot.dto.department.DepartmentTreeDTO;
import com.example.springboot.dto.department.DoctorDataDTO;
import com.example.springboot.dto.department.DepartmentDeleteResult;
import com.example.springboot.dto.doctor.DoctorResponse;
import com.example.springboot.entity.Department;
import com.example.springboot.entity.ParentDepartment;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.enums.DoctorStatus;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.ParentDepartmentRepository;
import com.example.springboot.repository.DoctorRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.repository.LocationRepository;
import com.example.springboot.entity.Location;
import com.example.springboot.specifications.DepartmentSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@Transactional
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ParentDepartmentRepository parentDepartmentRepository;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private LocationRepository locationRepository;

    /**
     * 创建新子科室
     * 
     * @param departmentDTO 包含新科室信息的DTO
     * @return 创建成功的科室的响应DTO
     * @throws RuntimeException 如果科室名称已存在或父科室不存在
     */
    @Transactional
    public DepartmentResponseDTO createDepartment(DepartmentDTO departmentDTO) {
        // 1. 检查科室名称是否已存在
        if (departmentRepository.existsByName(departmentDTO.getName())) {
            throw new RuntimeException("科室名称已存在: " + departmentDTO.getName());
        }

        // 2. 查找父科室
        String parentName = departmentDTO.getParentDepartmentName();
        if (parentName == null || parentName.trim().isEmpty()) {
            throw new RuntimeException("必须指定父科室");
        }
        
        ParentDepartment parentDepartment = parentDepartmentRepository.findByName(parentName)
                .orElseThrow(() -> new RuntimeException("父科室不存在: " + parentName));

        // 3. 创建新科室
        Department newDepartment = new Department(parentDepartment, departmentDTO.getName(), departmentDTO.getDescription());
        Department savedDepartment = departmentRepository.save(newDepartment);

        // 4. 转换为响应DTO
        return convertToResponseDTO(savedDepartment);
    }

    /**
     * 编辑科室的描述信息
     * 
     * @param departmentDTO 包含科室名称和新的描述
     * @return 更新成功的科室的响应DTO
     * @throws RuntimeException 如果科室不存在
     */
    @Transactional
    public DepartmentResponseDTO updateDepartmentDescription(DepartmentDTO departmentDTO) {
        String departmentName = departmentDTO.getName();
        String newDescription = departmentDTO.getDescription();

        // 1. 通过名称查找科室
        Department departmentToUpdate = departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new RuntimeException("要编辑的科室不存在: " + departmentName));

        // 2. 更新描述信息
        departmentToUpdate.setDescription(newDescription);

        // 3. 保存更新
        Department updatedDepartment = departmentRepository.save(departmentToUpdate);

        // 4. 转换为响应DTO
        return convertToResponseDTO(updatedDepartment);
    }

    /**
     * 删除科室（支持非空科室删除）
     * 
     * @param departmentId 科室ID
     * @return 删除结果信息
     */
    @Transactional
    public DepartmentDeleteResult deleteDepartment(Integer departmentId) {
        try {
            System.out.println("=== 开始删除科室: " + departmentId + " ===");
            
            // 1. 查找科室
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("科室不存在: " + departmentId));
            System.out.println("找到科室: " + department.getName());

            // 2. 检查是否为未分配科室（ID=999），禁止删除
            if (departmentId.equals(999)) {
                throw new RuntimeException("不能删除未分配科室");
            }
            
            // 3. 检查科室是否有未来的排班
            long futureScheduleCount = scheduleRepository.countFutureSchedulesByDepartment(
                departmentId, 
                LocalDate.now()
            );
            if (futureScheduleCount > 0) {
                throw new RuntimeException("该科室有 " + futureScheduleCount + " 个未来的排班，无法删除。请先删除或调整相关排班。");
            }

            // 4. 查询该科室下的医生和地点
            List<Doctor> doctorsInDepartment = doctorRepository.findByDepartmentDepartmentId(departmentId);
            List<Location> locationsInDepartment = locationRepository.findByDepartmentDepartmentId(departmentId);
            System.out.println("科室 " + departmentId + " 下有 " + doctorsInDepartment.size() + " 位医生");
            System.out.println("科室 " + departmentId + " 下有 " + locationsInDepartment.size() + " 个地点");
            
            DepartmentDeleteResult result = new DepartmentDeleteResult();
            result.setDepartmentId(departmentId);
            result.setDepartmentName(department.getName());
            result.setDoctorCount(doctorsInDepartment.size());
            result.setMovedDoctors(new ArrayList<>());
            result.setLocationCount(locationsInDepartment.size());
            result.setMovedLocations(new ArrayList<>());

            // 5. 如果有医生，先将他们移动到未分配科室
            if (!doctorsInDepartment.isEmpty()) {
                Department unassignedDepartment = departmentRepository.findById(999)
                        .orElseThrow(() -> new RuntimeException("未分配科室不存在，请先执行数据库初始化脚本"));
                System.out.println("找到未分配科室: " + unassignedDepartment.getName());

                for (Doctor doctor : doctorsInDepartment) {
                    System.out.println("移动医生: " + doctor.getIdentifier() + " - " + doctor.getFullName());
                    doctor.setDepartment(unassignedDepartment);
                    result.getMovedDoctors().add(doctor.getIdentifier());
                }
                
                // 批量保存医生
                doctorRepository.saveAll(doctorsInDepartment);
                System.out.println("医生移动完成");
            }

            // 6. 如果有地点，先将它们设置为未分配（department_id = NULL）
            if (!locationsInDepartment.isEmpty()) {
                System.out.println("开始处理地点...");
                
                for (Location location : locationsInDepartment) {
                    System.out.println("解除地点绑定: " + location.getLocationName());
                    location.setDepartment(null);  // 设置为未分配
                    result.getMovedLocations().add(location.getLocationName());
                }
                
                // 批量保存地点
                locationRepository.saveAll(locationsInDepartment);
                System.out.println("地点处理完成");
            }
            
            // 6.5 处理症状科室映射表（symptom_department_mapping）
            try {
                int updatedMappings = departmentRepository.updateSymptomDepartmentMappings(departmentId, 999);
                System.out.println("更新了 " + updatedMappings + " 条症状映射记录到未分配科室");
            } catch (Exception e) {
                System.out.println("处理症状映射时发生错误: " + e.getMessage());
                // 如果更新失败，不中断删除流程，继续执行
            }

            // 7. 删除科室
            System.out.println("开始删除科室: " + department.getName());
            departmentRepository.delete(department);
            System.out.println("科室删除成功");
            
            result.setSuccess(true);
            result.setMessage("成功删除科室，移动了 " + result.getDoctorCount() + " 位医生和 " + result.getLocationCount() + " 个地点");
            System.out.println("=== 科室删除完成 ===");
            return result;
            
        } catch (Exception e) {
            System.err.println("删除科室时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("删除科室失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从科室中移除医生（将department_id设置为999）
     * 
     * @param departmentId 科室ID
     * @param doctorIdentifier 医生工号
     */
    @Transactional
    public void removeDoctorFromDepartment(Integer departmentId, String doctorIdentifier) {
        // 1. 查找医生
        Doctor doctor = doctorRepository.findByIdentifier(doctorIdentifier)
                .orElseThrow(() -> new RuntimeException("医生不存在: " + doctorIdentifier));

        // 2. 验证医生是否属于该科室
        if (doctor.getDepartment() == null || !doctor.getDepartment().getDepartmentId().equals(departmentId)) {
            throw new RuntimeException("医生不属于该科室");
        }

        // 3. 检查医生是否有未来的排班
        long futureScheduleCount = scheduleRepository.countFutureSchedulesByDoctor(
            doctor,
            LocalDate.now()
        );
        if (futureScheduleCount > 0) {
            throw new RuntimeException("该医生有 " + futureScheduleCount + " 个未来的排班，无法移除。请先删除或调整相关排班。");
        }

        // 4. 将医生移动到未分配科室（ID=999）
        Department unassignedDepartment = departmentRepository.findById(999)
                .orElseThrow(() -> new RuntimeException("未分配科室不存在，请先执行数据库初始化脚本"));
        
        doctor.setDepartment(unassignedDepartment);
        doctorRepository.save(doctor);
    }

    /**
     * 查询科室列表（分页、过滤和排序）
     * 
     * @param queryDTO 包含查询条件、分页和排序信息的DTO
     * @return 分页结果的响应DTO Page<DepartmentResponseDTO>
     */
    @Transactional(readOnly = true)
    public Page<DepartmentResponseDTO> queryDepartments(DepartmentQueryDTO queryDTO) {
        // 1. 设置分页和排序参数
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 0;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;
        
        // 确保页码不小于0
        if (page < 0) {
            page = 0;
        }
        
        // 确保页面大小在合理范围内
        if (size <= 0) {
            size = 10;
        } else if (size > 1000) {
            size = 1000; // 限制最大页面大小
        }
        
        String sortBy = queryDTO.getSortBy() != null ? queryDTO.getSortBy() : "departmentId";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(queryDTO.getSortOrder()) ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        // 构建 Pageable 对象
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        // 2. 构建查询条件 Specification
        Specification<Department> spec = DepartmentSpecification.buildSpecification(queryDTO);

        // 3. 执行查询
        Page<Department> departmentPage = departmentRepository.findAll(spec, pageable);

        // 4. 转换实体 Page 为 Response DTO Page
        return departmentPage.map(department -> {
            // 强制加载parentDepartment避免LazyInitializationException
            ParentDepartment parent = department.getParentDepartment();
            if (parent != null && !Hibernate.isInitialized(parent)) {
                Hibernate.initialize(parent);
            }
            return convertToResponseDTO(department);
        });
    }

    /**
     * 根据科室ID获取科室信息
     * 
     * @param departmentId 科室ID
     * @return 科室响应DTO，如果不存在返回null
     */
    @Transactional(readOnly = true)
    public DepartmentResponseDTO getDepartmentById(Integer departmentId) {
        Optional<Department> departmentOpt = departmentRepository.findById(departmentId);

        if (departmentOpt.isEmpty()) {
            return null;
        }

        Department department = departmentOpt.get();
        return convertToResponseDTO(department);
    }

    /**
     * 根据父科室ID获取所有子科室
     * 
     * @param parentId 父科室ID
     * @return 子科室列表
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponseDTO> getDepartmentsByParentId(Integer parentId) {
        List<Department> departments = departmentRepository.findByParentDepartmentId(parentId);
        return departments.stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 获取所有父科室
     * 
     * @return 父科室列表
     */
    @Transactional(readOnly = true)
    public List<ParentDepartment> getAllParentDepartments() {
        return parentDepartmentRepository.findAll();
    }

    /**
     * 获取科室树形结构数据（排除ID=999的未分配科室）
     * 
     * @return 树形结构的科室数据
     */
    @Transactional(readOnly = true)
    public List<DepartmentTreeDTO> getDepartmentTree() {
        // 1. 获取所有父科室（排除ID=999）
        List<ParentDepartment> parentDepartments = parentDepartmentRepository.findAll()
                .stream()
                .filter(parent -> !parent.getParentDepartmentId().equals(999))
                .toList();

        // 2. 获取所有子科室（排除ID=999）
        List<Department> allDepartments = departmentRepository.findAll()
                .stream()
                .filter(dept -> !dept.getDepartmentId().equals(999))
                .toList();

        // 3. 构建树形结构
        return parentDepartments.stream()
                .map(parent -> {
                    DepartmentTreeDTO parentNode = new DepartmentTreeDTO();
                    parentNode.setId(parent.getParentDepartmentId());
                    parentNode.setName(parent.getName());
                    parentNode.setType("parent");
                    parentNode.setDescription(parent.getDescription());
                    
                    // 添加子科室
                    List<DepartmentTreeDTO> children = allDepartments.stream()
                            .filter(dept -> dept.getParentDepartment().getParentDepartmentId().equals(parent.getParentDepartmentId()))
                            .map(this::convertToTreeDTO)
                            .toList();
                    parentNode.setChildren(children);
                    
                    return parentNode;
                })
                .toList();
    }

    /**
     * 根据科室ID获取该科室下的医生列表
     * 
     * @param departmentId 科室ID
     * @return 医生列表
     */
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsByDepartmentId(Integer departmentId) {
        // 验证科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("科室不存在: " + departmentId));

        // 获取该科室下的医生，排除已删除的医生
        List<Doctor> doctors = department.getDoctors().stream()
                .filter(doctor -> doctor.getStatus() != DoctorStatus.deleted)
                .toList();
        
        return doctors.stream()
                .map(this::convertToDoctorResponse)
                .toList();
    }

    /**
     * 将医生添加到指定科室
     * 
     * @param departmentId 科室ID
     * @param doctorData 医生信息
     * @return 添加后的医生信息
     */
    @Transactional
    public DoctorResponse addDoctorToDepartment(Integer departmentId, DoctorDataDTO doctorData) {
        // 1. 验证科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("科室不存在: " + departmentId));

        // 2. 查找或创建医生
        Doctor doctor = doctorRepository.findByIdentifier(doctorData.getIdentifier())
                .orElse(new Doctor());

        // 3. 更新医生信息
        doctor.setIdentifier(doctorData.getIdentifier());
        doctor.setFullName(doctorData.getFullName());
        doctor.setTitle(doctorData.getTitle());
        doctor.setDepartment(department);
        
        // 设置默认值
        if (doctor.getPasswordHash() == null) {
            doctor.setPasswordHash("$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"); // 默认密码
        }
        if (doctor.getStatus() == null) {
            doctor.setStatus(DoctorStatus.inactive);
        }

        // 4. 保存医生
        Doctor savedDoctor = doctorRepository.save(doctor);
        
        return convertToDoctorResponse(savedDoctor);
    }

    /**
     * 获取所有未分配科室的医生（department_id = 999）
     * 
     * @return 未分配医生的列表
     */
    @Transactional(readOnly = true)
    public List<DoctorResponse> getUnassignedDoctors() {
        try {
            System.out.println("=== 开始查询未分配医生 ===");
            
            // 查询department_id为999的医生
            List<Doctor> unassignedDoctors = doctorRepository.findByDepartmentDepartmentId(999);
            System.out.println("查询到的医生数量: " + (unassignedDoctors != null ? unassignedDoctors.size() : "null"));
            
            // 确保返回非null的列表
            if (unassignedDoctors == null) {
                System.out.println("查询结果为null，返回空列表");
                unassignedDoctors = new ArrayList<>();
            }
            
            // 打印每个医生的信息
            for (Doctor doctor : unassignedDoctors) {
                System.out.println("医生: " + doctor.getIdentifier() + " - " + doctor.getFullName() + 
                    " (科室ID: " + (doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentId() : "null") + ")");
            }
            
            List<DoctorResponse> result = unassignedDoctors.stream()
                    .map(this::convertToDoctorResponse)
                    .toList();
            
            System.out.println("转换后的响应数量: " + result.size());
            System.out.println("=== 查询未分配医生完成 ===");
            
            return result;
        } catch (Exception e) {
            // 如果出现异常，返回空列表而不是null
            System.err.println("获取未分配医生时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 批量将医生添加到指定科室
     * 
     * @param departmentId 科室ID
     * @param doctorIdentifiers 医生工号列表
     * @return 添加成功的医生列表
     */
    @Transactional
    public List<DoctorResponse> batchAddDoctorsToDepartment(Integer departmentId, List<String> doctorIdentifiers) {
        // 1. 验证科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("科室不存在: " + departmentId));

        // 2. 查找所有指定的医生
        List<Doctor> doctorsToAdd = new ArrayList<>();
        for (String identifier : doctorIdentifiers) {
            Doctor doctor = doctorRepository.findByIdentifier(identifier)
                    .orElseThrow(() -> new RuntimeException("医生不存在: " + identifier));
            
            // 验证医生是否属于未分配科室
            if (doctor.getDepartment() == null || !doctor.getDepartment().getDepartmentId().equals(999)) {
                throw new RuntimeException("医生 " + identifier + " 不属于未分配科室，无法添加");
            }
            
            doctorsToAdd.add(doctor);
        }

        // 3. 批量更新医生的科室
        for (Doctor doctor : doctorsToAdd) {
            doctor.setDepartment(department);
        }

        // 4. 保存所有医生
        List<Doctor> savedDoctors = doctorRepository.saveAll(doctorsToAdd);
        
        // 5. 转换为响应DTO
        return savedDoctors.stream()
                .map(this::convertToDoctorResponse)
                .toList();
    }

    /**
     * 将 Department 实体转换为 DepartmentTreeDTO
     */
    private DepartmentTreeDTO convertToTreeDTO(Department department) {
        DepartmentTreeDTO dto = new DepartmentTreeDTO();
        dto.setId(department.getDepartmentId());
        dto.setName(department.getName());
        dto.setType("department");
        dto.setDescription(department.getDescription());
        dto.setParentId(department.getParentDepartment().getParentDepartmentId());
        return dto;
    }

    /**
     * 将 Doctor 实体转换为 DoctorResponse
     */
    private DoctorResponse convertToDoctorResponse(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setDoctorId(doctor.getDoctorId());
        response.setIdentifier(doctor.getIdentifier());
        response.setFullName(doctor.getFullName());
        response.setTitle(doctor.getTitle());
        response.setPhoneNumber(doctor.getPhoneNumber());
        response.setSpecialty(doctor.getSpecialty());
        response.setBio(doctor.getBio());
        response.setPhotoUrl(doctor.getPhotoUrl());
        response.setStatus(doctor.getStatus());
        
        // 设置科室信息
        if (doctor.getDepartment() != null) {
            DepartmentDTO deptDto = new DepartmentDTO();
            deptDto.setDepartmentId(doctor.getDepartment().getDepartmentId());
            deptDto.setName(doctor.getDepartment().getName());
            response.setDepartment(deptDto);
        }
        
        return response;
    }

    /**
     * 将 Department 实体转换为 DepartmentResponseDTO
     */
    private DepartmentResponseDTO convertToResponseDTO(Department department) {
        DepartmentResponseDTO dto = new DepartmentResponseDTO();
        dto.setDepartmentId(department.getDepartmentId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // 设置父科室信息
        ParentDepartment parent = department.getParentDepartment();
        if (parent != null) {
            dto.setParentDepartmentId(parent.getParentDepartmentId());
            dto.setParentDepartmentName(parent.getName());
        }

        return dto;
    }
}