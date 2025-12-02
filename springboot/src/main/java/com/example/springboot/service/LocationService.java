// 路径：src/main/java/com/example/springboot/service/LocationService.java
package com.example.springboot.service;

import com.example.springboot.dto.location.LocationResponse;
import com.example.springboot.entity.Department;
import com.example.springboot.entity.Location;
import com.example.springboot.repository.DepartmentRepository;
import com.example.springboot.repository.LocationRepository;
import com.example.springboot.repository.ScheduleRepository;
import com.example.springboot.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;

    /**
     * 根据科室ID获取所有门诊室名称
     * 
     * @param departmentId 科室ID
     * @return 门诊室名称列表
     */
    public List<String> getLocationNamesByDepartmentId(Integer departmentId) {
        // 查询该科室下的所有门诊室
        List<Location> locations = locationRepository.findByDepartmentDepartmentId(departmentId);

        // 提取locationName并返回
        return locations.stream()
                .map(Location::getLocationName)
                .collect(Collectors.toList());
    }

    /**
     * 根据科室ID获取所有门诊室完整信息
     * 
     * @param departmentId 科室ID
     * @return 门诊室完整信息列表
     */
    public List<LocationResponse> getLocationsByDepartmentId(Integer departmentId) {
        // 查询该科室下的所有门诊室
        return locationRepository.findByDepartmentDepartmentId(departmentId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 为指定科室创建新地点
     * 
     * @param departmentId 科室ID
     * @param locationData 地点信息
     * @return 创建成功的地点
     */
    @Transactional
    public Location createLocation(Integer departmentId, Map<String, Object> locationData) {
        // 1. 验证科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("科室不存在，ID: " + departmentId));

        // 2. 创建新地点实体
        Location location = new Location();
        location.setDepartment(department);
        
        // 3. 设置地点属性
        if (locationData.containsKey("locationName")) {
            location.setLocationName((String) locationData.get("locationName"));
        }
        if (locationData.containsKey("building")) {
            location.setBuilding((String) locationData.get("building"));
        }
        if (locationData.containsKey("floorLevel")) {
            location.setFloorLevel(Integer.parseInt(locationData.get("floorLevel").toString()));
        }
        if (locationData.containsKey("roomNumber")) {
            location.setRoomNumber((String) locationData.get("roomNumber"));
        }
        if (locationData.containsKey("capacity")) {
            location.setCapacity(Integer.parseInt(locationData.get("capacity").toString()));
        }

        // 4. 保存并返回
        return locationRepository.save(location);
    }

    /**
     * 删除指定地点
     * 
     * @param locationId 地点ID
     */
    @Transactional
    public void deleteLocation(Integer locationId) {
        // 1. 验证地点是否存在
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("地点不存在，ID: " + locationId));

        // 2. 检查地点是否在未来的排班中被使用
        long futureScheduleCount = scheduleRepository.countFutureSchedulesByLocation(
            locationId,
            LocalDate.now()
        );
        if (futureScheduleCount > 0) {
            throw new RuntimeException("该地点在 " + futureScheduleCount + " 个未来的排班中被使用，无法删除。请先删除或调整相关排班。");
        }

        // 3. 删除地点
        locationRepository.delete(location);
    }

    /**
     * 获取所有地点
     * 
     * @return 所有地点列表
     */
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有未分配科室的地点
     * 
     * @return 未分配地点列表
     */
    public List<LocationResponse> getUnassignedLocations() {
        // 查询department为null的地点
        return locationRepository.findAll().stream()
                .filter(location -> location.getDepartment() == null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换Location实体为LocationResponse DTO
     * 
     * @param location Location实体
     * @return LocationResponse DTO
     */
    private LocationResponse convertToResponse(Location location) {
        LocationResponse response = new LocationResponse();
        response.setLocationId(location.getLocationId());
        response.setLocationName(location.getLocationName());
        response.setFloorLevel(location.getFloorLevel());
        response.setBuilding(location.getBuilding());
        response.setRoomNumber(location.getRoomNumber());
        response.setCapacity(location.getCapacity());
        response.setMapNodeId(location.getMapNodeId());
        
        if (location.getDepartment() != null) {
            response.setDepartmentId(location.getDepartment().getDepartmentId());
            response.setDepartmentName(location.getDepartment().getName());
        }
        
        return response;
    }

    /**
     * 批量将地点分配到指定科室
     * 
     * @param departmentId 科室ID
     * @param locationIds 地点ID列表
     * @return 分配成功的地点列表
     */
    @Transactional
    public List<LocationResponse> batchAssignLocationsToDepartment(Integer departmentId, List<Integer> locationIds) {
        // 1. 验证科室是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("科室不存在，ID: " + departmentId));

        // 2. 批量分配地点
        List<LocationResponse> assignedLocations = new java.util.ArrayList<>();
        for (Integer locationId : locationIds) {
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("地点不存在，ID: " + locationId));
            
            location.setDepartment(department);
            Location savedLocation = locationRepository.save(location);
            assignedLocations.add(convertToResponse(savedLocation));
        }

        return assignedLocations;
    }

    /**
     * 将地点从科室中移除（设置department为null）
     * 
     * @param departmentId 科室ID
     * @param locationId 地点ID
     */
    @Transactional
    public void removeLocationFromDepartment(Integer departmentId, Integer locationId) {
        // 1. 验证地点是否存在
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("地点不存在，ID: " + locationId));

        // 2. 验证地点是否属于该科室
        if (location.getDepartment() == null || 
            !location.getDepartment().getDepartmentId().equals(departmentId)) {
            throw new RuntimeException("该地点不属于指定科室");
        }

        // 3. 检查地点是否在未来的排班中被使用
        long futureScheduleCount = scheduleRepository.countFutureSchedulesByLocation(
            locationId,
            LocalDate.now()
        );
        if (futureScheduleCount > 0) {
            throw new RuntimeException("该地点在 " + futureScheduleCount + " 个未来的排班中被使用，无法移除。请先删除或调整相关排班。");
        }

        // 4. 移除科室关联
        location.setDepartment(null);
        locationRepository.save(location);
    }
}