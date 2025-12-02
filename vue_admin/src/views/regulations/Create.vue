<template>
  <div class="create-regulation">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>新增就医规范</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px;"
      >
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

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            保存
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
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAdminStore } from '@/stores/adminStore';
import BackButton from '@/components/BackButton.vue';
import { createGuideline } from '@/api/guideline';

const router = useRouter();
const adminStore = useAdminStore();

// --- 数据 ---
const formRef = ref(null);
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
  title: '',
  category: '',
  content: '',
  status: 'active'
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

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate();
    if (valid) {
      submitting.value = true;
      
      const submitData = {
        title: form.title,
        content: form.content,
        category: form.category,
        status: form.status,
        createdBy: parseInt(adminStore.currentAdminId) || 1
      };
      
      // 调试信息
      console.log('adminStore.currentAdminId:', adminStore.currentAdminId);
      console.log('parseInt result:', parseInt(adminStore.currentAdminId));
      console.log('submitData:', submitData);
      await createGuideline(submitData);
      ElMessage.success('规范创建成功！');
      router.push('/regulations');
    }
  } catch (error) {
    if (error !== false) { // false表示验证失败
      ElMessage.error('创建失败：' + (error.message || '未知错误'));
    } else {
      ElMessage.error('请填写完整信息');
    }
  } finally {
    submitting.value = false;
  }
};

const handleReset = () => {
  formRef.value.resetFields();
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
</script>

<style scoped>
.create-regulation {
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

