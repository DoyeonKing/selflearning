<template>
  <div class="slot-approval-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>加号申请审批管理</span>
          <div class="header-actions">
            <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 120px; margin-right: 10px;" @change="fetchSlotApplications">
              <el-option label="全部" value=""></el-option>
              <el-option label="待审批" value="PENDING"></el-option>
              <el-option label="已批准" value="APPROVED"></el-option>
              <el-option label="已拒绝" value="REJECTED"></el-option>
              <el-option label="已取消" value="CANCELLED"></el-option>
            </el-select>
            <el-button type="primary" :icon="Refresh" @click="fetchSlotApplications">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="slotApplications"
          style="width: 100%"
          border
          stripe
      >
        <el-table-column prop="applicationId" label="申请ID" width="80" />
        <el-table-column label="申请医生" width="150">
          <template #default="{ row }">
            <div>{{ row.doctorName || '未知' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="排班信息" width="200">
          <template #default="{ row }">
            <div>{{ row.scheduleDate }}</div>
            <div style="color: #909399; font-size: 12px;">
              {{ formatTimeSlot(row.timeSlot) }} {{ row.startTime }}-{{ row.endTime }}
            </div>
            <div style="color: #909399; font-size: 12px;">
              地点: {{ row.location || '未知' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="患者信息" width="150">
          <template #default="{ row }">
            <div>{{ row.patientName || '未知' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.patientPhone || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="addedSlots" label="加号数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning">+{{ row.addedSlots }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getUrgencyTag(row.urgencyLevel)">
              {{ formatUrgencyLevel(row.urgencyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
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

      <el-empty v-if="!loading && slotApplications.length === 0" description="暂无加号申请" />
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog
        v-model="approvalDialogVisible"
        :title="approvalAction === 'approve' ? '批准加号申请' : '拒绝加号申请'"
        width="600px"
    >
      <div v-if="currentApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请医生" :span="2">{{ currentApplication.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="排班日期">{{ currentApplication.scheduleDate }}</el-descriptions-item>
          <el-descriptions-item label="时段">{{ formatTimeSlot(currentApplication.timeSlot) }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ currentApplication.startTime }} - {{ currentApplication.endTime }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ currentApplication.location }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ currentApplication.patientName }}</el-descriptions-item>
          <el-descriptions-item label="患者电话">{{ currentApplication.patientPhone }}</el-descriptions-item>
          <el-descriptions-item label="加号数量">
            <el-tag type="warning">+{{ currentApplication.addedSlots }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="getUrgencyTag(currentApplication.urgencyLevel)">
              {{ formatUrgencyLevel(currentApplication.urgencyLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请理由" :span="2">{{ currentApplication.reason }}</el-descriptions-item>
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
            :loading="submitting"
        >
          确认{{ approvalAction === 'approve' ? '批准' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="加号申请详情"
        width="700px"
    >
      <div v-if="currentApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID" :span="2">{{ currentApplication.applicationId }}</el-descriptions-item>
          <el-descriptions-item label="申请医生" :span="2">{{ currentApplication.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="排班日期">{{ currentApplication.scheduleDate }}</el-descriptions-item>
          <el-descriptions-item label="时段">{{ formatTimeSlot(currentApplication.timeSlot) }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ currentApplication.startTime }} - {{ currentApplication.endTime }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ currentApplication.location }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ currentApplication.patientName }}</el-descriptions-item>
          <el-descriptions-item label="患者电话">{{ currentApplication.patientPhone || '无' }}</el-descriptions-item>
          <el-descriptions-item label="加号数量">
            <el-tag type="warning">+{{ currentApplication.addedSlots }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="getUrgencyTag(currentApplication.urgencyLevel)">
              {{ formatUrgencyLevel(currentApplication.urgencyLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请理由" :span="2">{{ currentApplication.reason }}</el-descriptions-item>
          <el-descriptions-item label="申请状态" :span="2">
            <el-tag :type="getStatusTag(currentApplication.status)">
              {{ formatStatus(currentApplication.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">{{ formatDateTime(currentApplication.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentApplication.approvedAt" label="处理时间" :span="2">{{ formatDateTime(currentApplication.approvedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentApplication.approverName" label="审批人" :span="2">{{ currentApplication.approverName }}</el-descriptions-item>
          <el-descriptions-item v-if="currentApplication.approverComments" label="审批意见" :span="2">{{ currentApplication.approverComments }}</el-descriptions-item>
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
import { getAllSlotApplications, getSlotApplicationsByStatus, updateSlotApplication } from '@/api/slotApplication';
import { useAdminStore } from '@/stores/adminStore';

const router = useRouter();

// 状态管理
const loading = ref(false);
const submitting = ref(false);
const slotApplications = ref([]);
const statusFilter = ref('');
const approvalDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const currentApplication = ref(null);
const approvalAction = ref(''); // 'approve' 或 'reject'

const adminStore = useAdminStore();

// 审批表单
const approvalForm = reactive({
  comments: ''
});

// 获取加号申请列表
const fetchSlotApplications = async () => {
  loading.value = true;
  try {
    let response;
    if (statusFilter.value) {
      response = await getSlotApplicationsByStatus(statusFilter.value);
    } else {
      response = await getAllSlotApplications();
    }
    
    slotApplications.value = response.data || response || [];
    console.log('获取到的加号申请:', slotApplications.value);
  } catch (error) {
    console.error('获取加号申请失败:', error);
    ElMessage.error('获取加号申请失败: ' + (error.message || '网络错误'));
    slotApplications.value = [];
  } finally {
    loading.value = false;
  }
};

// 批准申请
const handleApprove = (row) => {
  currentApplication.value = row;
  approvalAction.value = 'approve';
  approvalForm.comments = '';
  approvalDialogVisible.value = true;
};

// 拒绝申请
const handleReject = (row) => {
  currentApplication.value = row;
  approvalAction.value = 'reject';
  approvalForm.comments = '';
  approvalDialogVisible.value = true;
};

// 提交审批（参考请假审批的实现）
const submitApproval = async () => {
  if (!currentApplication.value) return;
  
  try {
    submitting.value = true;
    
    // 调试 adminStore 状态（参考请假审批）
    console.log('=== AdminStore 状态 ===');
    console.log('loggedInAdminBasicInfo:', adminStore.loggedInAdminBasicInfo);
    console.log('detailedAdminInfo:', adminStore.detailedAdminInfo);
    console.log('currentAdminId getter:', adminStore.currentAdminId);
    
    const approverIdRaw = adminStore.currentAdminId; // 从store获取当前管理员ID
    const approverId = parseInt(approverIdRaw); // 确保转换为数字
    const comments = approvalForm.comments;
    
    console.log('提交审批 - applicationId:', currentApplication.value.applicationId);
    console.log('提交审批 - approverIdRaw:', approverIdRaw);
    console.log('提交审批 - approverId (parsed):', approverId);
    console.log('提交审批 - comments:', comments);
    console.log('提交审批 - action:', approvalAction.value);
    
    // 验证管理员ID（参考请假审批）
    if (!approverId || isNaN(approverId)) {
      ElMessage({
        type: 'error',
        message: '无法获取管理员ID，请退出后重新登录',
        duration: 5000
      });
      return;
    }
    
    // 拒绝时必须填写理由
    if (approvalAction.value === 'reject' && !comments.trim()) {
      ElMessage.warning('请输入拒绝理由');
      return;
    }
    
    const updateData = {
      status: approvalAction.value === 'approve' ? 'APPROVED' : 'REJECTED',
      approverId: approverId,
      approverComments: comments.trim() || null
    };

    console.log('提交审批数据:', updateData);

    const response = await updateSlotApplication(currentApplication.value.applicationId, updateData);
    console.log('审批响应:', response);
    
    ElMessage.success(approvalAction.value === 'approve' ? '批准成功！系统已自动创建预约并通知患者' : '拒绝成功');
    approvalDialogVisible.value = false;
    fetchSlotApplications(); // 刷新列表
  } catch (error) {
    console.error('审批失败:', error);
    console.error('错误详情:', error.response?.data || error);
    ElMessage.error('审批失败: ' + (error.response?.data?.message || error.message || '网络错误'));
  } finally {
    submitting.value = false;
  }
};

// 查看详情
const viewDetails = (row) => {
  currentApplication.value = row;
  detailDialogVisible.value = true;
};

// 格式化时段
const formatTimeSlot = (slot) => {
  const map = {
    'MORNING': '上午',
    'AFTERNOON': '下午',
    'EVENING': '晚上'
  };
  return map[slot] || slot;
};

// 格式化紧急程度
const formatUrgencyLevel = (level) => {
  const map = {
    'LOW': '常规加号',
    'MEDIUM': '较为紧急',
    'HIGH': '非常紧急',
    'CRITICAL': '危急情况'
  };
  return map[level] || level;
};

// 获取紧急程度标签类型
const getUrgencyTag = (level) => {
  const map = {
    'LOW': 'info',
    'MEDIUM': '',
    'HIGH': 'warning',
    'CRITICAL': 'danger'
  };
  return map[level] || 'info';
};

// 格式化状态
const formatStatus = (status) => {
  const map = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已拒绝',
    'CANCELLED': '已取消'
  };
  return map[status] || status;
};

// 获取状态标签类型
const getStatusTag = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'CANCELLED': 'info'
  };
  return map[status] || 'info';
};

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '';
  const date = new Date(dateTime);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 页面加载时获取数据
onMounted(() => {
  fetchSlotApplications();
});
</script>

<style scoped>
.slot-approval-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-descriptions__label) {
  font-weight: bold;
  width: 120px;
}
</style>
