// 路径：src/main/java/com/example/springboot/controller/LocationController.java
package com.example.springboot.controller;

import com.example.springboot.dto.location.LocationResponse;
import com.example.springboot.entity.Location;
import com.example.springboot.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    /**
     * 根据科室ID获取门诊室名称列表
     * 
     * @param departmentId 科室ID
     * @return 门诊室名称列表
     */
    @GetMapping("/department/{departmentId}/names")
    public ResponseEntity<?> getLocationNamesByDepartmentId(@PathVariable Integer departmentId) {
        try {
            List<String> locationNames = locationService.getLocationNamesByDepartmentId(departmentId);
            return new ResponseEntity<>(locationNames, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取门诊室名称失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取所有地点信息列表（用于二维码生成）
     * GET /api/locations
     * 
     * @return 所有地点信息列表
     */
    @GetMapping("")
    public ResponseEntity<?> getAllLocations() {
        try {
            List<LocationResponse> locations = locationService.getAllLocations();
            return new ResponseEntity<>(locations, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取地点信息失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据科室ID获取门诊室完整信息列表
     * 
     * @param departmentId 科室ID
     * @return 门诊室完整信息列表
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<?> getLocationsByDepartmentId(@PathVariable Integer departmentId) {
        try {
            List<LocationResponse> locations = locationService.getLocationsByDepartmentId(departmentId);
            return new ResponseEntity<>(locations, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取门诊室信息失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 为指定科室添加新地点
     * POST /api/locations/department/{departmentId}
     * 
     * @param departmentId 科室ID
     * @param locationData 地点信息
     * @return 创建成功的地点信息
     */
    @PostMapping("/department/{departmentId}")
    public ResponseEntity<?> addLocationToDepartment(
            @PathVariable Integer departmentId,
            @RequestBody Map<String, Object> locationData) {
        try {
            Location location = locationService.createLocation(departmentId, locationData);
            return new ResponseEntity<>(location, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("添加地点时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 删除指定地点
     * DELETE /api/locations/{locationId}
     * 
     * @param locationId 地点ID
     * @return 操作结果
     */
    @DeleteMapping("/{locationId}")
    public ResponseEntity<?> deleteLocation(@PathVariable Integer locationId) {
        try {
            locationService.deleteLocation(locationId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("删除地点时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 获取所有未分配科室的地点（department_id 为 NULL）
     * GET /api/locations/unassigned
     * 
     * @return 未分配地点的列表
     */
    @GetMapping("/unassigned")
    public ResponseEntity<?> getUnassignedLocations() {
        try {
            List<LocationResponse> locations = locationService.getUnassignedLocations();
            return new ResponseEntity<>(locations, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取未分配地点列表时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 批量将地点分配到指定科室
     * POST /api/locations/department/{departmentId}/batch-assign
     * 
     * @param departmentId 科室ID
     * @param locationIds 地点ID列表
     * @return 分配成功的地点列表
     */
    @PostMapping("/department/{departmentId}/batch-assign")
    public ResponseEntity<?> batchAssignLocationsToDepartment(
            @PathVariable Integer departmentId,
            @RequestBody List<Integer> locationIds) {
        try {
            List<LocationResponse> locations = locationService.batchAssignLocationsToDepartment(departmentId, locationIds);
            return new ResponseEntity<>(locations, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("批量分配地点时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 将地点从科室中移除（设置department_id为NULL）
     * DELETE /api/locations/department/{departmentId}/location/{locationId}
     * 
     * @param departmentId 科室ID
     * @param locationId 地点ID
     * @return 操作结果
     */
    @DeleteMapping("/department/{departmentId}/location/{locationId}")
    public ResponseEntity<?> removeLocationFromDepartment(
            @PathVariable Integer departmentId,
            @PathVariable Integer locationId) {
        try {
            locationService.removeLocationFromDepartment(departmentId, locationId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("移除地点时发生错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}