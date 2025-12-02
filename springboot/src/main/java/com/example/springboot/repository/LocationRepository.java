// 路径：src/main/java/com/example/springboot/repository/LocationRepository.java
package com.example.springboot.repository;

import com.example.springboot.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Integer> {

    /**
     * 根据科室ID查询所有关联的门诊室
     * @param departmentId 科室ID
     * @return 门诊室列表
     */
    List<Location> findByDepartmentDepartmentId(Integer departmentId);
}