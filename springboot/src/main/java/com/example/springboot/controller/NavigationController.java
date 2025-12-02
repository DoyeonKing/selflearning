package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 导航控制器
 * 提供路径规划和导航指引接口
 */
@RestController
@RequestMapping("/api/navigation")
@CrossOrigin(origins = "*")
public class NavigationController {
    
    @Autowired
    private NavigationService navigationService;
    
    /**
     * 计算导航路径
     * GET /api/navigation/path?startNodeId={startNodeId}&endNodeId={endNodeId}
     */
    @GetMapping("/path")
    public ResponseEntity<Result> calculatePath(
            @RequestParam Integer startNodeId,
            @RequestParam Integer endNodeId) {
        try {
            NavigationService.NavigationPath path = navigationService.calculatePath(startNodeId, endNodeId);
            return ResponseEntity.ok(Result.success(path));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "路径规划失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取下一步指引
     * GET /api/navigation/next?currentNodeId={currentNodeId}&targetNodeId={targetNodeId}
     */
    @GetMapping("/next")
    public ResponseEntity<Result> getNextStep(
            @RequestParam Integer currentNodeId,
            @RequestParam Integer targetNodeId) {
        try {
            NavigationService.PathStep step = navigationService.getNextStep(currentNodeId, targetNodeId);
            if (step == null) {
                return ResponseEntity.ok(Result.error("404", "无法找到路径"));
            }
            return ResponseEntity.ok(Result.success(step));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取指引失败: " + e.getMessage()));
        }
    }
}

