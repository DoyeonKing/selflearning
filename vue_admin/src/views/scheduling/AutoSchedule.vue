<template>
  <div class="auto-schedule-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><MagicStick /></el-icon> 自动排班配置</span>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
        <!-- 基础配置 -->
        <el-form-item label="选择科室" required>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-select 
                v-model="selectedParentDept" 
                placeholder="请先选择一级科室"
                @change="handleParentDeptChange"
                clearable
                filterable
                style="width: 100%">
                <el-option
                  v-for="parent in departmentTree"
                  :key="parent.id"
                  :label="parent.name"
                  :value="parent.id">
                  <span>{{ parent.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 12px;">{{ parent.children?.length || 0 }}个子科室</span>
                </el-option>
              </el-select>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="departmentId" style="margin-bottom: 0;">
                <el-select 
                  v-model="form.departmentId" 
                  placeholder="请选择具体科室"
                  :disabled="!selectedParentDept || availableSubDepts.length === 0"
                  clearable
                  filterable
                  style="width: 100%">
                  <el-option
                    v-for="sub in availableSubDepts"
                    :key="sub.id"
                    :label="sub.name"
                    :value="sub.id">
                    <span>{{ sub.name }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <!-- 显示当前选择 -->
          <div v-if="selectedDepartmentInfo" style="font-size: 12px; color: #409EFF; margin-top: 8px;">
            ✓ 已选择: {{ selectedDepartmentInfo }}
          </div>
        </el-form-item>
        
        <el-form-item label="排班时间范围" required>
          <el-col :span="11">
            <el-form-item prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="开始日期"
                style="width: 100%"
                :disabled-date="disabledStartDate"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="2" class="text-center">至</el-col>
          <el-col :span="11">
            <el-form-item prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="结束日期"
                style="width: 100%"
                :disabled-date="disabledEndDate"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-form-item>
        
        <!-- 高级规则配置 -->
        <el-divider content-position="left">排班规则</el-divider>
        
        <el-form-item label="每时段医生数">
          <el-col :span="11">
            <el-input-number
              v-model="form.rules.minDoctorsPerSlot"
              :min="1"
              :max="10"
              placeholder="最少"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="2" class="text-center">~</el-col>
          <el-col :span="11">
            <el-input-number
              v-model="form.rules.maxDoctorsPerSlot"
              :min="1"
              :max="20"
              placeholder="最多"
              style="width: 100%"
            />
          </el-col>
        </el-form-item>
        
        <el-form-item label="默认号源数">
          <el-input-number
            v-model="form.rules.defaultTotalSlots"
            :min="1"
            :max="100"
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="默认挂号费">
          <el-input-number
            v-model="form.rules.defaultFee"
            :min="0"
            :max="1000"
            :precision="2"
            :step="0.5"
            style="width: 200px"
          />
          <span class="ml-2">元</span>
        </el-form-item>
        
        <el-form-item label="连续工作限制">
          <el-input-number
            v-model="form.rules.consecutiveWorkDaysLimit"
            :min="1"
            :max="10"
            style="width: 200px"
          />
          <span class="ml-2">天</span>
        </el-form-item>
        
        <el-form-item label="工作量均衡">
          <el-switch v-model="form.rules.balanceWorkload" />
        </el-form-item>
        
        <el-form-item label="严格模式">
          <el-switch v-model="form.rules.strictMode" />
          <span class="ml-2 text-info">
            注意：开启后会严格遵守连续工作限制，可能导致部分时段无人排班
          </span>
        </el-form-item>
        
        <el-form-item label="覆盖已有排班">
          <el-switch v-model="form.overwriteExisting" />
          <span class="ml-2 text-warning">
            注意：开启后会覆盖选定时间范围内的已有排班
          </span>
        </el-form-item>
        
        <!-- 操作按钮 -->
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handlePreview"
            :loading="previewLoading"
          >
            <el-icon><View /></el-icon> 预览排班
          </el-button>
          <el-button 
            type="success" 
            @click="handleGenerate"
            :loading="generateLoading"
          >
            <el-icon><Check /></el-icon> 生成并保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 结果展示 -->
    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon v-if="result.success" color="green" size="20"><SuccessFilled /></el-icon>
            <el-icon v-else color="red" size="20"><CircleCloseFilled /></el-icon>
            <span class="ml-2">排班结果</span>
          </span>
        </div>
      </template>
      
      <!-- 统计概览 -->
      <div class="statistics-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总排班数" :value="result.statistics?.totalSchedules || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="覆盖天数" :value="result.statistics?.coveredDays || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic 
              title="排班覆盖率" 
              :value="((result.statistics?.coverageRate || 0) * 100).toFixed(1)" 
              suffix="%"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic title="参与医生" :value="result.statistics?.doctorsInvolved || 0" />
          </el-col>
        </el-row>
      </div>
      
      <!-- 工作量分布图表 -->
      <el-divider content-position="left">工作量分布</el-divider>
      <div id="workloadChart" style="height: 300px"></div>
      
      <!-- 冲突列表 -->
      <div v-if="result.conflicts && result.conflicts.length > 0">
        <el-divider content-position="left">
          冲突列表 ({{ result.conflicts.length }})
        </el-divider>
        <el-table 
          :data="result.conflicts"
          border
          stripe
        >
          <el-table-column prop="type" label="类型" width="150">
            <template #default="{ row }">
              <el-tag type="danger">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="doctorName" label="医生" width="100" />
          <el-table-column prop="conflictDate" label="日期" width="120" />
          <el-table-column prop="timeSlot" label="时段" width="150" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="suggestion" label="建议" />
        </el-table>
      </div>
      
      <!-- 未分配时间段 -->
      <div v-if="result.unassignedSlots && result.unassignedSlots.length > 0">
        <el-divider content-position="left">
          未分配时间段 ({{ result.unassignedSlots.length }})
        </el-divider>
        <el-table 
          :data="result.unassignedSlots"
          border
          stripe
        >
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="slotName" label="时段" width="180" />
          <el-table-column prop="reason" label="原因" />
        </el-table>
      </div>
      
      <!-- 警告信息 -->
      <el-alert
        v-if="result.warnings && result.warnings.length > 0"
        type="warning"
        :closable="false"
        class="mt-4"
      >
        <template #title>
          <div v-for="(warning, index) in result.warnings" :key="index">
            {{ warning }}
          </div>
        </template>
      </el-alert>
      
      <!-- 排班预览表 -->
      <div v-if="result.schedulePreview && result.schedulePreview.length > 0" class="mt-4">
        <el-divider content-position="left">
          排班预览表
        </el-divider>
        
        <!-- 颜色说明 -->
        <el-alert type="info" :closable="false" class="mb-3">
          <template #title>
            <div class="flex items-center gap-4">
              <span>说明：</span>
              <el-tag type="success" size="small">绿色：正常工作</el-tag>
              <el-tag type="danger" size="small">红色：连续工作≥{{ form.rules.consecutiveWorkDaysLimit }}天（已达上限）</el-tag>
            </div>
          </template>
        </el-alert>
        
        <el-table 
          :data="result.schedulePreview"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="dayOfWeekName" label="星期" width="80" />
           <el-table-column label="门诊时段" width="180">
             <template #default="{ row }">
               <div v-if="row.slots && row.slots.length > 0">
                 <div v-for="slot in row.slots" :key="slot.slotId" class="mb-1">
                   <el-tag 
                     :type="slot.consecutiveDays >= form.rules.consecutiveWorkDaysLimit ? 'danger' : 'success'"
                     size="small"
                   >
                     {{ slot.slotName }}
                   </el-tag>
                 </div>
               </div>
               <el-text v-else type="info">-</el-text>
             </template>
           </el-table-column>
           
           <el-table-column label="医生" width="120">
             <template #default="{ row }">
               <div v-if="row.slots && row.slots.length > 0">
                 <div v-for="slot in row.slots" :key="slot.slotId" class="mb-1">
                   {{ slot.doctorName }}
                 </div>
               </div>
               <el-text v-else type="info">-</el-text>
             </template>
           </el-table-column>
           
           <el-table-column label="诊室" width="150">
             <template #default="{ row }">
               <div v-if="row.slots && row.slots.length > 0">
                 <div v-for="slot in row.slots" :key="slot.slotId" class="mb-1">
                   {{ slot.locationName }}
                 </div>
               </div>
               <el-text v-else type="info">-</el-text>
             </template>
           </el-table-column>
           
           <el-table-column label="连续工作" width="120">
             <template #default="{ row }">
               <div v-if="row.slots && row.slots.length > 0">
                 <div v-for="slot in row.slots" :key="slot.slotId" class="mb-1">
                   <el-text 
                     :type="slot.consecutiveDays >= form.rules.consecutiveWorkDaysLimit ? 'danger' : 'success'"
                     size="small"
                   >
                     {{ slot.consecutiveDays }}天
                   </el-text>
                 </div>
               </div>
               <el-text v-else type="info">-</el-text>
             </template>
           </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SuccessFilled, CircleCloseFilled, MagicStick, View, Check } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { autoGenerateSchedule, previewAutoSchedule } from '@/api/schedule'
import { getDepartmentTree } from '@/api/department'

// 数据定义
const router = useRouter()
const formRef = ref()
const departmentTree = ref([])
const result = ref(null)
const previewLoading = ref(false)
const generateLoading = ref(false)

// 🆕 科室选择相关
const selectedParentDept = ref(null)

const form = reactive({
  departmentId: null,
  startDate: '',
  endDate: '',
  overwriteExisting: false,
  rules: {
    minDoctorsPerSlot: 1,
    maxDoctorsPerSlot: 3,
    maxShiftsPerDoctor: 999,
    defaultTotalSlots: 20,
    defaultFee: 5.0,
    consecutiveWorkDaysLimit: 6,
    minRestDays: 1,
    balanceWorkload: true,
    considerPreferences: false,
    strictMode: false
  }
})

const rules = {
  departmentId: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' }
  ]
}

// 🆕 可用的子科室列表（根据选中的父科室）
const availableSubDepts = computed(() => {
  if (!selectedParentDept.value) return []
  const parent = departmentTree.value.find(p => p.id === selectedParentDept.value)
  return parent?.children || []
})

// 🆕 显示选中的科室信息
const selectedDepartmentInfo = computed(() => {
  if (!form.departmentId || !selectedParentDept.value) return ''
  
  const parent = departmentTree.value.find(p => p.id === selectedParentDept.value)
  const sub = availableSubDepts.value.find(s => s.id === form.departmentId)
  
  if (parent && sub) {
    return `${parent.name} → ${sub.name}`
  }
  return ''
})

// 🆕 父科室变化处理
const handleParentDeptChange = (parentId) => {
  console.log('父科室变化:', parentId)
  // 清空子科室选择
  form.departmentId = null
  
  // 如果选中的父科室没有子科室，自动将父科室ID设为departmentId
  if (parentId) {
    const parent = departmentTree.value.find(p => p.id === parentId)
    if (parent && (!parent.children || parent.children.length === 0)) {
      form.departmentId = parentId
      ElMessage.info(`${parent.name} 没有子科室，已自动选择`)
    }
  }
}

// 日期禁用逻辑
const disabledStartDate = (time) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const disabledEndDate = (time) => {
  if (!form.startDate) return false
  const startTime = new Date(form.startDate).getTime()
  const maxTime = startTime + 90 * 24 * 60 * 60 * 1000 // 最多90天
  return time.getTime() < startTime || time.getTime() > maxTime
}

// 加载科室列表（树形结构）
const loadDepartments = async () => {
  try {
    const res = await getDepartmentTree()
    console.log('科室树原始响应:', res)
    console.log('科室树数据类型:', typeof res)
    
    // axios拦截器已经返回了response.data，所以res就是数据本身
    const data = res
    console.log('科室树数据:', data)
    
    // 检查数据格式
    if (Array.isArray(data)) {
      departmentTree.value = data
      console.log('✅ 科室树已加载，数量:', data.length)
      if (data.length > 0) {
        console.log('第一个科室示例:', data[0])
      }
    } else {
      console.error('❌ 科室数据格式错误，期望数组但得到:', typeof data, data)
      ElMessage.error('科室数据格式错误')
    }
  } catch (error) {
    console.error('❌ 加载科室列表失败:', error)
    ElMessage.error('加载科室列表失败: ' + (error.message || error))
  }
}

// 预览排班
const handlePreview = async () => {
  try {
    await formRef.value.validate()
    previewLoading.value = true
    
    console.log('发送预览请求，参数:', form)
    const res = await previewAutoSchedule(form)
    console.log('预览响应:', res)
    
    // axios拦截器已经返回了response.data，所以res就是数据本身
    result.value = res
    
    if (res.success) {
      ElMessage.success('预览生成成功')
      // 等待DOM更新后渲染图表
      await nextTick()
      renderWorkloadChart(res.workloadDistribution)
    } else {
      ElMessage.warning(res.message)
    }
  } catch (error) {
    if (error !== false) {  // 排除表单验证失败
      console.error('预览失败:', error)
      ElMessage.error('预览失败：' + (error.message || error))
    }
  } finally {
    previewLoading.value = false
  }
}

// 生成排班
const handleGenerate = async () => {
  try {
    await formRef.value.validate()
    
    await ElMessageBox.confirm(
      '确认生成排班？此操作将直接保存到数据库。',
      '确认',
      { type: 'warning' }
    )
    
    generateLoading.value = true
    console.log('发送生成请求，参数:', form)
    const res = await autoGenerateSchedule(form)
    console.log('生成响应:', res)
    
    // axios拦截器已经返回了response.data，所以res就是数据本身
    result.value = res
    
    if (res.success) {
      ElMessage.success(`排班生成成功！共生成${res.statistics.totalSchedules}条排班记录`)
      await nextTick()
      renderWorkloadChart(res.workloadDistribution)
      
      // 🔥 提示用户并跳转到排班管理页面
      await ElMessageBox.confirm(
        '排班已保存！是否立即查看排班表？',
        '提示',
        {
          confirmButtonText: '查看排班表',
          cancelButtonText: '留在此页',
          type: 'success'
        }
      ).then(() => {
        // 跳转到排班管理页面
        router.push('/scheduling/dashboard')
      }).catch(() => {
        // 用户选择留在当前页面
        console.log('用户选择留在自动排班页面')
      })
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== false) {
      console.error('生成失败:', error)
      ElMessage.error('生成失败：' + (error.message || error))
    }
  } finally {
    generateLoading.value = false
  }
}

// 渲染工作量分布图表
const renderWorkloadChart = (workloadDistribution) => {
  const chartDom = document.getElementById('workloadChart')
  if (!chartDom) return
  
  const chart = echarts.init(chartDom)
  
  const doctors = Object.values(workloadDistribution || {})
  if (doctors.length === 0) {
    chart.clear()
    return
  }
  
  const option = {
    title: { 
      text: '医生工作量对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: doctors.map(d => d.doctorName),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '班次数'
    },
    series: [
      {
        name: '工作量',
        type: 'bar',
        data: doctors.map(d => d.totalShifts),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 1, color: '#188df0' }
          ])
        },
        label: {
          show: true,
          position: 'top'
        }
      }
    ]
  }
  
  chart.setOption(option)
}

// 重置表单
const handleReset = () => {
  formRef.value.resetFields()
  result.value = null
  selectedParentDept.value = null  // 🆕 清空父科室选择
  form.rules = {
    minDoctorsPerSlot: 1,
    maxDoctorsPerSlot: 3,
    maxShiftsPerDoctor: 999,
    defaultTotalSlots: 20,
    defaultFee: 5.0,
    consecutiveWorkDaysLimit: 6,
    minRestDays: 1,
    balanceWorkload: true,
    considerPreferences: false,
    strictMode: false
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.auto-schedule-container {
  padding: 20px;
}

.form-card, .result-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.statistics-section {
  margin-bottom: 30px;
}

.text-center {
  text-align: center;
  line-height: 32px;
}

.text-warning {
  color: #E6A23C;
  font-size: 12px;
}

.ml-2 {
  margin-left: 8px;
}

.mt-4 {
  margin-top: 16px;
}

:deep(.el-statistic__head) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.mb-1 {
  margin-bottom: 4px;
}

.mb-3 {
  margin-bottom: 12px;
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.gap-4 {
  gap: 16px;
}
</style>

