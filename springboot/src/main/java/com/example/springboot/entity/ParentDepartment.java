package com.example.springboot.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

/**
 * 父科室表
 */
@Entity
@Table(name = "parent_departments")
public class ParentDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parent_department_id")
    private Integer parentDepartmentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 父科室名称

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 科室职能描述

    // 子科室关系
    @JsonIgnore
    @OneToMany(mappedBy = "parentDepartment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Department> departments;

    // 构造函数
    public ParentDepartment() {}

    public ParentDepartment(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getter 和 Setter 方法
    public Integer getParentDepartmentId() {
        return parentDepartmentId;
    }

    public void setParentDepartmentId(Integer parentDepartmentId) {
        this.parentDepartmentId = parentDepartmentId;
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

    public Set<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments;
    }
}
