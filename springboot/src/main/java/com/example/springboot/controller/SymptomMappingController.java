package com.example.springboot.controller;

import com.example.springboot.dto.symptom.SymptomMappingRequest;
import com.example.springboot.dto.symptom.SymptomMappingResponse;
import com.example.springboot.service.SymptomMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/symptom-mappings")
public class SymptomMappingController {
    
    @Autowired
    private SymptomMappingService symptomMappingService;
    
    /**
     * 获取所有症状映射
     * GET /api/symptom-mappings
     */
    @GetMapping
    public ResponseEntity<?> getAllMappings() {
        try {
            List<SymptomMappingResponse> mappings = symptomMappingService.getAllMappings();
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取症状映射失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 根据科室ID获取症状映射
     * GET /api/symptom-mappings/department/{departmentId}
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<?> getMappingsByDepartmentId(@PathVariable Integer departmentId) {
        try {
            List<SymptomMappingResponse> mappings = symptomMappingService.getMappingsByDepartmentId(departmentId);
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取症状映射失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 创建症状映射
     * POST /api/symptom-mappings
     */
    @PostMapping
    public ResponseEntity<?> createMapping(@RequestBody SymptomMappingRequest request) {
        try {
            SymptomMappingResponse response = symptomMappingService.createMapping(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("创建症状映射失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 更新症状映射
     * PUT /api/symptom-mappings/{mappingId}
     */
    @PutMapping("/{mappingId}")
    public ResponseEntity<?> updateMapping(
            @PathVariable Integer mappingId,
            @RequestBody SymptomMappingRequest request) {
        try {
            SymptomMappingResponse response = symptomMappingService.updateMapping(mappingId, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("更新症状映射失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 删除症状映射
     * DELETE /api/symptom-mappings/{mappingId}
     */
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<?> deleteMapping(@PathVariable Integer mappingId) {
        try {
            symptomMappingService.deleteMapping(mappingId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("删除症状映射失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}

