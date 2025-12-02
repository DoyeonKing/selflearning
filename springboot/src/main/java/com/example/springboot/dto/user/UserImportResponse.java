package com.example.springboot.dto.user;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserImportResponse {
    private int total; // 总条数
    private int success; // 成功条数
    private int failed; // 失败条数
    private List<String> errorMessages = new ArrayList<>(); // 错误信息
}