<template>
  <div class="check-in-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>患者签到</span>
          <div>
            <el-button type="info" @click="showTokenHelp" style="margin-right: 10px;">如何获取Token？</el-button>
            <el-button type="primary" @click="scanQRCode">扫描二维码</el-button>
          </div>
        </div>
      </template>
      
      <!-- Token帮助信息 -->
      <el-alert
        v-if="showHelp"
        title="如何获取患者二维码Token？"
        type="info"
        :closable="true"
        @close="showHelp = false"
        style="margin-bottom: 20px;"
      >
        <div style="margin-top: 10px;">
          <p><strong>方式一：从患者小程序获取</strong></p>
          <p>1. 患者在小程序中打开预约详情页</p>
          <p>2. 点击"生成二维码"按钮</p>
          <p>3. 二维码中包含Token，格式为：<code>APPOINTMENT_预约ID_时间戳_随机数</code></p>
          <p>4. 扫描二维码或手动输入Token进行签到</p>
          <p style="margin-top: 10px;"><strong>方式二：通过预约ID生成（测试用）</strong></p>
          <el-input
            v-model="testAppointmentId"
            placeholder="输入预约ID"
            style="width: 200px; margin-right: 10px;"
          />
          <el-button type="success" @click="generateTestToken" :loading="generatingToken">
            生成测试Token
          </el-button>
          <div v-if="testToken" style="margin-top: 10px;">
            <p><strong>生成的Token：</strong></p>
            <el-input
              :value="testToken"
              readonly
              style="margin-top: 5px;"
            >
              <template #append>
                <el-button @click="copyToken">复制</el-button>
              </template>
            </el-input>
            <p style="color: #909399; font-size: 12px; margin-top: 5px;">
              ⚠️ Token有效期为5分钟，过期后需要重新生成
            </p>
          </div>
        </div>
      </el-alert>
      
      <div v-if="checkInResult" class="check-in-result">
        <el-alert 
          :title="checkInResult.success ? '签到成功' : '签到失败'" 
          :type="checkInResult.success ? 'success' : 'error'" 
          :description="checkInResult.message" 
          show-icon 
          :closable="false" 
        />
        <div v-if="checkInResult.success && checkInResult.data" class="patient-info">
          <el-descriptions title="患者信息" :column="2" border>
            <el-descriptions-item label="患者姓名">{{ checkInResult.data.patientName }}</el-descriptions-item>
            <el-descriptions-item label="科室">{{ checkInResult.data.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="医生姓名">{{ checkInResult.data.doctorName }}</el-descriptions-item>
            <el-descriptions-item label="就诊序号">{{ checkInResult.data.appointmentNumber }}</el-descriptions-item>
            <el-descriptions-item label="签到时间" :span="2">{{ formatDateTime(checkInResult.data.checkInTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      
      <div v-else class="tip-info">
        <el-empty description="点击上方按钮扫描患者二维码进行签到" />
      </div>
      
      <!-- 叫号队列 -->
      <!-- 
        注意：此页面包含叫号功能，用于测试。
        正式环境中，叫号功能应由医生端完成，管理员/分诊台仅负责签到和队列管理。
      -->
      <el-card style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>叫号队列（测试用）</span>
            <div>
              <el-select 
                v-model="selectedScheduleId" 
                placeholder="选择排班" 
                style="width: 300px; margin-right: 10px;"
                @change="loadCallQueue"
                filterable
              >
                <el-option
                  v-for="schedule in scheduleList"
                  :key="schedule.scheduleId"
                  :label="`${schedule.doctorName || '未知医生'} - ${schedule.scheduleDate} ${schedule.slotName || '未知时段'}`"
                  :value="schedule.scheduleId"
                >
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ schedule.doctorName || '未知医生' }}</span>
                    <span style="color: #909399; margin-left: 10px;">
                      {{ schedule.scheduleDate }} {{ schedule.slotName || '未知时段' }}
                    </span>
                  </div>
                </el-option>
              </el-select>
              <el-button type="primary" @click="loadCallQueue" :loading="loadingQueue">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>
        </template>
        
        <div v-if="selectedScheduleId">
          <div v-if="loadingQueue" style="text-align: center; padding: 20px;">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span style="margin-left: 10px;">加载中...</span>
          </div>
          <div v-else-if="callQueue.length === 0" style="text-align: center; padding: 20px;">
            <el-empty description="暂无已签到的患者" />
          </div>
          <el-table v-else :data="Array.isArray(callQueue) ? callQueue : []" stripe style="width: 100%">
            <el-table-column prop="appointmentNumber" label="就诊序号" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="large">{{ row.appointmentNumber }}号</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="患者姓名" width="120">
              <template #default="{ row }">
                {{ row.patient?.fullName || '未知患者' }}
              </template>
            </el-table-column>
            <el-table-column label="签到状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isOnTime ? 'success' : 'warning'">
                  {{ row.isOnTime ? '按时' : '迟到' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="checkInTime" label="签到时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.checkInTime) }}
              </template>
            </el-table-column>
            <el-table-column label="叫号状态" width="140" align="center">
              <template #default="{ row }">
                <div style="display: flex; flex-direction: column; gap: 4px; align-items: center;">
                  <el-tag v-if="row.calledAt && row.recheckInTime" type="danger" size="small">已过号</el-tag>
                  <el-tag v-else-if="row.calledAt" type="info" size="small">已叫号</el-tag>
                  <el-tag v-else type="success" size="small">待叫号</el-tag>
                  <span v-if="row.missedCallCount > 0" style="font-size: 12px; color: #f56c6c;">
                    过号{{ row.missedCallCount }}次
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="叫号时间" width="180">
              <template #default="{ row }">
                {{ row.calledAt ? formatDateTime(row.calledAt) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="重新签到时间" width="180">
              <template #default="{ row }">
                {{ row.recheckInTime ? formatDateTime(row.recheckInTime) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <!-- 未叫号：显示叫号按钮 -->
                <el-button 
                  v-if="!row.calledAt" 
                  type="primary" 
                  size="small" 
                  @click="handleCall(row.appointmentId)"
                  :loading="callingId === row.appointmentId"
                >
                  叫号
                </el-button>
                <!-- 已叫号：显示就诊完成和标记过号按钮 -->
                <div v-if="row.calledAt" style="display: flex; gap: 8px;">
                <el-button 
                    type="success"
                  size="small" 
                    @click="handleCompleteAppointment(row.appointmentId)"
                    :loading="completingId === row.appointmentId"
                >
                    就诊完成
                </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click="handleMarkMissedCall(row.appointmentId)"
                    :loading="markingId === row.appointmentId"
                  >
                    标记过号
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 下一个叫号提示 -->
          <div v-if="nextToCall" style="margin-top: 20px; padding: 15px; background-color: #f0f9ff; border-radius: 4px;">
            <el-alert
              title="下一个叫号"
              type="info"
              :closable="false"
            >
              <template #default>
                <div style="display: flex; align-items: center; justify-content: space-between;">
                  <div>
                    <strong v-if="nextToCall.appointmentNumber">{{ nextToCall.appointmentNumber }}号</strong>
                    <strong v-else>待叫号</strong>
                    <span v-if="nextToCall.patient?.fullName"> - {{ nextToCall.patient.fullName }}</span>
                    <span v-else> - 未知患者</span>
                    <el-tag :type="nextToCall.isOnTime ? 'success' : 'warning'" style="margin-left: 10px;">
                      {{ nextToCall.isOnTime ? '按时' : '迟到' }}
                    </el-tag>
                  </div>
                  <div>
                    <el-button 
                      v-if="nextToCall.calledAt" 
                      type="success"
                      size="small" 
                      @click="handleCompleteAppointment(nextToCall.appointmentId)"
                      :loading="completingId === nextToCall.appointmentId"
                      style="margin-right: 10px;"
                    >
                      就诊完成
                    </el-button>
                    <el-button 
                      v-if="nextToCall.calledAt"
                      type="danger"
                      size="small"
                      @click="handleMarkMissedCall(nextToCall.appointmentId)"
                      :loading="markingId === nextToCall.appointmentId"
                      style="margin-right: 10px;"
                    >
                      标记过号
                    </el-button>
                    <el-button
                      v-if="!nextToCall.calledAt"
                      type="primary" 
                      @click="handleCall(nextToCall.appointmentId)" 
                      :loading="callingId === nextToCall.appointmentId"
                      :disabled="!nextToCall.appointmentId"
                    >
                      立即叫号
                    </el-button>
                  </div>
                </div>
              </template>
            </el-alert>
          </div>
        </div>
        <div v-else style="text-align: center; padding: 20px;">
          <el-empty description="请先选择排班" />
        </div>
      </el-card>
      
      <!-- 二维码扫描对话框 -->
      <el-dialog
        v-model="showScanner"
        title="扫描二维码"
        width="90%"
        :close-on-click-modal="false"
        @close="handleDialogClose"
      >
        <div class="scanner-container">
          <div v-if="!isScanning" class="scanner-placeholder">
            <el-icon :size="64" color="#909399"><Camera /></el-icon>
            <p>准备启动摄像头...</p>
          </div>
          <div id="qr-reader" ref="qrReaderRef" style="width: 100%;"></div>
          <div v-if="scanError" class="scan-error">
            <el-alert :title="scanError" type="error" :closable="false" />
          </div>
          <div class="scanner-tips">
            <p>📱 请将患者小程序中的二维码对准扫描框</p>
            <p>💡 如果无法使用摄像头，可以点击"手动输入"按钮</p>
          </div>
        </div>
        <template #footer>
          <el-button @click="showScanner = false">取消</el-button>
          <el-button type="primary" @click="showManualInput">手动输入</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onUnmounted, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Camera, Refresh, Loading } from '@element-plus/icons-vue'
import { checkInAppointment, getAppointmentQrCode, clearCheckIn, getCallQueue, getNextAppointmentToCall, callAppointment, markMissedCall, recheckInAfterMissedCall, completeAppointment } from '@/api/appointment.js'
import { getAllSchedules } from '@/api/schedule.js'
import { Html5Qrcode } from 'html5-qrcode'

const checkInResult = ref(null)
const showHelp = ref(false)
const testAppointmentId = ref('')
const testToken = ref('')
const generatingToken = ref(false)
const showScanner = ref(false)
const isScanning = ref(false)
const scanError = ref('')
const qrReaderRef = ref(null)
let html5QrCode = null

// 叫号队列相关
const selectedScheduleId = ref(null)
const scheduleList = ref([])
const callQueue = ref([])
const nextToCall = ref(null)
const loadingQueue = ref(false)
const callingId = ref(null)
const markingId = ref(null) // 标记过号的loading状态
const completingId = ref(null) // 就诊完成的loading状态
const recheckingId = ref(null)

const scanQRCode = () => {
  try {
    // 检查浏览器是否支持摄像头
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      ElMessage.warning('您的浏览器不支持摄像头功能，请使用手动输入方式')
      showManualInput()
      return
    }
    
    showScanner.value = true
    // 延迟启动扫描，等待对话框渲染完成
    setTimeout(() => {
      if (showScanner.value) {
        startScanning()
      }
    }, 300)
  } catch (error) {
    console.error('启动扫描功能失败:', error)
    ElMessage.error('启动扫描功能失败，请使用手动输入方式')
    showManualInput()
  }
}

const startScanning = async () => {
  try {
    // 检查对话框是否仍然打开
    if (!showScanner.value) {
      return
    }
    
    isScanning.value = true
    scanError.value = ''
    
    const qrCodeSuccessCallback = (decodedText, decodedResult) => {
      try {
        // 扫描成功
        console.log('扫描成功，原始文本:', decodedText)
        console.log('扫描结果对象:', decodedResult)
        
        stopScanning()
        showScanner.value = false
        
        // 清理文本：去除首尾空格和换行符，保持原始大小写（与手动输入保持一致）
        // 注意：不要转换为大写，保持原始格式，让 handleCheckIn 统一处理
        const cleanedText = decodedText ? decodedText.trim().replace(/\r?\n/g, '') : ''
        console.log('清理后的文本:', cleanedText)
        console.log('文本长度:', cleanedText.length)
        console.log('是否以APPOINTMENT开头（不区分大小写）:', /^APPOINTMENT/i.test(cleanedText))
        
        // 验证Token格式：APPOINTMENT_{appointmentId}_{timestamp}_{random}
        // 兼容两种格式：
        // 1. 标准格式：APPOINTMENT_{appointmentId}_{timestamp}_{random}
        // 2. 降级格式：APPOINTMENT{appointmentId}_{timestamp}（缺少下划线和随机字符串）
        if (!cleanedText) {
          ElMessage.warning('扫描到的内容为空，请重新扫描')
          return
        }
        
        // 不区分大小写检查
        const upperText = cleanedText.toUpperCase()
        if (upperText.startsWith('APPOINTMENT')) {
          const parts = cleanedText.split('_')
          console.log('[扫码] Token部分:', parts)
          console.log('[扫码] 部分数量:', parts.length)
          console.log('[扫码] 第一部分:', parts[0])
          console.log('[扫码] 第一部分是否以APPOINTMENT开头（不区分大小写）:', /^APPOINTMENT/i.test(parts[0] || ''))
          console.log('[扫码] 是否以APPOINTMENT_开头（不区分大小写）:', /^APPOINTMENT_/i.test(cleanedText))
          
          // 标准格式：APPOINTMENT_{appointmentId}_{timestamp}_{random}（至少3个部分）
          if (/^APPOINTMENT_/i.test(cleanedText) && parts.length >= 3) {
            console.log('[扫码] 检测到标准格式Token，准备调用handleCheckIn')
            // 格式正确，直接调用 handleCheckIn，让它统一处理Token格式
            handleCheckIn(cleanedText)
          } 
          // 兼容降级格式：APPOINTMENT{appointmentId}_{timestamp}（2个部分）
          else if (parts.length === 2 && parts[0] && /^APPOINTMENT/i.test(parts[0])) {
            console.log('检测到降级格式Token，部分数量:', parts.length)
            // 提取appointmentId（从APPOINTMENT后面，不区分大小写）
            const appointmentIdMatch = parts[0].match(/^APPOINTMENT(\d+)$/i)
            console.log('appointmentId匹配结果:', appointmentIdMatch)
            
            if (appointmentIdMatch && appointmentIdMatch[1]) {
              const appointmentId = appointmentIdMatch[1]
              console.log('提取到的预约ID:', appointmentId)
              
              // 显示详细的错误提示，并提供应急方案
              ElMessageBox.confirm(
                `检测到格式不完整的二维码\n\n` +
                `预约ID: ${appointmentId}\n\n` +
                `可能原因：\n` +
                `• 患者小程序生成二维码时网络异常\n` +
                `• 使用了旧版本的二维码格式\n\n` +
                `推荐方案：\n` +
                `让患者在小程序中点击"手动刷新"按钮重新生成二维码\n\n` +
                `应急方案：\n` +
                `如果患者无法重新生成二维码，可以尝试通过预约ID生成临时Token`,
                '二维码格式不完整',
                {
                  confirmButtonText: '生成临时Token',
                  cancelButtonText: '手动输入Token',
                  type: 'warning'
                }
              ).then(() => {
                // 用户选择生成临时Token
                console.log('用户选择生成临时Token，预约ID:', appointmentId)
                generateTempTokenForAppointment(appointmentId)
              }).catch(() => {
                // 用户选择手动输入
                console.log('用户选择手动输入Token')
                showManualInput()
              })
            } else {
              console.warn('无法匹配appointmentId，parts[0]:', parts[0])
              ElMessage.warning('无法解析二维码中的预约ID，请让患者重新生成二维码')
            }
          } else {
            // 尝试匹配降级格式：APPOINTMENT{数字}_{时间戳}
            console.warn('Token格式不匹配标准格式，尝试匹配降级格式，部分数量:', parts.length)
            
            // 检查是否是降级格式：APPOINTMENT{数字}_{时间戳}
            if (parts.length >= 1 && parts[0]) {
              const firstPartMatch = parts[0].match(/^APPOINTMENT(\d+)$/i)
              if (firstPartMatch && firstPartMatch[1]) {
                const appointmentId = firstPartMatch[1]
                console.log('检测到降级格式Token（通过正则匹配），预约ID:', appointmentId)
                
                // 显示详细的错误提示，并提供应急方案
                ElMessageBox.confirm(
                  `检测到格式不完整的二维码\n\n` +
                  `预约ID: ${appointmentId}\n\n` +
                  `可能原因：\n` +
                  `• 患者小程序生成二维码时网络异常\n` +
                  `• 使用了旧版本的二维码格式\n\n` +
                  `推荐方案：\n` +
                  `让患者在小程序中点击"手动刷新"按钮重新生成二维码\n\n` +
                  `应急方案：\n` +
                  `如果患者无法重新生成二维码，可以尝试通过预约ID生成临时Token`,
                  '二维码格式不完整',
                  {
                    confirmButtonText: '生成临时Token',
                    cancelButtonText: '手动输入Token',
                    type: 'warning'
                  }
                ).then(() => {
                  console.log('用户选择生成临时Token，预约ID:', appointmentId)
                  generateTempTokenForAppointment(appointmentId)
                }).catch(() => {
                  console.log('用户选择手动输入Token')
                  showManualInput()
                })
                return
              }
            }
            
            console.warn('Token格式不正确，无法识别，部分数量:', parts.length, '是否以APPOINTMENT_开头（不区分大小写）:', /^APPOINTMENT_/i.test(cleanedText))
            ElMessage.warning('扫描到的二维码格式不正确，请确保扫描的是患者小程序中的二维码')
          }
        } else {
          console.warn('Token不以APPOINTMENT开头，文本:', cleanedText, '开头字符:', cleanedText.substring(0, 20))
          ElMessage.warning('扫描到的不是有效的预约二维码，请确保扫描的是患者小程序中的二维码。扫描内容：' + (cleanedText.substring(0, 50) || '空'))
        }
      } catch (error) {
        console.error('处理扫描结果失败:', error)
        ElMessage.error('处理扫描结果时出错，请重试')
      }
    }
    
    const qrCodeErrorCallback = (errorMessage) => {
      // 扫描错误（正常情况，持续扫描中）
      // 不显示错误，因为这是正常的扫描过程
      // 只在调试时输出
      if (errorMessage && !errorMessage.includes('NotFoundException')) {
        // 忽略常见的"未找到二维码"错误，这是正常的扫描过程
      }
    }
    
    // 检查 Html5Qrcode 是否可用
    if (typeof Html5Qrcode === 'undefined') {
      throw new Error('Html5Qrcode 库未正确加载')
    }
    
    html5QrCode = new Html5Qrcode('qr-reader')
    
    await html5QrCode.start(
      { facingMode: 'environment' }, // 使用后置摄像头
      {
        fps: 10, // 扫描帧率
        qrbox: { width: 250, height: 250 }, // 扫描框大小
        aspectRatio: 1.0
      },
      qrCodeSuccessCallback,
      qrCodeErrorCallback
    )
    
  } catch (error) {
    console.error('启动扫描失败:', error)
    scanError.value = '无法启动摄像头，请检查浏览器权限设置'
    isScanning.value = false
    
    // 如果摄像头权限被拒绝，提供手动输入选项
    if (error.name === 'NotAllowedError') {
      ElMessage.warning('摄像头权限被拒绝，请允许浏览器访问摄像头，或使用手动输入方式')
    } else if (error.name === 'NotFoundError') {
      ElMessage.warning('未找到摄像头设备，请使用手动输入方式')
    } else if (error.message && error.message.includes('Html5Qrcode')) {
      ElMessage.error('二维码扫描库加载失败，请刷新页面重试')
    } else {
      ElMessage.warning('启动摄像头失败：' + (error.message || '未知错误'))
    }
  }
}

const stopScanning = () => {
  try {
    if (html5QrCode) {
      // 使用更安全的方式停止扫描器
      html5QrCode.stop().then(() => {
        try {
          html5QrCode.clear()
        } catch (clearError) {
          // 忽略清理错误
        }
        html5QrCode = null
        isScanning.value = false
      }).catch((error) => {
        // 如果扫描器未运行或已暂停，忽略这个错误
        const errorMessage = error.message || error.toString()
        if (errorMessage.includes('not running') || 
            errorMessage.includes('not paused') ||
            errorMessage.includes('Cannot stop')) {
          // 扫描器未运行，直接清理状态即可
          try {
            html5QrCode.clear()
          } catch (clearError) {
            // 忽略清理错误
          }
        } else {
          // 其他错误才记录
          console.warn('停止扫描时出现错误:', error)
        }
        html5QrCode = null
        isScanning.value = false
      })
    } else {
      isScanning.value = false
    }
  } catch (error) {
    // 捕获同步错误，直接清理状态
    const errorMessage = error.message || error.toString()
    if (!errorMessage.includes('not running') && 
        !errorMessage.includes('not paused') &&
        !errorMessage.includes('Cannot stop')) {
      console.warn('stopScanning 函数执行错误:', error)
    }
    html5QrCode = null
    isScanning.value = false
  }
}

const handleDialogClose = () => {
  stopScanning()
}

const showManualInput = () => {
  try {
    showScanner.value = false
    stopScanning()
    ElMessageBox.prompt('请输入患者二维码Token', '手动输入Token', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: 'APPOINTMENT_123_...'
    }).then(({ value }) => {
      if (value) {
        handleCheckIn(value)
      }
    }).catch(() => {
      // 用户取消输入，不做任何操作
    })
  } catch (error) {
    console.error('显示手动输入对话框失败:', error)
    ElMessage.error('打开输入对话框失败，请重试')
  }
}

// 组件卸载时清理
onUnmounted(() => {
  stopScanning()
})

const handleCheckIn = async (qrToken) => {
  try {
    // 清理Token：去除首尾空格和换行符，统一处理（不区分大小写）
    const cleanedToken = qrToken ? qrToken.trim().replace(/\r?\n/g, '') : ''
    console.log('处理签到，原始Token:', qrToken)
    console.log('清理后的Token:', cleanedToken)
    console.log('Token格式:', {
      length: cleanedToken.length,
      startsWithAPPOINTMENT: /^APPOINTMENT/i.test(cleanedToken),
      startsWithAPPOINTMENT_: /^APPOINTMENT_/i.test(cleanedToken),
      parts: cleanedToken.split('_'),
      partsLength: cleanedToken.split('_').length
    })
    
    // 不区分大小写验证Token格式
    if (!cleanedToken || !/^APPOINTMENT_/i.test(cleanedToken)) {
      console.error('Token格式验证失败:', cleanedToken)
      ElMessage.error('无效的二维码Token格式: ' + cleanedToken.substring(0, 50))
      return
    }
    
    console.log('准备发送签到请求，Token:', cleanedToken)
    const response = await checkInAppointment({ qrToken: cleanedToken })
    console.log('[签到] 完整响应对象:', response)
    console.log('[签到] 响应类型:', typeof response)
    console.log('[签到] 响应键:', Object.keys(response || {}))
    
    // 后端直接返回CheckInResponse对象，没有包装成{code, data}格式
    // 判断成功：响应对象存在且有appointmentId字段
    if (response && (response.appointmentId || response.code === '200')) {
      // 兼容两种响应格式：
      // 1. 直接返回CheckInResponse对象：{ appointmentId, patientName, ... }
      // 2. 统一响应格式：{ code: '200', data: { appointmentId, ... } }
      const checkInData = response.data || response
      
      console.log('[签到] 签到成功，数据:', checkInData)
      checkInResult.value = { 
        success: true, 
        message: '签到成功', 
        data: checkInData
      }
      ElMessage.success('签到成功')
      
      // 如果已选择排班，自动刷新队列；如果未选择，尝试根据签到信息选择排班
      if (selectedScheduleId.value) {
        loadCallQueue()
      } else if (checkInData.scheduleId) {
        selectedScheduleId.value = checkInData.scheduleId
        loadCallQueue()
      }
      
      // 5秒后清空结果
      setTimeout(() => { 
        checkInResult.value = null 
      }, 5000)
    } else {
      const errorMsg = response?.msg || response?.message || '签到失败'
      console.error('[签到] 签到失败，响应:', response)
      checkInResult.value = { 
        success: false, 
        message: errorMsg
      }
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || error.message || '签到失败，请重试'
    
    // 检查是否是"已签到"错误，提供清除选项
    if (errorMsg.includes('已签到') || errorMsg.includes('重复操作')) {
      checkInResult.value = { 
        success: false, 
        message: errorMsg,
        canClear: true,
        qrToken: qrToken
      }
      
      // 显示清除选项
      ElMessageBox.confirm(
        errorMsg + '\n\n是否清除之前的签到记录？',
        '签到失败',
        {
          confirmButtonText: '清除签到记录',
          cancelButtonText: '取消',
          type: 'warning',
        }
      ).then(() => {
        handleClearCheckIn(qrToken)
      }).catch(() => {})
    } else {
      checkInResult.value = { 
        success: false, 
        message: errorMsg 
      }
      ElMessage.error(errorMsg)
    }
  }
}

const handleClearCheckIn = async (qrToken) => {
  try {
    // 从Token中解析预约ID
    // Token格式：APPOINTMENT_{appointmentId}_{timestamp}_{random}
    const tokenParts = qrToken.split('_')
    if (tokenParts.length >= 2 && tokenParts[0] === 'APPOINTMENT') {
      const appointmentId = parseInt(tokenParts[1])
      if (!isNaN(appointmentId)) {
        await clearCheckIn(appointmentId)
        ElMessage.success('签到记录已清除，请重新扫描二维码签到')
        checkInResult.value = null
      } else {
        ElMessage.error('无法解析预约ID')
      }
    } else {
      // 尝试从错误信息中提取预约ID
      const errorMsg = checkInResult.value?.message || ''
      const idMatch = errorMsg.match(/预约ID[：:]\s*(\d+)/)
      if (idMatch && idMatch[1]) {
        const appointmentId = parseInt(idMatch[1])
        await clearCheckIn(appointmentId)
        ElMessage.success('签到记录已清除，请重新扫描二维码签到')
        checkInResult.value = null
      } else {
        ElMessage.error('无法获取预约ID，请手动清除签到记录')
      }
    }
  } catch (clearError) {
    const clearErrorMsg = clearError.response?.data?.message || clearError.message || '清除失败'
    ElMessage.error('清除签到记录失败：' + clearErrorMsg)
  }
}

const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const showTokenHelp = () => {
  showHelp.value = !showHelp.value
}

const generateTempTokenForAppointment = async (appointmentId) => {
  if (!appointmentId) {
    ElMessage.warning('预约ID不能为空')
    return
  }
  
  generatingToken.value = true
  try {
    console.log('开始生成临时Token，预约ID:', appointmentId)
    const response = await getAppointmentQrCode(appointmentId)
    console.log('生成Token响应:', response)
    
    if (response && (response.code === '200' || response.qrToken)) {
      const token = response.qrToken || response.data?.qrToken
      console.log('提取到的Token:', token)
      
      if (token) {
        console.log('Token格式验证:', {
          token: token,
          startsWithAPPOINTMENT: token.startsWith('APPOINTMENT'),
          parts: token.split('_'),
          partsLength: token.split('_').length
        })
        
        ElMessage.success('临时Token生成成功，正在尝试签到...')
        // 延迟一小段时间，确保Redis写入完成
        setTimeout(() => {
          // 使用生成的Token进行签到
          handleCheckIn(token)
        }, 100)
      } else {
        console.error('未获取到Token，响应:', response)
        ElMessage.error('未获取到Token，请检查预约ID是否正确')
      }
    } else {
      console.error('生成Token失败，响应:', response)
      ElMessage.error(response?.msg || '生成Token失败')
    }
  } catch (error) {
    console.error('生成Token异常:', error)
    const errorMsg = error.response?.data?.message || error.message || '生成Token失败'
    ElMessage.error(errorMsg)
  } finally {
    generatingToken.value = false
  }
}

const generateTestToken = async () => {
  if (!testAppointmentId.value) {
    ElMessage.warning('请输入预约ID')
    return
  }
  
  generatingToken.value = true
  try {
    const response = await getAppointmentQrCode(testAppointmentId.value)
    if (response && (response.code === '200' || response.qrToken)) {
      const token = response.qrToken || response.data?.qrToken
      if (token) {
        testToken.value = token
        ElMessage.success('Token生成成功！')
      } else {
        ElMessage.error('未获取到Token，请检查预约ID是否正确')
      }
    } else {
      ElMessage.error(response.msg || '生成Token失败')
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || error.message || '生成Token失败'
    ElMessage.error(errorMsg)
  } finally {
    generatingToken.value = false
  }
}

const copyToken = () => {
  if (testToken.value) {
    navigator.clipboard.writeText(testToken.value).then(() => {
      ElMessage.success('Token已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('复制失败，请手动复制')
    })
  }
}

// 加载排班列表
const loadScheduleList = async () => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const response = await getAllSchedules(0, 1000)
    if (response && response.content) {
      // 只显示今天及以后的排班
      scheduleList.value = response.content
        .filter(schedule => schedule.scheduleDate >= today)
        .map(schedule => ({
          scheduleId: schedule.scheduleId,
          doctorName: schedule.doctorName || '未知医生',
          scheduleDate: schedule.scheduleDate,
          slotName: schedule.slotName || '未知时段',
          departmentName: schedule.departmentName || ''
        }))
        .sort((a, b) => {
          // 按日期和时间排序
          if (a.scheduleDate !== b.scheduleDate) {
            return a.scheduleDate.localeCompare(b.scheduleDate)
          }
          return a.slotName.localeCompare(b.slotName)
        })
    }
  } catch (error) {
    console.error('加载排班列表失败:', error)
    ElMessage.error('加载排班列表失败：' + (error.response?.data?.message || error.message))
  }
}

// 加载叫号队列
const loadCallQueue = async () => {
  if (!selectedScheduleId.value) {
    callQueue.value = []
    nextToCall.value = null
    return
  }
  
  loadingQueue.value = true
  try {
    const [queueResponse, nextResponse] = await Promise.all([
      getCallQueue(selectedScheduleId.value),
      getNextAppointmentToCall(selectedScheduleId.value).catch(() => null)
    ])
    
    // 确保返回的是数组
    if (Array.isArray(queueResponse)) {
      callQueue.value = queueResponse
    } else if (queueResponse && Array.isArray(queueResponse.data)) {
      callQueue.value = queueResponse.data
    } else if (queueResponse && Array.isArray(queueResponse.content)) {
      callQueue.value = queueResponse.content
    } else {
      console.warn('叫号队列数据格式异常:', queueResponse)
      callQueue.value = []
    }
    
    // 处理下一个叫号
    if (nextResponse && typeof nextResponse === 'object') {
      nextToCall.value = nextResponse.data || nextResponse
    } else {
      nextToCall.value = nextResponse || null
    }
  } catch (error) {
    console.error('加载叫号队列失败:', error)
    ElMessage.error('加载叫号队列失败：' + (error.response?.data?.message || error.message))
    callQueue.value = []
    nextToCall.value = null
  } finally {
    loadingQueue.value = false
  }
}

// 执行叫号
const handleCall = async (appointmentId) => {
  callingId.value = appointmentId
  try {
    await callAppointment(appointmentId)
    ElMessage.success('叫号成功')
    // 刷新队列
    await loadCallQueue()
    // 如果签到成功，也刷新队列
    if (checkInResult.value?.success) {
      setTimeout(() => {
        loadCallQueue()
      }, 500)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || error.message || '叫号失败'
    ElMessage.error(errorMsg)
  } finally {
    callingId.value = null
  }
}

// 标记过号（状态改回scheduled，患者可重新扫码）
const handleMarkMissedCall = async (appointmentId) => {
  markingId.value = appointmentId
  try {
    await ElMessageBox.confirm(
      '确认该患者已过号？系统将清除签到记录，患者可重新扫码签到。',
      '标记过号',
      {
        confirmButtonText: '确认过号',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await markMissedCall(appointmentId)
    ElMessage.success('已标记过号，签到记录已清除，患者可重新扫码签到')
    // 刷新队列
    await loadCallQueue()
  } catch (error) {
    if (error !== 'cancel') {
      const errorMsg = error.response?.data?.message || error.message || '标记过号失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    markingId.value = null
  }
}

// 就诊完成处理
const handleCompleteAppointment = async (appointmentId) => {
  completingId.value = appointmentId
  try {
    await ElMessageBox.confirm(
      '确认该患者就诊已完成？系统将自动叫号下一位患者。',
      '就诊完成',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await completeAppointment(appointmentId)
    ElMessage.success('就诊完成，已自动叫号下一位患者')
    // 刷新队列
    await loadCallQueue()
  } catch (error) {
    if (error !== 'cancel') {
      const errorMsg = error.response?.data?.message || error.message || '标记就诊完成失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    completingId.value = null
  }
}

// 组件挂载时加载排班列表
onMounted(() => {
  loadScheduleList()
  // 如果签到成功，自动刷新队列
  if (checkInResult.value?.success && checkInResult.value?.data?.scheduleId) {
    selectedScheduleId.value = checkInResult.value.data.scheduleId
    loadCallQueue()
  }
})
</script>

<style scoped>
.check-in-container {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.check-in-result {
  margin-top: 20px;
}

.patient-info {
  margin-top: 20px;
}

.tip-info {
  text-align: center;
  margin-top: 50px;
}

.scanner-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.scanner-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.scanner-placeholder p {
  margin-top: 16px;
  font-size: 14px;
}

#qr-reader {
  width: 100%;
  max-width: 500px;
  margin: 20px 0;
}

.scan-error {
  width: 100%;
  margin-top: 10px;
}

.scanner-tips {
  margin-top: 20px;
  text-align: center;
  color: #606266;
  font-size: 14px;
}

.scanner-tips p {
  margin: 5px 0;
}
</style>

