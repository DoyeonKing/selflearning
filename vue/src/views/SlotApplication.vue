<template>
  <div class="slot-application-page">
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="navbar-content">
        <div class="navbar-left">
          <el-button :icon="ArrowLeft" @click="goBack" text>返回</el-button>
          <div class="page-title">
            <el-icon :size="24"><DocumentAdd /></el-icon>
            <h2>申请加号</h2>
          </div>
        </div>
        <div class="navbar-right">
          <div class="user-info">
            <el-avatar :size="36" :src="getAvatarUrl(doctorStore.detailedDoctorInfo.photoUrl)" />
            <span class="user-name">{{ doctorStore.displayName }} 医生</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="content-wrapper">
        <!-- 左侧：申请表单 -->
        <div class="form-section">
          <el-card class="form-card" shadow="never">
            <template #header>
              <div class="card-header-content">
                <span class="card-title">加号申请表</span>
                <el-tag type="info" size="small">请填写完整信息</el-tag>
              </div>
            </template>

            <el-form
              ref="applicationFormRef"
              :model="applicationForm"
              :rules="formRules"
              label-width="120px"
              label-position="top"
              class="application-form"
            >
              <!-- 基本信息 -->
              <div class="form-section-title">
                <el-icon><Calendar /></el-icon>
                <span>选择排班</span>
              </div>

              <el-form-item label="选择加号时间" prop="scheduleId">
                <el-select 
                  v-model="applicationForm.scheduleId" 
                  placeholder="请选择您的排班时间" 
                  style="width: 100%"
                  filterable
                  :loading="schedulesLoading"
                  @change="handleScheduleChange"
                >
                  <el-option
                    v-for="schedule in availableSchedules"
                    :key="schedule.scheduleId"
                    :label="formatScheduleOption(schedule)"
                    :value="schedule.scheduleId"
                  >
                    <div class="schedule-option">
                      <div class="schedule-option-main">
                        <el-icon v-if="isScheduleMorning(schedule)"><Sunrise /></el-icon>
                        <el-icon v-else><Sunset /></el-icon>
                        <span class="schedule-date">{{ schedule.scheduleDate }}</span>
                        <el-tag :type="isScheduleMorning(schedule) ? 'success' : 'warning'" size="small">
                          {{ isScheduleMorning(schedule) ? '上午' : '下午' }}
                        </el-tag>
                      </div>
                      <div class="schedule-option-detail">
                        <span>{{ schedule.startTime }} - {{ schedule.endTime }}</span>
                        <span class="schedule-location">{{ schedule.location }}</span>
                      </div>
                    </div>
                  </el-option>
                </el-select>
                <div v-if="selectedSchedule" class="selected-schedule-info">
                  <el-tag type="info" size="small">已选择</el-tag>
                  <span>{{ selectedSchedule.scheduleDate }} {{ isScheduleMorning(selectedSchedule) ? '上午' : '下午' }} {{ selectedSchedule.startTime }}-{{ selectedSchedule.endTime }}</span>
                </div>
              </el-form-item>

              <el-form-item label="加号数量" prop="addedSlots">
                <el-input-number
                  v-model="applicationForm.addedSlots"
                  :min="1"
                  :max="20"
                  style="width: 100%"
                  placeholder="请输入加号数量"
                />
              </el-form-item>

              <!-- 紧急程度 -->
              <div class="form-section-title">
                <el-icon><Warning /></el-icon>
                <span>紧急程度</span>
              </div>

              <el-form-item label="紧急程度" prop="urgencyLevel">
                <el-radio-group v-model="applicationForm.urgencyLevel" class="urgency-radio-group">
                  <el-radio value="LOW" border>
                    <div class="radio-content">
                      <el-tag type="info" size="small">低</el-tag>
                      <span>常规加号</span>
                    </div>
                  </el-radio>
                  <el-radio value="MEDIUM" border>
                    <div class="radio-content">
                      <el-tag type="warning" size="small">中</el-tag>
                      <span>较为紧急</span>
                    </div>
                  </el-radio>
                  <el-radio value="HIGH" border>
                    <div class="radio-content">
                      <el-tag type="danger" size="small">高</el-tag>
                      <span>非常紧急</span>
                    </div>
                  </el-radio>
                  <el-radio value="CRITICAL" border>
                    <div class="radio-content">
                      <el-tag type="danger" effect="dark" size="small">紧急</el-tag>
                      <span>危急情况</span>
                    </div>
                  </el-radio>
                </el-radio-group>
              </el-form-item>

              <!-- 申请理由 -->
              <div class="form-section-title">
                <el-icon><Document /></el-icon>
                <span>申请理由</span>
              </div>

              <el-form-item label="申请理由" prop="reason">
                <el-input
                  v-model="applicationForm.reason"
                  type="textarea"
                  :rows="6"
                  placeholder="请详细说明加号的原因，例如：患者病情紧急、复诊需求、特殊情况等..."
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <!-- 患者信息 -->
              <div class="form-section-title">
                <el-icon><User /></el-icon>
                <span>患者信息</span>
              </div>

              <el-form-item label="选择患者" prop="patientId">
                <el-select
                  v-model="applicationForm.patientId"
                  placeholder="输入患者姓名进行搜索"
                  style="width: 100%"
                  filterable
                  remote
                  :remote-method="searchPatients"
                  :loading="patientsLoading"
                  clearable
                  @change="handlePatientChange"
                >
                  <el-option
                    v-for="patient in patientOptions"
                    :key="patient.patientId"
                    :label="formatPatientOption(patient)"
                    :value="patient.patientId"
                  >
                    <div class="patient-option">
                      <div class="patient-option-main">
                        <span class="patient-name">{{ patient.fullName || patient.name }}</span>
                        <el-tag v-if="patient.gender" :type="patient.gender === 'MALE' ? 'primary' : 'danger'" size="small">
                          {{ patient.gender === 'MALE' ? '男' : '女' }}
                        </el-tag>
                      </div>
                      <div class="patient-option-detail">
                        <span v-if="patient.phoneNumber || patient.phone">{{ patient.phoneNumber || patient.phone }}</span>
                        <span v-if="patient.idCardNumber || patient.idCard">{{ maskIdCard(patient.idCardNumber || patient.idCard) }}</span>
                      </div>
                    </div>
                  </el-option>
                </el-select>
                <div v-if="selectedPatient" class="selected-patient-info">
                  <el-tag type="success" size="small">已选择患者</el-tag>
                  <span>{{ selectedPatient.fullName || selectedPatient.name }} ({{ selectedPatient.phoneNumber || selectedPatient.phone || '无电话' }})</span>
                </div>
              </el-form-item>

              <!-- 提交按钮 -->
              <el-form-item class="submit-section">
                <el-button type="primary" size="large" :icon="Check" @click="submitApplication" :loading="submitting">
                  提交申请
                </el-button>
                <el-button size="large" :icon="RefreshLeft" @click="resetForm">
                  重置表单
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>

        <!-- 右侧：申请记录 -->
        <div class="records-section">
          <el-card class="records-card" shadow="never">
            <template #header>
              <div class="card-header-content">
                <span class="card-title">我的申请记录</span>
                <el-button :icon="Refresh" circle size="small" @click="loadApplicationRecords" />
              </div>
            </template>

            <div v-loading="recordsLoading" class="records-list">
              <el-empty v-if="!recordsLoading && applicationRecords.length === 0" description="暂无申请记录" />

              <div v-else class="record-items">
                <div
                  v-for="record in applicationRecords"
                  :key="record.applicationId"
                  class="record-item"
                  :class="`status-${record.status.toLowerCase()}`"
                >
                  <div class="record-header">
                    <div class="record-date">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ record.scheduleDate }}</span>
                      <el-tag :type="getTimeSlotType(record.timeSlot)" size="small">
                        {{ record.timeSlot === 'MORNING' ? '上午' : '下午' }}
                      </el-tag>
                    </div>
                    <el-tag :type="getStatusType(record.status)" size="small">
                      {{ getStatusText(record.status) }}
                    </el-tag>
                  </div>

                  <div class="record-body">
                    <div class="record-info">
                      <span class="info-label">加号数量：</span>
                      <span class="info-value">{{ record.addedSlots }} 个</span>
                    </div>
                    <div class="record-info">
                      <span class="info-label">紧急程度：</span>
                      <el-tag :type="getUrgencyType(record.urgencyLevel)" size="small">
                        {{ getUrgencyText(record.urgencyLevel) }}
                      </el-tag>
                    </div>
                  </div>

                  <div class="record-reason">
                    <span class="reason-label">申请理由：</span>
                    <p class="reason-text">{{ record.reason }}</p>
                  </div>

                  <div v-if="record.approverComments" class="record-comments">
                    <span class="comments-label">审批意见：</span>
                    <p class="comments-text">{{ record.approverComments }}</p>
                  </div>

                  <div class="record-footer">
                    <span class="record-time">{{ formatDateTime(record.createdAt) }}</span>
                    <el-button
                      v-if="record.status === 'PENDING'"
                      type="danger"
                      size="small"
                      text
                      :icon="Close"
                      @click="cancelApplication(record.applicationId)"
                    >
                      取消申请
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, DocumentAdd, Calendar, Sunrise, Sunset,
  User, Warning, Document,
  Check, RefreshLeft, Refresh, Close
} from '@element-plus/icons-vue'
import { useDoctorStore } from '@/stores/doctorStore'
import { getSchedulesByDoctorId } from '@/api/schedule'
import { searchPatientsByName } from '@/api/patient'
import { 
  createSlotApplication, 
  getSlotApplicationsByDoctor,
  cancelSlotApplication 
} from '@/api/slotApplication'
import defaultAvatar from '@/assets/doctor.jpg'

const router = useRouter()
const doctorStore = useDoctorStore()

// 表单引用
const applicationFormRef = ref(null)

// 提交状态
const submitting = ref(false)

// 排班相关状态
const schedulesLoading = ref(false)
const availableSchedules = ref([])
const selectedSchedule = ref(null)

// 患者相关状态
const patientsLoading = ref(false)
const patientOptions = ref([])
const selectedPatient = ref(null)

// 申请表单数据
const applicationForm = reactive({
  scheduleId: '',
  addedSlots: 1,
  urgencyLevel: 'MEDIUM',
  reason: '',
  patientId: ''
})

// 表单验证规则
const formRules = {
  scheduleId: [
    { required: true, message: '请选择排班时间', trigger: 'change' }
  ],
  addedSlots: [
    { required: true, message: '请输入加号数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 20, message: '加号数量必须在1-20之间', trigger: 'blur' }
  ],
  urgencyLevel: [
    { required: true, message: '请选择紧急程度', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请填写申请理由', trigger: 'blur' },
    { min: 10, max: 500, message: '申请理由长度在10-500个字符之间', trigger: 'blur' }
  ],
  patientId: [
    { required: true, message: '请选择患者', trigger: 'change' }
  ]
}

// 申请记录
const recordsLoading = ref(false)
const applicationRecords = ref([])

// 加载医生排班
const loadDoctorSchedules = async () => {
  const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'))
  const doctorId = savedInfo?.doctorId || doctorStore.currentDoctorId
  
  if (!doctorId) {
    ElMessage.error('无法获取医生ID')
    return
  }
  
  schedulesLoading.value = true
  
  try {
    const today = new Date()
    const endDate = new Date()
    endDate.setDate(today.getDate() + 30) // 获取未来1个月的排班
    
    const formatDate = (date) => {
      const year = date.getFullYear()
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    
    const response = await getSchedulesByDoctorId(doctorId, {
      startDate: formatDate(today),
      endDate: formatDate(endDate),
      page: 0,
      size: 100
    })
    
    if (response && response.content) {
      availableSchedules.value = response.content
    } else {
      availableSchedules.value = []
    }
  } catch (error) {
    console.error('加载排班失败:', error)
    ElMessage.error('加载排班失败')
    availableSchedules.value = []
  } finally {
    schedulesLoading.value = false
  }
}

// 判断是否为上午排班
const isScheduleMorning = (schedule) => {
  if (!schedule || !schedule.startTime) return false
  const hour = parseInt(schedule.startTime.split(':')[0])
  return hour < 12
}

// 格式化排班选项
const formatScheduleOption = (schedule) => {
  const timeSlot = isScheduleMorning(schedule) ? '上午' : '下午'
  return `${schedule.scheduleDate} ${timeSlot} ${schedule.startTime}-${schedule.endTime} (${schedule.location})`
}

// 处理排班选择
const handleScheduleChange = (scheduleId) => {
  selectedSchedule.value = availableSchedules.value.find(s => s.scheduleId === scheduleId)
}

// 搜索患者
const searchPatients = async (query) => {
  if (!query || query.trim() === '') {
    patientOptions.value = []
    return
  }
  
  patientsLoading.value = true
  
  try {
    const response = await searchPatientsByName(query.trim())
    
    if (response && response.content) {
      patientOptions.value = response.content
    } else if (Array.isArray(response)) {
      patientOptions.value = response
    } else {
      patientOptions.value = []
    }
  } catch (error) {
    console.error('搜索患者失败:', error)
    patientOptions.value = []
  } finally {
    patientsLoading.value = false
  }
}

// 格式化患者选项
const formatPatientOption = (patient) => {
  let info = patient.fullName || patient.name || ''
  if (patient.gender) {
    info += ` (${patient.gender === 'MALE' ? '男' : '女'})`
  }
  if (patient.phoneNumber || patient.phone) {
    info += ` - ${patient.phoneNumber || patient.phone}`
  }
  return info
}

// 处理患者选择
const handlePatientChange = (patientId) => {
  selectedPatient.value = patientOptions.value.find(p => p.patientId === patientId)
}

// 身份证号脱敏
const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 6) + '********' + idCard.substring(idCard.length - 4)
}

// 获取头像URL
const getAvatarUrl = (url) => {
  if (!url) return defaultAvatar
  if (url.startsWith('blob:')) return url
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) {
    return `http://localhost:8080${url}`
  }
  return url || defaultAvatar
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 提交申请
const submitApplication = async () => {
  if (!applicationFormRef.value) return

  try {
    const valid = await applicationFormRef.value.validate()
    if (!valid) return
  } catch (error) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认提交加号申请？提交后将等待管理员审批。',
      '确认提交',
      {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    submitting.value = true

    // 获取医生ID
    const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'))
    const doctorId = savedInfo?.doctorId || doctorStore.currentDoctorId

    // 调用后端API提交申请
    const requestData = {
      doctorId: doctorId,
      scheduleId: applicationForm.scheduleId,
      addedSlots: applicationForm.addedSlots,
      patientId: applicationForm.patientId,
      urgencyLevel: applicationForm.urgencyLevel,
      reason: applicationForm.reason
    }

    await createSlotApplication(requestData)

    ElMessage.success('申请提交成功，请等待审批')
    
    // 重置表单
    resetForm()
    
    // 刷新申请记录
    loadApplicationRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  applicationFormRef.value?.resetFields()
  applicationForm.scheduleId = ''
  applicationForm.addedSlots = 1
  applicationForm.urgencyLevel = 'MEDIUM'
  applicationForm.reason = ''
  applicationForm.patientId = ''
  selectedSchedule.value = null
  selectedPatient.value = null
  patientOptions.value = []
}

// 加载申请记录
const loadApplicationRecords = async () => {
  const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'))
  const doctorId = savedInfo?.doctorId || doctorStore.currentDoctorId
  
  if (!doctorId) {
    console.error('无法获取医生ID')
    return
  }
  
  recordsLoading.value = true

  try {
    // 调用后端API获取申请记录
    const response = await getSlotApplicationsByDoctor(doctorId)
    applicationRecords.value = Array.isArray(response) ? response : []
    console.log('加载申请记录成功:', applicationRecords.value)
  } catch (error) {
    console.error('加载申请记录失败:', error)
    ElMessage.error('加载申请记录失败')
    applicationRecords.value = []
  } finally {
    recordsLoading.value = false
  }
}

// 取消申请
const cancelApplication = async (applicationId) => {
  try {
    await ElMessageBox.confirm(
      '确认取消该申请？取消后无法恢复。',
      '确认取消',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        type: 'warning'
      }
    )

    const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'))
    const doctorId = savedInfo?.doctorId || doctorStore.currentDoctorId

    // 调用后端API取消申请
    await cancelSlotApplication(applicationId, doctorId)

    ElMessage.success('申请已取消')
    loadApplicationRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败：' + (error.message || '未知错误'))
    }
  }
}

// 辅助函数：获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return typeMap[status] || 'info'
}

// 辅助函数：获取状态文本
const getStatusText = (status) => {
  const textMap = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消'
  }
  return textMap[status] || status
}

// 辅助函数：获取时段类型
const getTimeSlotType = (timeSlot) => {
  return timeSlot === 'MORNING' ? 'success' : 'warning'
}

// 辅助函数：获取紧急程度类型
const getUrgencyType = (urgency) => {
  const typeMap = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return typeMap[urgency] || 'info'
}

// 辅助函数：获取紧急程度文本
const getUrgencyText = (urgency) => {
  const textMap = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '紧急'
  }
  return textMap[urgency] || urgency
}

// 辅助函数：格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  return dateTimeStr.replace('T', ' ').substring(0, 16)
}

// 页面加载时
onMounted(() => {
  loadDoctorSchedules()
  loadApplicationRecords()
})
</script>

<style scoped>
.slot-application-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
}

/* 顶部导航栏 */
.top-navbar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 32px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
  color: #2c3e50;
}

.page-title .el-icon {
  color: #667eea;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 20px;
}

.user-name {
  font-size: 0.95rem;
  font-weight: 500;
  color: #2c3e50;
}

/* 主要内容区域 */
.main-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 32px;
}

.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 450px;
  gap: 24px;
}

/* 表单区域 */
.form-section {
  min-height: 100%;
}

.form-card {
  border-radius: 16px;
  border: 1px solid #f0f0f0;
}

.form-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 24px;
  border-bottom: none;
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #fff;
}

.form-card :deep(.el-card__body) {
  padding: 32px;
}

.application-form {
  max-width: 100%;
}

.form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #303133;
  margin: 24px 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.form-section-title:first-child {
  margin-top: 0;
}

.form-section-title .el-icon {
  color: #667eea;
}

.urgency-radio-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.urgency-radio-group :deep(.el-radio) {
  margin-right: 0;
  width: 100%;
}

.urgency-radio-group :deep(.el-radio__label) {
  width: 100%;
}

.radio-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.submit-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.submit-section :deep(.el-form-item__content) {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 申请记录区域 */
.records-section {
  min-height: 100%;
}

.records-card {
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  height: 100%;
}

.records-card :deep(.el-card__header) {
  background: #fff;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.records-card :deep(.el-card__body) {
  padding: 0;
  height: calc(100% - 64px);
  overflow-y: auto;
}

.records-list {
  min-height: 400px;
}

.record-items {
  padding: 16px;
}

.record-item {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.record-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.record-item:last-child {
  margin-bottom: 0;
}

.record-item.status-pending {
  border-left: 4px solid #e6a23c;
}

.record-item.status-approved {
  border-left: 4px solid #67c23a;
}

.record-item.status-rejected {
  border-left: 4px solid #f56c6c;
}

.record-item.status-cancelled {
  border-left: 4px solid #909399;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-date {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #303133;
}

.record-body {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.record-info {
  font-size: 0.85rem;
  color: #606266;
}

.info-label {
  color: #909399;
}

.info-value {
  font-weight: 500;
  color: #303133;
}

.record-reason,
.record-comments {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.reason-label,
.comments-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #606266;
  display: block;
  margin-bottom: 6px;
}

.reason-text,
.comments-text {
  font-size: 0.85rem;
  color: #606266;
  line-height: 1.6;
  margin: 0;
  word-break: break-word;
}

.record-comments {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  margin-top: 12px;
}

.comments-text {
  color: #303133;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.record-time {
  font-size: 0.8rem;
  color: #909399;
}

/* 排班选项样式 */
.schedule-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.schedule-option-main {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.schedule-date {
  font-size: 0.95rem;
  color: #303133;
}

.schedule-option-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.85rem;
  color: #606266;
  margin-left: 24px;
}

.schedule-location {
  color: #909399;
}

.selected-schedule-info {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #606266;
}

/* 患者选项样式 */
.patient-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.patient-option-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.patient-name {
  font-weight: 500;
  font-size: 0.95rem;
  color: #303133;
}

.patient-option-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.85rem;
  color: #909399;
  margin-left: 4px;
}

.selected-patient-info {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .content-wrapper {
    grid-template-columns: 1fr 400px;
  }
}

@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .records-section {
    min-height: 500px;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 20px;
  }

  .navbar-content {
    padding: 0 16px;
  }

  .user-name {
    display: none;
  }

  .urgency-radio-group {
    grid-template-columns: 1fr;
  }

  .form-card :deep(.el-card__body) {
    padding: 20px;
  }
}
</style>
