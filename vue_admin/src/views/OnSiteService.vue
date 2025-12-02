<template>
  <div class="on-site-service-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>现场服务</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 现场挂号 -->
        <el-tab-pane label="现场挂号" name="walkIn">
          <el-form :model="walkInForm" :rules="walkInRules" ref="walkInFormRef" label-width="120px">
            <!-- 步骤1：选择患者 -->
            <el-form-item label="患者" prop="patientId">
              <el-select
                v-model="walkInForm.patientId"
                placeholder="请输入患者学号/工号搜索"
                filterable
                remote
                :remote-method="searchPatientsHandler"
                :loading="patientSearchLoading"
                style="width: 100%"
                clearable
                @change="handlePatientChange"
              >
                <el-option
                  v-for="patient in patientList"
                  :key="patient.patientId"
                  :label="`${patient.name} (${patient.id})`"
                  :value="patient.patientId"
                >
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ patient.name }}</span>
                    <span style="color: #8492a6; font-size: 12px;">{{ patient.id }}</span>
                  </div>
                </el-option>
              </el-select>
              <div style="margin-top: 8px; font-size: 12px; color: #909399;">
                提示：请输入学号/工号进行搜索
              </div>
            </el-form-item>

            <!-- 步骤2和3：选择科室和排班（并排显示） -->
            <el-form-item label="科室和排班" prop="scheduleId" v-if="walkInForm.patientId">
              <div class="department-schedule-container">
                <!-- 左侧：科室选择 -->
                <div class="department-section">
                  <div class="section-title">选择科室</div>
                  <div class="department-selector">
                    <div v-if="loadingDepartments" class="loading-container">
                      <el-icon class="is-loading"><Loading /></el-icon>
                      <span>加载科室数据中...</span>
                    </div>
                    <template v-else>
                      <!-- 左侧：父科室菜单 -->
                      <el-menu 
                        :default-active="selectedParentId" 
                        class="department-menu" 
                        @select="handleParentSelect"
                      >
                        <el-menu-item 
                          v-for="parent in parentDepartments" 
                          :key="parent.parentDepartmentId" 
                          :index="String(parent.parentDepartmentId)"
                        >
                          <span>{{ parent.name }}</span>
                        </el-menu-item>
                      </el-menu>

                      <!-- 右侧：子科室面板 -->
                      <div class="sub-department-panel" v-if="currentSubDepartments.length > 0">
                        <div 
                          v-for="sub in currentSubDepartments" 
                          :key="sub.departmentId" 
                          class="sub-department-item" 
                          :class="{ 'active': walkInForm.departmentId === sub.departmentId }" 
                          @click="selectSubDepartment(sub.departmentId)"
                        >
                          {{ sub.name }}
                        </div>
                      </div>
                      <div v-else-if="selectedParentId && currentSubDepartments.length === 0" class="no-sub-departments">
                        <el-empty description="该科室暂无子科室" :image-size="60"/>
                      </div>
                    </template>
                  </div>
                </div>

                <!-- 右侧：排班选择 -->
                <div class="schedule-section" v-if="walkInForm.departmentId">
                  <div class="section-title">选择排班</div>
                  <div class="schedule-selector">
                    <!-- 日期筛选 -->
                    <div class="date-filter">
                      <el-button 
                        :type="selectedDate === 'all' ? 'primary' : ''" 
                        size="small"
                        @click="selectDate('all')"
                      >
                        全部
                      </el-button>
                      <el-button 
                        v-for="date in dateOptions" 
                        :key="date.value"
                        :type="selectedDate === date.value ? 'primary' : ''" 
                        size="small"
                        @click="selectDate(date.value)"
                      >
                        {{ date.label }}
                      </el-button>
                    </div>

                    <!-- 医生和排班选择区域 -->
                    <div class="doctor-schedule-container" v-loading="scheduleLoading">
                      <!-- 左侧：医生列表 -->
                      <div class="doctor-list" v-if="doctorList.length > 0">
                        <div
                          v-for="doctor in doctorList"
                          :key="doctor.doctorId"
                          class="doctor-item"
                          :class="{ active: selectedDoctorId === doctor.doctorId }"
                          @click="selectDoctor(doctor.doctorId)"
                        >
                          <el-avatar :size="40" :src="doctor.photoUrl" class="doctor-avatar">
                            <el-icon><User /></el-icon>
                          </el-avatar>
                          <div class="doctor-info">
                            <div class="doctor-name">{{ doctor.doctorName }}</div>
                            <div class="doctor-title">{{ doctor.doctorTitle || '医生' }}</div>
                          </div>
                        </div>
                      </div>
                      <div v-else class="empty-doctor-list">
                        <el-empty description="该科室暂无医生" :image-size="60"/>
                      </div>

                      <!-- 右侧：排班列表 -->
                      <div class="schedule-list" v-if="selectedDoctorId">
                        <div v-if="currentDoctorSchedules.length > 0">
                          <!-- 按日期分组显示 -->
                          <div v-for="(group, date) in groupedCurrentDoctorSchedules" :key="date" class="date-group">
                            <div class="date-header">
                              <el-icon><Calendar /></el-icon>
                              <span>{{ formatDateHeader(date) }}</span>
                            </div>
                            <div class="schedule-cards">
                              <div
                                v-for="schedule in group"
                                :key="schedule.scheduleId"
                                class="schedule-card"
                                :class="{ 
                                  'selected': walkInForm.scheduleId === schedule.scheduleId,
                                  'disabled': schedule.totalSlots <= schedule.bookedSlots
                                }"
                                @click="selectSchedule(schedule)"
                              >
                                <div class="schedule-card-header">
                                  <div class="time-info">
                                    <el-icon><Clock /></el-icon>
                                    <span class="time-value">{{ schedule.slotName }}</span>
                                  </div>
                                  <el-tag 
                                    :type="schedule.totalSlots > schedule.bookedSlots ? 'success' : 'danger'"
                                    size="small"
                                  >
                                    {{ schedule.totalSlots > schedule.bookedSlots ? '可预约' : '已约满' }}
                                  </el-tag>
                                </div>
                                <div class="schedule-card-body">
                                  <div class="schedule-info-item" v-if="schedule.location">
                                    <el-icon><Location /></el-icon>
                                    <span>{{ schedule.location }}</span>
                                  </div>
                                </div>
                                <div class="schedule-card-footer">
                                  <div class="slots-info">
                                    <span class="slots-value">{{ schedule.totalSlots - schedule.bookedSlots }}</span>
                                    <span class="slots-separator">/</span>
                                    <span class="slots-total">{{ schedule.totalSlots }}</span>
                                    <span class="slots-label">号源</span>
                                  </div>
                                  <div class="price-info">
                                    <span class="price-symbol">¥</span>
                                    <span class="price-value">{{ schedule.fee }}</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                        <div v-else class="empty-schedule">
                          <el-empty description="该医生暂无可用排班" :image-size="60"/>
                        </div>
                      </div>
                      <div v-else class="empty-schedule">
                        <el-empty description="请先选择医生" :image-size="60"/>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="schedule-section-placeholder">
                  <el-empty description="请先选择科室" :image-size="60"/>
                </div>
              </div>
            </el-form-item>

            <!-- 已选排班信息显示 -->
            <el-form-item v-if="selectedSchedule">
              <el-alert type="success" :closable="false" show-icon>
                <template #title>
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <span>已选择：</span>
                    <strong>{{ selectedSchedule.doctorName }}</strong>
                    <span>{{ selectedSchedule.scheduleDate }}</span>
                    <span>{{ selectedSchedule.slotName }}</span>
                    <span style="color: #67C23A;">剩余号源：{{ selectedSchedule.totalSlots - selectedSchedule.bookedSlots }}</span>
                    <span style="color: #E6A23C;">挂号费：¥{{ selectedSchedule.fee }}</span>
                  </div>
                </template>
              </el-alert>
            </el-form-item>

            <!-- 支付方式选择 -->
            <el-form-item label="支付方式" prop="paymentMethod" v-if="selectedSchedule">
              <el-radio-group v-model="walkInForm.paymentMethod">
                <el-radio label="cash">现金</el-radio>
                <el-radio label="wechat">微信</el-radio>
                <el-radio label="alipay">支付宝</el-radio>
                <el-radio label="card">刷卡</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 交易流水号 -->
            <el-form-item label="交易流水号" v-if="selectedSchedule && walkInForm.paymentMethod">
              <el-input
                v-model="walkInForm.transactionId"
                placeholder="请输入交易流水号（可选）"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleCreateWalkIn" :loading="walkInLoading">
                创建并支付
              </el-button>
              <el-button @click="resetWalkInForm">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 创建成功后的预约详情 -->
          <el-card v-if="createdAppointment" style="margin-top: 20px;" shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: #67C23A; font-weight: bold;">✓ 挂号成功</span>
                <el-button type="text" @click="createdAppointment = null">关闭</el-button>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="预约号">{{ createdAppointment.appointmentNumber || createdAppointment.appointmentId }}</el-descriptions-item>
              <el-descriptions-item label="排队号">{{ createdAppointment.queueNumber || '-' }}</el-descriptions-item>
              <el-descriptions-item label="患者姓名">{{ createdAppointment.patientName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="医生">{{ createdAppointment.doctorName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="科室">{{ createdAppointment.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="就诊地点">
                <span v-if="createdAppointment.location">
                  <el-icon><Location /></el-icon>
                  {{ createdAppointment.location }}
                </span>
                <span v-else-if="createdAppointment.schedule?.location">
                  <el-icon><Location /></el-icon>
                  {{ createdAppointment.schedule.location }}
                </span>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="就诊日期">{{ createdAppointment.scheduleDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="时段">{{ createdAppointment.slotName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="挂号费">¥{{ createdAppointment.fee || '0.00' }}</el-descriptions-item>
              <el-descriptions-item label="支付状态">
                <el-tag :type="createdAppointment.paymentStatus === 'paid' ? 'success' : 'warning'">
                  {{ createdAppointment.paymentStatus === 'paid' ? '已支付' : '未支付' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="支付方式">{{ getPaymentMethodText(createdAppointment.paymentMethod) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <!-- 退款管理 -->
        <el-tab-pane label="退款管理" name="refund">
          <el-form :inline="true" :model="refundSearchForm" class="search-form">
            <el-form-item label="学号/工号">
              <el-input 
                v-model="refundSearchForm.identifier" 
                placeholder="请输入患者学号/工号" 
                clearable 
                @keyup.enter="searchRefundAppointments"
                style="width: 200px;"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchRefundAppointments" :loading="refundSearchLoading">搜索</el-button>
              <el-button @click="resetRefundSearch">重置</el-button>
            </el-form-item>
          </el-form>
          
          <el-alert
            v-if="refundAppointments.length > 0"
            type="info"
            :closable="false"
            style="margin-top: 10px; margin-bottom: 10px;"
          >
            <template #default>
              显示所有非已完成的预约，已付款且未签到的预约可进行退款操作
            </template>
          </el-alert>

          <el-table :data="refundAppointments" border stripe v-loading="refundSearchLoading" style="margin-top: 20px">
            <el-table-column prop="appointmentId" label="预约ID" width="100" />
            <el-table-column label="患者姓名" width="120">
              <template #default="{ row }">
                {{ row.patient?.fullName || row.patient?.name || row.patientName || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="学号/工号" width="120">
              <template #default="{ row }">
                {{ row.patient?.identifier || row.patient?.id || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="医生" width="120">
              <template #default="{ row }">
                {{ row.schedule?.doctorName || row.doctorName || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="科室" width="120">
              <template #default="{ row }">
                {{ row.schedule?.departmentName || row.departmentName || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="就诊日期" width="120">
              <template #default="{ row }">
                {{ row.schedule?.scheduleDate || row.scheduleDate || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="时段" width="150">
              <template #default="{ row }">
                {{ row.schedule?.slotName || row.slotName || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="费用" width="100">
              <template #default="{ row }">
                ¥{{ row.schedule?.fee || row.fee || '0.00' }}
              </template>
            </el-table-column>
            <el-table-column prop="paymentStatus" label="支付状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getPaymentStatusTagType(row.paymentStatus)">
                  {{ getPaymentStatusText(row.paymentStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="预约状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAppointmentStatusTagType(row)">
                  {{ getAppointmentStatusText(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="canRefund(row)"
                  type="danger"
                  size="small"
                  @click="handleRefund(row)"
                  :loading="refundingAppointmentId === row.appointmentId"
                >
                  退款
                </el-button>
                <span v-else style="color: #909399;">
                  {{ getRefundDisabledReason(row) }}
                </span>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="refundTotal > 0"
            v-model:current-page="refundPage"
            v-model:page-size="refundPageSize"
            :total="refundTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="searchRefundAppointments"
            @current-change="searchRefundAppointments"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Loading, User, Calendar, Clock, Location } from '@element-plus/icons-vue';
import { createWalkInAppointment, refundAppointment, searchAppointments, payForAppointment, getPatientAppointments } from '@/api/appointment';
import { getAllSchedules } from '@/api/schedule';
import { searchPatients } from '@/api/user';
import { getAllParentDepartments, getDepartmentsByParentId, getDoctorsByDepartmentId } from '@/api/department';

const activeTab = ref('walkIn');

// 现场挂号相关
const walkInFormRef = ref(null);
const walkInForm = reactive({
  patientId: null,
  departmentId: null,
  scheduleId: null,
  paymentMethod: 'cash',
  transactionId: ''
});

const walkInRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  scheduleId: [{ required: true, message: '请选择排班', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
};

// 自定义验证：确保选择了科室和排班
const validateDepartmentAndSchedule = (rule, value, callback) => {
  if (!walkInForm.departmentId) {
    callback(new Error('请选择科室'));
  } else if (!walkInForm.scheduleId) {
    callback(new Error('请选择排班'));
  } else {
    callback();
  }
};

const patientList = ref([]);
const patientSearchLoading = ref(false);

// 科室相关
const parentDepartments = ref([]);
const selectedParentId = ref(null);
const subDepartmentsMap = ref({}); // 存储每个父科室的子科室
const loadingDepartments = ref(false);
const scheduleLoading = ref(false);

// 医生和排班相关
const doctorList = ref([]);
const selectedDoctorId = ref(null);
const availableSchedules = ref([]);
const selectedSchedule = ref(null);
const selectedDate = ref('all');
const dateOptions = ref([]);
const walkInLoading = ref(false);
const createdAppointment = ref(null);

// 退款相关
const refundSearchForm = reactive({
  identifier: '' // 学号/工号
});

const refundAppointments = ref([]);
const refundSearchLoading = ref(false);
const refundingAppointmentId = ref(null);
const refundPage = ref(1);
const refundPageSize = ref(20);
const refundTotal = ref(0);

// 当前选中的子科室列表
const currentSubDepartments = computed(() => {
  if (!selectedParentId.value) return [];
  const parentId = Number(selectedParentId.value);
  return subDepartmentsMap.value[parentId] || [];
});

// 当前选中医生的排班（按日期筛选，并过滤已过期的排班）
const currentDoctorSchedules = computed(() => {
  if (!selectedDoctorId.value) return [];
  
  let filtered = availableSchedules.value.filter(s => s.doctorId === selectedDoctorId.value);
  
  // 根据选中的日期筛选
  if (selectedDate.value !== 'all') {
    filtered = filtered.filter(s => s.scheduleDate === selectedDate.value);
  }
  
  // 过滤已过期的排班
  const now = new Date();
  const today = now.toISOString().split('T')[0]; // YYYY-MM-DD
  const currentTime = now.getHours() * 100 + now.getMinutes(); // HHMM格式，便于比较
  
  filtered = filtered.filter(schedule => {
    const scheduleDate = schedule.scheduleDate;
    
    // 如果排班日期在今天之前，过滤掉
    if (scheduleDate < today) {
      return false;
    }
    
    // 如果排班日期是今天，检查结束时间是否已过
    if (scheduleDate === today && schedule.endTime) {
      let endHour = null;
      let endMinute = null;
      
      // 处理不同的 endTime 格式
      if (typeof schedule.endTime === 'string') {
        const timeParts = schedule.endTime.split(':');
        if (timeParts.length >= 2) {
          endHour = parseInt(timeParts[0]);
          endMinute = parseInt(timeParts[1]);
        }
      } else if (schedule.endTime && typeof schedule.endTime === 'object') {
        // 如果是对象格式，尝试提取 hour 和 minute
        endHour = schedule.endTime.hour || schedule.endTime.h;
        endMinute = schedule.endTime.minute || schedule.endTime.m;
      }
      
      if (endHour !== null && endMinute !== null && !isNaN(endHour) && !isNaN(endMinute)) {
        const endTime = endHour * 100 + endMinute;
        // 如果结束时间已过，过滤掉
        if (endTime < currentTime) {
          return false;
        }
      }
    }
    
    return true;
  });
  
  return filtered;
});

// 按日期分组的排班列表
const groupedCurrentDoctorSchedules = computed(() => {
  const groups = {};
  currentDoctorSchedules.value.forEach(schedule => {
    const date = schedule.scheduleDate;
    if (!groups[date]) {
      groups[date] = [];
    }
    groups[date].push(schedule);
  });
  
  // 按日期排序，每个日期内的排班按时间段排序
  const sortedDates = Object.keys(groups).sort();
  const result = {};
  sortedDates.forEach(date => {
    result[date] = groups[date].sort((a, b) => {
      return (a.slotName || '').localeCompare(b.slotName || '');
    });
  });
  
  return result;
});

// 初始化日期选项（未来7天）
const initDateOptions = () => {
  const options = [];
  const today = new Date();
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    const dateStr = date.toISOString().split('T')[0];
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const weekday = weekdays[date.getDay()];
    const month = date.getMonth() + 1;
    const day = date.getDate();
    
    options.push({
      value: dateStr,
      label: i === 0 ? '今天' : `${month}/${day} ${weekday}`
    });
  }
  
  dateOptions.value = options;
};

// 格式化日期标题
const formatDateHeader = (dateStr) => {
  const date = new Date(dateStr);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(today.getDate() + 1);
  
  if (dateStr === today.toISOString().split('T')[0]) {
    return '今天 ' + dateStr;
  } else if (dateStr === tomorrow.toISOString().split('T')[0]) {
    return '明天 ' + dateStr;
  } else {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const weekday = weekdays[date.getDay()];
    return `${date.getMonth() + 1}月${date.getDate()}日 ${weekday}`;
  }
};

// 加载父科室列表
const loadParentDepartments = async () => {
  loadingDepartments.value = true;
  try {
    const response = await getAllParentDepartments();
    if (Array.isArray(response)) {
      parentDepartments.value = response;
    } else if (response && response.content) {
      parentDepartments.value = response.content;
    } else if (response && response.data) {
      parentDepartments.value = Array.isArray(response.data) ? response.data : response.data.content || [];
    }
    
    // 默认选中第一个父科室
    if (parentDepartments.value.length > 0) {
      selectedParentId.value = String(parentDepartments.value[0].parentDepartmentId);
      loadSubDepartments(Number(selectedParentId.value));
    }
  } catch (error) {
    console.error('加载父科室列表失败:', error);
    ElMessage.error('加载科室列表失败');
  } finally {
    loadingDepartments.value = false;
  }
};

// 加载子科室列表
const loadSubDepartments = async (parentId) => {
  if (subDepartmentsMap.value[parentId]) {
    return; // 已加载过，直接返回
  }
  
  try {
    const response = await getDepartmentsByParentId(parentId);
    let subDepts = [];
    
    if (Array.isArray(response)) {
      subDepts = response;
    } else if (response && response.content) {
      subDepts = response.content;
    } else if (response && response.data) {
      subDepts = Array.isArray(response.data) ? response.data : response.data.content || [];
    }
    
    subDepartmentsMap.value[parentId] = subDepts;
  } catch (error) {
    console.error('加载子科室列表失败:', error);
    subDepartmentsMap.value[parentId] = [];
  }
};

// 选择父科室（el-menu 的 select 事件）
const handleParentSelect = (index) => {
  selectedParentId.value = index;
  const parentId = Number(index);
  walkInForm.departmentId = null; // 清空已选子科室
  walkInForm.scheduleId = null; // 清空已选排班
  selectedSchedule.value = null;
  availableSchedules.value = [];
  
  // 加载子科室
  loadSubDepartments(parentId);
};

// 选择子科室
const selectSubDepartment = (departmentId) => {
  walkInForm.departmentId = departmentId;
  walkInForm.scheduleId = null; // 清空已选排班
  selectedSchedule.value = null;
  selectedDoctorId.value = null;
  doctorList.value = [];
  availableSchedules.value = [];
  loadDoctorsAndSchedules(departmentId);
};

// 根据科室加载医生和排班
const loadDoctorsAndSchedules = async (departmentId) => {
  scheduleLoading.value = true;
  try {
    // 并行加载医生列表和排班列表
    const [doctorsResponse, schedulesResponse] = await Promise.all([
      getDoctorsByDepartmentId(departmentId),
      getAllSchedules(0, 1000)
    ]);
    
    // 处理医生列表
    if (Array.isArray(doctorsResponse)) {
      doctorList.value = doctorsResponse.map(d => ({
        doctorId: d.doctorId || d.id,
        doctorName: d.fullName || d.name,
        doctorTitle: d.title || '医生',
        photoUrl: d.photoUrl || ''
      }));
    } else if (doctorsResponse && doctorsResponse.content) {
      doctorList.value = doctorsResponse.content.map(d => ({
        doctorId: d.doctorId || d.id,
        doctorName: d.fullName || d.name,
        doctorTitle: d.title || '医生',
        photoUrl: d.photoUrl || ''
      }));
    } else {
      doctorList.value = [];
    }
    
    // 处理排班列表，过滤已过期的排班
    if (schedulesResponse && schedulesResponse.content) {
      const now = new Date();
      const today = now.toISOString().split('T')[0];
      const currentTime = now.getHours() * 100 + now.getMinutes(); // HHMM格式
      
      availableSchedules.value = schedulesResponse.content.filter(s => {
        // 基本过滤条件
        if (s.departmentId !== departmentId) return false;
        if (s.status !== 'available') return false;
        if (s.totalSlots <= s.bookedSlots) return false;
        
        const scheduleDate = s.scheduleDate;
        
        // 如果排班日期在今天之前，过滤掉
        if (scheduleDate < today) return false;
        
        // 如果排班日期是今天，检查结束时间
        if (scheduleDate === today && s.endTime) {
          let endHour = null;
          let endMinute = null;
          
          // 处理不同的 endTime 格式
          if (typeof s.endTime === 'string') {
            const timeParts = s.endTime.split(':');
            if (timeParts.length >= 2) {
              endHour = parseInt(timeParts[0]);
              endMinute = parseInt(timeParts[1]);
            }
          } else if (s.endTime && typeof s.endTime === 'object') {
            // 如果是对象格式，尝试提取 hour 和 minute
            endHour = s.endTime.hour || s.endTime.h;
            endMinute = s.endTime.minute || s.endTime.m;
          }
          
          if (endHour !== null && endMinute !== null && !isNaN(endHour) && !isNaN(endMinute)) {
            const endTime = endHour * 100 + endMinute;
            // 如果结束时间已过，过滤掉
            if (endTime < currentTime) return false;
          }
        }
        
        return true;
      });
      
      // 如果没有医生列表，从排班中提取医生信息
      if (doctorList.value.length === 0) {
        const doctorMap = new Map();
        availableSchedules.value.forEach(s => {
          if (s.doctorId && !doctorMap.has(s.doctorId)) {
            doctorMap.set(s.doctorId, {
              doctorId: s.doctorId,
              doctorName: s.doctorName || '未知医生',
              doctorTitle: s.doctorTitle || '医生',
              photoUrl: s.doctorPhotoUrl || ''
            });
          }
        });
        doctorList.value = Array.from(doctorMap.values());
      }
      
      // 默认选中第一个医生
      if (doctorList.value.length > 0 && !selectedDoctorId.value) {
        selectedDoctorId.value = doctorList.value[0].doctorId;
      }
    } else {
      availableSchedules.value = [];
    }
  } catch (error) {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败: ' + (error.response?.data?.message || error.message));
    doctorList.value = [];
    availableSchedules.value = [];
  } finally {
    scheduleLoading.value = false;
  }
};

// 选择医生
const selectDoctor = (doctorId) => {
  selectedDoctorId.value = doctorId;
  walkInForm.scheduleId = null;
  selectedSchedule.value = null;
};

// 选择日期
const selectDate = (date) => {
  selectedDate.value = date;
  walkInForm.scheduleId = null;
  selectedSchedule.value = null;
};

// 选择排班
const selectSchedule = (schedule) => {
  if (schedule.totalSlots <= schedule.bookedSlots) {
    ElMessage.warning('该排班已约满');
    return;
  }
  walkInForm.scheduleId = schedule.scheduleId;
  selectedSchedule.value = schedule;
  if (!walkInForm.paymentMethod) {
    walkInForm.paymentMethod = 'cash';
  }
};

// 搜索患者
const searchPatientsHandler = async (query) => {
  if (!query || query.trim().length < 1) {
    patientList.value = [];
    return;
  }
  patientSearchLoading.value = true;
  try {
    const response = await searchPatients({ id: query.trim(), page: 1 });
    
    if (response && response.content && response.content.length > 0) {
      patientList.value = response.content.map(item => {
        const patientDetail = item.userDetails;
        return {
          patientId: patientDetail.patientId,
          name: patientDetail.fullName || patientDetail.name,
          id: patientDetail.identifier
        };
      });
    } else {
      patientList.value = [];
    }
  } catch (error) {
    console.error('搜索患者失败:', error);
    ElMessage.error('搜索患者失败: ' + (error.response?.data?.message || error.message));
    patientList.value = [];
  } finally {
    patientSearchLoading.value = false;
  }
};

// 患者选择变化
const handlePatientChange = () => {
  // 患者变化时，清空科室和排班选择
  walkInForm.departmentId = null;
  walkInForm.scheduleId = null;
  selectedSchedule.value = null;
  selectedDoctorId.value = null;
  doctorList.value = [];
  availableSchedules.value = [];
  selectedDate.value = 'all';
};


// 创建现场挂号并支付
const handleCreateWalkIn = async () => {
  if (!walkInFormRef.value) return;
  
  await walkInFormRef.value.validate(async (valid) => {
    if (!valid) return;

    walkInLoading.value = true;
    try {
      // 验证必要参数
      if (!walkInForm.patientId) {
        throw new Error('请选择患者');
      }
      if (!walkInForm.scheduleId) {
        throw new Error('请选择排班');
      }
      
      // 确保参数类型正确（patientId 是 Long，scheduleId 是 Integer）
      const requestData = {
        patientId: Number(walkInForm.patientId),
        scheduleId: Number(walkInForm.scheduleId)
      };
      
      console.log('创建现场挂号请求参数:', requestData);
      console.log('表单数据:', {
        patientId: walkInForm.patientId,
        scheduleId: walkInForm.scheduleId,
        patientIdType: typeof walkInForm.patientId,
        scheduleIdType: typeof walkInForm.scheduleId
      });
      
      // 1. 创建现场挂号
      const createResponse = await createWalkInAppointment(requestData);

      console.log('创建现场挂号响应:', createResponse);
      console.log('响应类型:', typeof createResponse);
      console.log('响应键:', createResponse ? Object.keys(createResponse) : 'null');
      
      // 根据 request.js 的响应拦截器，返回的是 response.data
      // 后端返回 ResponseEntity<AppointmentResponse>，所以 createResponse 就是 AppointmentResponse 对象
      // 但也可能被包装在其他结构中，需要兼容处理
      let appointment = createResponse;
      
      // 如果响应有 data 字段，使用 data
      if (createResponse?.data) {
        appointment = createResponse.data;
      }
      
      // 如果响应有 code 字段（统一响应格式），也尝试使用 data
      if (createResponse?.code && createResponse?.data) {
        appointment = createResponse.data;
      }
      
      console.log('解析后的预约对象:', appointment);
      console.log('预约对象键:', appointment ? Object.keys(appointment) : 'null');
      
      // 检查 appointmentId 字段（后端 AppointmentResponse 使用 appointmentId）
      if (!appointment || (!appointment.appointmentId && !appointment.id)) {
        console.error('响应格式错误，完整响应:', JSON.stringify(createResponse, null, 2));
        console.error('解析后的对象:', JSON.stringify(appointment, null, 2));
        throw new Error('创建预约失败：响应格式错误，未找到预约ID。请查看控制台日志获取详细信息。');
      }

      const appointmentId = appointment.appointmentId || appointment.id;
      console.log('提取的预约ID:', appointmentId);

      // 2. 立即支付
      try {
        const paymentResponse = await payForAppointment(appointmentId, {
          paymentMethod: walkInForm.paymentMethod,
          transactionId: walkInForm.transactionId || `TXN${Date.now()}`,
          amount: selectedSchedule.value?.fee
        });

        // 支付响应也可能是直接返回对象或包装在 data 中
        const paymentData = paymentResponse?.data || paymentResponse;
        
        if (paymentData) {
          createdAppointment.value = {
            ...appointment,
            ...paymentData,
            patientName: patientList.value.find(p => p.patientId === walkInForm.patientId)?.name,
            doctorName: selectedSchedule.value?.doctorName || appointment.schedule?.doctorName,
            departmentName: selectedSchedule.value?.departmentName || appointment.schedule?.departmentName,
            scheduleDate: selectedSchedule.value?.scheduleDate || appointment.schedule?.scheduleDate,
            slotName: selectedSchedule.value?.slotName || appointment.schedule?.slotName,
            location: selectedSchedule.value?.location || appointment.schedule?.location,
            fee: selectedSchedule.value?.fee || appointment.schedule?.fee
          };

          ElMessage.success('现场挂号创建并支付成功！');
          resetWalkInForm();
        } else {
          throw new Error('支付失败：响应格式错误');
        }
      } catch (paymentError) {
        console.error('支付失败:', paymentError);
        ElMessage.warning('预约创建成功，但支付失败。请稍后手动完成支付。');
        createdAppointment.value = {
          ...appointment,
          patientName: patientList.value.find(p => p.patientId === walkInForm.patientId)?.name,
          doctorName: selectedSchedule.value?.doctorName || appointment.schedule?.doctorName,
          departmentName: selectedSchedule.value?.departmentName || appointment.schedule?.departmentName,
          scheduleDate: selectedSchedule.value?.scheduleDate || appointment.schedule?.scheduleDate,
          slotName: selectedSchedule.value?.slotName || appointment.schedule?.slotName,
          location: selectedSchedule.value?.location || appointment.schedule?.location,
          fee: selectedSchedule.value?.fee || appointment.schedule?.fee,
          paymentStatus: 'unpaid'
        };
      }
    } catch (error) {
      console.error('创建现场挂号失败:', error);
      console.error('错误对象:', error);
      console.error('错误响应:', error.response);
      console.error('错误响应数据:', error.response?.data);
      
      let errorMessage = '创建现场挂号失败';
      
      if (error.response) {
        // HTTP 错误响应
        const responseData = error.response.data;
        if (typeof responseData === 'string') {
          errorMessage = responseData;
        } else if (responseData?.message) {
          errorMessage = responseData.message;
        } else if (responseData?.error) {
          errorMessage = responseData.error;
        } else if (responseData) {
          errorMessage = JSON.stringify(responseData);
        } else {
          errorMessage = `HTTP ${error.response.status}: ${error.response.statusText}`;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      ElMessage.error(errorMessage);
    } finally {
      walkInLoading.value = false;
    }
  });
};

// 重置现场挂号表单
const resetWalkInForm = () => {
  walkInForm.patientId = null;
  walkInForm.departmentId = null;
  walkInForm.scheduleId = null;
  walkInForm.paymentMethod = 'cash';
  walkInForm.transactionId = '';
  selectedSchedule.value = null;
  selectedDoctorId.value = null;
  selectedDate.value = 'all';
  patientList.value = [];
  doctorList.value = [];
  availableSchedules.value = [];
  walkInFormRef.value?.resetFields();
};

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const methodMap = {
    'cash': '现金',
    'wechat': '微信',
    'alipay': '支付宝',
    'card': '刷卡'
  };
  return methodMap[method] || method || '-';
};

// 搜索退款预约
const searchRefundAppointments = async () => {
  if (!refundSearchForm.identifier || !refundSearchForm.identifier.trim()) {
    ElMessage.warning('请输入患者学号/工号');
    return;
  }
  
  refundSearchLoading.value = true;
  try {
    // 1. 先根据学号/工号搜索患者
    const patientResponse = await searchPatients({ id: refundSearchForm.identifier.trim() });
    
    let patients = [];
    if (Array.isArray(patientResponse)) {
      patients = patientResponse;
    } else if (patientResponse?.content) {
      patients = patientResponse.content;
    } else if (patientResponse?.data?.content) {
      patients = patientResponse.data.content;
    } else if (patientResponse?.data && Array.isArray(patientResponse.data)) {
      patients = patientResponse.data;
    }
    
    if (!patients || patients.length === 0) {
      ElMessage.warning('未找到该学号/工号对应的患者');
      refundAppointments.value = [];
      refundTotal.value = 0;
      return;
    }
    
    // 2. 获取该患者的所有预约
    const allAppointments = [];
    for (const patient of patients) {
      const patientId = patient.patientId || patient.userDetails?.patientId;
      if (patientId) {
        try {
          const appointmentsResponse = await getPatientAppointments(patientId);
          let appointments = [];
          
          if (Array.isArray(appointmentsResponse)) {
            appointments = appointmentsResponse;
          } else if (appointmentsResponse?.data && Array.isArray(appointmentsResponse.data)) {
            appointments = appointmentsResponse.data;
          }
          
          // 3. 筛选：显示所有非已完成的预约
          const refundableAppointments = appointments.filter(apt => {
            const isCompleted = apt.status === 'completed' || apt.status === 'COMPLETED';
            
            console.log('预约筛选:', {
              appointmentId: apt.appointmentId,
              paymentStatus: apt.paymentStatus,
              checkInTime: apt.checkInTime,
              status: apt.status,
              isCompleted,
              show: !isCompleted
            });
            
            // 只过滤掉已完成的预约
            return !isCompleted;
          });
          
          allAppointments.push(...refundableAppointments);
        } catch (error) {
          console.error(`获取患者 ${patientId} 的预约失败:`, error);
        }
      }
    }
    
    // 按预约日期倒序排列（最新的在前）
    allAppointments.sort((a, b) => {
      const dateA = a.schedule?.scheduleDate || a.scheduleDate || '';
      const dateB = b.schedule?.scheduleDate || b.scheduleDate || '';
      return dateB.localeCompare(dateA);
    });
    
    refundAppointments.value = allAppointments;
    refundTotal.value = allAppointments.length;
    
    if (allAppointments.length === 0) {
      ElMessage.info('该患者没有非已完成的预约');
    }
  } catch (error) {
    console.error('搜索退款预约失败:', error);
    ElMessage.error(error.response?.data?.message || '搜索预约失败');
    refundAppointments.value = [];
    refundTotal.value = 0;
  } finally {
    refundSearchLoading.value = false;
  }
};

// 重置退款搜索
const resetRefundSearch = () => {
  refundSearchForm.identifier = '';
  refundAppointments.value = [];
  refundTotal.value = 0;
  refundPage.value = 1;
};

// 获取支付状态标签类型
const getPaymentStatusTagType = (paymentStatus) => {
  const status = String(paymentStatus || '').toLowerCase();
  if (status === 'paid') return 'success';
  if (status === 'refunded') return 'info';
  return 'warning';
};

// 获取支付状态文本
const getPaymentStatusText = (paymentStatus) => {
  const status = String(paymentStatus || '').toLowerCase();
  if (status === 'paid') return '已支付';
  if (status === 'refunded') return '已退款';
  return '未支付';
};

// 获取预约状态标签类型（基于实际状态和签到时间）
const getAppointmentStatusTagType = (row) => {
  const status = String(row.status || '').toLowerCase();
  
  // 如果已取消，显示红色
  if (status === 'cancelled') return 'danger';
  
  // 如果已完成，显示灰色
  if (status === 'completed') return 'info';
  
  // 如果有签到时间，显示蓝色（已签到）
  if (row.checkInTime) return 'primary';
  
  // 如果已预约，显示绿色
  if (status === 'scheduled') return 'success';
  
  // 待支付显示橙色
  if (status === 'pending_payment') return 'warning';
  
  return '';
};

// 获取预约状态文本（统一显示逻辑）
const getAppointmentStatusText = (row) => {
  const status = String(row.status || '').toLowerCase();
  
  // 优先判断是否已取消
  if (status === 'cancelled') return '已取消';
  
  // 优先判断是否已完成
  if (status === 'completed') return '已完成';
  
  // 如果有签到时间，显示"已签到"（这是最准确的状态）
  if (row.checkInTime) return '已签到';
  
  // 如果状态是 CHECKED_IN 但没有签到时间，显示"已预约"（数据不一致的情况）
  if (status === 'checked_in' && !row.checkInTime) return '已预约';
  
  // 已预约状态
  if (status === 'scheduled') return '已预约';
  
  // 待支付状态
  if (status === 'pending_payment') return '待支付';
  
  // 爽约状态
  if (status === 'no_show') return '爽约';
  
  return status || '-';
};

// 判断是否可以退款（统一判断逻辑）
const canRefund = (row) => {
  // 支付状态必须是已支付
  const paymentStatus = String(row.paymentStatus || '').toLowerCase();
  if (paymentStatus !== 'paid') {
    return false;
  }
  
  // 不能已签到（以 checkInTime 为准）
  if (row.checkInTime) {
    return false;
  }
  
  // 不能已完成
  const status = String(row.status || '').toLowerCase();
  if (status === 'completed') {
    return false;
  }
  
  // 不能已取消
  if (status === 'cancelled') {
    return false;
  }
  
  return true;
};

// 获取退款禁用原因（统一显示逻辑）
const getRefundDisabledReason = (row) => {
  const paymentStatus = String(row.paymentStatus || '').toLowerCase();
  
  // 支付状态判断
  if (paymentStatus === 'refunded') {
    return '已退款';
  }
  if (paymentStatus !== 'paid') {
    return '未支付';
  }
  
  // 签到状态判断（以 checkInTime 为准）
  if (row.checkInTime) {
    return '已签到';
  }
  
  // 预约状态判断
  const status = String(row.status || '').toLowerCase();
  if (status === 'completed') {
    return '已完成';
  }
  if (status === 'cancelled') {
    return '已取消';
  }
  
  return '-';
};

// 处理退款
const handleRefund = async (appointment) => {
  console.log('点击退款按钮，预约信息:', appointment);
  
  const patientName = appointment.patient?.fullName || appointment.patient?.name || appointment.patientName || '未知患者';
  const fee = appointment.schedule?.fee || appointment.fee || '0.00';
  
  try {
    await ElMessageBox.confirm(
      `确定要退款预约 ${appointment.appointmentId} 吗？\n患者：${patientName}\n费用：¥${fee}\n\n退款后将同时取消该预约。`,
      '确认退款',
      {
        confirmButtonText: '确定退款',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    refundingAppointmentId.value = appointment.appointmentId;
    try {
      console.log('开始退款，预约ID:', appointment.appointmentId);
      const response = await refundAppointment(appointment.appointmentId);
      console.log('退款响应:', response);
      
      // 后端直接返回 AppointmentResponse 对象，不是包装在 data 中
      const refundResult = response?.data || response;
      
      if (refundResult) {
        ElMessage.success('退款成功，预约已取消');
        // 刷新列表
        await searchRefundAppointments();
      } else {
        throw new Error('退款响应格式错误');
      }
    } catch (error) {
      console.error('退款失败:', error);
      console.error('错误详情:', error.response);
      const errorMessage = error.response?.data?.message 
        || error.response?.data 
        || error.message 
        || '退款失败';
      ElMessage.error(errorMessage);
    } finally {
      refundingAppointmentId.value = null;
    }
  } catch (error) {
    // 用户取消确认对话框
    if (error !== 'cancel' && error !== 'close') {
      console.error('退款操作异常:', error);
    }
  }
};

// 获取状态标签类型
const getStatusTagType = (status) => {
  const statusMap = {
    'scheduled': 'success',
    'CHECKED_IN': 'primary',
    'completed': 'info',
    'cancelled': 'danger',
    'PENDING_PAYMENT': 'warning'
  };
  return statusMap[status] || '';
};

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'scheduled': '已预约',
    'CHECKED_IN': '已签到',
    'completed': '已完成',
    'cancelled': '已取消',
    'PENDING_PAYMENT': '待支付'
  };
  return statusMap[status] || status;
};

// Tab切换
const handleTabChange = (tabName) => {
  if (tabName === 'walkIn') {
    if (parentDepartments.value.length === 0) {
      loadParentDepartments();
    }
  } else if (tabName === 'refund') {
    searchRefundAppointments();
  }
};

onMounted(() => {
  loadParentDepartments();
  initDateOptions();
});
</script>

<style scoped>
.on-site-service-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

/* 科室和排班容器（并排显示） */
.department-schedule-container {
  display: flex;
  gap: 15px;
  height: 600px;
}

.department-section {
  flex: 0 0 400px;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
}

.schedule-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.schedule-section-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

/* 科室选择器样式（参考 ScheduleDashboard） */
.department-selector {
  display: flex;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  flex: 1;
  overflow: hidden;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  gap: 12px;
  width: 100%;
}

.loading-container .el-icon {
  font-size: 24px;
}

.department-menu {
  width: 200px;
  border-right: none;
  overflow-y: auto;
}

.sub-department-panel {
  flex: 1;
  padding: 8px;
  border-left: 1px solid #e2e8f0;
  overflow-y: auto;
}

.sub-department-item {
  padding: 10px 15px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
  transition: all 0.3s;
}

.sub-department-item:hover {
  background-color: #f5f7fa;
}

.sub-department-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: bold;
}

.no-sub-departments {
  padding: 20px;
  text-align: center;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 排班选择器样式 */
.schedule-selector {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  background: #fff;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.date-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.doctor-schedule-container {
  display: flex;
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  min-height: 0;
}

.doctor-list {
  width: 200px;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  background: #f5f7fa;
}

.doctor-item {
  display: flex;
  align-items: center;
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #e4e7ed;
  transition: all 0.3s;
  gap: 12px;
}

.doctor-item:hover {
  background: #ecf5ff;
}

.doctor-item.active {
  background: #ffffff;
  border-right: 3px solid #409eff;
}

.doctor-avatar {
  flex-shrink: 0;
}

.doctor-info {
  flex: 1;
  min-width: 0;
}

.doctor-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doctor-title {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-doctor-list {
  width: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.schedule-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  background: #fff;
}

.empty-schedule {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.date-group {
  margin-bottom: 25px;
}

.date-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
}

.schedule-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.schedule-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.schedule-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.schedule-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.schedule-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

.schedule-card.disabled:hover {
  border-color: #e4e7ed;
  box-shadow: none;
}

.schedule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.time-value {
  color: #409eff;
}

.schedule-card-body {
  margin-bottom: 12px;
  min-height: 20px;
}

.schedule-info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.schedule-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.slots-info {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-size: 13px;
  color: #606266;
}

.slots-value {
  font-size: 18px;
  font-weight: 600;
  color: #67C23A;
}

.slots-separator {
  color: #909399;
}

.slots-total {
  color: #909399;
}

.slots-label {
  margin-left: 4px;
  color: #909399;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price-symbol {
  font-size: 14px;
  color: #E6A23C;
}

.price-value {
  font-size: 20px;
  font-weight: 600;
  color: #E6A23C;
}
</style>
