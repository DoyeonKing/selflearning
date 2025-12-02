<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always">
      <template #header>
        <div class="card-header-title">
          <h2>症状-科室映射管理</h2>
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            添加映射
          </el-button>
        </div>
      </template>

      <div v-loading="loading">
        <!-- 搜索筛选 -->
        <el-form :inline="true" class="filter-form">
          <el-form-item label="科室">
            <el-select 
              v-model="filterDepartmentId" 
              placeholder="选择科室" 
              clearable
              @change="handleFilter"
              style="width: 200px"
            >
              <el-option label="全部" :value="null" />
              <el-option
                v-for="dept in departments"
                :key="dept.departmentId"
                :label="dept.name"
                :value="dept.departmentId"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleFilter">搜索</el-button>
            <el-button :icon="Refresh" @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 数据表格 -->
        <el-table :data="filteredMappings" border stripe style="margin-top: 20px;">
          <el-table-column prop="mappingId" label="映射ID" width="100" />
          <el-table-column prop="symptomKeywords" label="症状关键词" min-width="250">
            <template #default="{ row }">
              <el-tag 
                v-for="(keyword, index) in row.symptomKeywords.split(',')" 
                :key="index"
                style="margin: 2px"
                size="small"
              >
                {{ keyword.trim() }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="推荐科室" width="150" />
          <el-table-column prop="priority" label="优先级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.priority === 1 ? 'success' : 'info'">
                {{ row.priority }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                :icon="Edit"
                @click="openEditDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(row.mappingId)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && filteredMappings.length === 0" description="暂无症状映射数据" />
      </div>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑症状映射' : '添加症状映射'" 
      width="600px"
    >
      <el-form 
        ref="formRef"
        :model="form" 
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="症状关键词" prop="symptomKeywords">
          <el-input 
            v-model="form.symptomKeywords" 
            type="textarea"
            :rows="3"
            placeholder="请输入症状关键词，多个关键词用逗号分隔，如：咳嗽,咳痰,气喘"
          />
          <div style="margin-top: 5px; color: #909399; font-size: 12px;">
            提示：多个关键词用英文逗号分隔
          </div>
        </el-form-item>
        <el-form-item label="推荐科室" prop="departmentId">
          <el-select 
            v-model="form.departmentId" 
            placeholder="选择科室"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="dept in departments"
              :key="dept.departmentId"
              :label="dept.name"
              :value="dept.departmentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number 
            v-model="form.priority" 
            :min="1" 
            :max="10"
            style="width: 200px"
          />
          <div style="margin-top: 5px; color: #909399; font-size: 12px;">
            提示：数值越小优先级越高
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitForm" 
            :loading="submitting"
          >
            确认{{ isEdit ? '保存' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Delete, Edit, Search, Refresh } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';

import { 
  getAllSymptomMappings, 
  createSymptomMapping, 
  updateSymptomMapping, 
  deleteSymptomMapping 
} from '@/api/symptomMapping';
import { getAllDepartments } from '@/api/department';

// 数据状态
const loading = ref(false);
const mappings = ref([]);
const departments = ref([]);
const filterDepartmentId = ref(null);
const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);

const formRef = ref(null);
const form = reactive({
  mappingId: null,
  symptomKeywords: '',
  departmentId: null,
  priority: 1
});

// 表单验证规则
const formRules = {
  symptomKeywords: [
    { required: true, message: '请输入症状关键词', trigger: 'blur' }
  ],
  departmentId: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'blur' }
  ]
};

// 过滤后的映射数据
const filteredMappings = computed(() => {
  if (!filterDepartmentId.value) {
    return mappings.value;
  }
  return mappings.value.filter(m => m.departmentId === filterDepartmentId.value);
});

// 获取所有症状映射
const fetchMappings = async () => {
  loading.value = true;
  try {
    const response = await getAllSymptomMappings();
    mappings.value = response || [];
  } catch (error) {
    console.error('获取症状映射失败:', error);
    ElMessage.error('获取症状映射失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 获取所有科室
const fetchDepartments = async () => {
  try {
    const response = await getAllDepartments();
    // 过滤掉未分配科室
    departments.value = (response.content || response || []).filter(
      dept => dept.departmentId !== 999
    );
  } catch (error) {
    console.error('获取科室列表失败:', error);
    ElMessage.error('获取科室列表失败');
  }
};

// 打开添加对话框
const openAddDialog = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

// 打开编辑对话框
const openEditDialog = (row) => {
  isEdit.value = true;
  form.mappingId = row.mappingId;
  form.symptomKeywords = row.symptomKeywords;
  form.departmentId = row.departmentId;
  form.priority = row.priority;
  dialogVisible.value = true;
};

// 重置表单
const resetForm = () => {
  form.mappingId = null;
  form.symptomKeywords = '';
  form.departmentId = null;
  form.priority = 1;
  if (formRef.value) {
    formRef.value.resetFields();
  }
};

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    try {
      submitting.value = true;
      
      const data = {
        symptomKeywords: form.symptomKeywords,
        departmentId: form.departmentId,
        priority: form.priority
      };
      
      if (isEdit.value) {
        await updateSymptomMapping(form.mappingId, data);
        ElMessage.success('症状映射更新成功');
      } else {
        await createSymptomMapping(data);
        ElMessage.success('症状映射添加成功');
      }
      
      dialogVisible.value = false;
      await fetchMappings();
      
    } catch (error) {
      console.error('操作失败:', error);
      ElMessage.error('操作失败: ' + (error.message || '未知错误'));
    } finally {
      submitting.value = false;
    }
  });
};

// 删除映射
const handleDelete = async (mappingId) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该症状映射吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await deleteSymptomMapping(mappingId);
    ElMessage.success('删除成功');
    await fetchMappings();
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败: ' + (error.message || '未知错误'));
    }
  }
};

// 筛选
const handleFilter = () => {
  // filteredMappings 是计算属性，会自动更新
};

// 重置筛选
const resetFilter = () => {
  filterDepartmentId.value = null;
};

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-';
  const date = new Date(dateTime);
  return date.toLocaleString('zh-CN');
};

// 组件挂载时获取数据
onMounted(() => {
  fetchDepartments();
  fetchMappings();
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

.card-header-title h2 {
  margin: 0;
  color: #303133;
}

.filter-form {
  margin-bottom: 20px;
}

.dialog-footer {
  text-align: right;
}
</style>

