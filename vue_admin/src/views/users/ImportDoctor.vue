<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>批量导入医生</span>
        </div>
      </template>

      <div class="import-steps">
        <el-steps :active="importResult ? 3 : activeStep" finish-status="success" align-center>
          <el-step title="下载模板" />
          <el-step title="上传Excel文件" />
          <el-step title="确认并导入" />
        </el-steps>
      </div>

      <!-- 步骤1: 下载模板 -->
      <div v-if="activeStep === 0" class="step-content">
        <h3>第一步：下载并填写医生模板</h3>
        <p>请下载医生专用Excel模板，按照模板格式填写医生信息</p>
        <el-button type="primary" :icon="Download" @click="downloadTemplate">下载医生模板.xlsx</el-button>
        <el-button type="success" @click="nextStep" style="margin-left: 12px;">下一步</el-button>
      </div>

      <!-- 步骤2: 上传文件 -->
      <div v-if="activeStep === 1" class="step-content">
        <h3>第二步：上传医生Excel文件</h3>
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
              请上传.xlsx或.xls格式的文件，大小不超过10MB
            </div>
          </template>
        </el-upload>
        <div class="button-group">
          <el-button @click="prevStep">上一步</el-button>
        </div>
      </div>

      <!-- 步骤3: 确认并导入 -->
      <div v-if="activeStep === 2" class="step-content">
        <!-- 导入前：显示数据预览 -->
        <div v-if="!importResult">
          <h3>第三步：确认医生数据</h3>
          <p>共 {{ parsedData.length }} 条医生数据，请确认信息无误后点击"开始导入"</p>
          
          <el-table :data="parsedData" border stripe max-height="400" style="margin: 20px 0;">
            <el-table-column prop="id" label="工号" width="120" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="departmentId" label="科室ID" width="100" />
            <el-table-column prop="title" label="职称" width="120" />
            <el-table-column prop="specialty" label="专长" show-overflow-tooltip />
            <el-table-column prop="id_card" label="身份证号" width="180" />
            <el-table-column prop="phone" label="手机号" width="120" />
          </el-table>
          
          <div class="button-group">
            <el-button @click="prevStep">上一步</el-button>
            <el-button 
              type="success" 
              :disabled="parsedData.length === 0 || importing" 
              :loading="importing"
              @click="startImport">
              {{ importing ? `导入中 (${importProgress}/${parsedData.length})` : '开始导入' }}
            </el-button>
          </div>
        </div>

        <!-- 导入后：显示结果 -->
        <div v-else>
          <h3>导入完成</h3>
          <el-result 
            :icon="importResult.type === 'success' ? 'success' : 'warning'"
            :title="importResult.title">
            <template #sub-title>
              <div class="result-details">
                <p style="font-size: 16px; margin: 10px 0;">
                  <span style="color: #67c23a;">✓ 成功: {{ importResult.successCount }} 条</span>
                </p>
                <p v-if="importResult.failureCount > 0" style="font-size: 16px; margin: 10px 0;">
                  <span style="color: #f56c6c;">✗ 失败: {{ importResult.failureCount }} 条</span>
                </p>
                <div v-if="importResult.errors.length > 0" style="margin-top: 20px;">
                  <el-divider>错误详情</el-divider>
                  <el-scrollbar max-height="200px">
                    <div class="error-list">
                      <p v-for="(error, index) in importResult.errors" :key="index" class="error-item">
                        {{ error }}
                      </p>
                    </div>
                  </el-scrollbar>
                </div>
              </div>
            </template>
            <template #extra>
              <el-button type="primary" @click="resetImport">继续导入</el-button>
              <el-button @click="$router.push('/users')">返回用户管理</el-button>
            </template>
          </el-result>
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
import { createUser } from '@/api/user';

const activeStep = ref(0);
const parsedData = ref([]);
const importing = ref(false);
const importProgress = ref(0);
const importResult = ref(null);

const nextStep = () => {
  if (activeStep.value++ > 2) activeStep.value = 0;
};
const prevStep = () => {
  if (activeStep.value-- < 0) activeStep.value = 0;
};

// 下载医生模板
const downloadTemplate = () => {
  const templateData = [
    { 
      '工号*': 'D001', 
      '姓名*': '张医生', 
      '密码*': '123456', 
      '身份证号*': '110101198001011234', 
      '手机号*': '13800138000',
      '科室ID*': '1',
      '职称': '主任医师',
      '专长': '内科、心血管疾病',
      '个人简介': '从事临床工作20年'
    },
    { 
      '工号*': 'D002', 
      '姓名*': '李医生', 
      '密码*': '123456', 
      '身份证号*': '110101199001011234', 
      '手机号*': '13900139000',
      '科室ID*': '2',
      '职称': '副主任医师',
      '专长': '外科、骨科',
      '个人简介': '从事临床工作15年'
    }
  ];
  
  const worksheet = XLSX.utils.json_to_sheet(templateData);
  
  // 添加说明
  const range = XLSX.utils.decode_range(worksheet['!ref']);
  let lastRow = range.e.r + 2;
  
  worksheet[`A${lastRow}`] = { t: 's', v: '重要说明：' };
  lastRow++;
  worksheet[`A${lastRow}`] = { t: 's', v: '1. 带*号的字段为必填项' };
  lastRow++;
  worksheet[`A${lastRow}`] = { t: 's', v: '2. 密码长度至少6位' };
  lastRow++;
  worksheet[`A${lastRow}`] = { t: 's', v: '3. 职称、专长、个人简介为选填项' };
  
  range.e.r = lastRow - 1;
  worksheet['!ref'] = XLSX.utils.encode_range(range);
  
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "医生模板");
  XLSX.writeFile(workbook, "医生导入模板.xlsx");
};

// 处理文件上传
const handleFileChange = (file) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const worksheet = workbook.Sheets[workbook.SheetNames[0]];
      const jsonData = XLSX.utils.sheet_to_json(worksheet);

      if (jsonData.length === 0) {
        ElMessage.error('Excel文件为空！');
        return;
      }

      // 解析并验证数据
      const doctors = [];
      for (let i = 0; i < jsonData.length; i++) {
        const row = jsonData[i];
        
        // 验证必填字段
        if (!row['工号*'] || !row['姓名*'] || !row['密码*'] || 
            !row['身份证号*'] || !row['手机号*'] || !row['科室ID*']) {
          ElMessage.error(`第${i + 2}行数据不完整，请检查必填项（包括科室ID）`);
          return;
        }

        doctors.push({
          id: String(row['工号*']),
          name: row['姓名*'],
          password: row['密码*'],
          id_card: String(row['身份证号*']),
          phone: String(row['手机号*']),
          title: row['职称'] || '',
          specialty: row['专长'] || '',
          bio: row['个人简介'] || '',
          departmentId: row['科室ID*'] ? parseInt(row['科室ID*']) : 1 // 科室ID，默认为1
        });
      }

      parsedData.value = doctors;
      importResult.value = null;
      nextStep();
      
    } catch (error) {
      ElMessage.error('文件解析失败，请确保文件格式正确！');
      console.error(error);
    }
  };
  reader.readAsArrayBuffer(file.raw);
};

// 开始导入
const startImport = async () => {
  importing.value = true;
  importProgress.value = 0;
  
  const successList = [];
  const errorList = [];

  for (let i = 0; i < parsedData.value.length; i++) {
    const doctor = parsedData.value[i];
    
    try {
      await createUser({
        role: 'DOCTOR',
        id: doctor.id,
        name: doctor.name,
        password: doctor.password,
        doctorStatus: 'inactive',
        id_card: doctor.id_card,
        phone: doctor.phone,
        title: doctor.title,
        specialty: doctor.specialty,
        bio: doctor.bio,
        departmentId: doctor.departmentId || 1 // 使用Excel中的科室ID，如果没有则默认为1
      });
      
      successList.push(doctor.id);
      importProgress.value++;
      
    } catch (error) {
      const errorMsg = `工号 ${doctor.id} (${doctor.name}): ${error.response?.data?.message || error.message}`;
      errorList.push(errorMsg);
      importProgress.value++;
    }
  }

  importing.value = false;
  
  importResult.value = {
    title: errorList.length === 0 ? '导入成功！' : '导入完成（部分失败）',
    type: errorList.length === 0 ? 'success' : 'warning',
    successCount: successList.length,
    failureCount: errorList.length,
    errors: errorList
  };

  if (errorList.length === 0) {
    ElMessage.success(`成功导入 ${successList.length} 条医生数据`);
  } else {
    ElMessage.warning(`成功 ${successList.length} 条，失败 ${errorList.length} 条`);
  }
};

// 重置导入流程
const resetImport = () => {
  activeStep.value = 0;
  parsedData.value = [];
  importResult.value = null;
  importProgress.value = 0;
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
.result-details {
  text-align: center;
}
.error-list {
  text-align: left;
}
.error-item {
  font-size: 12px;
  color: #f56c6c;
  margin: 4px 0;
  padding: 8px;
  background-color: #fef0f0;
  border-radius: 4px;
}
</style>

