package com.example.springboot.controller;

// 确保您的 DTO 路径正确，如果放在 patient 包下，请使用正确的路径
import com.example.springboot.common.Result;
import com.example.springboot.dto.auth.LoginRequest;
import com.example.springboot.dto.auth.LoginResponse;
import com.example.springboot.dto.patient.VerifyRequest;
import com.example.springboot.dto.patient.ActivateRequest;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.PatientService;
import com.example.springboot.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // 认证相关的公共路由
@CrossOrigin(origins = "*")
public class AuthController {

    private final PatientService patientService;
    private final AdminService adminService;

    @Autowired
    public AuthController(PatientService patientService, AdminService adminService) {
        this.patientService = patientService;
        this.adminService = adminService;
    }

    /**
     * 患者登录
     * URL: POST /api/auth/patient/login
     */
    @PostMapping("/patient/login")
    public ResponseEntity<Result> patientLogin(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = patientService.login(request.getIdentifier(), request.getPassword());
            return ResponseEntity.ok(Result.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 管理员登录
     * URL: POST /api/auth/admin/login
     */
    @PostMapping("/admin/login")
    public ResponseEntity<Result> adminLogin(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = adminService.login(request.getIdentifier(), request.getPassword());
            return ResponseEntity.ok(Result.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 接口 1: 验证初始信息（前端第一步）
     * URL: POST /api/auth/verify-patient
     */
    @PostMapping("/verify-patient")
    public ResponseEntity<?> verifyInitialInfo(@RequestBody VerifyRequest request) {
        try {
            // 注意：传入两个参数
            patientService.verifyInitialActivation(
                    request.getIdentifier(),
                    request.getInitialPassword()
            );
            // 验证成功，返回 200，前端可以进入第二步
            return ResponseEntity.ok().body("{\"message\": \"初始信息验证成功，请继续身份验证\"}");
        } catch (ResourceNotFoundException e) {
            // 捕获“找不到”异常
            return ResponseEntity.badRequest().body("{\"error\": \"学号/工号不存在\"}");
        } catch (IllegalArgumentException e) {
            // 捕获“已激活”或“密码错误”等业务异常
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 接口 2: 身份验证与激活（对应前端第二步）
     * URL: POST /api/auth/activate-patient
     * @param request 包含 identifier, idCardEnding, newPassword, confirmPassword
     */
    @PostMapping("/activate-patient")
    public ResponseEntity<?> activateAccount(@RequestBody ActivateRequest request) {
        try {
            patientService.activateAccount( // 调用 Service 逻辑
                    request.getIdentifier(),
                    request.getIdCardEnding(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );
            return ResponseEntity.ok().body("{\"message\": \"账户激活成功，请返回登录\"}");
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            // 捕获业务逻辑中抛出的所有校验失败异常
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            // 捕获未预期的错误
            e.printStackTrace(); // 打印堆栈跟踪以便调试
            return ResponseEntity.internalServerError().body("{\"error\": \"服务器内部错误，激活失败\"}");
        }
    }
}