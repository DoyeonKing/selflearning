package com.example.springboot.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.ArrayList;

/**
 * 子科室表
 */
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Integer departmentId;

    // 关联到父科室表
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentDepartment parentDepartment;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 子科室名称

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 科室职能描述

    // 与医生的关联关系（一对多）
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Doctor> doctors = new ArrayList<>();

    // =======================================================
    // 手动生成的 Getter 和 Setter 方法 (保持不变)
    // =======================================================

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public ParentDepartment getParentDepartment() {
        return parentDepartment;
    }

    public void setParentDepartment(ParentDepartment parentDepartment) {
        this.parentDepartment = parentDepartment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctors = doctors;
    }

    // 构造函数
    public Department() {}

    public Department(ParentDepartment parentDepartment, String name, String description) {
        this.parentDepartment = parentDepartment;
        this.name = name;
        this.description = description;
    }
}