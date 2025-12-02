<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>批量导入用户</span>
        </div>
      </template>

      <div class="import-steps">
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step title="下载模板" />
          <el-step title="上传Excel文件" />
          <el-step title="确认并导入" />
        </el-steps>
      </div>

      <!-- 步骤1: 下载模板 -->
      <div v-if="activeStep === 0" class="step-content">
        <h3>第一步：下载并填写模板文件</h3>
        <p>请下载标准的Excel模板，并按照模板中的格式填写用户信息。完成后，请进入下一步上传文件。</p>
        <el-button type="primary" :icon="Download" @click="downloadTemplate">下载模板.xlsx</el-button>
        <el-button type="success" @click="nextStep" style="margin-left: 12px;">下一步</el-button>
      </div>

      <!-- 步骤2: 上传文件 -->
      <div v-if="activeStep === 1" class="step-content">
        <h3>第二步：选择并上传Excel文件</h3>
        <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".xlsx, .xls"
            drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              请上传.xlsx或.xls格式的文件，大小不超过10MB。
            </div>
          </template>
        </el-upload>
        <div class="button-group">
          <el-button @click="prevStep">上一步</el-button>
        </div>
      </div>

      <!-- 步骤3: 确认数据并导入 -->
      <div v-if="activeStep === 2" class="step-content">
        <h3>第三步：预览数据并确认导入</h3>
        <p>请检查从Excel文件中解析出的数据是否正确。确认无误后，点击“确认导入”按钮完成操作。</p>
        <el-table :data="parsedData" border stripe height="300">
          <el-table-column v-for="header in headers" :key="header" :prop="header" :label="header" />
        </el-table>
        <div class="button-group">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="success" :disabled="parsedData.length === 0" @click="confirmImport">确认导入</el-button>
        </div>
      </div>

    </el-card>
  </div>
</template>

<script setup>
import BackButton from '@/components/BackButton.vue';
import { ref } from 'vue';
import * as XLSX from 'xlsx';
import { ElMessage } from 'element-plus';
import { Download, UploadFilled } from '@element-plus/icons-vue';

const activeStep = ref(0);
const parsedData = ref([]);
const headers = ref([]);

const nextStep = () => {
  if (activeStep.value++ > 2) activeStep.value = 0;
};
const prevStep = () => {
  if (activeStep.value-- < 0) activeStep.value = 0;
};

// 下载模板
const downloadTemplate = () => {
  const templateData = [
    { '学号/工号*': '2023001', '姓名*': '示例用户', '密码*': '123456', '角色*': 'patient', '身份证号*': '110xxxxxxxxxxxxxxx', '手机号*': '13800138000', '过敏史': '无', '既往病史': '无' }
  ];
  const worksheet = XLSX.utils.json_to_sheet(templateData);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "用户模板");
  XLSX.writeFile(workbook, "用户导入模板.xlsx");
};

// 处理文件上传
const handleFileChange = (file) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];
      const jsonData = XLSX.utils.sheet_to_json(worksheet);

      if (jsonData.length === 0) {
        ElMessage.error('Excel文件为空或格式不正确！');
        return;
      }

      // 自动提取表头
      headers.value = Object.keys(jsonData[0]);
      parsedData.value = jsonData;
      nextStep(); // 自动进入下一步
    } catch (error) {
      ElMessage.error('文件解析失败，请确保文件格式正确！');
      console.error(error);
    }
  };
  reader.readAsArrayBuffer(file.raw);
};

// 确认导入
const confirmImport = () => {
  console.log('正在导入以下数据:', parsedData.value);
  // 在这里，您可以将 parsedData.value 发送到后端API
  ElMessage.success(`成功导入 ${parsedData.value.length} 条用户数据 (模拟)`);
  // 导入成功后可以重置
  activeStep.value = 0;
  parsedData.value = [];
  headers.value = [];
};
</script>

<style scoped>
.app-container {
  padding: 24px;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
.import-steps {
  margin-bottom: 40px;
}
.step-content {
  text-align: center;
}
.step-content h3 {
  margin-bottom: 8px;
  font-size: 18px;
}
.step-content p {
  color: #606266;
  margin-bottom: 20px;
}
.button-group {
  margin-top: 20px;
}
</style>
