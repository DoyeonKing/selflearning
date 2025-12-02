package com.example.springboot.specifications;

import com.example.springboot.entity.Department;
import com.example.springboot.dto.department.DepartmentQueryDTO;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于构建 Department 实体的 JPA Specification，实现动态过滤
 */
public class DepartmentSpecification {

    public static Specification<Department> buildSpecification(DepartmentQueryDTO queryDTO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 根据 name 模糊搜索 (like '%name%')
            if (queryDTO.getName() != null && !queryDTO.getName().trim().isEmpty()) {
                String likePattern = "%" + queryDTO.getName().trim() + "%";
                predicates.add(builder.like(root.get("name"), likePattern));
            }

            // 2. 根据 description 模糊搜索 (like '%description%')
            if (queryDTO.getDescription() != null && !queryDTO.getDescription().trim().isEmpty()) {
                String likePattern = "%" + queryDTO.getDescription().trim() + "%";
                predicates.add(builder.like(root.get("description"), likePattern));
            }

            // 3. 根据父科室ID搜索
            if (queryDTO.getParentDepartmentId() != null) {
                predicates.add(builder.equal(root.get("parentDepartment").get("parentDepartmentId"), queryDTO.getParentDepartmentId()));
            }

            // 将所有条件用 AND 连接
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}