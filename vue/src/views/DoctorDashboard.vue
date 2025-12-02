<template>
  <div class="doctor-dashboard">
    <div class="top-navbar">
      <div class="navbar-content">
        <div class="navbar-left">
          <div class="logo-section">
            <el-icon :size="28"><DataAnalysis /></el-icon>
            <h2>医生工作台</h2>
          </div>
        </div>
        <div class="navbar-right">
          <div class="user-info">
            <el-avatar :size="36" :src="getAvatarUrl(doctorStore.detailedDoctorInfo.photoUrl)" />
            <span class="user-name">{{ doctorStore.displayName }} 医生</span>
          </div>
          <el-button type="danger" size="default" :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </div>

    <div class="main-content">

      <div class="banner-card">
        <div class="banner-text">
          恪守医德 造福于民
        </div>
      </div>

      <div class="stats-row" v-loading="statsLoading">
        <div class="stat-card">
          <div class="stat-icon patients">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日总接诊</p>
            <h3 class="stat-value">{{ todayStats.total }}</h3>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon appointments">
            <el-icon :size="32"><Sunrise /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">上午班患者</p>
            <h3 class="stat-value">{{ todayStats.morning }}</h3>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon records">
            <el-icon :size="32"><Sunny /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">下午班患者</p>
            <h3 class="stat-value">{{ todayStats.afternoon }}</h3>
          </div>
        </div>

      </div>

      <div class="content-grid">
        <div class="left-column">
          <div class="doctor-info-card">
            <div class="card-header">
              <h3>个人信息</h3>
            </div>
            <div class="doctor-profile">
              <div class="doctor-avatar-large">
                <el-avatar :size="100" :src="getAvatarUrl(doctorStore.detailedDoctorInfo.photoUrl)" />
              </div>
              <div class="doctor-details">
                <h2>{{ doctorStore.displayName }}</h2>
                <div class="detail-item">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>{{ doctorStore.currentDepartment || '科室待定' }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ doctorStore.currentPosition || '职称待定' }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><Phone /></el-icon>
                  <span>{{ doctorStore.detailedDoctorInfo.phone || '暂无电话' }}</span>
                </div>
              </div>
              <div class="doctor-actions">
                <el-button type="primary" :icon="Edit" @click="editProfile">编辑资料</el-button>
                <el-button :icon="Key" @click="changePassword">修改密码</el-button>
              </div>
            </div>
          </div>

          <div class="schedule-card" v-loading="scheduleLoading">
            <div class="card-header">
              <h3>今日排班</h3>
              <el-tag type="info" size="small">{{ new Date().toLocaleDateString() }}</el-tag>
            </div>

            <div class="schedule-list" v-if="todaySchedules.length > 0">
              <div class="shift-group">
                <h4 class="shift-title">上午 (AM)</h4>
                <div class="slot-list" v-if="morningSchedules.length > 0">
                  <div class="slot-card" v-for="item in morningSchedules" :key="item.scheduleId">
                    <div class="slot-time">{{ item.startTime }} - {{ item.endTime }}</div>
                    <div class="slot-details">{{ item.slotName }} ({{ item.location }})</div>
                  </div>
                </div>
                <el-text v-else type="info" size="small" class="no-schedule-text">上午无排班</el-text>
              </div>

              <div class="shift-group">
                <h4 class="shift-title">下午 (PM)</h4>
                <div class="slot-list" v-if="afternoonSchedules.length > 0">
                  <div class="slot-card" v-for="item in afternoonSchedules" :key="item.scheduleId">
                    <div class="slot-time">{{ item.startTime }} - {{ item.endTime }}</div>
                    <div class="slot-details">{{ item.slotName }} ({{ item.location }})</div>
                  </div>
                </div>
                <el-text v-else type="info" size="small" class="no-schedule-text">下午无排班</el-text>
              </div>
            </div>

            <el-empty v-if="!scheduleLoading && todaySchedules.length === 0" description="今日无排班" :image-size="100" />
          </div>
        </div>

        <div class="right-column">
          <div class="functions-section">
            <div class="card-header">
              <h3>快速功能</h3>
            </div>
            <div class="function-grid">
              <div class="function-card" @click="goToPatientInfo">
                <div class="card-icon primary">
                  <el-icon :size="36"><UserFilled /></el-icon>
                </div>
                <h4>查看患者信息</h4>
                <p>快速查看已预约患者档案</p>
              </div>

              <div class="function-card" @click="goToStatistics">
                <div class="card-icon danger">
                  <el-icon :size="36"><DataAnalysis /></el-icon>
                </div>
                <h4>数据统计</h4>
                <p>查看工作统计</p>
              </div>

              <div class="function-card" @click="viewTodayAppointments">
                <div class="card-icon primary">
                  <el-icon :size="36"><Bell /></el-icon>
                </div>
                <h4>今日预约</h4>
                <p>查看今日预约</p>
              </div>

              <div class="function-card" @click="goToSchedule">
                <div class="card-icon success">
                  <el-icon :size="36"><Calendar /></el-icon>
                </div>
                <h4>我的排班</h4>
                <p>查看和管理排班信息</p>
              </div>

              <div class="function-card" @click="goToLeaveRequest">
                <div class="card-icon warning">
                  <el-icon :size="36"><Box /></el-icon>
                </div>
                <h4>休假申请</h4>
                <p>提交和查看休假请求</p>
              </div>

              <div class="function-card" @click="goToSlotApplication">
                <div class="card-icon info">
                  <el-icon :size="36"><DocumentAdd /></el-icon>
                </div>
                <h4>申请加号</h4>
                <p>申请临时增加号源</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="600px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px" label-position="top">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="头像" prop="photoUrl">
              <el-upload
                  class="avatar-uploader"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleAvatarChange"
                  :before-upload="beforeAvatarUpload"
                  accept="image/jpeg,image/png"
              >
                <img v-if="editForm.photoUrl" :src="getAvatarUrl(editForm.photoUrl)" class="avatar-image" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>

          <el-col :span="16">
            <el-form-item label="姓名 (工号)" prop="name">
              <el-input v-model="editForm.name" disabled>
                <template #append>{{ doctorStore.detailedDoctorInfo.username || '工号未知' }}</template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="phoneNumber">
              <el-input v-model="editForm.phoneNumber" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="擅长领域" prop="specialty">
          <el-input
              v-model="editForm.specialty"
              type="textarea"
              :rows="3"
              placeholder="请输入您的专业擅长领域，例如：高血压、糖尿病等内科疾病"
          />
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
              v-model="editForm.bio"
              type="textarea"
              :rows="5"
              placeholder="请输入您的个人简介，将展示给患者"
          />
        </el-form-item>

      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// 【已修改】导入新图标
import {
  UserFilled, Document, Calendar, DataAnalysis,
  User, Clock, Star, SwitchButton,
  OfficeBuilding, Phone, Edit, Key,
  Plus, Bell, Box, DocumentAdd,
  Sunrise, Sunny // <-- 新增图标
} from '@element-plus/icons-vue'
import { useDoctorStore } from '@/stores/doctorStore'
import { getSchedulesByDoctorId } from '@/api/schedule'
import { getTodaysPatients } from '@/api/patient'
import defaultAvatar from '@/assets/doctor.jpg';

const router = useRouter()
const doctorStore = useDoctorStore()

// --- 统计数据状态 ---
const statsLoading = ref(false)
const todayStats = reactive({
  total: 0,
  morning: 0,
  afternoon: 0
})

// --- 对话框状态 ---
const editDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

// --- 表单引用 ---
const editFormRef = ref(null)
const passwordFormRef = ref(null)

// --- 编辑表单 ---
const editForm = reactive({
  name: '',
  phoneNumber: '',
  specialty: '',
  bio: '',
  photoUrl: '',
  avatarFile: null // 保存头像文件对象
})

// --- 密码表单 ---
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// --- 编辑表单验证规则 ---
const editRules = reactive({
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  specialty: [{ max: 500, message: '擅长领域描述过长', trigger: 'blur' }],
  bio: [{ max: 1000, message: '个人简介过长', trigger: 'blur' }]
})

// --- 密码表单验证规则 ---
const passwordRules = reactive({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// --- 今日排班状态 ---
const scheduleLoading = ref(false)
const todaySchedules = ref([])
const currentDoctorId = computed(() => {
  return doctorStore.currentDoctorId;
})
const formatDateForAPI = (date) => {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// --- 加载今日排班 (调用真实API) ---
const loadTodaySchedule = async () => {
  // 从localStorage获取doctorId
  const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'))
  const doctorId = savedInfo?.doctorId || currentDoctorId.value;
  
  if (!doctorId) {
    console.error('无法获取医生ID');
    return;
  }
  
  scheduleLoading.value = true;

  try {
    const today = formatDateForAPI(new Date());
    console.log('=== 加载今日排班 ===');
    console.log('doctorId:', doctorId);
    console.log('today:', today);
    
    // 调用后端API获取今日排班
    const response = await getSchedulesByDoctorId(doctorId, {
      startDate: today,
      endDate: today,
      page: 0,
      size: 100
    });
    
    console.log('API响应:', response);
    
    // 处理分页响应
    if (response && response.content) {
      todaySchedules.value = response.content;
      console.log('今日排班数据:', todaySchedules.value);
    } else {
      todaySchedules.value = [];
      console.log('今日无排班数据');
    }
  } catch (error) {
    console.error('加载今日排班失败:', error);
    ElMessage.error("加载今日排班失败: " + (error.message || '未知错误'));
    todaySchedules.value = [];
  } finally {
    scheduleLoading.value = false;
  }
};

// --- 上午/下午排班 (计算属性) ---
const morningSchedules = computed(() => {
  return todaySchedules.value.filter(s => parseInt(s.startTime.split(':')[0]) < 12)
});
const afternoonSchedules = computed(() => {
  return todaySchedules.value.filter(s => parseInt(s.startTime.split(':')[0]) >= 12)
});


// --- 登出处理 ---
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    doctorStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消操作
  }
}

// --- 编辑资料 ---
const editProfile = () => {
  // 如果之前有 blob URL，先释放
  if (editForm.photoUrl && editForm.photoUrl.startsWith('blob:')) {
    URL.revokeObjectURL(editForm.photoUrl);
  }
  
  // 填充当前信息
  const info = doctorStore.detailedDoctorInfo;
  editForm.name = info.name || doctorStore.displayName;
  editForm.phoneNumber = info.phone || '';
  editForm.specialty = info.specialty || '';
  editForm.bio = info.bio || '';
  editForm.photoUrl = info.photoUrl || defaultAvatar;
  editForm.avatarFile = null; // 重置头像文件

  editDialogVisible.value = true;
}

// --- 保存资料 (调用异步 action) ---
const saveProfile = async () => {
  if (!editFormRef.value) return

  try {
    const valid = await editFormRef.value.validate()
    if (!valid) return
  } catch (error) {
    return
  }

  // 检查头像文件
  console.log('保存前的表单数据:', {
    phoneNumber: editForm.phoneNumber,
    specialty: editForm.specialty,
    bio: editForm.bio,
    avatarFile: editForm.avatarFile,
    isFile: editForm.avatarFile instanceof File,
    fileName: editForm.avatarFile instanceof File ? editForm.avatarFile.name : null
  });

  const dataToUpdate = {
    phoneNumber: editForm.phoneNumber,
    specialty: editForm.specialty,
    bio: editForm.bio,
    avatarFile: editForm.avatarFile // 传递文件对象（可能是 null 或 File）
  };

  const success = await doctorStore.updateDoctorInfo(dataToUpdate)

  if (success) {
    // 清除 blob URL（如果有）
    if (editForm.photoUrl && editForm.photoUrl.startsWith('blob:')) {
      URL.revokeObjectURL(editForm.photoUrl);
    }
    
    ElMessage.success('资料更新成功')
    editDialogVisible.value = false
    
    // 刷新医生信息（包括头像）
    await doctorStore.fetchDetailedDoctorInfo();
  } else {
    ElMessage.error(doctorStore.error || '更新失败')
  }
}

// --- 修改密码 ---
const changePassword = () => {
  passwordFormRef.value?.resetFields()
  passwordDialogVisible.value = true
}

// --- 保存密码 ---
const savePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return
  } catch (error) {
    return
  }

  const success = await doctorStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword, passwordForm.confirmPassword)

  if (success) {
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    doctorStore.logout()
    router.push('/login')
  } else {
    ElMessage.error(doctorStore.error || '密码修改失败')
  }
}

// --- 头像上传处理 ---
const handleAvatarChange = (file) => {
  // 如果之前有 blob URL，先释放
  if (editForm.photoUrl && editForm.photoUrl.startsWith('blob:')) {
    URL.revokeObjectURL(editForm.photoUrl);
  }
  
  // 保存文件对象
  editForm.avatarFile = file.raw;
  // 创建预览 URL
  if (file.raw) {
    editForm.photoUrl = URL.createObjectURL(file.raw);
  }
}

const beforeAvatarUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('头像必须是 JPG 或 PNG 格式!');
    return false;
  } else if (rawFile.size / 1024 / 1024 > 1) {
    ElMessage.error('头像大小不能超过 1MB!');
    return false;
  }
  return true;
}

// 获取头像 URL（处理 blob URL 和普通 URL）
const getAvatarUrl = (url) => {
  if (!url) return defaultAvatar;
  if (url.startsWith('blob:')) return url;
  if (url.startsWith('http')) return url;
  // 如果是相对路径（以 / 开头），拼接完整的 baseURL
  if (url.startsWith('/')) {
    return `http://localhost:8080${url}`;
  }
  return url || defaultAvatar;
}

// --- 加载今日统计数据 ---
const loadTodayStats = async () => {
  const savedInfo = JSON.parse(localStorage.getItem('xm-pro-doctor'));
  const doctorId = savedInfo?.doctorId || currentDoctorId.value;
  
  if (!doctorId) {
    console.error('无法获取医生ID');
    return;
  }
  
  statsLoading.value = true;
  
  try {
    const today = formatDateForAPI(new Date());
    console.log('=== 加载今日统计 ===');
    console.log('doctorId:', doctorId);
    console.log('today:', today);
    
    // 调用API获取今日患者列表
    const response = await getTodaysPatients(doctorId, today);
    console.log('统计API响应:', response);
    
    const patients = Array.isArray(response) ? response : [];
    
    // 计算总数
    todayStats.total = patients.length;
    
    // 计算上午患者数（开始时间 < 12:00）
    todayStats.morning = patients.filter(p => {
      if (!p.startTime) return false;
      const hour = parseInt(p.startTime.split(':')[0]);
      return hour < 12;
    }).length;
    
    // 计算下午患者数（开始时间 >= 12:00）
    todayStats.afternoon = patients.filter(p => {
      if (!p.startTime) return false;
      const hour = parseInt(p.startTime.split(':')[0]);
      return hour >= 12;
    }).length;
    
    console.log('统计结果:', todayStats);
  } catch (error) {
    console.error('加载统计数据失败:', error);
    todayStats.total = 0;
    todayStats.morning = 0;
    todayStats.afternoon = 0;
  } finally {
    statsLoading.value = false;
  }
};

// --- 功能导航方法 ---
const goToPatientInfo = () => {
  router.push('/patient-info')
}
const goToStatistics = () => {
  ElMessage.info('数据统计功能开发中...')
}
const viewTodayAppointments = () => {
  ElMessage.info('今日预约功能开发中...')
}
const goToSchedule = () => {
  router.push('/my-schedule')
}
const goToLeaveRequest = () => {
  router.push('/leave-request')
}
const goToSlotApplication = () => {
  router.push('/slot-application')
}

// --- 页面加载时 ---
onMounted(() => {
  if (doctorStore.isAuthenticated) {
    doctorStore.fetchDetailedDoctorInfo();
  }
  loadTodaySchedule();
  loadTodayStats();
})
</script>

<style scoped>
.doctor-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
}
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
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 32px;
}
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-section h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
  color: #2c3e50;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.logo-section .el-icon {
  color: #667eea;
  margin-right: 4px;
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
.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px;
}

/* 【新增】标语卡片 */
.banner-card {
  height: 120px;
  border-radius: 16px;
  margin-bottom: 32px;
  /* 【已修改】使用您 assets 目录下的图片 */
  background-image: url('@/assets/7d5af898a55918db907cb51a56353c6b.jpg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.banner-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4); /* 40% 黑色遮罩 */
  z-index: 1;
}
.banner-text {
  font-size: 2.5rem;
  font-weight: 600;
  color: #ffffff;
  z-index: 2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}


.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
/* 【已修改】调整了图标对应的渐变色 */
.stat-icon.patients { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.appointments { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
.stat-icon.records { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-info { flex: 1; }
.stat-label { font-size: 0.9rem; color: #909399; margin: 0 0 8px 0; }
.stat-value { font-size: 2rem; font-weight: 600; color: #2c3e50; margin: 0; }
.content-grid { display: grid; grid-template-columns: 380px 1fr; gap: 24px; }
.left-column { display: flex; flex-direction: column; gap: 24px; }
.doctor-info-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
}
.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 { margin: 0; font-size: 1.1rem; font-weight: 600; color: #2c3e50; }
.doctor-profile { padding: 24px; text-align: center; }
.doctor-avatar-large { margin-bottom: 20px; }
.doctor-details h2 { font-size: 1.5rem; font-weight: 600; color: #2c3e50; margin: 0 0 16px 0; }
.detail-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 10px 0;
  color: #606266;
  font-size: 0.95rem;
}
.doctor-actions { display: flex; gap: 12px; margin-top: 24px; justify-content: center; }
.schedule-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}
.schedule-list { padding: 16px 24px 24px; }
.shift-group { margin-bottom: 16px; }
.shift-group:last-child { margin-bottom: 0; }
.shift-title {
  font-size: 1rem;
  font-weight: 600;
  color: #303133;
  padding-bottom: 12px;
  margin: 0 0 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.no-schedule-text { padding-top: 8px; display: block; text-align: center; }
.slot-list { display: grid; grid-template-columns: 1fr; gap: 12px; }
.slot-card {
  background: #f9fafb;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px 16px;
  transition: all 0.2s ease;
}
.slot-card:hover { box-shadow: 0 4px 8px rgba(0,0,0,0.05); border-color: #667eea; }
.slot-time { font-size: 1rem; font-weight: 600; color: #303133; margin-bottom: 8px; }
.slot-details { font-size: 0.9rem; color: #606266; }
.right-column { display: flex; flex-direction: column; }
.functions-section {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  flex: 1;
}
.function-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  padding: 16px 24px 24px;
}
.function-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 24px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}
.function-card:hover {
  background: #fff;
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.15);
}
.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
}
.card-icon.primary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.card-icon.success { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
.card-icon.danger { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); color: #f56c6c; }
.card-icon.warning { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); color: #d9822b; }
.card-icon.info { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); color: #409eff; }
.function-card h4 { font-size: 1.1rem; font-weight: 600; color: #2c3e50; margin: 0 0 8px 0; }
.function-card p { font-size: 0.85rem; color: #909399; margin: 0; line-height: 1.4; }

/* 【新增】头像上传样式 */
.avatar-uploader {
  width: 120px;
  height: 120px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader:hover {
  border-color: var(--el-color-primary);
}
.avatar-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 【新增】修改 el-form-item 的 label 样式 */
:deep(.el-form-item__label) {
  font-weight: 600 !important;
  color: #303133 !important;
}


@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) {
  .main-content { padding: 20px; }
  .navbar-content { padding: 0 16px; }
  .stats-row { grid-template-columns: 1fr 1fr; }
  .function-grid { grid-template-columns: 1fr; }
  .user-name { display: none; }
}
@media (max-width: 480px) { .stats-row { grid-template-columns: 1fr; } }
</style>