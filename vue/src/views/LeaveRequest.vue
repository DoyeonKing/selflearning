<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always" class="leave-card">
      <template #header>
        <div class="card-header">
          <span>我的休假申请</span>
          <el-button
              type="primary"
              :icon="Plus"
              @click="openApplyDialog">
            申请休假
          </el-button>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="leaveHistory"
          style="width: 100%; margin-top: 20px;"
          border
          stripe
      >
        <el-table-column prop="requestType" label="假期类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.requestType === 'SICK_LEAVE' ? 'warning' : 'info'">
              {{ formatRequestType(row.requestType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="150" sortable />
        <el-table-column prop="endDate" label="结束日期" width="150" sortable />
        <el-table-column prop="reason" label="请假事由" min-width="200" />
        <el-table-column label="请假证明" width="120" align="center">
          <template #default="{ row }">
            <el-button
                v-if="row.proofDocumentUrl"
                type="primary"
                link
                size="small"
                @click="viewProof(row.proofDocumentUrl)"
            >
              查看证明
            </el-button>
            <span v-else style="color: #909399;">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审批状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="handleCancel(row)"
                v-if="row.status === 'PENDING' || row.status === 'pending'">
              撤销申请
            </el-button>
            <el-button
                type="primary"
                link
                size="small"
                @click="viewFeedback(row)"
                v-else-if="row.status === 'REJECTED' || row.status === 'rejected'">
              查看返回意见
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && leaveHistory.length === 0" description="暂无休假记录" />

    </el-card>

    <!-- 查看返回意见对话框 -->
    <el-dialog
        v-model="feedbackDialogVisible"
        title="返回意见"
        width="500px"
    >
      <div v-if="currentFeedback">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请时间">
            {{ currentFeedback.startDate }} 至 {{ currentFeedback.endDate }}
          </el-descriptions-item>
          <el-descriptions-item label="请假事由">
            {{ currentFeedback.reason }}
          </el-descriptions-item>
          <el-descriptions-item label="审批状态">
            <el-tag type="danger">已拒绝</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批人">
            {{ currentFeedback.approver?.fullName || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="返回意见">
            <div style="color: #f56c6c; font-weight: 500;">
              {{ currentFeedback.approverComments || '无' }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间" v-if="currentFeedback.updatedAt">
            {{ formatDateTime(currentFeedback.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="feedbackDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="dialogVisible"
        title="申请休假"
        width="600px"
        @close="resetForm"
    >
      <el-form :model="applyForm" :rules="formRules" ref="applyFormRef" label-position="top">
        <el-form-item label="假期类型" prop="requestType">
          <el-radio-group v-model="applyForm.requestType">
            <el-radio label="PERSONAL_LEAVE">事假</el-radio>
            <el-radio label="SICK_LEAVE">病假</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
              v-model="applyForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input
              v-model="applyForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入详细的请假事由"
          />
        </el-form-item>
        <el-form-item label="请假证明" prop="proofDocument">
          <el-upload
              class="upload-demo"
              :action="uploadAction"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              :file-list="fileList"
              :limit="1"
              accept=".jpg,.jpeg,.png,.pdf"
              list-type="picture"
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/pdf文件，且不超过5MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">
          提交申请
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Delete } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';
import { getMyLeaveRequests, createLeaveRequest, cancelLeaveRequest } from '@/api/leave';

// --- 状态 ---
const loading = ref(false);
const dialogVisible = ref(false);
const feedbackDialogVisible = ref(false);
const leaveHistory = ref([]);
const applyFormRef = ref(null);
const fileList = ref([]);
const currentFeedback = ref(null);
const uploadAction = ref('http://localhost:8080/api/upload'); // 文件上传接口地址

// 从 localStorage 获取当前医生ID（参考 MySchedule.vue 的实现）
const getCurrentDoctorId = () => {
  const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'));
  const doctorId = savedInfo?.doctorId;
  
  console.log('=== 获取医生ID ===');
  console.log('localStorage中的savedInfo:', savedInfo);
  console.log('提取的doctorId:', doctorId);
  
  if (doctorId) {
    console.log('成功获取医生ID:', doctorId);
    return doctorId;
  }
  
  console.error('未能从 localStorage 获取医生ID');
  return null;
};

const currentDoctorId = ref(getCurrentDoctorId());

// --- 表单 ---
const applyForm = reactive({
  requestType: 'PERSONAL_LEAVE',
  dateRange: [],
  reason: '',
  proofDocumentUrl: '' // 请假证明文件URL
});

const formRules = reactive({
  requestType: [{ required: true, message: '请选择假期类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择起止日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }]
});

// --- 方法 ---

// 加载历史记录
const fetchHistory = async () => {
  loading.value = true;
  try {
    console.log('正在加载医生ID:', currentDoctorId.value, '的请假记录');
    const response = await getMyLeaveRequests(currentDoctorId.value);
    console.log('获取到的数据:', response);
    
    let data = response;
    if (response && response.data) {
      data = response.data;
    }
    
    if (Array.isArray(data)) {
      // 转换数据格式
      leaveHistory.value = data.map(item => ({
        ...item,
        leaveRequestId: item.requestId,
        startDate: item.startTime ? item.startTime.split('T')[0] : '',
        endDate: item.endTime ? item.endTime.split('T')[0] : '',
        status: item.status || 'PENDING',
        // 修复 requestType 显示
        requestType: item.requestType === 'leave' ? 'PERSONAL_LEAVE' : 
                    item.requestType === 'schedule_change' ? 'SCHEDULE_CHANGE' : 
                    item.requestType
      }));
    } else {
      leaveHistory.value = [];
    }
    
    console.log('处理后的数据:', leaveHistory.value);

  } catch (error) {
    console.error('加载历史记录失败:', error);
    ElMessage.error('加载历史记录失败: ' + (error.message || '网络错误'));
    leaveHistory.value = [];
  } finally {
    loading.value = false;
  }
};

// 打开弹窗
const openApplyDialog = () => {
  dialogVisible.value = true;
};

// 重置表单
const resetForm = () => {
  applyFormRef.value?.resetFields();
  fileList.value = [];
  applyForm.proofDocumentUrl = '';
};

// 提交表单
const handleSubmit = async () => {
  if (!applyFormRef.value) return;
  await applyFormRef.value.validate(async (valid) => {
    if (valid) {
      const data = {
        doctorId: currentDoctorId.value,
        requestType: applyForm.requestType === 'PERSONAL_LEAVE' ? 'leave' : 'leave',
        startTime: applyForm.dateRange[0] + 'T00:00:00',
        endTime: applyForm.dateRange[1] + 'T23:59:59',
        reason: applyForm.reason,
        proofDocumentUrl: applyForm.proofDocumentUrl
      };

      try {
        console.log('提交请假申请数据:', data);
        const result = await createLeaveRequest(data);
        console.log('提交结果:', result);
        ElMessage.success('申请提交成功');
        dialogVisible.value = false;
        fetchHistory(); // 重新加载列表
      } catch (error) {
        console.error('提交失败:', error);
        ElMessage.error('提交失败：' + (error.message || error.response?.data?.message || '网络错误'));
      }
    }
  });
};

// 撤销申请
const handleCancel = (row) => {
  ElMessageBox.confirm(
      `确定要撤销从 ${row.startDate} 到 ${row.endDate} 的休假申请吗？`,
      '确认撤销',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(async () => {
    try {
      await cancelLeaveRequest(row.leaveRequestId || row.requestId);
      ElMessage.success('撤销成功');
      fetchHistory(); // 重新加载列表
    } catch (error) {
      ElMessage.error('撤销失败：' + (error.message || '未知错误'));
    }
  }).catch(() => {
    // 用户取消
  });
};

// --- 格式化辅助函数 ---
const formatRequestType = (type) => {
  const map = {
    'PERSONAL_LEAVE': '事假',
    'SICK_LEAVE': '病假',
    'leave': '请假',
    'schedule_change': '调班'
  };
  return map[type] || '其他';
};

const formatStatus = (status) => {
  const map = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已驳回',
    'DENIED': '已驳回',
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已驳回'
  };
  return map[status] || '未知';
};

const getStatusTag = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'DENIED': 'danger',
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  };
  return map[status] || 'info';
};

// 文件上传前验证
const beforeUpload = (file) => {
  const isValidType = ['image/jpeg', 'image/png', 'application/pdf'].includes(file.type);
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isValidType) {
    ElMessage.error('只能上传 JPG/PNG/PDF 格式的文件!');
    return false;
  }
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!');
    return false;
  }
  return true;
};

// 文件上传成功
const handleUploadSuccess = (response, file) => {
  if (response.success) {
    ElMessage.success('文件上传成功');
    applyForm.proofDocumentUrl = 'http://localhost:8080' + response.url;
  } else {
    ElMessage.error(response.message || '文件上传失败');
  }
};

// 文件上传失败
const handleUploadError = () => {
  ElMessage.error('文件上传失败，请重试');
};

// 查看证明文件
const viewProof = (url) => {
  window.open(url, '_blank');
};

// 查看返回意见
const viewFeedback = (row) => {
  currentFeedback.value = row;
  feedbackDialogVisible.value = true;
};

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const date = new Date(dateTimeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// --- 生命周期 ---
onMounted(() => {
  // 显示当前使用的医生ID
  console.log('当前医生ID:', currentDoctorId.value);
  
  // 检查是否获取到医生ID
  if (!currentDoctorId.value) {
    ElMessage.error('未获取到医生信息，请重新登录');
    return;
  }
  
  fetchHistory();
});
</script>

<style scoped>
.app-container {
  padding: 24px;
  background-color: #f7fafc;
  min-height: calc(100vh - 50px);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}
:deep(.el-form-item__label) {
  font-weight: 600 !important;
  color: #303133 !important;
}
</style>
