package com.example.springboot.controller;

import com.example.springboot.common.BaseResponse;
import com.example.springboot.common.Result;
import com.example.springboot.service.ModelTrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型训练控制器
 * 提供Word2Vec模型训练的HTTP接口
 */
@RestController
@RequestMapping("/api/ai-model")
@Tag(name = "AI模型训练", description = "Word2Vec模型训练接口")
public class ModelTrainingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainingController.class);
    
    @Autowired
    private ModelTrainingService modelTrainingService;
    
    /**
     * 训练Word2Vec模型
     * 
     * 调用此接口将：
     * 1. 自动从数据库读取医生数据
     * 2. 读取外部语料文件（如果存在）
     * 3. 训练模型并保存
     * 
     * @return 训练结果
     */
    @PostMapping("/train")
    @Operation(summary = "训练Word2Vec模型", description = "全自动从数据库读取数据并训练模型，无需Python")
    @PreAuthorize("hasRole('ADMIN')") // 仅管理员可以调用
    public ResponseEntity<BaseResponse<ModelTrainingService.TrainingResult>> trainModel() {
        logger.info("收到模型训练请求");
        
        try {
            ModelTrainingService.TrainingResult result = modelTrainingService.trainModel();
            
            if (result.isSuccess()) {
                logger.info("模型训练成功: {}", result.getMessage());
                return ResponseEntity.ok(BaseResponse.success(result, "模型训练成功"));
            } else {
                logger.error("模型训练失败: {}", result.getMessage());
                return ResponseEntity.ok(BaseResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            logger.error("模型训练异常", e);
            return ResponseEntity.ok(BaseResponse.error("训练失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取训练状态（用于异步训练时查询进度）
     * 当前版本为同步训练，此接口预留
     */
    @PostMapping("/train/status")
    @Operation(summary = "查询训练状态", description = "查询当前模型训练状态（预留接口）")
    public ResponseEntity<BaseResponse<String>> getTrainingStatus() {
        return ResponseEntity.ok(BaseResponse.success("当前版本为同步训练，请直接调用 /train 接口"));
    }
}

