package com.example.springboot.service;

import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.patient.MedicalHistoryResponse;
import com.example.springboot.dto.patient.MedicalHistoryUpdateRequest;
import com.example.springboot.dto.user.UserCreateRequest;
import com.example.springboot.dto.user.UserResponse;
import com.example.springboot.dto.user.UserImportResponse;
import com.example.springboot.dto.user.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);

    UserImportResponse importUsers(List<UserCreateRequest> requests);

    //PageResponse<UserResponse> searchUsers(String id, String name, int page, int pageSize);

    // 新增医生信息查询接口
    PageResponse<UserResponse> searchDoctors(String id, String name, Integer departmentId, int page, int pageSize);

    // 新增患者信息查询接口
    PageResponse<UserResponse> searchPatients(String id, String name, int page, int pageSize);

    UserResponse updateUser(Long patient_id, Integer doctor_id, UserUpdateRequest request);

    PageResponse<MedicalHistoryResponse> getMedicalHistories(Integer page, Integer pageSize);

    MedicalHistoryResponse updateMedicalHistory(Long id, MedicalHistoryUpdateRequest request);

    void softDeleteUser(Long userId, String role);
}