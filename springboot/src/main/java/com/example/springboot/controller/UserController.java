package com.example.springboot.controller;

import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.patient.MedicalHistoryResponse;
import com.example.springboot.dto.patient.MedicalHistoryUpdateRequest;
import com.example.springboot.dto.user.UserCreateRequest;
import com.example.springboot.dto.user.UserImportResponse;
import com.example.springboot.dto.user.UserResponse;
import com.example.springboot.dto.user.UserUpdateRequest;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.service.UserService;
import com.example.springboot.util.ExcelUserParser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    private ExcelUserParser excelUserParser;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 新增批量导入接口
    @PostMapping("/import")
    public ResponseEntity<UserImportResponse> importUsers(
            @RequestParam("file") MultipartFile file) throws IOException {

        // 1. 文件非空校验
        if (file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }

        // 2. 文件大小校验（限制10MB）
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("文件大小不能超过10MB");
        }

        // 3. 文件类型校验（仅允许.xls和.xlsx）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xls") && !originalFilename.endsWith(".xlsx"))) {
            throw new BadRequestException("仅支持.xls和.xlsx格式的Excel文件");
        }

        // 解析Excel
        var requests = excelUserParser.parseExcel(file.getInputStream());
        // 批量创建用户（后续会在Service层补充内部查重）
        var result = userService.importUsers(requests);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/doctorInfo")
    public ResponseEntity<PageResponse<UserResponse>> searchDoctors(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer departmentId) { // 新增departmentId参数

        // 校验分页参数
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 调用修改后的searchDoctors方法，传入departmentId
        PageResponse<UserResponse> response = userService.searchDoctors(id, name, departmentId, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/medical-history")
    public ResponseEntity<MedicalHistoryResponse> updateMedicalHistory(
            @PathVariable Long id,
            @RequestBody MedicalHistoryUpdateRequest request) {

        MedicalHistoryResponse response = userService.updateMedicalHistory(id, request);
        return ResponseEntity.ok(response);
    }

    // 患者信息查询接口 (强制每页10条)
    @GetMapping("/patientInfo")
    public ResponseEntity<PageResponse<UserResponse>> searchPatients(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name) {

        // 校验分页参数 (只校验页码，页大小固定为10)
        if (page < 1) {
            page = 1;
        }

        PageResponse<UserResponse> response = userService.searchPatients(id, name, page, 10);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,  // 添加ID路径参数
            @Valid @RequestBody UserUpdateRequest request) {
        // 根据角色判断是更新患者还是医生
        UserResponse updatedUser;
        if ("PATIENT".equals(request.getRole().toUpperCase())) {
            updatedUser = userService.updateUser(id, null, request);
        } else if ("DOCTOR".equals(request.getRole().toUpperCase())) {
            // 医生ID通常为Integer类型，这里做类型转换
            updatedUser = userService.updateUser(null, Integer.valueOf(id.intValue()), request);
        } else {
            throw new BadRequestException("不支持的用户角色: " + request.getRole());
        }
        return ResponseEntity.ok(updatedUser);
    }

    // 在UserController的getMedicalHistories方法中添加参数校验
    @GetMapping("/medical-history")
    public ResponseEntity<PageResponse<MedicalHistoryResponse>> getMedicalHistories(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        // 校验分页参数
        if (page < 1) {
            page = 1;
        }
        // 补充pageSize的校验（可选，确保不小于1）
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 关键修复：传入正确的pageSize，而非null
        PageResponse<MedicalHistoryResponse> response = userService.getMedicalHistories(page, pageSize);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestParam String role) {
        userService.softDeleteUser(id, role);
        return ResponseEntity.noContent().build();
    }
}