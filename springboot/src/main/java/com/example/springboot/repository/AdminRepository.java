package com.example.springboot.repository; // 包名调整

import com.example.springboot.entity.Admin; // 导入路径调整
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> , JpaSpecificationExecutor<Admin> {
    Optional<Admin> findByUsername(String username);
    boolean existsByUsername(String username);
}
