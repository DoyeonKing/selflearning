<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>班次定义与管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增班次</el-button>
        </div>
      </template>

      <el-table :data="shiftTemplates" border stripe>
        <el-table-column prop="name" label="班次名称" />
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑班次弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班次名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：上午班" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" placeholder="选择开始时间" style="width: 100%;"/>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" placeholder="选择结束时间" style="width: 100%;"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';

// 模拟班次模板数据
const shiftTemplates = ref([
  { id: 1, name: '上午班', startTime: '08:00:00', endTime: '12:00:00' },
  { id: 2, name: '下午班', startTime: '14:00:00', endTime: '18:00:00' },
  { id: 3, name: '夜班', startTime: '18:00:00', endTime: '23:00:00' },
]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);

const form = reactive({
  id: null,
  name: '',
  startTime: '',
  endTime: '',
});

const rules = {
  name: [{ required: true, message: '请输入班次名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
};

const dialogTitle = computed(() => (isEdit.value ? '编辑班次' : '新增班次'));

// 新增
const handleAdd = () => {
  isEdit.value = false;
  Object.assign(form, { id: null, name: '', startTime: '', endTime: '' });
  dialogVisible.value = true;
};

// 编辑
const handleEdit = (row) => {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除班次 "${row.name}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    // 模拟删除
    const index = shiftTemplates.value.findIndex(item => item.id === row.id);
    if (index !== -1) {
      shiftTemplates.value.splice(index, 1);
    }
    ElMessage.success('删除成功');
  }).catch(() => {});
};

// 保存
const handleSave = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      if (isEdit.value) {
        // 模拟编辑
        const index = shiftTemplates.value.findIndex(item => item.id === form.id);
        if (index !== -1) {
          shiftTemplates.value[index] = { ...form };
        }
      } else {
        // 模拟新增
        const newId = Math.max(...shiftTemplates.value.map(item => item.id), 0) + 1;
        shiftTemplates.value.push({ ...form, id: newId });
      }
      ElMessage.success('保存成功');
      dialogVisible.value = false;
    }
  });
};
</script>

<style scoped>
.app-container {
  padding: 24px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}
</style>

