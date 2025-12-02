<template>
  <div class="login-container">
    <!-- 左侧装饰区域 -->
    <div class="decoration-section">
      <div class="decoration-content">
        <h1>医院后台管理系统</h1>
        <p>高效管理，贴心服务</p>
        <div class="decoration-image">
          <img src="@/assets/doctor.jpg" alt="医院管理系统" />
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-section">
      <div class="form-container">
        <div class="form-header">
          <h2>管理员登录</h2>
          <p>请输入您的管理员账户信息</p>
        </div>

        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="rules" 
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="adminId">
            <el-input
              v-model="loginForm.adminId"
              placeholder="请输入管理员ID"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <p class="help-text">
            <el-icon><InfoFilled /></el-icon>
            如有登录问题，请联系系统管理员
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/adminStore'
import request from '@/utils/request'

const router = useRouter()
const adminStore = useAdminStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  adminId: '',
  password: ''
})

const rules = reactive({
  adminId: [
    { required: true, message: '请输入管理员ID', trigger: 'blur' },
    { min: 3, max: 20, message: '管理员ID长度在3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return
  } catch (error) {
    return
  }

  loading.value = true

  try {
    const response = await request({
      url: '/api/auth/admin/login',
      method: 'POST',
      data: {
        identifier: loginForm.adminId,
        password: loginForm.password
      }
    })

    if (response.code === '200' || response.code === 200) {
      // 保存登录信息到store
      const loginData = response.data
      console.log('登录响应数据:', loginData)
      
      // 从 userInfo 中提取管理员信息
      const adminInfo = loginData.userInfo || {}
      console.log('管理员信息:', adminInfo)
      
      adminStore.loginSuccess(adminInfo, {
        adminId: adminInfo.adminId,
        username: adminInfo.username,
        fullName: adminInfo.fullName
      })

      ElMessage.success('登录成功')
      
      // 立即跳转到主页
      router.push('/')
    } else {
      ElMessage.error(response.msg || '登录失败')
    }
  } catch (error) {
    console.error('登录请求失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 页面加载时检查是否已登录
onMounted(() => {
  if (adminStore.isAuthenticated) {
    router.push('/')
  }
})
</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.decoration-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.decoration-content {
  text-align: center;
  color: white;
  padding: 40px;
}

.decoration-content h1 {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 20px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.decoration-content p {
  font-size: 1.2rem;
  margin-bottom: 40px;
  opacity: 0.9;
}

.decoration-image {
  margin-top: 40px;
}

.decoration-image img {
  width: 300px;
  height: 300px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-container {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 50px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.form-header p {
  color: #666;
  font-size: 1rem;
}

.login-form {
  margin-bottom: 30px;
}

.login-form .el-form-item {
  margin-bottom: 25px;
}

.login-form .el-input {
  height: 50px;
}

.login-form .el-input__wrapper {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.login-form .el-input__wrapper:hover {
  border-color: #667eea;
}

.login-form .el-input__wrapper.is-focus {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  background: linear-gradient(45deg, #667eea, #764ba2);
  border: none;
  font-size: 1.1rem;
  font-weight: bold;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.6);
}

.login-btn:active {
  transform: translateY(0);
}

.form-footer {
  text-align: center;
}

.help-text {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 0.9rem;
  margin: 0;
}

.help-text .el-icon {
  margin-right: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }
  
  .decoration-section {
    flex: none;
    height: 200px;
  }
  
  .decoration-content h1 {
    font-size: 1.8rem;
  }
  
  .decoration-image img {
    width: 120px;
    height: 120px;
  }
  
  .form-section {
    flex: 1;
    padding: 20px;
  }
  
  .form-container {
    padding: 30px 20px;
  }
}
</style>
