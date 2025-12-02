<template>
  <div class="edit-regulation">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>编辑就医规范</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px;"
      >
        <el-form-item label="规范ID">
          <el-input v-model="form.guidelineId" disabled />
        </el-form-item>

        <el-form-item label="规范标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入规范标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select
            v-model="form.category"
            placeholder="选择分类"
            style="width: 100%;"
            allow-create
            filterable
          >
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
          <span class="help-text">可选择已有分类或输入新分类</span>
        </el-form-item>

        <el-form-item label="规范内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="请输入规范内容，支持HTML格式"
          />
          <div class="editor-toolbar">
            <el-button size="small" @click="insertHeading">插入标题</el-button>
            <el-button size="small" @click="insertParagraph">插入段落</el-button>
            <el-button size="small" @click="insertList">插入列表</el-button>
            <el-button size="small" @click="insertBold">加粗</el-button>
          </div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
          <div class="help-text">禁用后患者端将不会显示此规范</div>
        </el-form-item>

        <el-form-item label="创建信息">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="创建人">
              {{ form.createdByName }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ form.createdAt }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间" :span="2">
              {{ form.updatedAt }}
            </el-descriptions-item>
          </el-descriptions>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            保存修改
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handlePreview">预览</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="预览规范"
      width="700px"
    >
      <div class="preview-content">
        <h2>{{ form.title }}</h2>
        <el-tag :type="getCategoryTagType(form.category)" style="margin-bottom: 15px;">
          {{ form.category }}
        </el-tag>
        <el-divider />
        <div v-html="form.content" class="regulation-content"></div>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAdminStore } from '@/stores/adminStore';
import BackButton from '@/components/BackButton.vue';
import { getGuidelineById, updateGuideline } from '@/api/guideline';

const router = useRouter();
const route = useRoute();
const adminStore = useAdminStore();

// --- 数据 ---
const formRef = ref(null);
const loading = ref(false);
const submitting = ref(false);
const previewDialogVisible = ref(false);

const categories = ref([
  '就医流程',
  '就医须知',
  '退号政策',
  '支付说明',
  '候补规则',
  '其他'
]);

const form = reactive({
  guidelineId: '',
  title: '',
  category: '',
  content: '',
  status: 'active',
  createdBy: null,  // 管理员ID (INT)
  createdByName: '',  // 管理员名称（用于显示）
  createdAt: '',
  updatedAt: ''
});

const rules = {
  title: [
    { required: true, message: '请输入规范标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度应在2-200个字符之间', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择或输入分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入规范内容', trigger: 'blur' },
    { min: 10, message: '内容至少10个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
};

// --- 方法 ---
const getCategoryTagType = (category) => {
  const types = {
    '就医流程': 'primary',
    '就医须知': 'success',
    '退号政策': 'warning',
    '支付说明': 'info',
    '候补规则': 'danger',
    '其他': ''
  };
  return types[category] || '';
};

const loadRegulationData = async () => {
  loading.value = true;
  const id = route.params.id;
  
  try {
    const response = await getGuidelineById(id);
    
    // 处理响应格式（前端拦截器已经处理了response.data）
    let regulationData = response;
    if (!regulationData) {
      throw new Error('响应数据为空');
    }
    
    // 将数据赋值给表单
    form.guidelineId = regulationData.guidelineId;
    form.title = regulationData.title || '';
    form.content = regulationData.content || '';
    form.category = regulationData.category || '';
    form.status = regulationData.status || 'active';
    form.createdBy = regulationData.createdBy;
    form.createdByName = regulationData.createdByName || '未知';
    form.createdAt = regulationData.createdAt || '';
    form.updatedAt = regulationData.updatedAt || '';
  } catch (error) {
    ElMessage.error('加载规范数据失败：' + (error.message || '未知错误'));
    router.push('/regulations');
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate();
    if (valid) {
      submitting.value = true;
      
      const submitData = {
        title: form.title,
        content: form.content,
        category: form.category,
        status: form.status
      };
      await updateGuideline(form.guidelineId, submitData);
      ElMessage.success('规范更新成功！');
      router.push('/regulations');
    }
  } catch (error) {
    if (error !== false) { // false表示验证失败
      ElMessage.error('更新失败：' + (error.message || '未知错误'));
    } else {
      ElMessage.error('请填写完整信息');
    }
  } finally {
    submitting.value = false;
  }
};

const handleReset = () => {
  loadRegulationData(); // 重新加载原始数据
};

const handlePreview = () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请先填写标题和内容');
    return;
  }
  previewDialogVisible.value = true;
};

// 编辑器辅助功能
const insertHeading = () => {
  form.content += '\n<h3>标题</h3>\n';
};

const insertParagraph = () => {
  form.content += '\n<p>段落内容</p>\n';
};

const insertList = () => {
  form.content += '\n<ul>\n  <li>项目1</li>\n  <li>项目2</li>\n  <li>项目3</li>\n</ul>\n';
};

const insertBold = () => {
  form.content += '<strong>加粗文字</strong>';
};

onMounted(() => {
  loadRegulationData();
});
</script>

<style scoped>
.edit-regulation {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.help-text {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.editor-toolbar {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.preview-content {
  max-height: 600px;
  overflow-y: auto;
}

.preview-content h2 {
  color: #303133;
  margin-bottom: 15px;
}

.regulation-content {
  padding: 15px;
  background-color: #f9fafb;
  border-radius: 8px;
  min-height: 200px;
  line-height: 1.8;
}

.regulation-content h3 {
  color: #303133;
  margin: 15px 0 10px 0;
}

.regulation-content p {
  margin-bottom: 10px;
  color: #606266;
}

.regulation-content ul {
  margin-left: 20px;
  color: #606266;
}

.regulation-content li {
  margin-bottom: 8px;
}
</style>

