package com.example.springboot.controller;

import com.example.springboot.entity.Patient;
import com.example.springboot.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 患者控制器
 */
@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientRepository patientRepository;

    @Autowired
    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * 搜索患者（根据姓名）
     * @param name 患者姓名（模糊搜索）
     * @param page 页码
     * @param size 每页大小
     * @return 患者列表
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Patient>> searchPatients(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // 使用Specification进行动态查询
        Specification<Patient> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 如果提供了姓名，进行模糊搜索
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("fullName")),
                    "%" + name.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Patient> patients = patientRepository.findAll(spec, pageable);
        return ResponseEntity.ok(patients);
    }

    /**
     * 根据ID获取患者
     * @param id 患者ID
     * @return 患者信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        return ResponseEntity.ok(patient);
    }

    /**
     * 获取所有患者
     * @param page 页码
     * @param size 每页大小
     * @return 患者列表
     */
    @GetMapping
    public ResponseEntity<Page<Patient>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patients = patientRepository.findAll(pageable);
        return ResponseEntity.ok(patients);
    }
}
