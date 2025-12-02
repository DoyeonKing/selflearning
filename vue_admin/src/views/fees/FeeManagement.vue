<template>
  <div class="fee-management-container">
    <div class="department-sidebar">
      <el-menu :default-active="activeParent" class="department-menu" @select="handleParentSelect">
        <el-menu-item v-for="parent in departments" :key="parent.id" :index="parent.id">
          <span>{{ parent.name }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sub-department-panel" v-if="subDepartments.length > 0">
        <div
            v-for="sub in subDepartments"
            :key="sub.id"
            class="sub-department-item"
            :class="{ 'active': activeSub === sub.id }"
            @click="handleSubSelect(sub.id)">
          {{ sub.name }}
        </div>
      </div>
    </div>

    <div class="fee-content">
      <el-card shadow="always">
        <template #header>
          <div class="card-header">
            <span>{{ selectedDepartmentName }} - 挂号费用规则</span>
          </div>
        </template>

        <div v-if="activeSub">
          <el-table :data="doctorsInDept" border stripe>
            <el-table-column prop="id" label="医生ID" width="120" />
            <el-table-column prop="name" label="姓名" width="150" />
            <el-table-column prop="title" label="职称" />
            <el-table-column prop="fee" label="挂号费用 (元)" width="150">
              <template #default="{ row }">
                <span>¥ {{ row.fee.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" :icon="Edit" @click="handleEditFee(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="placeholder">
          <el-empty description="请在左侧选择一个科室以设置费用规则" />
        </div>
      </el-card>
    </div>

    <el-dialog v-model="editDialogVisible" title="修改挂号费用" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="医生">
          <span>{{ editForm.name }}</span>
        </el-form-item>
        <el-form-item label="新费用">
          <el-input-number v-model="editForm.fee" :precision="2" :step="1" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFee">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue';
import { Edit } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

// Mock Data
const departments = ref([
  { id: 'p1', name: '内科', children: [{ id: 's1-1', name: '呼吸内科' }, { id: 's1-2', name: '心血管科' }] },
  { id: 'p2', name: '外科', children: [{ id: 's2-1', name: '普外科' }] },
  { id: 'p3', name: '妇产科', children: [] },
]);

const doctorsData = ref({
  's1-1': [
    { id: 4, name: '张医生', title: '主治医师', fee: 15.00 },
  ],
  's1-2': [
    { id: 1, name: '杨青松', title: '主任医师', fee: 25.00 },
    { id: 2, name: '杨林', title: '副主任医师', fee: 20.00 },
    { id: 3, name: '席紫明', title: '主治医师', fee: 15.00 }
  ],
  'p3': [
    { id: 6, name: '王莉', title: '主任医师', fee: 25.00 }
  ],
});

// Component State
const activeParent = ref(null);
const activeSub = ref(null);
const editDialogVisible = ref(false);
const editForm = reactive({
  id: null,
  name: '',
  fee: 0,
});

// Computed Properties
const subDepartments = computed(() => {
  if (!activeParent.value) return [];
  const parent = departments.value.find(p => p.id === activeParent.value);
  return parent ? parent.children : [];
});

const selectedDepartmentName = computed(() => {
  if (!activeSub.value) return '请选择科室';
  for (const parent of departments.value) {
    if (parent.id === activeSub.value) return parent.name;
    const sub = parent.children.find(c => c.id === activeSub.value);
    if (sub) return sub.name;
  }
  return '未知科室';
});

const doctorsInDept = computed(() => {
  return activeSub.value ? doctorsData.value[activeSub.value] || [] : [];
});

// Methods
const handleParentSelect = (index) => {
  activeParent.value = index;
  const parent = departments.value.find(p => p.id === index);
  if (parent) {
    activeSub.value = (parent.children && parent.children.length > 0) ? parent.children[0].id : parent.id;
  }
};

const handleSubSelect = (id) => {
  activeSub.value = id;
};

const handleEditFee = (doctor) => {
  editForm.id = doctor.id;
  editForm.name = doctor.name;
  editForm.fee = doctor.fee;
  editDialogVisible.value = true;
};

const handleSaveFee = () => {
  const deptDoctors = doctorsData.value[activeSub.value];
  if (deptDoctors) {
    const doctor = deptDoctors.find(d => d.id === editForm.id);
    if (doctor) {
      doctor.fee = editForm.fee;
      ElMessage.success('费用更新成功');
    }
  }
  editDialogVisible.value = false;
};

onMounted(() => {
  if (departments.value.length > 0) {
    handleParentSelect(departments.value[0].id);
  }
});
</script>

<style scoped>
.fee-management-container {
  display: flex;
  height: calc(100vh - 50px);
  background-color: #f7fafc;
}
.department-sidebar {
  width: 320px;
  display: flex;
  background-color: #fff;
  border-right: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.department-menu {
  width: 120px;
  border-right: none;
}
.sub-department-panel {
  flex: 1;
  padding: 8px;
  border-left: 1px solid #e2e8f0;
}
.sub-department-item {
  padding: 10px 15px;
  cursor: pointer;
  border-radius: 4px;
}
.sub-department-item:hover {
  background-color: #f5f7fa;
}
.sub-department-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: bold;
}
.fee-content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}
.placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>