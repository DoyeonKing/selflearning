<template>
  <div class="leave-approval-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>请假审批管理</span>
          <div class="header-actions">
            <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 120px; margin-right: 10px;" @change="fetchLeaveRequests">
              <el-option label="全部" value=""></el-option>
              <el-option label="待审批" value="PENDING"></el-option>
              <el-option label="已批准" value="APPROVED"></el-option>
              <el-option label="已拒绝" value="REJECTED"></el-option>
            </el-select>
            <el-button type="primary" :icon="Refresh" @click="fetchLeaveRequests">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="leaveRequests"
          style="width: 100%"
          border
          stripe
      >
        <el-table-column prop="requestId" label="申请ID" width="80" />
        <el-table-column label="申请医生" width="120">
          <template #default="{ row }">
            <div>{{ row.doctor?.fullName || '未知' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.doctor?.department?.name || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="requestType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.requestType === 'leave' ? 'info' : 'warning'">
              {{ formatRequestType(row.requestType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请假时间" width="200">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.startTime) }}</div>
            <div style="color: #909399;">至 {{ formatDateTime(row.endTime) }}</div>
          </template>
        </el-table-column>
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
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 'PENDING'">
              <el-button
                  type="success"
                  size="small"
                  @click="handleApprove(row)"
              >
                批准
              </el-button>
              <el-button
                  type="danger"
                  size="small"
                  @click="handleReject(row)"
              >
                拒绝
              </el-button>
            </div>
            <div v-else>
              <el-button
                  type="info"
                  size="small"
                  @click="viewDetails(row)"
              >
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && leaveRequests.length === 0" description="暂无请假申请" />
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog
        v-model="approvalDialogVisible"
        :title="approvalAction === 'approve' ? '批准请假申请' : '拒绝请假申请'"
        width="500px"
    >
      <div v-if="currentRequest">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请医生">{{ currentRequest.doctor?.fullName }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">{{ formatRequestType(currentRequest.requestType) }}</el-descriptions-item>
          <el-descriptions-item label="请假时间">
            {{ formatDateTime(currentRequest.startTime) }} 至 {{ formatDateTime(currentRequest.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="请假事由">{{ currentRequest.reason }}</el-descriptions-item>
        </el-descriptions>
        
        <el-form :model="approvalForm" style="margin-top: 20px;">
          <el-form-item label="审批意见">
            <el-input
                v-model="approvalForm.comments"
                type="textarea"
                :rows="4"
                :placeholder="approvalAction === 'approve' ? '请输入批准意见（可选）' : '请输入拒绝理由'"
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button 
            :type="approvalAction === 'approve' ? 'success' : 'danger'" 
            @click="submitApproval"
        >
          确认{{ approvalAction === 'approve' ? '批准' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="请假申请详情"
        width="600px"
    >
      <div v-if="currentRequest">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请ID">{{ currentRequest.requestId }}</el-descriptions-item>
          <el-descriptions-item label="申请医生">{{ currentRequest.doctor?.fullName }}</el-descriptions-item>
          <el-descriptions-item label="所属科室">{{ currentRequest.doctor?.department?.name }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">{{ formatRequestType(currentRequest.requestType) }}</el-descriptions-item>
          <el-descriptions-item label="请假时间">
            {{ formatDateTime(currentRequest.startTime) }} 至 {{ formatDateTime(currentRequest.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="请假事由">{{ currentRequest.reason }}</el-descriptions-item>
          <el-descriptions-item label="申请状态">
            <el-tag :type="getStatusTag(currentRequest.status)">
              {{ formatStatus(currentRequest.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(currentRequest.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRequest.updatedAt" label="处理时间">{{ formatDateTime(currentRequest.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRequest.approver" label="审批人">{{ currentRequest.approver.fullName }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRequest.approverComments" label="审批意见">{{ currentRequest.approverComments }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';
import { getAllLeaveRequests, getLeaveRequestsByStatus, approveLeaveRequest, rejectLeaveRequest } from '@/api/leave';
import { useAdminStore } from '@/stores/adminStore';

const router = useRouter();

// 状态管理
const loading = ref(false);
const leaveRequests = ref([]);
const statusFilter = ref('');
const approvalDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const currentRequest = ref(null);
const approvalAction = ref(''); // 'approve' 或 'reject'

const adminStore = useAdminStore();

// 审批表单
const approvalForm = reactive({
  comments: ''
});

// 获取请假申请列表
const fetchLeaveRequests = async () => {
  loading.value = true;
  try {
    let response;
    if (statusFilter.value) {
      response = await getLeaveRequestsByStatus(statusFilter.value);
    } else {
      response = await getAllLeaveRequests();
    }
    
    leaveRequests.value = response.data || response || [];
    console.log('获取到的请假申请:', leaveRequests.value);
    
    // 调试：检查第一条数据的结构
    if (leaveRequests.value.length > 0) {
      console.log('第一条请假申请详细信息:', leaveRequests.value[0]);
      console.log('医生信息:', leaveRequests.value[0].doctor);
      console.log('请假类型:', leaveRequests.value[0].requestType);
    }
  } catch (error) {
    console.error('获取请假申请失败:', error);
    ElMessage.error('获取请假申请失败: ' + (error.message || '网络错误'));
    leaveRequests.value = [];
  } finally {
    loading.value = false;
  }
};

// 批准请假
const handleApprove = (row) => {
  currentRequest.value = row;
  approvalAction.value = 'approve';
  approvalForm.comments = '';
  approvalDialogVisible.value = true;
};

// 拒绝请假
const handleReject = (row) => {
  currentRequest.value = row;
  approvalAction.value = 'reject';
  approvalForm.comments = '';
  approvalDialogVisible.value = true;
};

// 查看详情
const viewDetails = (row) => {
  currentRequest.value = row;
  detailDialogVisible.value = true;
};

// 提交审批
const submitApproval = async () => {
  if (!currentRequest.value) return;
  
  try {
    const requestId = currentRequest.value.requestId;
    
    // 调试 adminStore 状态
    console.log('=== AdminStore 状态 ===');
    console.log('loggedInAdminBasicInfo:', adminStore.loggedInAdminBasicInfo);
    console.log('detailedAdminInfo:', adminStore.detailedAdminInfo);
    console.log('currentAdminId getter:', adminStore.currentAdminId);
    
    const approverIdRaw = adminStore.currentAdminId; // 从store获取当前管理员ID
    const approverId = parseInt(approverIdRaw); // 确保转换为数字
    const comments = approvalForm.comments;
    
    console.log('提交审批 - requestId:', requestId);
    console.log('提交审批 - approverIdRaw:', approverIdRaw);
    console.log('提交审批 - approverId (parsed):', approverId);
    console.log('提交审批 - comments:', comments);
    console.log('提交审批 - action:', approvalAction.value);
    
    if (!approverId || isNaN(approverId)) {
      ElMessage({
        type: 'error',
        message: '无法获取管理员ID，请退出后重新登录',
        duration: 5000
      });
      return;
    }
    
    if (approvalAction.value === 'approve') {
      const response = await approveLeaveRequest(requestId, approverId, comments);
      console.log('批准响应:', response);
      ElMessage.success('请假申请已批准');
      
      // 关闭对话框
      approvalDialogVisible.value = false;
      
      // 跳转到替班医生选择页面
      router.push({
        name: 'SubstituteSelection',
        params: { id: requestId }
      });
    } else {
      if (!comments.trim()) {
        ElMessage.warning('请输入拒绝理由');
        return;
      }
      const response = await rejectLeaveRequest(requestId, approverId, comments);
      console.log('拒绝响应:', response);
      ElMessage.success('请假申请已拒绝');
      
      approvalDialogVisible.value = false;
      fetchLeaveRequests(); // 刷新列表
    }
  } catch (error) {
    console.error('审批失败:', error);
    console.error('错误详情:', error.response?.data || error);
    ElMessage.error('审批失败: ' + (error.response?.data?.message || error.message || '网络错误'));
  }
};

// 查看证明文件
const viewProof = (url) => {
  window.open(url, '_blank');
};

// 格式化函数
const formatRequestType = (type) => {
  const map = {
    'leave': '请假',
    'schedule_change': '调班'
  };
  return map[type] || '其他';
};

const formatStatus = (status) => {
  const map = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已拒绝',
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已拒绝'
  };
  return map[status] || '未知';
};

const getStatusTag = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  };
  return map[status] || 'info';
};

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

// 生命周期
onMounted(() => {
  // 调试：显示当前管理员信息
  console.log('=== 页面加载时的 AdminStore 状态 ===');
  console.log('loggedInAdminBasicInfo:', adminStore.loggedInAdminBasicInfo);
  console.log('detailedAdminInfo:', adminStore.detailedAdminInfo);
  console.log('currentAdminId:', adminStore.currentAdminId);
  
  if (!adminStore.currentAdminId) {
    console.warn('警告：未检测到管理员ID，审批功能可能无法使用');
  }
  
  fetchLeaveRequests();
});
</script>

<style scoped>
.leave-approval-container {
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

.header-actions {
  display: flex;
  align-items: center;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
}
</style>
