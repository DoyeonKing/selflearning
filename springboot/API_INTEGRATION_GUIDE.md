# 科室医生列表接口集成完成

## 接口信息

### 后端接口
- **URL**: `GET /api/departments/{departmentId}/doctors`
- **功能**: 获取指定科室下的所有医生列表
- **参数**: 
  - `departmentId` (路径参数): 科室ID
- **响应格式**:
```json
{
  "departmentName": "内科",
  "departmentId": 1,
  "doctors": [
    {
      "doctorId": 1,
      "identifier": "D001",
      "fullName": "张医生",
      "title": "主治医师",
      "specialty": "心血管疾病",
      "phoneNumber": "13800138000",
      "status": "active"
    }
  ],
  "totalCount": 1
}
```

## 已完成的修改

### 后端修改
1. **新增DTO**: `DepartmentDoctorsResponseDTO.java` - 科室医生列表响应DTO
2. **更新Repository**: `DoctorRepository.java` - 添加 `findByDepartmentDepartmentId` 方法
3. **更新Service**: 
   - `DoctorService.java` - 添加 `getDoctorsByDepartmentId` 方法
   - `DepartmentService.java` - 添加 `getDepartmentById` 方法
4. **更新Controller**: `DepartmentController.java` - 添加 `getDepartmentDoctors` 接口

### 前端修改
1. **更新API**: `department.js` - 添加 `getDepartmentDoctors` 方法
2. **更新页面**: `Members.vue` - 集成真实API调用，替换模拟数据

## 使用方式

### 前端调用示例
```javascript
import { getDepartmentDoctors } from '@/api/department';

// 获取科室ID为1的所有医生
const response = await getDepartmentDoctors(1);
console.log(response.data.doctors); // 医生列表
console.log(response.data.departmentName); // 科室名称
```

### 后端测试示例
```bash
# 使用curl测试
curl -X GET "http://localhost:8080/api/departments/1/doctors" \
     -H "Content-Type: application/json"
```

## 注意事项

1. **数据映射**: 前端将API返回的 `identifier` 映射为 `id`，`fullName` 映射为 `name`
2. **错误处理**: 接口包含完整的错误处理，包括科室不存在、服务器错误等
3. **加载状态**: 前端添加了loading状态，提升用户体验
4. **路由参数**: 页面通过路由参数 `:id` 获取科室ID

## 测试建议

1. 确保数据库中有科室和医生数据
2. 测试不同科室ID的医生列表获取
3. 测试空科室（无医生）的情况
4. 测试不存在的科室ID（应返回404错误）
