package com.example.springboot.controller;

import com.example.springboot.entity.ParentDepartment;
import com.example.springboot.service.ParentDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/parent-departments")
public class ParentDepartmentController {

    @Autowired
    private ParentDepartmentService parentDepartmentService;

    /**
     * 创建父科室
     * 接收: { name: "父科室名", description: "描述" }
     */
    @PostMapping
    public ResponseEntity<?> createParentDepartment(@RequestBody ParentDepartment parentDepartment) {
        try {
            if (parentDepartment.getName() == null || parentDepartment.getName().trim().isEmpty()) {
                return new ResponseEntity<>("父科室名称不能为空", HttpStatus.BAD_REQUEST);
            }

            ParentDepartment created = parentDepartmentService.createParentDepartment(
                    parentDepartment.getName(), 
                    parentDepartment.getDescription()
            );
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("创建父科室时发生内部错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取所有父科室
     */
    @GetMapping
    public ResponseEntity<?> getAllParentDepartments() {
        try {
            List<ParentDepartment> parentDepartments = parentDepartmentService.findAll();
            return new ResponseEntity<>(parentDepartments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("查询父科室时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据ID获取父科室
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getParentDepartmentById(@PathVariable Integer id) {
        try {
            Optional<ParentDepartment> parentDepartment = parentDepartmentService.findById(id);
            if (parentDepartment.isEmpty()) {
                return new ResponseEntity<>("父科室不存在", HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(parentDepartment.get(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("查询父科室详情时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 更新父科室
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateParentDepartment(@PathVariable Integer id, @RequestBody ParentDepartment parentDepartment) {
        try {
            if (parentDepartment.getName() == null || parentDepartment.getName().trim().isEmpty()) {
                return new ResponseEntity<>("父科室名称不能为空", HttpStatus.BAD_REQUEST);
            }

            ParentDepartment updated = parentDepartmentService.updateParentDepartment(
                    id, 
                    parentDepartment.getName(), 
                    parentDepartment.getDescription()
            );
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("更新父科室时发生内部错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除父科室
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteParentDepartment(@PathVariable Integer id) {
        try {
            parentDepartmentService.deleteParentDepartment(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("删除父科室时发生内部错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据名称模糊查询父科室
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchParentDepartments(@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String description) {
        try {
            List<ParentDepartment> results;
            if (name != null && !name.trim().isEmpty()) {
                results = parentDepartmentService.findByNameContaining(name);
            } else if (description != null && !description.trim().isEmpty()) {
                results = parentDepartmentService.findByDescriptionContaining(description);
            } else {
                results = parentDepartmentService.findAll();
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("搜索父科室时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
