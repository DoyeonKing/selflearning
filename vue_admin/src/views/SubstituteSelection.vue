<template>
  <div class="substitute-selection-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always">
      <template #header>
        <div class="card-header">
          <span>替班医生选择 - 请假申请 #{{ requestId }}</span>
        </div>
      </template>

      <div v-loading="loading">
        <!-- 请假信息摘要 -->
        <el-alert
          v-if="leaveRequest"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        >
          <template #title>
            <div style="font-size: 14px;">
              <strong>{{ leaveRequest.doctor?.fullName }}</strong> 的请假已批准
              <br />
              <span style="color: #909399;">
                请假时间：{{ formatDateTime(leaveRequest.startTime) }} 至 {{ formatDateTime(leaveRequest.endTime) }}
              </span>
            </div>
          </template>
        </el-alert>

        <!-- 受影响的排班列表 -->
        <div v-if="affectedSchedules && affectedSchedules.length > 0">
          <h3 style="margin-bottom: 16px;">受影响的排班（{{ affectedSchedules.length }}个）</h3>
          
          <div v-for="(item, index) in affectedSchedules" :key="item.schedule.scheduleId" class="schedule-item">
            <el-card shadow="hover" :class="{ 'schedule-highlighted': true }">
              <template #header>
                <div class="schedule-header">
                  <el-icon class="warning-icon"><Warning /></el-icon>
                  <span class="schedule-title">
                    {{ formatDate(item.schedule.scheduleDate) }} 
                    {{ item.schedule.slotName }} 
                    ({{ formatTime(item.schedule.startTime) }} - {{ formatTime(item.schedule.endTime) }})
                  </span>
                  <el-tag type="warning" size="small">需要替班</el-tag>
                </div>
              </template>

              <div class="schedule-info">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="原医生">{{ item.schedule.doctorName }}</el-descriptions-item>
                  <el-descriptions-item label="科室">{{ item.schedule.departmentName }}</el-descriptions-item>
                  <el-descriptions-item label="地点">{{ item.schedule.locationName }}</el-descriptions-item>
                  <el-descriptions-item label="号源">{{ item.schedule.bookedSlots }}/{{ item.schedule.totalSlots }}</el-descriptions-item>
                  <el-descriptions-item label="挂号费">¥{{ item.schedule.fee }}</el-descriptions-item>
                  <el-descriptions-item label="状态">
                    <el-tag :type="getStatusType(item.schedule.status)">{{ formatStatus(item.schedule.status) }}</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <!-- 替班医生选择 -->
              <div class="substitute-selection" style="margin-top: 20px;">
                <h4 style="margin-bottom: 12px;">
                  <el-icon><User /></el-icon>
                  选择替班医生
                  <span style="color: #909399; font-size: 12px; font-weight: normal; margin-left: 8px;">
                    （优先级：平级 > 升级 > 降级）
                  </span>
                </h4>

                <div v-if="item.substituteDoctors && item.substituteDoctors.length > 0" class="doctor-list">
                  <el-popover
                    v-for="doctor in item.substituteDoctors"
                    :key="doctor.doctorId"
                    placement="right"
                    :width="400"
                    trigger="hover"
                    :show-after="300"
                    @show="loadDoctorDetail(doctor.doctorId, item.schedule.scheduleDate)"
                  >
                    <template #reference>
                      <div
                        class="doctor-card"
                        :class="{
                          'selected': selectedDoctors[item.schedule.scheduleId] === doctor.doctorId,
                          'has-conflict': doctor.hasConflict,
                          'on-leave': doctor.isOnLeave,
                          'match-high': doctor.matchLevel === 'high',
                          'match-medium': doctor.matchLevel === 'medium',
                          'match-low': doctor.matchLevel === 'low'
                        }"
                        @click="selectDoctor(item.schedule.scheduleId, doctor)"
                      >
                        <div class="doctor-avatar">
                          <img :src="doctor.photoUrl || getDefaultAvatar()" :alt="doctor.fullName" />
                        </div>
                        <div class="doctor-info">
                          <div class="doctor-name">
                            {{ doctor.fullName }}
                            <el-tag :type="getMatchLevelType(doctor.matchLevel)" size="small">
                              {{ getMatchLevelText(doctor.matchLevel) }}
                            </el-tag>
                            <el-tag v-if="doctor.isOnLeave" type="warning" size="small">
                              <el-icon><Warning /></el-icon> 请假中
                            </el-tag>
                            <el-tag v-if="doctor.hasConflict" type="danger" size="small">
                              <el-icon><Warning /></el-icon> 时间冲突
                            </el-tag>
                          </div>
                          <div class="doctor-title">{{ doctor.title || '医生' }}</div>
                          <div class="doctor-specialty">{{ doctor.specialty || '暂无专业描述' }}</div>
                          <div class="match-reason">{{ doctor.matchReason }}</div>
                        </div>
                        <div class="select-indicator">
                          <el-icon v-if="selectedDoctors[item.schedule.scheduleId] === doctor.doctorId" class="check-icon">
                            <CircleCheck />
                          </el-icon>
                        </div>
                      </div>
                    </template>
                    
                    <!-- Popover 内容：医生排班和请假信息 -->
                    <template #default>
                      <div v-loading="doctorDetailLoading" class="doctor-detail-popover">
                        <div class="popover-header">
                          <h4>{{ doctor.fullName }} 的排班情况</h4>
                        </div>
                        
                        <div v-if="doctorDetail" class="popover-content">
                          <!-- 排班信息 -->
                          <div class="detail-section">
                            <h5><el-icon><Calendar /></el-icon> 近期排班</h5>
                            <div v-if="doctorDetail.schedules && doctorDetail.schedules.length > 0" class="schedule-list">
                              <div
                                v-for="schedule in doctorDetail.schedules"
                                :key="schedule.scheduleId"
                                class="schedule-item-mini"
                              >
                                <div class="schedule-date">{{ formatDate(schedule.scheduleDate) }}</div>
                                <div class="schedule-slot">{{ schedule.slotName }}</div>
                                <el-tag :type="getScheduleStatusType(schedule.status)" size="small">
                                  {{ formatStatus(schedule.status) }}
                                </el-tag>
                              </div>
                            </div>
                            <el-empty v-else description="暂无排班" :image-size="40" />
                          </div>
                          
                          <!-- 请假信息 -->
                          <div class="detail-section">
                            <h5><el-icon><DocumentCopy /></el-icon> 请假记录</h5>
                            <div v-if="doctorDetail.leaves && doctorDetail.leaves.length > 0" class="leave-list">
                              <div
                                v-for="leave in doctorDetail.leaves"
                                :key="leave.requestId"
                                class="leave-item-mini"
                              >
                                <div class="leave-date">
                                  {{ formatDateTime(leave.startTime) }} 至 {{ formatDateTime(leave.endTime) }}
                                </div>
                                <el-tag :type="getLeaveStatusType(leave.status)" size="small">
                                  {{ formatLeaveStatus(leave.status) }}
                                </el-tag>
                              </div>
                            </div>
                            <el-empty v-else description="暂无请假记录" :image-size="40" />
                          </div>
                        </div>
                        
                        <div v-else-if="!doctorDetailLoading" class="popover-error">
                          <el-empty description="加载失败" :image-size="40" />
                        </div>
                      </div>
                    </template>
                  </el-popover>
                </div>
                <el-empty v-else description="暂无可用替班医生" :image-size="60" />

                <!-- 取消排班选项 -->
                <div class="cancel-option">
                  <el-button
                    :type="selectedDoctors[item.schedule.scheduleId] === null ? 'danger' : 'default'"
                    @click="selectCancelSchedule(item.schedule.scheduleId)"
                    style="width: 100%;"
                  >
                    <el-icon><Close /></el-icon>
                    取消该排班（无合适替班医生）
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 底部操作按钮 -->
          <div class="action-buttons">
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" @click="handleConfirmSubstitution" :disabled="!allSchedulesHandled">
              确认替班安排
            </el-button>
          </div>
        </div>

        <el-empty v-else-if="!loading" description="没有受影响的排班" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Warning, User, CircleCheck, Close, Calendar, DocumentCopy } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';
import { getLeaveApprovalDetail, confirmSubstitution, getDoctorScheduleAndLeave } from '@/api/leave';

const route = useRoute();
const router = useRouter();

const requestId = ref(route.params.id || route.query.requestId);
const loading = ref(false);
const leaveRequest = ref(null);
const affectedSchedules = ref([]);
const selectedDoctors = ref({}); // { scheduleId: doctorId or null }

// 医生详情相关
const doctorDetailLoading = ref(false);
const doctorDetail = ref(null);
const doctorDetailCache = ref({}); // 缓存医生详情，避免重复请求

// 检查是否所有排班都已处理
const allSchedulesHandled = computed(() => {
  if (!affectedSchedules.value || affectedSchedules.value.length === 0) return false;
  return affectedSchedules.value.every(item => 
    selectedDoctors.value[item.schedule.scheduleId] !== undefined
  );
});

// 获取请假批准详情
const fetchApprovalDetail = async () => {
  loading.value = true;
  try {
    const response = await getLeaveApprovalDetail(requestId.value);
    const data = response.data || response;
    
    leaveRequest.value = data.leaveRequest;
    affectedSchedules.value = data.affectedSchedules || [];
    
    console.log('获取到的批准详情:', data);
  } catch (error) {
    console.error('获取批准详情失败:', error);
    ElMessage.error('获取批准详情失败: ' + (error.message || '网络错误'));
  } finally {
    loading.value = false;
  }
};

// 选择替班医生
const selectDoctor = (scheduleId, doctor) => {
  if (doctor.isOnLeave) {
    ElMessage.warning('该医生在此时间段正在请假，建议选择其他医生');
  } else if (doctor.hasConflict) {
    ElMessage.warning('该医生在此时间段已有排班，建议选择其他医生');
  }
  selectedDoctors.value[scheduleId] = doctor.doctorId;
};

// 选择取消排班
const selectCancelSchedule = (scheduleId) => {
  selectedDoctors.value[scheduleId] = null;
};

// 加载医生详情（排班和请假信息）
const loadDoctorDetail = async (doctorId, scheduleDate) => {
  // 检查缓存
  const cacheKey = `${doctorId}_${scheduleDate}`;
  if (doctorDetailCache.value[cacheKey]) {
    doctorDetail.value = doctorDetailCache.value[cacheKey];
    return;
  }
  
  doctorDetailLoading.value = true;
  doctorDetail.value = null;
  
  try {
    // 计算查询日期范围（前后各7天）
    const targetDate = new Date(scheduleDate);
    const startDate = new Date(targetDate);
    startDate.setDate(startDate.getDate() - 7);
    const endDate = new Date(targetDate);
    endDate.setDate(endDate.getDate() + 7);
    
    const startDateStr = startDate.toISOString().split('T')[0];
    const endDateStr = endDate.toISOString().split('T')[0];
    
    const response = await getDoctorScheduleAndLeave(doctorId, startDateStr, endDateStr);
    const data = response.data || response;
    
    doctorDetail.value = data;
    // 缓存结果
    doctorDetailCache.value[cacheKey] = data;
    
    console.log('医生详情:', data);
  } catch (error) {
    console.error('加载医生详情失败:', error);
    doctorDetail.value = null;
  } finally {
    doctorDetailLoading.value = false;
  }
};

// 确认替班安排
const handleConfirmSubstitution = async () => {
  try {
    await ElMessageBox.confirm(
      '确认提交替班安排？此操作将更新排班信息并转移相关预约。',
      '确认替班',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    loading.value = true;
    
    const data = {
      leaveRequestId: parseInt(requestId.value),
      substitutions: selectedDoctors.value
    };
    
    console.log('提交替班安排:', data);
    
    const response = await confirmSubstitution(data);
    const result = response.data || response;
    
    console.log('替班结果:', result);
    
    // 显示详细结果
    let message = `替班安排已完成！\n`;
    message += `成功: ${result.successCount} 个\n`;
    if (result.cancelledCount > 0) {
      message += `取消: ${result.cancelledCount} 个\n`;
    }
    if (result.failedCount > 0) {
      message += `失败: ${result.failedCount} 个\n`;
    }
    
    if (result.messages && result.messages.length > 0) {
      message += `\n详细信息:\n${result.messages.join('\n')}`;
    }
    
    ElMessageBox.alert(message, '替班结果', {
      confirmButtonText: '确定',
      type: result.failedCount > 0 ? 'warning' : 'success',
      callback: () => {
        // 跳转到排班页面
        router.push('/scheduling/dashboard');
      }
    });
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认替班失败:', error);
      ElMessage.error('确认替班失败: ' + (error.response?.data?.message || error.message || '网络错误'));
    }
  } finally {
    loading.value = false;
  }
};

// 返回
const goBack = () => {
  router.back();
};

// 格式化函数
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

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  });
};

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  return timeStr.substring(0, 5); // HH:mm
};

const formatStatus = (status) => {
  const map = {
    'active': '正常',
    'cancelled': '已取消',
    'completed': '已完成'
  };
  return map[status] || status;
};

const getStatusType = (status) => {
  const map = {
    'active': 'success',
    'cancelled': 'danger',
    'completed': 'info'
  };
  return map[status] || 'info';
};

const getMatchLevelType = (level) => {
  const map = {
    'high': 'success',
    'medium': 'warning',
    'low': 'info'
  };
  return map[level] || 'info';
};

const getMatchLevelText = (level) => {
  const map = {
    'high': '平级',
    'medium': '升级',
    'low': '降级'
  };
  return map[level] || '未知';
};

const getDefaultAvatar = () => {
  return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
};

// 排班状态格式化
const getScheduleStatusType = (status) => {
  const map = {
    'available': 'success',
    'full': 'warning',
    'cancelled': 'danger'
  };
  return map[status?.toLowerCase()] || 'info';
};

// 请假状态格式化
const formatLeaveStatus = (status) => {
  const map = {
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已拒绝',
    'cancelled': '已取消'
  };
  return map[status?.toLowerCase()] || status;
};

const getLeaveStatusType = (status) => {
  const map = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger',
    'cancelled': 'info'
  };
  return map[status?.toLowerCase()] || 'info';
};

onMounted(() => {
  if (!requestId.value) {
    ElMessage.error('缺少请假申请ID');
    router.back();
    return;
  }
  fetchApprovalDetail();
});
</script>

<style scoped>
.substitute-selection-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.schedule-item {
  margin-bottom: 24px;
}

.schedule-highlighted {
  border: 2px solid #e6a23c;
}

.schedule-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.warning-icon {
  color: #e6a23c;
  font-size: 18px;
}

.schedule-title {
  flex: 1;
  font-weight: bold;
}

.schedule-info {
  margin-bottom: 16px;
}

.doctor-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.doctor-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.doctor-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.doctor-card.selected {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.doctor-card.has-conflict {
  opacity: 0.7;
}

.doctor-card.on-leave {
  opacity: 0.7;
}

.doctor-card.match-high {
  border-left: 4px solid #67c23a;
}

.doctor-card.match-medium {
  border-left: 4px solid #e6a23c;
}

.doctor-card.match-low {
  border-left: 4px solid #909399;
}

.doctor-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.doctor-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.doctor-info {
  flex: 1;
}

.doctor-name {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.doctor-title {
  color: #606266;
  font-size: 12px;
  margin-bottom: 2px;
}

.doctor-specialty {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-reason {
  color: #409eff;
  font-size: 11px;
  font-style: italic;
}

.select-indicator {
  flex-shrink: 0;
}

.check-icon {
  color: #67c23a;
  font-size: 24px;
}

.cancel-option {
  margin-top: 12px;
}

.action-buttons {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

/* Popover 样式 */
.doctor-detail-popover {
  max-height: 500px;
  overflow-y: auto;
}

.popover-header h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.popover-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.detail-section h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}

.schedule-list,
.leave-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-item-mini,
.leave-item-mini {
  background: white;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 3px solid #409eff;
}

.schedule-date,
.leave-date {
  font-size: 13px;
  color: #303133;
  flex: 1;
}

.schedule-slot {
  font-size: 12px;
  color: #606266;
  margin-right: 8px;
}

.popover-error {
  padding: 20px;
  text-align: center;
}
</style>
