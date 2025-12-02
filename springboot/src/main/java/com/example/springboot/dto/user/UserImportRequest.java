package com.example.springboot.dto.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserImportRequest {
    private MultipartFile file; // Excel文件
}