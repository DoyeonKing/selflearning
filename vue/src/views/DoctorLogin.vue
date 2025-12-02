<template>
  <div class="login-container">
    <div class="decoration-section">
      <div class="decoration-content">
        <h1>医生工作台</h1>
        <p>专业医疗，高效服务</p>
        <div class="decoration-image">
          <img src="@/assets/doctor.jpg" alt="医生工作台" />
        </div>
        <div class="features">
          <div class="feature-item">
            <span class="feature-icon">🏥</span>
            <span>患者管理</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📋</span>
            <span>病历记录</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <span>数据统计</span>
          </div>
        </div>
      </div>
    </div>

    <div class="form-section">
      <div class="form-container">
        <div class="form-header">
          <h2>医生登录</h2>
          <p>{{ isActivation ? '账户激活' : '请输入您的医生账户信息' }}</p>
        </div>

        <div v-if="!isActivation" class="login-form">
          <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              @submit.prevent="handleLogin"
          >
            <el-form-item prop="identifier">
              <el-input
                  v-model="loginForm.identifier"
                  placeholder="请输入工号 (默认: D001)"
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
            <el-button type="primary" link @click="switchToActivation">
              首次使用？点击激活账户
            </el-button>
          </div>
        </div>

        <div v-else class="activation-form">
          <div class="step-indicator">
            <div class="step" :class="{ active: activationStep >= 1, completed: activationStep > 1 }">
              <div class="step-number">1</div>
              <div class="step-text">验证信息</div>
            </div>
            <div class="step-line" :class="{ active: activationStep > 1 }"></div>
            <div class="step" :class="{ active: activationStep >= 2, completed: activationStep > 2 }">
              <div class="step-number">2</div>
              <div class="step-text">身份验证</div>
            </div>
          </div>

          <div v-if="activationStep === 1" class="step-content">
            <h3 class="step-title">第一步：验证初始信息</h3>
            <el-form
                ref="activationFormRef"
                :model="activationForm"
                :rules="activationRules1"
            >
              <el-form-item prop="identifier">
                <el-input
                    v-model="activationForm.identifier"
                    placeholder="请输入工号"
                    size="large"
                    prefix-icon="User"
                    clearable
                />
              </el-form-item>

              <el-form-item prop="initialPassword">
                <el-input
                    v-model="activationForm.initialPassword"
                    type="password"
                    placeholder="请输入初始密码"
                    size="large"
                    prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button
                    type="primary"
                    size="large"
                    class="login-btn"
                    :loading="loading"
                    @click="handleActivationStep1"
                >
                  {{ loading ? '验证中...' : '下一步' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activationStep === 2" class="step-content">
            <h3 class="step-title">第二步：身份验证</h3>
            <div class="info-desc">
              <el-icon><Lock /></el-icon>
              <span>为了您的账户安全，请输入您的身份证号进行验证</span>
            </div>

            <el-form
                ref="activationFormRef2"
                :model="activationForm"
                :rules="activationRules2"
            >
              <el-form-item prop="idCardInput">
                <el-input
                    v-model="activationForm.idCardInput"
                    placeholder="请输入身份证号后6位"
                    size="large"
                    prefix-icon="CreditCard"
                    maxlength="6"
                    clearable
                />
              </el-form-item>

              <el-form-item prop="newPassword">
                <el-input
                    v-model="activationForm.newPassword"
                    type="password"
                    placeholder="请输入新密码（6-20位）"
                    size="large"
                    prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input
                    v-model="activationForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    size="large"
                    prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button
                    type="primary"
                    size="large"
                    class="login-btn"
                    :loading="loading"
                    @click="handleActivationStep2"
                >
                  {{ loading ? '激活中...' : '完成激活' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <div class="form-footer">
            <el-button type="primary" link @click="switchToLogin">
              返回登录
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Check, CreditCard } from '@element-plus/icons-vue'
import { useDoctorStore } from '@/stores/doctorStore'
import request from '@/utils/request'
import defaultAvatar from '@/assets/doctor.jpg';

const router = useRouter()
const doctorStore = useDoctorStore()

// 表单引用
const loginFormRef = ref(null)
const activationFormRef = ref(null)
const activationFormRef2 = ref(null)
const loading = ref(false)

// 激活状态
const isActivation = ref(false)
const activationStep = ref(1)

// 登录表单
const loginForm = reactive({
  identifier: 'D001',
  password: '123'
})

// 激活表单
const activationForm = reactive({
  identifier: '',
  initialPassword: '',
  idCard: '',
  idCardInput: '',
  newPassword: '',
  confirmPassword: ''
})

// 登录表单验证规则
const loginRules = reactive({
  identifier: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { min: 3, max: 20, message: '工号长度在3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 20, message: '密码长度在3-20个字符', trigger: 'blur' }
  ]
})

// 激活表单验证规则 - 第一步
const activationRules1 = reactive({
  identifier: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { min: 3, max: 20, message: '工号长度在3-20个字符', trigger: 'blur' }
  ],
  initialPassword: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 3, max: 20, message: '密码长度在3-20个字符', trigger: 'blur' }
  ]
})

// 激活表单验证规则 - 第二步
const activationRules2 = reactive({
  idCardInput: [
    { required: true, message: '请输入身份证号后6位', trigger: 'blur' },
    { len: 6, message: '请输入完整的6位数字', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '身份证号后6位必须为数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== activationForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 切换到激活模式
const switchToActivation = () => {
  isActivation.value = true
  activationStep.value = 1
  Object.assign(activationForm, {
    identifier: '',
    initialPassword: '',
    idCard: '',
    newPassword: '',
    confirmPassword: ''
  })
}

// 切换到登录模式
const switchToLogin = () => {
  isActivation.value = false
  activationStep.value = 1
  Object.assign(loginForm, {
    identifier: '',
    password: ''
  })
}

// 医生登录 (核心修改部分)
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
    // 1. 第一步：认证，获取Token
    const loginRes = await request({
      url: '/api/doctor/auth/login',
      method: 'POST',
      data: {
        identifier: loginForm.identifier,
        password: loginForm.password
      }
    })

    if (loginRes.code === '200' || loginRes.code === 200) {
      const loginData = loginRes.data || {}
      const loginDoctorInfo = loginData.userInfo || {}
      const token = loginData.token || `temp-token-${loginDoctorInfo.identifier || loginForm.identifier}`

      if (!loginData.token) {
        console.warn('登录响应未返回 token，已使用临时 token 占位，后端启用鉴权后请返回实际 token。')
      }

      // 2. 第二步：使用 Token 调取医生详细信息
      const detailRes = await request({
        url: `/api/doctors/identifier/${loginForm.identifier}`,
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}` // 【修改】加上 Bearer 前缀
        }
      });

      // 【新增】打印返回数据，方便调试
      console.log('医生详情接口返回:', detailRes);

      // 【关键修改】兼容多种返回格式
      let doctorData = null;

      // 情况 A: 标准 Result 格式 { code: 200, data: {...} }
      if ((detailRes.code === '200' || detailRes.code === 200) && detailRes.data) {
        doctorData = detailRes.data;
      }
      // 情况 B: 直接返回对象 (无 code 字段，但有 doctorId 或 identifier)
      else if (detailRes.doctorId || detailRes.identifier || detailRes.fullName) {
        doctorData = detailRes;
      }

      if (doctorData) {
        // 3. 第三步：如果有 doctorId，尝试获取完整信息（包含 specialty 和 bio）
        let fullDoctorData = doctorData;
        const doctorId = doctorData.doctorId || loginDoctorInfo.doctorId;
        
        if (doctorId) {
          try {
            console.log('登录后尝试获取完整医生信息，doctorId:', doctorId);
            const fullInfoRes = await request({
              url: `/api/doctors/${doctorId}`,
              method: 'GET',
              headers: {
                'Authorization': `Bearer ${token}`
              }
            });
            
            // 处理完整信息的响应格式
            if (fullInfoRes.code === '200' || fullInfoRes.code === 200) {
              fullDoctorData = fullInfoRes.data || fullDoctorData;
            } else if (fullInfoRes.fullName || fullInfoRes.doctorId) {
              fullDoctorData = fullInfoRes;
            }
            console.log('获取到完整医生信息:', fullDoctorData);
          } catch (error) {
            console.warn('获取完整医生信息失败，使用部分信息:', error);
            // 如果获取完整信息失败，继续使用部分信息
          }
        }

        // 4. 构造完整的医生信息对象
        const fullDoctorInfo = {
          doctorId: String(fullDoctorData.doctorId || doctorId || ''),
          name: fullDoctorData.fullName || fullDoctorData.name || loginDoctorInfo.fullName || '医生',
          department: fullDoctorData.department?.name 
              || fullDoctorData.departmentName
              || (fullDoctorData.department ? fullDoctorData.department.name : '')
              || loginDoctorInfo.departmentName
              || '未知科室',
          position: fullDoctorData.title || loginDoctorInfo.title || '职称未知',
          phone: fullDoctorData.phoneNumber || loginDoctorInfo.phoneNumber || '',
          specialty: fullDoctorData.specialty || loginDoctorInfo.specialty || '',
          bio: fullDoctorData.bio || loginDoctorInfo.bio || '',
          photoUrl: fullDoctorData.photoUrl || loginDoctorInfo.photoUrl || defaultAvatar,
          username: loginForm.identifier
        };

        // 5. 保存到 Store
        doctorStore.loginSuccess({ doctorInfo: fullDoctorInfo }, {
          identifier: loginForm.identifier,
          token: token
        })

        ElMessage.success('登录成功')
        router.push('/doctor-dashboard')

      } else {
        // 如果还是获取失败，打印详细错误
        console.error('无法解析医生详情数据:', detailRes);
        ElMessage.error('获取医生详情失败: 数据格式不匹配');
      }

    } else {
      ElMessage.error(loginRes.msg || '登录失败')
    }
  } catch (error) {
    console.error('登录请求失败:', error)
    ElMessage.error(error.msg || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 激活第一步
const handleActivationStep1 = async () => {
  if (!activationFormRef.value) return
  try {
    await activationFormRef.value.validate()
  } catch (error) { return }

  loading.value = true
  try {
    const response = await request({
      url: '/api/doctor/auth/verify',
      method: 'POST',
      data: {
        identifier: activationForm.identifier,
        initialPassword: activationForm.initialPassword
      }
    })
    if (response && response.message) {
      activationStep.value = 2
      ElMessage.success('初始信息验证成功')
    } else if (response && response.error) {
      ElMessage.error(response.error)
    } else {
      ElMessage.error('验证失败，响应格式错误')
    }
  } catch (error) {
    ElMessage.error('无法连接到服务器')
  } finally {
    loading.value = false
  }
}

// 激活第二步
const handleActivationStep2 = async () => {
  if (!activationFormRef2.value) return
  try {
    await activationFormRef2.value.validate()
  } catch (error) { return }

  loading.value = true
  try {
    const response = await request({
      url: '/api/doctor/auth/activate',
      method: 'POST',
      data: {
        identifier: activationForm.identifier,
        idCardEnding: activationForm.idCardInput,
        newPassword: activationForm.newPassword,
        confirmPassword: activationForm.confirmPassword
      }
    })
    if (response && response.message) {
      ElMessage.success('账户激活成功！请使用新密码登录。')
      setTimeout(() => { switchToLogin() }, 2000)
    } else if (response && response.error) {
      ElMessage.error(response.error)
    } else {
      ElMessage.error('激活失败')
    }
  } catch (error) {
    ElMessage.error('无法连接到服务器')
  } finally {
    loading.value = false
  }
}

// 页面加载时检查是否已登录
onMounted(() => {
  if (doctorStore.isAuthenticated) {
    router.push('/doctor-dashboard')
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
  margin: 40px 0;
}

.decoration-image img {
  width: 300px;
  height: 300px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.features {
  display: flex;
  justify-content: space-around;
  margin-top: 40px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
}

.feature-icon {
  font-size: 2rem;
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

/* 激活步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e9ecef;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.step.active .step-number {
  background-color: #667eea;
  color: #ffffff;
}

.step.completed .step-number {
  background-color: #28a745;
  color: #ffffff;
}

.step-text {
  font-size: 14px;
  color: #6c757d;
  transition: all 0.3s ease;
}

.step.active .step-text {
  color: #667eea;
  font-weight: bold;
}

.step.completed .step-text {
  color: #28a745;
  font-weight: bold;
}

.step-line {
  width: 80px;
  height: 3px;
  background-color: #e9ecef;
  margin: 0 15px;
  margin-top: -20px;
  transition: all 0.3s ease;
}

.step-line.active {
  background-color: #667eea;
}

/* 步骤内容 */
.step-content {
  margin-bottom: 30px;
}

.step-title {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.verification-desc {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 30px;
  padding: 15px;
  background-color: #e8f5e8;
  border-radius: 8px;
  border-left: 4px solid #28a745;
  color: #28a745;
  font-size: 0.9rem;
}

.info-desc {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 30px;
  padding: 15px;
  background-color: #eff6ff;
  border-radius: 8px;
  border-left: 4px solid #667eea;
  color: #667eea;
  font-size: 0.9rem;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
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

  .features {
    margin-top: 20px;
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