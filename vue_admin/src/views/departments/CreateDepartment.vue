<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <h2>创建新科室</h2>
    <el-card shadow="always">
      <el-form
          ref="departmentFormRef"
          :model="departmentForm"
          :rules="rules"
          label-width="120px"
          style="max-width: 600px; margin-top: 20px;"
      >
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="departmentForm.name" placeholder="请输入科室名称"></el-input>
        </el-form-item>

        <el-form-item label="父科室" prop="parentDepartmentName">
          <el-select
              v-model="departmentForm.parentDepartmentName"
              placeholder="请选择父科室"
              style="width: 100%;"
              filterable
              clearable
          >
            <el-option
                v-for="department in parentDepartments"
                :key="department.parentDepartmentId"
                :label="department.name"
                :value="department.name" />
          </el-select>
        </el-form-item>

        <el-form-item label="科室描述" prop="description">
          <el-input
              v-model="departmentForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入科室的详细描述"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">立即创建</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import BackButton from '@/components/BackButton.vue';

// 导入 API 服务
import { createDepartment, getAllParentDepartments } from '@/api/department';

const router = useRouter();

// 表单的引用
const departmentFormRef = ref(null);
const submitting = ref(false);

// 父科室列表
const parentDepartments = ref([]);

// 表单数据模型
const departmentForm = reactive({
  name: '',
  parentDepartmentName: '',
  description: ''
});

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入科室名称', trigger: 'blur' },
    { min: 2, max: 100, message: '科室名称长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  parentDepartmentName: [
    { required: true, message: '请选择父科室', trigger: 'change' }
  ],
  description: [
    { max: 500, message: '描述长度不能超过 500 个字符', trigger: 'blur' }
  ]
};

// 获取父科室列表
const fetchParentDepartments = async () => {
  try {
    const response = await getAllParentDepartments();
    parentDepartments.value = response || [];
    console.log('父科室列表:', parentDepartments.value);
  } catch (error) {
    console.error('获取父科室列表失败:', error);
    ElMessage.error('获取父科室列表失败: ' + (error.message || '未知错误'));
  }
};

// 提交表单
const submitForm = async () => {
  if (!departmentFormRef.value) return;
  
  try {
    // 验证表单
    await departmentFormRef.value.validate();
    
    submitting.value = true;
    
    // 调用创建科室API
    await createDepartment(departmentForm);
    
    ElMessage.success('科室创建成功！');
    
    // 跳转回科室列表页面
    router.push('/departments');
    
  } catch (error) {
    console.error('创建科室失败:', error);
    ElMessage.error('创建科室失败: ' + (error.message || '未知错误'));
  } finally {
    submitting.value = false;
  }
};

// 重置表单
const resetForm = () => {
  if (departmentFormRef.value) {
    departmentFormRef.value.resetFields();
  }
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 组件挂载时获取父科室列表
onMounted(() => {
  fetchParentDepartments();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.back-area {
  margin-bottom: 12px;
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}
</style>