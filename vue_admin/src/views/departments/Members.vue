<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always" v-if="currentDepartment" v-loading="loading">
      <template #header>
        <div class="card-header-title">
          <h2 class="department-name-title">{{ currentDepartment.name }}</h2>
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            添加成员
          </el-button>
        </div>
      </template>

      <div class="member-management-section">
        <el-table :data="currentDepartment.members" border stripe>
          <el-table-column prop="identifier" label="医生工号" width="150" />
          <el-table-column prop="fullName" label="医生姓名" width="180" />
          <el-table-column prop="title" label="职称" />
          <el-table-column prop="phoneNumber" label="联系电话" width="150" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="handleDelete(row.identifier)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!currentDepartment.members || currentDepartment.members.length === 0" description="该科室暂无成员" />
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="未找到指定的科室信息或获取失败" />

    <el-dialog v-model="addDialogVisible" title="添加医生到科室" width="800px">
      <div v-loading="loadingUnassignedDoctors">
        <div style="margin-bottom: 16px;">
          <el-text type="info">从以下未分配科室的医生中选择要添加到当前科室的医生：</el-text>
        </div>
        
        <el-table 
          :data="unassignedDoctors" 
          @selection-change="handleSelectionChange"
          style="margin-bottom: 16px;"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="identifier" label="医生工号" width="120" />
          <el-table-column prop="fullName" label="医生姓名" width="150" />
          <el-table-column prop="title" label="职称" width="120" />
          <el-table-column prop="phoneNumber" label="联系电话" width="150" />
          <el-table-column prop="specialty" label="擅长领域" />
        </el-table>
        
        <el-empty v-if="!loadingUnassignedDoctors && unassignedDoctors.length === 0" description="暂无未分配的医生" />
        
        <div v-if="selectedDoctors.length > 0" style="margin-top: 16px;">
          <el-text type="primary">已选择 {{ selectedDoctors.length }} 位医生</el-text>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitBatchAddDoctors" 
            :loading="addingMember"
            :disabled="selectedDoctors.length === 0"
          >
            确认添加 ({{ selectedDoctors.length }})
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
import { getDepartmentById, addDepartmentMember, deleteDepartmentMember, getDoctorsByDepartmentId, getUnassignedDoctors, batchAddDoctorsToDepartment } from '@/api/department';

const route = useRoute();
const router = useRouter();

// 数据状态
const currentDepartment = ref(null);
const loading = ref(false);
const addDialogVisible = ref(false);
const addingMember = ref(false);
const unassignedDoctors = ref([]);
const selectedDoctors = ref([]);
const loadingUnassignedDoctors = ref(false);

const addMemberFormRef = ref(null);

// 获取科室信息
const fetchDepartmentInfo = async () => {
  const departmentId = route.params.id;
  if (!departmentId) {
    ElMessage.error('缺少科室ID参数');
    router.back();
    return;
  }

  loading.value = true;
  try {
    // 并行获取科室信息和医生列表
    const [departmentResponse, doctorsResponse] = await Promise.all([
      getDepartmentById(departmentId),
      getDoctorsByDepartmentId(departmentId)
    ]);
    
    if (departmentResponse) {
      currentDepartment.value = departmentResponse;
      // 设置医生列表
      currentDepartment.value.members = doctorsResponse || [];
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

// 打开添加成员对话框
const openAddDialog = () => {
  selectedDoctors.value = [];
  addDialogVisible.value = true;
  fetchUnassignedDoctors();
};

// 获取未分配医生列表
const fetchUnassignedDoctors = async () => {
  loadingUnassignedDoctors.value = true;
  try {
    const response = await getUnassignedDoctors();
    console.log('未分配医生API响应:', response);
    
    // 确保响应是数组
    if (Array.isArray(response)) {
      unassignedDoctors.value = response;
    } else if (response && Array.isArray(response.data)) {
      unassignedDoctors.value = response.data;
    } else {
      console.warn('未分配医生响应格式异常:', response);
      unassignedDoctors.value = [];
    }
    
    console.log('处理后的未分配医生列表:', unassignedDoctors.value);
  } catch (error) {
    console.error('获取未分配医生列表失败:', error);
    ElMessage.error('获取未分配医生列表失败: ' + (error.message || '未知错误'));
    unassignedDoctors.value = [];
  } finally {
    loadingUnassignedDoctors.value = false;
  }
};

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedDoctors.value = selection;
};

// 批量添加医生
const submitBatchAddDoctors = async () => {
  if (selectedDoctors.value.length === 0) {
    ElMessage.warning('请选择要添加的医生');
    return;
  }

  try {
    addingMember.value = true;
    
    const doctorIdentifiers = selectedDoctors.value.map(doctor => doctor.identifier);
    await batchAddDoctorsToDepartment(currentDepartment.value.departmentId, doctorIdentifiers);
    
    ElMessage.success(`成功添加 ${selectedDoctors.value.length} 位医生到科室`);
    addDialogVisible.value = false;
    
    // 刷新科室信息
    await fetchDepartmentInfo();
    
  } catch (error) {
    console.error('批量添加医生失败:', error);
    ElMessage.error('批量添加医生失败: ' + (error.message || '未知错误'));
  } finally {
    addingMember.value = false;
  }
};

// 删除成员
const handleDelete = async (doctorIdentifier) => {
  try {
    await ElMessageBox.confirm(
      '确定要从该科室移除该医生吗？医生将被移动到未分配科室。',
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await deleteDepartmentMember(currentDepartment.value.departmentId, doctorIdentifier);
    ElMessage.success('医生移除成功');
    
    // 刷新科室信息
    await fetchDepartmentInfo();
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除医生失败:', error);
      ElMessage.error('移除医生失败: ' + (error.message || '未知错误'));
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

.member-management-section {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}
</style>