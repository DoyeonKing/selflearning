package com.example.springboot.controller;

import com.example.springboot.dto.department.DepartmentDTO;
import com.example.springboot.dto.department.DepartmentQueryDTO;
import com.example.springboot.dto.department.DepartmentResponseDTO;
import com.example.springboot.dto.department.DepartmentTreeDTO;
import com.example.springboot.dto.department.DoctorDataDTO;
import com.example.springboot.dto.department.DepartmentDeleteResult;
import com.example.springboot.dto.doctor.DoctorResponse;
import com.example.springboot.entity.ParentDepartment;
import com.example.springboot.service.DepartmentService;
import com.example.springboot.service.ParentDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private ParentDepartmentService parentDepartmentService;

    /**
     * 创建新子科室接口
     * 接收: { name: "新科室名", parentDepartmentName: "父科室名", description: "描述" }
     * 
     * @param departmentDTO 科室数据传输对象
     * @return 创建成功的科室信息
     */
    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        try {
            if (departmentDTO.getName() == null || departmentDTO.getName().trim().isEmpty()) {
                return new ResponseEntity<>("科室名称不能为空", HttpStatus.BAD_REQUEST);
            }
            
            if (departmentDTO.getParentDepartmentName() == null || departmentDTO.getParentDepartmentName().trim().isEmpty()) {
                return new ResponseEntity<>("父科室名称不能为空", HttpStatus.BAD_REQUEST);
            }

            DepartmentResponseDTO createdDepartment = departmentService.createDepartment(departmentDTO);
            return new ResponseEntity<>(createdDepartment, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("创建科室时发生内部错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 编辑科室描述信息接口
     * 接收: { name: "科室名", description: "新的描述" }
     * 使用 PUT /api/departments/description
     * 
     * @param departmentDTO 包含科室名称和新描述的DTO
     * @return 更新成功的科室信息
     */
    @PutMapping("/description")
    public ResponseEntity<?> updateDepartmentDescription(@RequestBody DepartmentDTO departmentDTO) {
        try {
            if (departmentDTO.getName() == null || departmentDTO.getName().trim().isEmpty()) {
                return new ResponseEntity<>("科室名称不能为空", HttpStatus.BAD_REQUEST);
            }

            DepartmentResponseDTO updatedDepartment = departmentService.updateDepartmentDescription(departmentDTO);
            return new ResponseEntity<>(updatedDepartment, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("更新科室时发生内部错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除科室（支持非空科室删除）
     * 使用 DELETE /api/departments/{departmentId}
     * 
     * @param departmentId 科室ID
     * @return 删除结果
     */
    @DeleteMapping("/{departmentId}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Integer departmentId) {
        try {
            DepartmentDeleteResult result = departmentService.deleteDepartment(departmentId);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("删除科室时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 查询和分页获取科室列表接口 (支持按名称、描述搜索、分页和排序)
     * 使用 GET /api/departments?page=0&size=10&name=内科&description=治疗&sortBy=departmentId&sortOrder=desc
     * 
     * @param queryDTO 包含查询条件、分页和排序信息的DTO，通过ModelAttribute自动绑定查询参数
     * @return 包含科室列表和分页信息的响应对象 Page<DepartmentResponseDTO>
     */
    @GetMapping
    public ResponseEntity<Page<DepartmentResponseDTO>> getDepartments(@ModelAttribute DepartmentQueryDTO queryDTO) {
        try {
            Page<DepartmentResponseDTO> departmentPage = departmentService.queryDepartments(queryDTO);
            return new ResponseEntity<>(departmentPage, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity(
                    "查询科室时发生内部错误: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据父科室ID获取子科室列表
     * 使用 GET /api/departments/parent/{parentId}
     * 
     * @param parentId 父科室ID
     * @return 子科室列表
     */
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<?> getDepartmentsByParentId(@PathVariable Integer parentId) {
        try {
            List<DepartmentResponseDTO> departments = departmentService.getDepartmentsByParentId(parentId);
            return new ResponseEntity<>(departments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("查询子科室时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取所有父科室列表
     * 使用 GET /api/departments/parents
     * 
     * @return 父科室列表
     */
    @GetMapping("/parents")
    public ResponseEntity<?> getAllParentDepartments() {
        try {
            List<ParentDepartment> parentDepartments = departmentService.getAllParentDepartments();
            return new ResponseEntity<>(parentDepartments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("查询父科室时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 测试接口 - 验证路由是否正常工作
     * 使用 GET /api/departments/test-unassigned
     * 
     * @return 测试响应
     */
    @GetMapping("/test-unassigned")
    public ResponseEntity<?> testUnassignedRoute() {
        return new ResponseEntity<>("路由测试成功 - 未分配医生接口可访问", HttpStatus.OK);
    }

    /**
     * 获取所有未分配科室的医生（department_id = 999）
     * 使用 GET /api/departments/unassigned-doctors
     * 
     * @return 未分配医生的列表
     */
    @GetMapping("/unassigned-doctors")
    public ResponseEntity<?> getUnassignedDoctors() {
        try {
            System.out.println("=== Controller: 收到获取未分配医生请求 ===");
            List<DoctorResponse> doctors = departmentService.getUnassignedDoctors();
            System.out.println("=== Controller: 返回医生数量: " + doctors.size() + " ===");
            return new ResponseEntity<>(doctors, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("=== Controller: 获取未分配医生时发生错误: " + e.getMessage() + " ===");
            e.printStackTrace();
            return new ResponseEntity<>("获取未分配医生列表时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取科室树形结构数据（排除ID=999的未分配科室）
     * 使用 GET /api/departments/tree
     * 
     * @return 树形结构的科室数据
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getDepartmentTree() {
        try {
            List<DepartmentTreeDTO> treeData = departmentService.getDepartmentTree();
            return new ResponseEntity<>(treeData, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取科室树形数据时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据ID获取科室详情
     * 使用 GET /api/departments/{id}
     * 
     * @param id 科室ID
     * @return 科室详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Integer id) {
        try {
            DepartmentResponseDTO department = departmentService.getDepartmentById(id);
            if (department != null) {
                return new ResponseEntity<>(department, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("科室不存在", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("查询科室时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据科室ID获取该科室下的医生列表
     * 使用 GET /api/departments/{departmentId}/doctors
     * 
     * @param departmentId 科室ID
     * @return 医生列表
     */
    @GetMapping("/{departmentId}/doctors")
    public ResponseEntity<?> getDoctorsByDepartmentId(@PathVariable Integer departmentId) {
        try {
            List<DoctorResponse> doctors = departmentService.getDoctorsByDepartmentId(departmentId);
            return new ResponseEntity<>(doctors, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取科室医生列表时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 将医生添加到指定科室
     * 使用 POST /api/departments/{departmentId}/members
     * 
     * @param departmentId 科室ID
     * @param doctorData 医生信息
     * @return 添加后的医生信息
     */
    @PostMapping("/{departmentId}/members")
    public ResponseEntity<?> addDoctorToDepartment(@PathVariable Integer departmentId, @RequestBody DoctorDataDTO doctorData) {
        try {
            DoctorResponse doctor = departmentService.addDoctorToDepartment(departmentId, doctorData);
            return new ResponseEntity<>(doctor, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("添加医生到科室时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 从科室中移除医生（将department_id设置为999）
     * 使用 DELETE /api/departments/{departmentId}/members/{doctorIdentifier}
     * 
     * @param departmentId 科室ID
     * @param doctorIdentifier 医生工号
     * @return 操作结果
     */
    @DeleteMapping("/{departmentId}/members/{doctorIdentifier}")
    public ResponseEntity<?> removeDoctorFromDepartment(@PathVariable Integer departmentId, @PathVariable String doctorIdentifier) {
        try {
            departmentService.removeDoctorFromDepartment(departmentId, doctorIdentifier);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("从科室移除医生时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 批量将医生添加到指定科室
     * 使用 POST /api/departments/{departmentId}/batch-add-doctors
     * 
     * @param departmentId 科室ID
     * @param doctorIdentifiers 医生工号列表
     * @return 添加成功的医生列表
     */
    @PostMapping("/{departmentId}/batch-add-doctors")
    public ResponseEntity<?> batchAddDoctorsToDepartment(@PathVariable Integer departmentId, @RequestBody List<String> doctorIdentifiers) {
        try {
            List<DoctorResponse> doctors = departmentService.batchAddDoctorsToDepartment(departmentId, doctorIdentifiers);
            return new ResponseEntity<>(doctors, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("批量添加医生到科室时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}