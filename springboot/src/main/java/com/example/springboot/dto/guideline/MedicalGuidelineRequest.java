package com.example.springboot.dto.guideline;

import com.example.springboot.entity.MedicalGuideline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MedicalGuidelineRequest {
    @NotBlank(message = "规范标题不能为空")
    private String title;

    @NotBlank(message = "规范内容不能为空")
    private String content;

    @NotBlank(message = "规范分类不能为空")
    private String category;

    @NotNull(message = "状态不能为空")
    private MedicalGuideline.GuidelineStatus status;

    private Integer createdBy; // 仅创建时使用
}