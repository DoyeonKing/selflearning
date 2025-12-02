<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always" v-if="currentDepartment" v-loading="loading">
      <template #header>
        <div class="card-header-title">
          <h2 class="department-name-title">{{ currentDepartment.name }} - 地点管理</h2>
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            添加地点
          </el-button>
        </div>
      </template>

      <div class="location-management-section">
        <el-table :data="locations" border stripe>
          <el-table-column prop="locationId" label="地点ID" width="100" />
          <el-table-column prop="locationName" label="地点名称" width="200" />
          <el-table-column prop="building" label="楼栋" width="150" />
          <el-table-column prop="floorLevel" label="楼层" width="100" />
          <el-table-column prop="roomNumber" label="房间号" width="120" />
          <el-table-column prop="capacity" label="容纳人数" width="120" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="handleRemove(row.locationId)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!locations || locations.length === 0" description="该科室暂无地点" />
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="未找到指定的科室信息或获取失败" />

    <el-dialog v-model="addDialogVisible" title="添加地点到科室" width="900px">
      <el-tabs v-model="activeTab" class="location-tabs">
        <!-- 从未分配地点选择 -->
        <el-tab-pane label="从未分配地点选择" name="unassigned">
          <div v-loading="loadingUnassignedLocations">
            <div style="margin-bottom: 16px;">
              <el-text type="info">从以下未分配科室的地点中选择要添加到当前科室的地点：</el-text>
            </div>
            
            <el-table 
              :data="unassignedLocations" 
              @selection-change="handleSelectionChange"
              style="margin-bottom: 16px;"
              max-height="400px"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="locationId" label="地点ID" width="100" />
              <el-table-column prop="locationName" label="地点名称" width="200" />
              <el-table-column prop="building" label="楼栋" width="120" />
              <el-table-column prop="floorLevel" label="楼层" width="80" />
              <el-table-column prop="roomNumber" label="房间号" width="100" />
              <el-table-column prop="capacity" label="容纳人数" width="100" />
            </el-table>
            
            <el-empty v-if="!loadingUnassignedLocations && unassignedLocations.length === 0" description="暂无未分配的地点" />
            
            <div v-if="selectedLocations.length > 0" style="margin-top: 16px;">
              <el-text type="primary">已选择 {{ selectedLocations.length }} 个地点</el-text>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 自定义添加 -->
        <el-tab-pane label="新建地点" name="custom">
          <el-form 
            ref="addFormRef"
            :model="addForm" 
            :rules="addFormRules"
            label-width="100px"
          >
            <el-form-item label="地点名称" prop="locationName">
              <el-input v-model="addForm.locationName" placeholder="例如：门诊楼201室" />
            </el-form-item>
            <el-form-item label="楼栋" prop="building">
              <el-input v-model="addForm.building" placeholder="例如：门诊楼" />
            </el-form-item>
            <el-form-item label="楼层" prop="floorLevel">
              <el-input-number v-model="addForm.floorLevel" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="房间号" prop="roomNumber">
              <el-input v-model="addForm.roomNumber" placeholder="例如：201" />
            </el-form-item>
            <el-form-item label="容纳人数" prop="capacity">
              <el-input-number v-model="addForm.capacity" :min="1" :max="1000" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button 
            v-if="activeTab === 'unassigned'"
            type="primary" 
            @click="submitAssignLocations" 
            :loading="addingLocation"
            :disabled="selectedLocations.length === 0"
          >
            确认添加 ({{ selectedLocations.length }})
          </el-button>
          <el-button 
            v-else
            type="primary" 
            @click="submitAddCustomLocation" 
            :loading="addingLocation"
          >
            确认添加
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Delete } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';
import { useRoute, useRouter } from 'vue-router';

// 导入 API 服务
import { getDepartmentById } from '@/api/department';
import { 
  getLocationsByDepartmentId, 
  addLocationToDepartment, 
  deleteLocation,
  getUnassignedLocations,
  batchAssignLocationsToDepartment,
  removeLocationFromDepartment
} from '@/api/location';

const route = useRoute();
const router = useRouter();

// 数据状态
const currentDepartment = ref(null);
const locations = ref([]);
const loading = ref(false);
const addDialogVisible = ref(false);
const addingLocation = ref(false);
const activeTab = ref('unassigned'); // 默认显示未分配地点选择
const unassignedLocations = ref([]);
const selectedLocations = ref([]);
const loadingUnassignedLocations = ref(false);

const addFormRef = ref(null);

// 添加地点表单
const addForm = reactive({
  locationName: '',
  building: '',
  floorLevel: 1,
  roomNumber: '',
  capacity: 10
});

// 表单验证规则
const addFormRules = {
  locationName: [
    { required: true, message: '请输入地点名称', trigger: 'blur' }
  ],
  building: [
    { required: true, message: '请输入楼栋', trigger: 'blur' }
  ],
  floorLevel: [
    { required: true, message: '请输入楼层', trigger: 'blur' }
  ],
  roomNumber: [
    { required: true, message: '请输入房间号', trigger: 'blur' }
  ]
};

// 获取科室信息和地点列表
const fetchDepartmentInfo = async () => {
  const departmentId = route.params.id;
  if (!departmentId) {
    ElMessage.error('缺少科室ID参数');
    router.back();
    return;
  }

  loading.value = true;
  try {
    // 并行获取科室信息和地点列表
    const [departmentResponse, locationsResponse] = await Promise.all([
      getDepartmentById(departmentId),
      getLocationsByDepartmentId(departmentId)
    ]);
    
    if (departmentResponse) {
      currentDepartment.value = departmentResponse;
      // 设置地点列表
      locations.value = locationsResponse || [];
    } else {
      ElMessage.error('科室不存在');
      router.back();
    }
  } catch (error) {
    console.error('获取科室信息失败:', error);
    ElMessage.error('获取科室信息失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 获取未分配地点列表
const fetchUnassignedLocations = async () => {
  loadingUnassignedLocations.value = true;
  try {
    const response = await getUnassignedLocations();
    unassignedLocations.value = response || [];
  } catch (error) {
    console.error('获取未分配地点列表失败:', error);
    ElMessage.error('获取未分配地点列表失败: ' + (error.message || '未知错误'));
    unassignedLocations.value = [];
  } finally {
    loadingUnassignedLocations.value = false;
  }
};

// 处理地点选择变化
const handleSelectionChange = (selection) => {
  selectedLocations.value = selection;
};

// 打开添加地点对话框
const openAddDialog = () => {
  // 重置表单
  addForm.locationName = '';
  addForm.building = '';
  addForm.floorLevel = 1;
  addForm.roomNumber = '';
  addForm.capacity = 10;
  
  // 重置选择
  selectedLocations.value = [];
  activeTab.value = 'unassigned';
  
  // 获取未分配地点
  fetchUnassignedLocations();
  
  addDialogVisible.value = true;
};

// 批量分配地点到科室
const submitAssignLocations = async () => {
  if (selectedLocations.value.length === 0) {
    ElMessage.warning('请选择要添加的地点');
    return;
  }

  try {
    addingLocation.value = true;
    
    // 获取选中地点的ID列表
    const locationIds = selectedLocations.value.map(location => location.locationId);
    
    // 批量分配地点
    await batchAssignLocationsToDepartment(currentDepartment.value.departmentId, locationIds);
    
    ElMessage.success(`成功添加 ${selectedLocations.value.length} 个地点到科室`);
    addDialogVisible.value = false;
    
    // 刷新地点列表
    await fetchDepartmentInfo();
    
  } catch (error) {
    console.error('批量添加地点失败:', error);
    ElMessage.error('批量添加地点失败: ' + (error.message || '未知错误'));
  } finally {
    addingLocation.value = false;
  }
};

// 提交自定义添加地点
const submitAddCustomLocation = async () => {
  // 验证表单
  if (!addFormRef.value) return;
  
  await addFormRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }

    try {
      addingLocation.value = true;
      
      const locationData = {
        locationName: addForm.locationName,
        building: addForm.building,
        floorLevel: addForm.floorLevel,
        roomNumber: addForm.roomNumber,
        capacity: addForm.capacity
      };
      
      await addLocationToDepartment(currentDepartment.value.departmentId, locationData);
      
      ElMessage.success('地点添加成功');
      addDialogVisible.value = false;
      
      // 刷新地点列表
      await fetchDepartmentInfo();
      
    } catch (error) {
      console.error('添加地点失败:', error);
      ElMessage.error('添加地点失败: ' + (error.message || '未知错误'));
    } finally {
      addingLocation.value = false;
    }
  });
};

// 移除地点（从科室中移除，地点将变为未分配状态）
const handleRemove = async (locationId) => {
  try {
    await ElMessageBox.confirm(
      '确定要从该科室移除该地点吗？地点将变为未分配状态。',
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await removeLocationFromDepartment(currentDepartment.value.departmentId, locationId);
    ElMessage.success('地点移除成功');
    
    // 刷新地点列表
    await fetchDepartmentInfo();
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除地点失败:', error);
      ElMessage.error('移除地点失败: ' + (error.message || '未知错误'));
    }
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchDepartmentInfo();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.back-area {
  margin-bottom: 12px;
}

.card-header-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.department-name-title {
  margin: 0;
  color: #303133;
}

.location-management-section {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}

.location-tabs {
  min-height: 300px;
}

:deep(.el-tabs__content) {
  padding-top: 20px;
}
</style>

