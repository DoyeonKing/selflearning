package com.example.springboot.controller;

import com.example.springboot.dto.common.PageResponse;
import com.example.springboot.dto.guideline.MedicalGuidelineRequest;
import com.example.springboot.dto.guideline.MedicalGuidelineResponse;
import com.example.springboot.service.MedicalGuidelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-guidelines")
@RequiredArgsConstructor
public class MedicalGuidelineController {

    private final MedicalGuidelineService guidelineService;

    /**
     * 获取就医规范列表（分页）
     */
    @GetMapping
    public ResponseEntity<PageResponse<MedicalGuidelineResponse>> getGuidelines(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        PageResponse<MedicalGuidelineResponse> response = guidelineService
                .getGuidelines(page, pageSize, keyword, category, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取规范详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<MedicalGuidelineResponse> getGuidelineById(@PathVariable Integer id) {
        MedicalGuidelineResponse response = guidelineService.getGuidelineById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建规范
     */
    @PostMapping
    public ResponseEntity<MedicalGuidelineResponse> createGuideline(
            @Valid @RequestBody MedicalGuidelineRequest request) {
        MedicalGuidelineResponse response = guidelineService.createGuideline(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 更新规范
     */
    @PutMapping("/{id}")
    public ResponseEntity<MedicalGuidelineResponse> updateGuideline(
            @PathVariable Integer id,
            @Valid @RequestBody MedicalGuidelineRequest request) {
        MedicalGuidelineResponse response = guidelineService.updateGuideline(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除规范
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuideline(@PathVariable Integer id) {
        guidelineService.deleteGuideline(id);
        return ResponseEntity.noContent().build();
    }
}