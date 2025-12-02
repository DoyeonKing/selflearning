package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.dto.auth.LoginRequest;
import com.example.springboot.dto.auth.LoginResponse;
import com.example.springboot.dto.doctor.DoctorActivateRequest;
import com.example.springboot.dto.doctor.DoctorVerifyRequest;
import com.example.springboot.service.DoctorService;
import com.example.springboot.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/auth") // 专为医生身份认证设置的路径
@CrossOrigin(origins = "*")
public class DoctorAuthController {

    private final DoctorService doctorService;

    @Autowired
    public DoctorAuthController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * 医生登录
     * URL: POST /api/doctor/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Result> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = doctorService.login(request.getIdentifier(), request.getPassword());
            return ResponseEntity.ok(Result.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 接口 1: 验证初始信息 (对应前端第一步)
     * URL: POST /api/doctor/auth/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyInitialInfo(@RequestBody DoctorVerifyRequest request) {
        try {
            doctorService.verifyInitialActivation(
                    request.getIdentifier(),
                    request.getInitialPassword()
            );
            // 验证成功，返回 200，前端可以进入第二步
            return ResponseEntity.ok().body("{\"message\": \"初始信息验证成功，请继续身份验证\"}");
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 接口 2: 激活账户 (对应前端第二步)
     * URL: POST /api/doctor/auth/activate
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestBody DoctorActivateRequest request) {
        try {
            doctorService.activateAccount(request);
            return ResponseEntity.ok().body("{\"message\": \"账户激活成功，请返回登录\"}");
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
