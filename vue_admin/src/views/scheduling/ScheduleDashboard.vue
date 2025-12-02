<template>
  <div class="schedule-dashboard">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    
    <!-- 折叠/展开按钮 -->
    <div class="sidebar-toggle" :class="{ collapsed: sidebarCollapsed }" @click="toggleSidebar" :title="sidebarCollapsed ? '展开科室列表' : '收起科室列表'">
      <el-icon><component :is="sidebarCollapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
    </div>
    
    <!-- 左侧科室导航 -->
    <div class="department-sidebar" :class="{ 'collapsed': sidebarCollapsed }">
      <div v-if="loadingDepartments" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载科室数据中...</span>
      </div>
      <template v-else>
        <el-menu :default-active="activeParent" class="department-menu" @select="handleParentSelect">
          <el-menu-item v-for="parent in departments" :key="parent.id" :index="parent.id">
            <span>{{ parent.name }}</span>
          </el-menu-item>
        </el-menu>

        <div class="sub-department-panel" v-if="subDepartments.length > 0">
          <div v-for="sub in subDepartments" :key="sub.id" class="sub-department-item" :class="{ 'active': activeSub === sub.id }" @click="handleSubSelect(sub.id)">
            {{ sub.name }}
          </div>
        </div>
        <div v-else-if="activeParent && departments.find(p => p.id === activeParent)?.children?.length === 0" class="no-sub-departments">
          <el-empty description="该科室暂无子科室" :image-size="60"/>
        </div>
      </template>
    </div>

    <!-- 右侧内容区 -->
    <div class="schedule-content" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <el-card shadow="always" class="schedule-card">
        <template #header>
          <div class="card-header">
             <!-- 科室标题行 -->
             <div class="header-title-row">
               <span class="department-title">{{ selectedDepartmentName }}<span v-if="selectedDepartmentCode && selectedDepartmentCode !== 'N/A'"> ({{ selectedDepartmentCode }})</span> - 排班管理</span>
             
             <!-- 排班状态指示器 -->
             <div class="schedule-status-indicator">
               <div v-if="scheduleStatus.saving" class="status-saving">
                 <el-icon class="is-loading"><Loading /></el-icon>
                 <span>正在保存排班...</span>
               </div>
               <div v-else-if="scheduleStatus.lastSaved" class="status-success">
                 <el-icon><CircleCheck /></el-icon>
                 <span>最后保存：{{ scheduleStatus.lastSaved.doctor }} - {{ scheduleStatus.lastSaved.timestamp }}</span>
               </div>
               <div v-else-if="scheduleStatus.error" class="status-error">
                 <el-icon><CircleClose /></el-icon>
                 <span>保存失败：{{ scheduleStatus.error.doctor }} - {{ scheduleStatus.error.timestamp }}</span>
                 </div>
               </div>
             </div>
             
             <!-- 按钮控制行 -->
             <div class="header-controls">
              <!-- 自动排班按钮 -->
              <el-button 
                class="action-btn btn-auto"
                type="primary" 
                :icon="MagicStick" 
                @click="goToAutoSchedule">
                自动排班
              </el-button>
              
              <!-- 视图切换按钮组 -->
              <el-button-group class="view-switcher">
                <el-button :type="currentView === 'day' ? 'primary' : ''" @click="changeView('day')">日视图</el-button>
                <el-button :type="currentView === 'week' ? 'primary' : ''" @click="changeView('week')">周视图</el-button>
                <el-button :type="currentView === 'month' ? 'primary' : ''" @click="changeView('month')">月视图</el-button>
              </el-button-group>
              
              <!-- 周视图导航按钮 -->
              <el-button-group v-if="currentView === 'week'" class="week-nav">
                <el-button :icon="ArrowLeft" @click="changeWeek(-1)">上一周</el-button>
                <el-button @click="changeWeek(0)">本周</el-button>
                <el-button :icon="ArrowRight" @click="changeWeek(1)">下一周</el-button>
              </el-button-group>
               
               <!-- 冲突信息显示 -->
               <div class="conflict-controls">
                 <div v-if="conflictData.hasConflicts" class="conflict-summary" @click="showConflictDialog" title="点击查看详细冲突信息">
                   <el-icon class="conflict-summary-icon" :class="conflictData.summary.critical > 0 ? 'critical-icon' : 'warning-icon'">
                     <Warning />
                   </el-icon>
                   <span class="conflict-text">
                     发现 {{ conflictData.summary.total }} 个冲突
                     <span v-if="conflictData.summary.critical > 0" class="critical-count">
                       (严重: {{ conflictData.summary.critical }})
                     </span>
                     <span v-if="conflictData.summary.warning > 0" class="warning-count">
                       (警告: {{ conflictData.summary.warning }})
                     </span>
                   </span>
                   <el-icon style="margin-left: 4px; font-size: 14px;"><ArrowRight /></el-icon>
                 </div>
               </div>
            </div>
          </div>
        </template>

         <!-- 日历视图 -->
         <div v-if="currentView !== 'week'" class="calendar-view">
           <div class="calendar-container">
             <FullCalendar 
               ref="fullCalendar"
               :options="calendarOptions"
             />
           </div>
         </div>

         <!-- 周视图表格 -->
         <div v-if="currentView === 'week'" class="table-container">
        <div v-if="activeSub">
          <table class="schedule-table">
            <thead>
            <tr>
              <th>门诊时段</th>
              <th v-for="day in weekDates" :key="day.fullDate">{{ day.date }} ({{ day.dayOfWeek }})</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="shift in ['上午', '下午']" :key="shift">
              <td class="time-slot-column" @dragover.prevent @drop="onDrop($event, null, shift)">
                <div class="shift-label">{{ shift }}</div>
                <!-- 时间段卡片区域 - 只显示在这个列中 -->
                <div class="time-slot-cards">
                  <div v-for="timeSlot in getTimeSlotsForShift(shift)" :key="timeSlot.slotId || timeSlot.slot_id"
                          class="time-slot-card" 
                          :class="{ 
                            'time-slot-mismatch': !isTimeSlotMatchShift(timeSlot, shift)
                          }"
                          draggable="true" 
                       @dragstart="onDragStart($event, { type: 'timeSlot', data: timeSlot })">
                    <div class="time-slot-card-content">
                      <div class="time-slot-name">{{ timeSlot.slotName || timeSlot.slot_name || `${timeSlot.startTime || timeSlot.start_time}-${timeSlot.endTime || timeSlot.end_time}` }}</div>
                      <div class="time-slot-time">{{ (timeSlot.startTime || timeSlot.start_time) }} - {{ (timeSlot.endTime || timeSlot.end_time) }}</div>
                         <!-- 班次不匹配警告 -->
                         <div v-if="!isTimeSlotMatchShift(timeSlot, shift)" class="shift-mismatch-warning">
                           <el-icon class="warning-icon"><Warning /></el-icon>
                           <span>班次不匹配</span>
                         </div>
                    </div>
                    <el-icon class="remove-icon" @click="removeTimeSlotFromColumn(timeSlot, shift)"><Close /></el-icon>
                  </div>
                </div>
              </td>
              <td v-for="day in weekDates" :key="day.fullDate + '-' + shift"
                  @dragover.prevent @drop="onDrop($event, day.fullDate, shift)">
                <div class="shift-cell">
                  <div class="doctor-tags">
                    <div v-for="doc in getDoctorsForShift(day.fullDate, shift)" :key="doc.id"
                            class="doctor-card-in-table" 
                            :class="getDoctorConflictClass(doc, day.fullDate, shift)"
                            :data-doctor-id="doc.id" 
                            draggable="true" 
                            @dragstart="onDragStart($event, { type: 'doctor', data: doc }, day.fullDate, shift)"
                            @click="showConflictDetails(doc, day.fullDate, shift)">
                      <div class="doctor-card-header">
                        <img :src="getDoctorAvatar(doc.id)" alt="医生头像" class="doctor-avatar-small">
                          <span>{{ doc.name }} (ID:{{ doc.identifier || doc.id }})</span>
                        <el-icon class="remove-icon" @click="removeDoctorFromShift(doc, day.fullDate, shift)"><Close /></el-icon>
                           <!-- [新增] 冲突图标 -->
                           <el-icon v-if="hasDoctorConflicts(doc, day.fullDate, shift)" class="conflict-icon" 
                                    :class="getDoctorConflictIconClass(doc, day.fullDate, shift)">
                             <Warning />
                           </el-icon>
                      </div>
                      <div class="doctor-card-location" :class="{ 'is-set': doc.location }">
                        <el-icon><Location /></el-icon>
                        <span>{{ doc.location || '待分配地点' }}</span>
                        <!-- [新增] 清除地点按钮 -->
                        <el-icon v-if="doc.location" class="clear-location-icon" @click.stop="clearLocation(doc)"><CircleCloseFilled /></el-icon>
                      </div>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="placeholder">
          <el-empty description="请在左侧选择一个子科室以查看排班表" />
           </div>
        </div>
      </el-card>

      <!-- 底部拖拽区域 - 只在周视图下显示 -->
      <div v-if="currentView === 'week'" class="bottom-panels">
        <!-- 待排班医生列表 -->
        <el-card shadow="always" class="draggable-list-card">
          <template #header>
            <div class="card-header">
              <span>待排班医生 (拖拽到上方进行排班)</span>
            </div>
          </template>
          <div v-if="loadingDoctors" class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载医生数据中...</span>
          </div>
          <div v-else class="draggable-list">
            <div v-for="doc in availableDoctors" :key="doc.id"
                 class="doctor-card" draggable="true" @dragstart="onDragStart($event, { type: 'doctor', data: doc })">
              <img :src="doc.gender === 'male' ? doctorMaleImg : doctorFemaleImg" alt="医生头像" class="doctor-avatar">
              <div class="doctor-info">
                <span class="doctor-name">{{ doc.name }} (ID:{{ doc.identifier }})</span>
                <span v-if="doc.title" class="doctor-title">{{ doc.title }}</span>
              </div>
            </div>
            <el-empty v-if="!availableDoctors.length" description="该科室暂无医生" :image-size="60"/>
          </div>
        </el-card>

        <!-- 时间段卡片列表 -->
        <el-card shadow="always" class="draggable-list-card">
          <template #header>
            <div class="card-header">
              <span>时间段卡片 (拖拽到上方进行排班)</span>
            </div>
          </template>
          <div class="draggable-list time-slot-list">
            
            <div v-for="timeSlot in timeSlots" :key="timeSlot.slotId || timeSlot.slot_id"
                 class="time-slot-card" draggable="true" @dragstart="onDragStart($event, { type: 'timeSlot', data: timeSlot })">
              <el-icon :size="20" class="time-slot-icon"><Clock /></el-icon>
              <div class="time-slot-info">
                <span class="time-slot-name">{{ timeSlot.slotName || timeSlot.slot_name || `${timeSlot.startTime || timeSlot.start_time}-${timeSlot.endTime || timeSlot.end_time}` }}</span>
                <span class="time-slot-time">{{ (timeSlot.startTime || timeSlot.start_time) }} - {{ (timeSlot.endTime || timeSlot.end_time) }}</span>
              </div>
            </div>
            <el-empty v-if="!timeSlots.length" description="暂无时间段" :image-size="60"/>
          </div>
        </el-card>

        <!-- 可用办公地点列表 -->
        <el-card shadow="always" class="draggable-list-card">
          <template #header>
            <div class="card-header">
              <span>可用办公地点 (拖拽到医生卡片上分配)</span>
            </div>
          </template>
          <div class="draggable-list location-list">
            <div v-for="loc in availableLocations" :key="loc.location_id"
                 class="location-card" draggable="true" @dragstart="onDragStart($event, { type: 'location', data: loc })">
              <el-icon :size="20" class="location-icon"><OfficeBuilding /></el-icon>
              <div class="location-info">
                <span class="location-name">{{ loc.name }}</span>
                <span class="location-desc">{{ `${loc.building} - ${loc.floor}` }}</span>
              </div>
            </div>
            <el-empty v-if="!availableLocations.length" description="暂无可用地点" :image-size="60"/>
          </div>
        </el-card>
      </div>

    </div>

    <!-- 冲突详情对话框 -->
    <el-dialog
      v-model="conflictDialogVisible"
      title="排班冲突详情"
      width="800px"
      :close-on-click-modal="false"
      class="conflict-dialog"
    >
      <div class="conflict-summary-header">
        <el-alert
          :title="`共发现 ${conflictData.summary.total} 个冲突`"
          :type="conflictData.summary.critical > 0 ? 'error' : 'warning'"
          :closable="false"
        >
          <template #default>
            <div class="conflict-stats">
              <span v-if="conflictData.summary.critical > 0" class="stat-item critical">
                <el-icon><Warning /></el-icon>
                严重冲突: {{ conflictData.summary.critical }} 个
              </span>
              <span v-if="conflictData.summary.warning > 0" class="stat-item warning">
                <el-icon><Warning /></el-icon>
                警告冲突: {{ conflictData.summary.warning }} 个
              </span>
            </div>
          </template>
        </el-alert>
      </div>

      <div class="conflict-list">
        <el-collapse v-model="activeConflictNames" accordion>
          <el-collapse-item
            v-for="(conflict, index) in conflictData.conflicts"
            :key="index"
            :name="index"
            :class="`conflict-item conflict-${conflict.severity}`"
          >
            <template #title>
              <div class="conflict-title">
                <el-icon :class="`conflict-icon ${conflict.severity}-icon`">
                  <Warning />
                </el-icon>
                <span class="conflict-type-badge" :class="`badge-${conflict.severity}`">
                  {{ conflict.title }}
                </span>
                <span class="conflict-desc">{{ conflict.description }}</span>
              </div>
            </template>
            <div class="conflict-details">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item
                  v-for="(detail, detailIndex) in conflict.details"
                  :key="detailIndex"
                  :label="detail.split(':')[0]"
                >
                  {{ detail.split(':').slice(1).join(':').trim() }}
                </el-descriptions-item>
              </el-descriptions>
              
              <!-- 如果有相关医生，显示医生信息 -->
              <div v-if="conflict.allDoctors && conflict.allDoctors.length > 0" class="conflict-doctors">
                <div class="doctors-title">涉及医生：</div>
                <div class="doctors-list">
                  <el-tag
                    v-for="doctor in conflict.allDoctors"
                    :key="doctor.id"
                    type="info"
                    effect="plain"
                    size="small"
                  >
                    {{ doctor.name }} ({{ doctor.identifier || doctor.id }})
                  </el-tag>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <template #footer>
        <el-button @click="conflictDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportConflictReport">
          <el-icon><Download /></el-icon>
          导出冲突报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
// [新增] 导入 CircleCloseFilled 图标
import { ArrowLeft, ArrowRight, Close, Location, OfficeBuilding, CircleCloseFilled, Clock, Document, Download, UploadFilled, Upload, Refresh, CircleCheck, CircleClose, Warning, Loading, MagicStick, DArrowLeft, DArrowRight } from '@element-plus/icons-vue';
// [新增] 导入 FullCalendar 组件和插件
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
// [新增] 导入 Excel 解析库
import * as XLSX from 'xlsx';
import { ElMessage } from 'element-plus';
import doctorMaleImg from '@/assets/doctor.jpg';
import doctorFemaleImg from '@/assets/doctor1.jpg';
import BackButton from '@/components/BackButton.vue';
import { getTimeSlots } from '@/api/timeslot';
import { getAllParentDepartments, getDepartmentsByParentId, getDoctorsByDepartmentId } from '@/api/department';
import { getLocationNamesByDepartmentId, getLocationsByDepartmentId } from '@/api/location';
import { createSchedule, getSchedules, deleteScheduleByParams, getAllSchedules } from '@/api/schedule';

const router = useRouter();

// --- 科室数据（从API获取） ---
const departments = ref([]);
const loadingDepartments = ref(false);

// --- 医生数据（从API获取） ---
const loadingDoctors = ref(false);

// --- 排班状态管理 ---
const scheduleStatus = ref({
  saving: false,
  lastSaved: null,
  error: null
});

const doctorsData = ref({
  's1-2': [
    {id: 1, name: '杨青松', identifier: 'D001', title: '主任医师', gender: 'male'},
    {id: 2, name: '杨林', identifier: 'D002', title: '副主任医师', gender: 'male'},
    {id: 3, name: '席紫明', identifier: 'D003', title: '主治医师', gender: 'female'}
  ],
  'p3': [ {id: 6, name: '王莉', identifier: 'D006', title: '主任医师', gender: 'female'} ],
});

const availableLocations = ref([]);

// 时间段数据 - 从API获取
const timeSlots = ref([]);


const scheduleData = ref({});

// 存储拖拽到时段列中的时间段卡片
const timeSlotColumns = ref({
  '上午': [],
  '下午': []
});

// --- 状态管理 ---
// 获取当前周的周一日期
const getCurrentWeekMonday = () => {
  const today = new Date();
  const dayOfWeek = today.getDay();
  const diff = today.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1); // 周一
  const monday = new Date(today);
  monday.setDate(diff);
  return monday;
};

const currentMonday = ref(getCurrentWeekMonday());
const activeParent = ref(null);
const activeSub = ref(null);

// 侧边栏折叠状态
const sidebarCollapsed = ref(false);

// [新增] 视图切换状态
const currentView = ref('week'); // 'day', 'week', 'month'
const fullCalendar = ref(null);
const calendarEvents = ref([]);

// [新增] 批量导入相关状态
const uploadRef = ref(null);
const selectedFile = ref(null);
const importing = ref(false);
const importProgress = ref({
  show: false,
  current: 0,
  total: 0,
  percentage: 0,
  status: 'success',
  message: ''
});
const importResult = ref({
  show: false,
  type: 'success', // 'success' | 'error'
  title: '',
  message: '',
  details: []
});

// [新增] 冲突检测相关状态
const conflictData = ref({
  hasConflicts: false,
  conflicts: [],
  summary: {
    total: 0,
    critical: 0,
    warning: 0
  }
});

// [新增] 冲突详情对话框状态
const conflictDialogVisible = ref(false);
const activeConflictNames = ref([]);

const subDepartments = computed(() => {
  if (!activeParent.value) return [];
  const parent = departments.value.find(p => p.id === activeParent.value);
  return parent ? parent.children : [];
});

// [新增] FullCalendar 配置
const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: currentView.value === 'day' ? 'timeGridDay' : 
               currentView.value === 'week' ? 'timeGridWeek' : 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: ''
  },
  locale: 'zh-cn',
  buttonText: {
    today: '今天',
    month: '月',
    week: '周',
    day: '日'
  },
  slotMinTime: '08:00:00',
  slotMaxTime: '18:00:00',
  allDaySlot: false,
  height: 'auto',
  events: calendarEvents.value,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  drop: handleCalendarDrop,
  eventDrop: handleEventDrop,
  eventResize: handleEventResize,
  editable: true,
  selectable: true,
  selectMirror: true,
  dayMaxEvents: true,
  weekends: true,
  slotDuration: '00:30:00',
  eventTimeFormat: {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  },
  droppable: true,
  dropAccept: '.time-slot-card, .location-card',
  datesSet: handleCalendarDatesSet,  // 🔥 新增：日期范围变化时自动加载数据
  eventContent: renderEventContent,  // 自定义事件内容渲染
  eventDidMount: handleEventDidMount  // 事件挂载后的处理
}));

const selectedDepartmentName = computed(() => {
  if (!activeSub.value) return '请选择科室';
  
  // 先尝试作为父科室查找
  const parentAsSub = departments.value.find(p => p.id === activeSub.value);
  if (parentAsSub) {
    return parentAsSub.name;
  }
  
  // 作为子科室查找
  for (const parent of departments.value) {
    const sub = parent.children.find(c => c.id === activeSub.value);
    if (sub) {
      return sub.name;
  }
  }
  
  return '未知科室';
});

const selectedDepartmentCode = computed(() => {
  if (!activeSub.value) return 'N/A';
  const parentAsSub = departments.value.find(p => p.id === activeSub.value);
  if (parentAsSub) return parentAsSub.code || 'N/A';
  for (const parent of departments.value) {
    const sub = parent.children.find(c => c.id === activeSub.value);
    if (sub) return sub.code || 'N/A';
  }
  return 'N/A';
});

const availableDoctors = computed(() => {
  if (!activeSub.value) return [];
  // 从科室ID中提取数字ID（去掉前缀 's' 或 'p'）
  const departmentId = activeSub.value.replace(/^[sp]/, '');
  return doctorsData.value[departmentId] || [];
});

// --- 日期和排班表逻辑 ---
const weekDates = computed(() => {
  const days = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];
  return Array.from({ length: 7 }).map((_, i) => {
    const date = new Date(currentMonday.value);
    date.setDate(date.getDate() + i);
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return {
      date: `${month}.${day}`,
      dayOfWeek: days[i],
      fullDate: `${year}-${month}-${day}`
    }
  });
});

const changeWeek = async (offset) => {
  if (offset === 0) {
    // 点击"本周"按钮，跳转到当前周的周一
    currentMonday.value = getCurrentWeekMonday();
  } else {
    const newDate = new Date(currentMonday.value);
    newDate.setDate(newDate.getDate() + (offset * 7));
    currentMonday.value = newDate;
  }
  // 切换周次时清空时间段列
  clearTimeSlotColumns();
  
  // 🔥 新增：重新加载新周次的排班数据
  await loadSchedulesFromBackend();
};

// 切换侧边栏折叠状态
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
};

const getDoctorsForShift = (date, shift) => {
  if (!activeSub.value || !scheduleData.value[activeSub.value]) return [];
  const entry = scheduleData.value[activeSub.value].find(item => item.date === date && item.shift === shift);
  return entry ? entry.doctors : [];
};

// 获取指定时段的时间段卡片（只显示在时段列中）
const getTimeSlotsForShift = (shift) => {
  // 只显示手动拖拽到时段列的时间段，不自动筛选
  return timeSlotColumns.value[shift] || [];
};

const getDoctorAvatar = (doctorId) => {
  for (const deptId in doctorsData.value) {
    const doctor = doctorsData.value[deptId].find(doc => doc.id === doctorId);
    if (doctor) {
      return doctor.gender === 'male' ? doctorMaleImg : doctorFemaleImg;
    }
  }
  return doctorMaleImg;
};

// [新增] 检查排班冲突 - 在创建排班前检查
const checkScheduleConflict = (doctor, date, shift) => {
  // 检查医生重复排班冲突
  const existingDoctors = getDoctorsForShift(date, shift);
  const isDoctorAlreadyScheduled = existingDoctors.some(d => d.id === doctor.id);
  
  if (isDoctorAlreadyScheduled) {
    return {
      hasConflict: true,
      message: `医生 ${doctor.name} 在 ${date} ${shift} 已经被排班`
    };
  }
  
  // 检查办公室冲突（同一时间段同一地点被多个医生使用）
  if (doctor.location) {
    const isLocationTaken = existingDoctors.some(d => d.location === doctor.location);
    
    if (isLocationTaken) {
      const conflictingDoctor = existingDoctors.find(d => d.location === doctor.location);
      return {
        hasConflict: true,
        message: `地点 ${doctor.location} 在 ${date} ${shift} 已被医生 ${conflictingDoctor.name} 占用`
      };
    }
  }
  
  return {
    hasConflict: false,
    message: ''
  };
};

// --- 拖拽逻辑 ---
const onDragStart = (event, item, fromDate = null, fromShift = null) => {
  const dragData = {
    type: item.type, // 'doctor' or 'location'
    data: item.data,
    source: { date: fromDate, shift: fromShift }
  };
  event.dataTransfer.setData('application/json', JSON.stringify(dragData));
  event.dataTransfer.effectAllowed = 'move';
};

const onDrop = (event, toDate, toShift) => {
  event.preventDefault();
  const dragData = JSON.parse(event.dataTransfer.getData('application/json'));

  if (dragData.type === 'doctor') {
    handleDoctorDrop(dragData, toDate, toShift);
  } else if (dragData.type === 'location') {
    handleLocationDrop(dragData, toDate, toShift, event.target);
  } else if (dragData.type === 'timeSlot') {
    handleTimeSlotDrop(dragData, toDate, toShift);
  }
};

const handleDoctorDrop = async (dragData, toDate, toShift) => {
  const { data: doctor, source } = dragData;
  if (source.date && source.shift) {
    if (source.date === toDate && source.shift === toShift) return;
    await removeDoctorFromShift(doctor, source.date, source.shift, false);
  }
  
  // [新增] 在添加医生前检查冲突
  const conflictCheck = checkScheduleConflict(doctor, toDate, toShift);
  if (conflictCheck.hasConflict) {
    ElMessage.error(`无法创建排班：${conflictCheck.message}`);
    return;
  }
  
  addDoctorToShift(doctor, toDate, toShift);
  
  // 保存排班到后端
  try {
    // 获取当前时段的时间段信息
    const timeSlotsForShift = getTimeSlotsForShift(toShift);
    const timeSlot = timeSlotsForShift.length > 0 ? timeSlotsForShift[0] : null;
    
    // 获取医生实际分配的地点，如果没有则不保存排班
    let location = null;
    console.log('当前选中的科室ID:', activeSub.value);
    console.log('当前availableLocations.value:', availableLocations.value);
    console.log('医生地点信息:', doctor.location);
    
    if (doctor.location) {
      // 根据医生分配的地点名称找到对应的location对象
      location = availableLocations.value.find(loc => loc.name === doctor.location);
      console.log('调试地点查找:', {
        doctorLocation: doctor.location,
        availableLocations: availableLocations.value,
        foundLocation: location
      });
    }
    
    if (timeSlot && location) {
      await saveScheduleToBackend(doctor, toDate, toShift, timeSlot, location);
    } else if (!timeSlot) {
      console.warn('没有找到对应的时间段信息，无法保存排班');
    } else if (!location) {
      console.warn('医生未分配地点，无法保存排班。请先拖拽地点到医生卡片上');
      ElMessage.warning('请先拖拽地点到医生卡片上，然后再进行排班');
    }
  } catch (error) {
    console.error('保存排班失败:', error);
    // 如果保存失败，可以选择是否回滚前端状态
    // removeDoctorFromShift(doctor, toDate, toShift);
  }
};

const handleLocationDrop = async (dragData, toDate, toShift, targetElement) => {
  const { data: location } = dragData;

  const doctorCard = targetElement.closest('.doctor-card-in-table');
  if (!doctorCard) {
    ElMessage.warning('请将地点直接拖拽到医生的卡片上。');
    return;
  }

  const doctorsInShift = getDoctorsForShift(toDate, toShift);
  if (!doctorsInShift || doctorsInShift.length === 0) return;

  // [修改] 优化逻辑，先找到目标医生
  const targetDoctorId = parseInt(doctorCard.dataset.doctorId, 10);
  const targetDoctor = doctorsInShift.find(doc => doc.id === targetDoctorId);

  if (!targetDoctor) return;

  // [修改] 检查新地点是否已被该班次中的【其他】医生占用
  const isLocationTakenByAnotherDoctor = doctorsInShift.some(
      doc => doc.id !== targetDoctor.id && doc.location === location.name
  );

  if (isLocationTakenByAnotherDoctor) {
    ElMessage.error(`地点【${location.name}】在该班次已被其他医生占用，请选择其他地点。`);
    return;
  }

  const oldLocation = targetDoctor.location;
  targetDoctor.location = location.name; // 分配或更换地点

  if (oldLocation && oldLocation !== location.name) {
    ElMessage.success(`已将【${targetDoctor.name}】医生的地点从“${oldLocation}”更换为“${location.name}”`);
  } else if (!oldLocation) {
    ElMessage.success(`已为【${targetDoctor.name}】医生分配地点：${location.name}`);
  }

  // 在地点分配后，若当前单元格已有医生并且该班次存在时间段，则立即持久化保存
  try {
    // 仅当存在具体日期与班次时才尝试保存
    if (toDate && toShift) {
      const timeSlotsForShift = getTimeSlotsForShift(toShift);
      const timeSlot = timeSlotsForShift.length > 0 ? timeSlotsForShift[0] : null;

      if (timeSlot) {
        // 直接使用拖拽过来的 location 对象，避免依赖 availableLocations 列表
        await saveScheduleToBackend(targetDoctor, toDate, toShift, timeSlot, location);
      } else {
        console.warn('没有找到对应的时间段信息，无法保存排班');
        ElMessage.warning('请先将时间段卡片拖拽到上方“上午/下午”列');
      }
    }
  } catch (error) {
    console.error('分配地点后保存排班失败:', error);
  }
};

const handleTimeSlotDrop = (dragData, toDate, toShift) => {
  const { data: timeSlot } = dragData;
  
  // 如果拖拽到时段列（toDate为null），将时间段添加到时段列中
  if (toDate === null) {
    // 检查时间段是否已存在于该时段列中
    if (!timeSlotColumns.value[toShift].some(ts => ts.slot_id === timeSlot.slot_id)) {
      timeSlotColumns.value[toShift].push({ ...timeSlot });
      ElMessage.success(`已将时间段 "${timeSlot.slotName || timeSlot.slot_name}" 添加到 ${toShift} 时段列中`);
    } else {
      ElMessage.warning(`时间段 "${timeSlot.slotName || timeSlot.slot_name}" 已存在于 ${toShift} 时段列中`);
    }
    return;
  }
  
  // 如果拖拽到具体日期，则存储数据
  if (!activeSub.value) {
    console.log('No activeSub, returning');
    return;
  }
  
  if (!scheduleData.value[activeSub.value]) {
    scheduleData.value[activeSub.value] = [];
    console.log('Initialized scheduleData for:', activeSub.value);
  }

  let shiftEntry = scheduleData.value[activeSub.value].find(s => s.date === toDate && s.shift === toShift);
  if (!shiftEntry) {
    shiftEntry = { date: toDate, shift: toShift, doctors: [], timeSlots: [] };
    scheduleData.value[activeSub.value].push(shiftEntry);
    console.log('Created new shiftEntry:', shiftEntry);
  }

  // 检查时间段是否已存在
  if (!shiftEntry.timeSlots.some(ts => ts.slot_id === timeSlot.slot_id)) {
    shiftEntry.timeSlots.push({ ...timeSlot });
    console.log('Added timeSlot to shiftEntry:', shiftEntry.timeSlots);
    ElMessage.success(`已将时间段 "${timeSlot.slot_name}" 排班到 ${toDate} ${toShift}`);
  } else {
    ElMessage.warning(`时间段 "${timeSlot.slot_name}" 已在该班次中。`);
  }
};

// --- 数据操作方法 ---
const addDoctorToShift = (doctor, date, shift) => {
  if (!activeSub.value) return;
  if (!scheduleData.value[activeSub.value]) scheduleData.value[activeSub.value] = [];

  let shiftEntry = scheduleData.value[activeSub.value].find(s => s.date === date && s.shift === shift);
  if (!shiftEntry) {
    shiftEntry = { date, shift, doctors: [] };
    scheduleData.value[activeSub.value].push(shiftEntry);
  }

  if (!shiftEntry.doctors.some(d => d.id === doctor.id)) {
    shiftEntry.doctors.push({ ...doctor }); // 保留医生的原始地点信息
    ElMessage.success(`已将 ${doctor.name} 排班到 ${date} ${shift}`);
  } else {
    ElMessage.warning(`${doctor.name} 医生已在该班次中。`);
  }
};

const removeDoctorFromShift = async (doctor, date, shift, showMessage = true) => {
  if (!activeSub.value || !scheduleData.value[activeSub.value]) return;
  const shiftEntry = scheduleData.value[activeSub.value].find(s => s.date === date && s.shift === shift);
  if (shiftEntry) {
    const docIndex = shiftEntry.doctors.findIndex(d => d.id === doctor.id);
    if (docIndex > -1) {
      // 先从前端数据中移除
      shiftEntry.doctors.splice(docIndex, 1);
      
      // 尝试从后端删除排班记录
      try {
        // 获取当前时段的时间段信息（只使用手动拖拽的时间段）
        const timeSlotsForShift = getTimeSlotsForShift(shift);
        console.log(`获取时间段数据 for ${shift}:`, timeSlotsForShift);
        console.log(`所有时间段数据:`, timeSlots.value);
        
        // 只使用手动拖拽到时段列的时间段
        let timeSlot = null;
        if (timeSlotsForShift.length > 0) {
          timeSlot = timeSlotsForShift[0];
          console.log(`使用拖拽时间段 for ${shift}:`, timeSlot);
        } else {
          console.warn(`没有找到 ${shift} 时段的手动拖拽时间段，请先拖拽时间段卡片到时段列`);
          // 如果时间段列为空，提示用户先拖拽时间段
          if (showMessage) {
            ElMessage.warning(`请先拖拽时间段卡片到"${shift}"时段列，然后再删除医生排班`);
          }
          return;
        }
        
        // 获取医生分配的地点
        let location = null;
        if (doctor.location) {
          location = availableLocations.value.find(loc => loc.name === doctor.location);
          console.log(`查找医生地点 "${doctor.location}":`, location);
        }
        
        console.log(`时间段信息:`, timeSlot);
        console.log(`地点信息:`, location);
        
        if (timeSlot && location) {
          // 构建删除参数
          const deleteData = {
            doctorId: parseInt(doctor.id),
            slotId: parseInt(timeSlot.slotId || timeSlot.slot_id || 1),
            locationId: parseInt(location.location_id || 1),
            scheduleDate: date // 确保日期格式为 YYYY-MM-DD
          };
          
          console.log('删除排班参数:', deleteData);
          console.log('日期格式检查:', {
            originalDate: date,
            dateType: typeof date,
            isValidFormat: /^\d{4}-\d{2}-\d{2}$/.test(date)
          });
          
          // 调用后端删除接口
          await deleteScheduleByParams(deleteData);
          
          console.log(`✅ 成功从后端删除排班: ${doctor.name} - ${date} ${shift}`);
          
          if (showMessage) {
            ElMessage.success(`已取消 ${doctor.name} 在 ${date} ${shift} 的排班`);
          }
        } else {
          if (!timeSlot) {
            console.warn('无法删除后端排班记录：缺少时间段信息');
          if (showMessage) {
              ElMessage.warning('无法删除后端排班记录：缺少时间段信息');
            }
          } else if (!location) {
            console.warn('无法删除后端排班记录：医生未分配地点');
            if (showMessage) {
              ElMessage.warning('无法删除后端排班记录：医生未分配地点');
            }
          }
        }
      } catch (error) {
        console.error('删除后端排班记录失败:', error);
        if (showMessage) {
          ElMessage.error(`删除排班失败: ${error.message || '未知错误'}`);
        }
        // 如果后端删除失败，可以选择是否回滚前端状态
        // shiftEntry.doctors.splice(docIndex, 0, doctor);
      }
      
      console.log(`移除医生 ${doctor.name} 从 ${date} ${shift}`);
      
      // 移除医生后重新检测冲突
      setTimeout(() => {
        detectAllConflicts();
      }, 100);
    }
  }
};

// [新增] 清除医生地点的方法
const clearLocation = (doctor) => {
  const oldLocation = doctor.location;
  doctor.location = null;
  ElMessage.info(`已取消【${doctor.name}】医生的地点"${oldLocation}"`);
};

// 从时段列中移除时间段
const removeTimeSlotFromColumn = (timeSlot, shift) => {
  const timeSlotIndex = timeSlotColumns.value[shift].findIndex(ts => ts.slot_id === timeSlot.slot_id);
  if (timeSlotIndex > -1) {
    timeSlotColumns.value[shift].splice(timeSlotIndex, 1);
    ElMessage.success(`已从 ${shift} 时段列中移除时间段 "${timeSlot.slot_name}"`);
  }
};

// 清空时间段列的方法
const clearTimeSlotColumns = () => {
  timeSlotColumns.value = {
    '上午': [],
    '下午': []
  };
};

// --- 侧边栏选择逻辑 ---
const handleParentSelect = (index) => {
  activeParent.value = index;
  const parent = departments.value.find(p => p.id === index);
  if (parent) {
    if (parent.children && parent.children.length > 0) {
      activeSub.value = parent.children[0].id;
    } else {
      activeSub.value = parent.id;
    }
  } else {
    activeSub.value = null;
  }
  // 切换科室时清空时间段列
  clearTimeSlotColumns();
};

const handleSubSelect = async (id) => {
  activeSub.value = id;
  
  // 🔥 切换科室时清空该科室的旧数据，强制重新加载
  if (id && scheduleData.value[id]) {
    scheduleData.value[id] = [];
    console.log(`🗑️ 切换科室，清空旧数据: ${id}`);
  }
  
  // 加载选中科室的医生和办公地点数据
  if (id) {
    // 从科室ID中提取数字ID（去掉前缀 's' 或 'p'）
    const departmentId = id.replace(/^[sp]/, '');
    
    // 并行加载基础数据
    await Promise.all([
      loadDoctorsForDepartment(departmentId),
      loadLocationsForDepartment(departmentId)
    ]);
    
    // 根据当前视图类型加载相应数据
    if (currentView.value === 'week') {
      // 周视图：加载当前周数据
      await loadSchedulesFromBackend();
    } else {
      // 日视图/月视图：触发日历重新加载
      if (fullCalendar.value) {
        const calendarApi = fullCalendar.value.getApi();
        // 强制刷新事件，确保触发 handleCalendarDatesSet
        calendarApi.refetchEvents();
      }
    }
    
    // 延迟自动填充，确保基础数据已加载
    setTimeout(() => {
      autoFillScheduleData();
    }, 500);
  }
};

// [新增] 视图切换函数
const changeView = async (viewType) => {
  const previousView = currentView.value;
  currentView.value = viewType;
  
  if (fullCalendar.value) {
    const calendarApi = fullCalendar.value.getApi();
    
    if (viewType === 'day') {
      calendarApi.changeView('timeGridDay');
    } else if (viewType === 'week') {
      calendarApi.changeView('timeGridWeek');
      // 切换到周视图时，重新加载当前周数据
      if (previousView !== 'week' && activeSub.value) {
        await loadSchedulesFromBackend();
      }
    } else if (viewType === 'month') {
      // 🔥 关键修复：切换到月视图时，清空数据并重新加载
      if (previousView !== 'month' && activeSub.value) {
        console.log('🔄 从周/日视图切换到月视图，清空并重新加载完整月度数据...');
        // 清空当前科室数据，强制重新加载
        scheduleData.value[activeSub.value] = [];
      }
      calendarApi.changeView('dayGridMonth');
      // handleCalendarDatesSet 会在 changeView 后自动被触发，加载整个月的数据
    }
  }
};

// [新增] 日历事件处理函数
const handleEventClick = (info) => {
  const event = info.event;
  const conflicts = event.extendedProps.conflicts;
  
  let message = `医生: ${event.title}\n时间: ${event.startStr} - ${event.endStr}\n地点: ${event.extendedProps.location || '未分配'}`;
  
  if (conflicts?.hasConflict) {
    message += `\n\n⚠️ 冲突警告: ${conflicts.conflictDetails}`;
    if (conflicts.severity === 'error') {
      ElMessage.error(message);
    } else {
      ElMessage.warning(message);
    }
  } else {
    ElMessage.info(message);
  }
};

const handleDateClick = (info) => {
  // 日期点击事件
};

// 日历日期范围变化时加载数据
const handleCalendarDatesSet = async (dateInfo) => {
  console.log(`📅 日历日期范围变化: ${currentView.value} 视图, 范围: ${formatDate(dateInfo.start)} 到 ${formatDate(dateInfo.end)}`);
  
  // 仅在日历视图下加载数据（周视图有自己的加载机制）
  if (currentView.value === 'week') {
    console.log('⏭️ 周视图使用独立的加载机制，跳过 handleCalendarDatesSet');
    return;
  }
  
  if (!activeSub.value) {
    console.log('⚠️ 未选择科室，跳过数据加载');
    return;
  }
  
  try {
    const departmentId = activeSub.value.replace(/^[sp]/, '');
    
    // 根据视图类型确定日期范围
    let startDate, endDate;
    if (currentView.value === 'day') {
      // 日视图：加载前后各3天的数据
      const centerDate = new Date(dateInfo.start);
      startDate = new Date(centerDate);
      startDate.setDate(startDate.getDate() - 3);
      endDate = new Date(centerDate);
      endDate.setDate(endDate.getDate() + 3);
      console.log(`📆 日视图加载范围: ${formatDate(startDate)} 到 ${formatDate(endDate)}`);
    } else {
      // 月视图：使用日历提供的范围（通常包含上月末和下月初）
      startDate = dateInfo.start;
      endDate = dateInfo.end;
      console.log(`📆 月视图加载范围: ${formatDate(startDate)} 到 ${formatDate(endDate)}`);
      console.log(`📊 当前已有数据: ${scheduleData.value[activeSub.value]?.length || 0} 个时间段`);
    }
    
    const params = {
      departmentId: departmentId,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
      page: 0,
      size: 500
    };
    
    const response = await getSchedules(params);
    
    if (response && response.content) {
      const schedules = response.content;
      const key = activeSub.value;
      
      // 🔥 关键修复：合并数据而不是替换
      // 如果当前科室还没有数据，初始化为空数组
      if (!scheduleData.value[key]) {
        scheduleData.value[key] = [];
      }
      
      // 使用 Map 来去重和合并医生数据
      const scheduleMap = new Map();
      
      // 首先将现有数据放入 Map（保留之前加载的数据）
      scheduleData.value[key].forEach(item => {
        const mapKey = `${item.date}_${item.shift}`;
        if (!scheduleMap.has(mapKey)) {
          scheduleMap.set(mapKey, {
            date: item.date,
            shift: item.shift,
            doctors: [...item.doctors] // 深拷贝医生数组
          });
        }
      });
      
      // 然后合并新加载的数据
      schedules.forEach(schedule => {
        const shift = getShiftFromTimeSlot(schedule.slotName, schedule.startTime);
        const mapKey = `${schedule.scheduleDate}_${shift}`;
        
        const doctorInfo = {
          id: schedule.doctorId,
          name: schedule.doctorName,
          identifier: schedule.doctorIdentifier || (schedule.doctorId ? schedule.doctorId.toString() : ''),
          location: schedule.location
        };
        
        if (scheduleMap.has(mapKey)) {
          // 检查医生是否已存在，避免重复
          const existingDoctors = scheduleMap.get(mapKey).doctors;
          const doctorExists = existingDoctors.some(d => d.id === doctorInfo.id);
          if (!doctorExists) {
            existingDoctors.push(doctorInfo);
          }
        } else {
          scheduleMap.set(mapKey, {
            date: schedule.scheduleDate,
            shift: shift,
            doctors: [doctorInfo]
          });
        }
      });
      
      // 将 Map 转换回数组并更新数据
      scheduleData.value[key] = Array.from(scheduleMap.values());
      
      console.log(`✅ 月视图数据合并完成，共 ${schedules.length} 条新记录，合并后 ${scheduleData.value[key].length} 个时间段`);
    }
  } catch (error) {
    console.error('日历视图加载数据失败:', error);
  }
};

// 辅助函数：格式化日期为 YYYY-MM-DD
const formatDate = (date) => {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// [新增] 自定义事件内容渲染函数
const renderEventContent = (eventInfo) => {
  const event = eventInfo.event;
  const props = event.extendedProps;
  const view = eventInfo.view.type;
  
  // 提取医生名称和identifier
  const titleMatch = event.title.match(/^(.+?)\s*\(ID:(.+?)\)/);
  const doctorName = titleMatch ? titleMatch[1] : event.title.split(' (ID:')[0];
  const doctorIdentifier = titleMatch ? titleMatch[2] : props.doctorId;
  const location = props.location || '待分配地点';
  const departmentName = props.departmentName || '';
  
  // 根据不同视图返回不同的内容
  if (view === 'dayGridMonth') {
    // 月视图：只显示医生名称和identifier（简洁版）
    return {
      html: `
        <div class="fc-event-custom-month">
          <div class="fc-event-title-month">
            <strong>${doctorName}</strong>
            <span class="fc-event-identifier">(${doctorIdentifier})</span>
          </div>
        </div>
      `
    };
  } else if (view === 'timeGridDay' || view === 'timeGridWeek') {
    // 日视图和周视图：显示详细信息
    return {
      html: `
        <div class="fc-event-custom-day">
          <div class="fc-event-time">${eventInfo.timeText}</div>
          <div class="fc-event-title-day">
            <strong>${doctorName}</strong>
            <span class="fc-event-identifier">(${doctorIdentifier})</span>
          </div>
          ${departmentName ? `<div class="fc-event-department">${departmentName}</div>` : ''}
          <div class="fc-event-location">
            <i class="el-icon-location"></i> ${location}
          </div>
        </div>
      `
    };
  }
  
  return { html: event.title };
};

// [新增] 事件挂载后的处理函数
const handleEventDidMount = (info) => {
  const event = info.event;
  const el = info.el;
  
  // 添加tooltip显示完整信息
  const props = event.extendedProps;
  const titleMatch = event.title.match(/^(.+?)\s*\(ID:(.+?)\)/);
  const doctorName = titleMatch ? titleMatch[1] : event.title;
  const doctorIdentifier = titleMatch ? titleMatch[2] : props.doctorId;
  
  let tooltipContent = `
    医生：${doctorName}
    工号：${doctorIdentifier}
    科室：${props.departmentName || '未知'}
    地点：${props.location || '待分配'}
    班次：${props.shift || '未知'}
  `;
  
  if (props.hasConflict) {
    tooltipContent += '\n⚠️ 存在排班冲突';
  }
  
  el.setAttribute('title', tooltipContent.trim());
  
  // 添加自定义样式类
  if (props.hasConflict) {
    el.classList.add('has-conflict');
  }
};

// [新增] 日历拖拽事件处理
const handleCalendarDrop = (info) => {
  const { date, allDay, resource } = info;
  
  // 尝试从不同位置获取拖拽数据
  let dragData = info.draggedEl.dragData || 
                 info.draggedEl.__vueParentComponent?.ctx?.dragData || 
                 info.draggedEl.__vueParentComponent?.setupState?.dragData;
  
  // 如果没有找到，尝试从全局拖拽状态获取
  if (!dragData && window.currentDragData) {
    dragData = window.currentDragData;
  }
  
  if (!dragData) {
    console.log('未找到拖拽数据');
    return;
  }
  
  const { type, data } = dragData;
  const dropDate = date.toISOString().split('T')[0];
  
  if (type === 'timeSlot') {
    // 拖拽时间段到日历
    handleTimeSlotDropToCalendar(data, dropDate, date);
  } else if (type === 'location') {
    // 拖拽地点到日历（这里可以显示提示）
    ElMessage.info('请将地点拖拽到医生卡片上');
  }
};

// [新增] 处理时间段拖拽到日历
const handleTimeSlotDropToCalendar = (timeSlot, date, dropDateTime) => {
  // 根据拖拽时间确定班次
  const hour = dropDateTime.getHours();
  const shift = hour < 12 ? '上午' : '下午';
  
  // 添加到时间段列
  if (!timeSlotColumns.value[shift].find(slot => slot.slot_id === timeSlot.slot_id)) {
    timeSlotColumns.value[shift].push(timeSlot);
  }
  
  ElMessage.success(`已将 ${timeSlot.slot_name} 添加到 ${shift} 时间段`);
};

// [新增] 处理日历事件拖拽
const handleEventDrop = (info) => {
  const event = info.event;
  const newStart = event.start;
  const newDate = newStart.toISOString().split('T')[0];
  
  // 更新排班数据中的日期
  updateScheduleDate(event.id, newDate);
  
  ElMessage.success('排班已更新');
};

// [新增] 处理日历事件调整大小
const handleEventResize = (info) => {
  const event = info.event;
  ElMessage.success('排班时间已调整');
};

// [新增] 更新排班日期
const updateScheduleDate = (eventId, newDate) => {
  if (!activeSub.value) return;
  
  // 解析事件ID获取原始信息
  const [originalDate, shift, doctorId] = eventId.split('-');
  
  // 找到原始排班记录
  const originalSchedule = scheduleData.value[activeSub.value].find(
    s => s.date === originalDate && s.shift === shift
  );
  
  if (originalSchedule) {
    // 移除原始记录中的医生
    const doctorIndex = originalSchedule.doctors.findIndex(d => d.id === doctorId);
    if (doctorIndex > -1) {
      const doctor = originalSchedule.doctors[doctorIndex];
      originalSchedule.doctors.splice(doctorIndex, 1);
      
      // 添加到新日期的排班
      addDoctorToSchedule(newDate, shift, doctor);
    }
  }
};

// [新增] 添加医生到排班
const addDoctorToSchedule = (date, shift, doctor) => {
  if (!activeSub.value) return;
  
  // 确保排班数据结构存在
  if (!scheduleData.value[activeSub.value]) {
    scheduleData.value[activeSub.value] = [];
  }
  
  // 查找或创建当天的排班记录
  let daySchedule = scheduleData.value[activeSub.value].find(s => s.date === date && s.shift === shift);
  if (!daySchedule) {
    daySchedule = { date, shift, doctors: [] };
    scheduleData.value[activeSub.value].push(daySchedule);
  }
  
  // 添加医生（如果不存在）
  if (!daySchedule.doctors.find(d => d.id === doctor.id)) {
    daySchedule.doctors.push({ ...doctor });
  }
};

// [新增] 批量导入功能函数
// 下载模板文件
const downloadTemplate = () => {
  const templateData = [
    ['日期', '班次', '医生姓名', '医生职称', '办公地点', '时间段1', '时间段2', '时间段3', '时间段4'],
    ['2025/10/20', '上午', '张三', '主治医师', '门诊楼-201诊室', '08:00-08:30', '08:30-09:00', '09:00-09:30', '09:30-10:00'],
    ['2025/10/20', '下午', '李四', '副主任医师', '门诊楼-203诊室', '14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00'],
    ['', '', '', '', '', '', '', '', ''],
    ['说明：', '', '', '', '', '', '', '', ''],
    ['1. 日期格式：YYYY/MM/DD 或 YYYY-MM-DD', '', '', '', '', '', '', '', ''],
    ['2. 班次：上午/下午', '', '', '', '', '', '', '', ''],
    ['3. 时间段格式：HH:MM-HH:MM', '', '', '', '', '', '', '', ''],
    ['4. 办公地点请从可用地点中选择', '', '', '', '', '', '', '', '']
  ];
  
  // 创建工作簿
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.aoa_to_sheet(templateData);
  
  // 设置列宽
  worksheet['!cols'] = [
    { wch: 12 }, // 日期
    { wch: 8 },  // 班次
    { wch: 12 }, // 医生姓名
    { wch: 12 }, // 医生职称
    { wch: 20 }, // 办公地点
    { wch: 12 }, // 时间段1
    { wch: 12 }, // 时间段2
    { wch: 12 }, // 时间段3
    { wch: 12 }  // 时间段4
  ];
  
  // 添加工作表到工作簿
  XLSX.utils.book_append_sheet(workbook, worksheet, '排班模板');
  
  // 生成Excel文件并下载
  XLSX.writeFile(workbook, '排班导入模板.xlsx');
  
  ElMessage.success('Excel模板文件下载成功');
};

// 文件大小格式化
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 文件选择处理
const handleFileChange = (file) => {
  selectedFile.value = file.raw || file;
  importResult.value.show = false;
};

// 上传前验证
const beforeUpload = (file) => {
  const isValidType = ['application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'text/csv'].includes(file.type);
  const isLt10M = file.size / 1024 / 1024 < 10;

  if (!isValidType) {
    ElMessage.error('只能上传 Excel 或 CSV 文件!');
    return false;
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!');
    return false;
  }
  return true;
};

// 移除文件
const removeFile = () => {
  selectedFile.value = null;
  if (uploadRef.value) {
    uploadRef.value.clearFiles();
  }
  importResult.value.show = false;
  importProgress.value.show = false;
};

// Excel文件解析函数
const parseExcelFile = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target.result);
        const workbook = XLSX.read(data, { type: 'array' });
        
        console.log('Excel工作表:', workbook.SheetNames);
        
        // 获取第一个工作表
        const sheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[sheetName];
        
        // 将工作表转换为JSON数组
        const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
        
        console.log('Excel原始数据:', jsonData);
        
        // 转换为标准格式
        const result = [];
        for (let i = 0; i < jsonData.length; i++) {
          const row = jsonData[i];
          if (row && row.length >= 3 && row[0] && row[1] && row[2]) {
            // 过滤掉说明行
            if (typeof row[0] === 'string' && 
                !row[0].startsWith('说明') && 
                !row[0].startsWith('1.') && 
                !row[0].startsWith('2.') && 
                !row[0].startsWith('3.') && 
                !row[0].startsWith('4.')) {
              result.push(row);
            }
          }
        }
        
        console.log('处理后的数据:', result);
        resolve(result);
      } catch (error) {
        console.error('Excel解析错误:', error);
        reject(error);
      }
    };
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
};

// 处理导入
const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件');
    return;
  }

  importing.value = true;
  importProgress.value = {
    show: true,
    current: 0,
    total: 0,
    percentage: 0,
    status: 'success',
    message: '开始解析文件...'
  };

  try {
    // 解析文件
    const data = await parseExcelFile(selectedFile.value);
    
    // 添加调试信息
    console.log('解析的数据:', data);
    
    if (data.length === 0) {
      throw new Error('文件中没有有效的排班数据');
    }

    importProgress.value.total = data.length;
    importProgress.value.message = `开始导入 ${data.length} 条排班记录...`;

    let successCount = 0;
    let errorCount = 0;
    const errors = [];

    // 模拟导入过程
    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      importProgress.value.current = i + 1;
      importProgress.value.percentage = Math.round(((i + 1) / data.length) * 100);
      importProgress.value.message = `正在导入第 ${i + 1} 条记录...`;

      try {
        await importScheduleRow(row);
        successCount++;
      } catch (error) {
        errors.push(`第${i + 1}行: ${error.message}`);
        errorCount++;
      }

      // 模拟处理时间
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    // 显示导入结果
    if (errors.length === 0) {
      importResult.value = {
        show: true,
        type: 'success',
        title: '导入成功',
        message: `成功导入 ${successCount} 条排班记录`,
        details: []
      };
      ElMessage.success('排班信息导入成功！');
    } else {
      importResult.value = {
        show: true,
        type: 'error',
        title: '导入完成（有错误）',
        message: `成功导入 ${successCount} 条，失败 ${errorCount} 条`,
        details: errors
      };
      ElMessage.warning(`导入完成，但有 ${errorCount} 条记录失败`);
    }
    
    // [新增] 导入完成后立即进行冲突检测
    setTimeout(() => {
      detectAllConflicts();
      if (conflictData.value.hasConflicts) {
        ElMessage.warning(
          `检测到 ${conflictData.value.summary.total} 个排班冲突，` +
          `其中严重冲突 ${conflictData.value.summary.critical} 个，` +
          `警告 ${conflictData.value.summary.warning} 个。请检查红色/黄色高亮的排班。`
        );
      }
    }, 500);

  } catch (error) {
    importResult.value = {
      show: true,
      type: 'error',
      title: '导入失败',
      message: error.message,
      details: []
    };
    ElMessage.error('导入失败：' + error.message);
  } finally {
    importing.value = false;
    importProgress.value.show = false;
  }
};

// 导入单行排班数据
const importScheduleRow = async (row) => {
  let [date, shift, doctorName, doctorTitle, location, ...timeSlots] = row;
  
  // 添加调试信息
  console.log('处理行数据:', row);
  console.log('解析后的字段:', { date, shift, doctorName, doctorTitle, location });
  
  // 验证必要字段
  if (!date || !shift || !doctorName) {
    throw new Error('日期、班次、医生姓名不能为空');
  }

  // 处理日期格式 - 支持多种格式
  console.log('原始日期:', date);
  
  // 移除可能的空白字符和特殊字符
  date = date.toString().trim().replace(/[\s\u00A0]/g, '');
  
  // 处理各种日期格式
  if (date.includes('/')) {
    // 处理 YYYY/MM/DD 格式
    const dateParts = date.split('/');
    if (dateParts.length === 3) {
      const year = dateParts[0];
      const month = dateParts[1].padStart(2, '0');
      const day = dateParts[2].padStart(2, '0');
      date = `${year}-${month}-${day}`;
    }
  } else if (date.includes('-')) {
    // 处理 YYYY-MM-DD 格式
    const dateParts = date.split('-');
    if (dateParts.length === 3) {
      const year = dateParts[0];
      const month = dateParts[1].padStart(2, '0');
      const day = dateParts[2].padStart(2, '0');
      date = `${year}-${month}-${day}`;
    }
  } else if (date.length === 8 && /^\d{8}$/.test(date)) {
    // 处理 YYYYMMDD 格式
    const year = date.substring(0, 4);
    const month = date.substring(4, 6);
    const day = date.substring(6, 8);
    date = `${year}-${month}-${day}`;
  }
  
  console.log('处理后的日期:', date);
  
  // 验证日期格式
  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(date)) {
    console.error('日期验证失败:', date);
    throw new Error(`日期格式不正确: "${date}"，应为 YYYY-MM-DD 或 YYYY/MM/DD`);
  }

  // 验证班次
  if (!['上午', '下午'].includes(shift)) {
    throw new Error('班次只能是"上午"或"下午"');
  }

  // [新增] 检查是否存在冲突 - 防止同一医生同一时间在多个地点
  if (activeSub.value && scheduleData.value[activeSub.value]) {
    const existingSchedule = scheduleData.value[activeSub.value].find(
      s => s.date === date && s.shift === shift
    );
    
    if (existingSchedule) {
      // 检查是否已经有同名医生在这个时间段
      const duplicateDoctor = existingSchedule.doctors.find(d => d.name === doctorName);
      if (duplicateDoctor) {
        // 检查办公地点是否不同
        if (duplicateDoctor.location && location && duplicateDoctor.location !== location) {
          throw new Error(
            `医生 ${doctorName} 在 ${date} ${shift} 已被分配到 ${duplicateDoctor.location}，` +
            `不能再分配到 ${location}。同一医生不能同时在两个地方。`
          );
        } else if (duplicateDoctor.location && location && duplicateDoctor.location === location) {
          // 如果是同一地点，跳过（避免重复导入）
          console.log(`医生 ${doctorName} 在 ${date} ${shift} 已在 ${location}，跳过重复导入`);
          return;
        }
      }
      
      // 检查办公室是否已被其他医生占用（同一天同一办公室）
      if (location) {
        // 检查当天所有班次的所有医生
        const allSchedulesOnDate = scheduleData.value[activeSub.value].filter(s => s.date === date);
        for (const schedule of allSchedulesOnDate) {
          const doctorInSameOffice = schedule.doctors.find(
            d => d.location === location && d.name !== doctorName
          );
          if (doctorInSameOffice) {
            throw new Error(
              `办公室 ${location} 在 ${date} 已被医生 ${doctorInSameOffice.name} 占用，` +
              `不能再分配给医生 ${doctorName}。每个办公室每天只能分配给一个医生。`
            );
          }
        }
      }
    }
  }

  // 创建或获取医生
  const doctor = {
    id: `import_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    identifier: `IMP${Date.now().toString().slice(-6)}`, // 生成导入医生的工号
    name: doctorName,
    title: doctorTitle || '医生',
    location: location || null,
    gender: 'male' // 默认性别，实际项目中可以从数据中获取
  };

  // 添加到排班数据
  addDoctorToSchedule(date, shift, doctor);

  // 添加时间段
  const validTimeSlots = timeSlots.filter(slot => slot && slot.toString().includes('-'));
  for (const timeSlot of validTimeSlots) {
    const timeSlotStr = timeSlot.toString();
    const [startTime, endTime] = timeSlotStr.split('-');
    const slotData = {
      slot_id: `import_slot_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      slot_name: `${shift}${startTime}-${endTime}`,
      start_time: startTime,
      end_time: endTime
    };

    // 添加到时间段列
    if (!timeSlotColumns.value[shift].find(slot => slot.slot_name === slotData.slot_name)) {
      timeSlotColumns.value[shift].push(slotData);
    }
  }
};

// 清空导入数据
const clearImportData = () => {
  selectedFile.value = null;
  importResult.value.show = false;
  importProgress.value.show = false;
  if (uploadRef.value) {
    uploadRef.value.clearFiles();
  }
  ElMessage.info('已清空导入数据');
};

// [新增] 将排班数据转换为日历事件
const convertScheduleToEvents = () => {
  const events = [];
  
  if (!activeSub.value || !scheduleData.value[activeSub.value]) {
    calendarEvents.value = [];
    return;
  }

  const schedules = scheduleData.value[activeSub.value];
  
  schedules.forEach(schedule => {
    const { date, shift, doctors } = schedule;
    
    // 确定时间段
    const startTime = shift === '上午' ? '08:00:00' : '14:00:00';
    const endTime = shift === '上午' ? '12:00:00' : '18:00:00';
    
    // 获取科室名称
    const departmentName = selectedDepartmentName.value || '未知科室';
    
    // 为每个医生创建事件
    doctors.forEach((doctor, index) => {
      // 修复：移除时间偏移，所有同班次医生使用相同时间段
      const start = new Date(`${date}T${startTime}`);
      const end = new Date(`${date}T${endTime}`);
      
      // 根据冲突状态设置颜色（月视图用白底黑字+彩色边框，日/周视图用彩色背景）
      let backgroundColor = '#ffffff';  // 白色背景
      let borderColor = shift === '上午' ? '#67C23A' : '#409EFF';  // 绿色/蓝色边框
      let className = shift === '上午' ? 'shift-morning' : 'shift-afternoon';
      
      // 检查该医生在这个日期和班次是否有冲突
      const hasConflict = hasDoctorConflicts(doctor, date, shift);
      if (hasConflict) {
        const conflictClass = getDoctorConflictClass(doctor, date, shift);
        if (conflictClass === 'conflict-error') {
          backgroundColor = '#FEF0F0';  // 淡红色背景
          borderColor = '#F56C6C';
          className += ' conflict-critical';
        } else if (conflictClass === 'conflict-warning') {
          backgroundColor = '#FDF6EC';  // 淡黄色背景
          borderColor = '#E6A23C';
          className += ' conflict-warning';
        }
      }
      
      events.push({
        id: `${date}-${shift}-${doctor.id}`,
        title: `${doctor.name} (ID:${doctor.identifier || doctor.id}) - ${departmentName}`,
        start: start.toISOString(),
        end: end.toISOString(),
        backgroundColor,
        borderColor,
        className,
        extendedProps: {
          doctorId: doctor.id,
          doctorTitle: doctor.title || '医生',
          location: doctor.location,
          shift: shift,
          departmentId: activeSub.value,
          departmentName: departmentName,
          hasConflict: hasConflict
        }
      });
    });
  });
  
  calendarEvents.value = events;
};

// [新增] 完整的冲突检测系统
const detectAllConflicts = () => {
  if (!activeSub.value || !scheduleData.value[activeSub.value]) {
    conflictData.value = {
      hasConflicts: false,
      conflicts: [],
      summary: { total: 0, critical: 0, warning: 0 }
    };
    return;
  }

  // 创建深拷贝，避免修改原始响应式数据
  const schedules = JSON.parse(JSON.stringify(scheduleData.value[activeSub.value]));
  const conflicts = [];
  
  console.log('开始冲突检测，排班数据:', schedules);
  
  // 1. 检测医生重复排班冲突（同一医生同一时间段多次排班）
  const doctorConflicts = detectDoctorDoubleBooking(schedules);
  console.log('医生重复排班冲突:', doctorConflicts);
  conflicts.push(...doctorConflicts);
  
  // 2. 检测办公室冲突（同一办公室同一时间段被多个医生占用）
  const officeConflicts = detectOfficeConflicts(schedules);
  console.log('办公室冲突:', officeConflicts);
  conflicts.push(...officeConflicts);
  
  // 3. 检测医生跨办公室冲突（同一医生同一时间段在不同办公室）
  conflicts.push(...detectDoctorMultiOfficeConflicts(schedules));
  
  // 4. 检测工作时间冲突（医生连续工作时间过长）
  conflicts.push(...detectWorkDurationConflicts(schedules));
  
  // 5. 检测医生休息时间冲突（医生没有足够的休息时间）
  conflicts.push(...detectRestTimeConflicts(schedules));
  
  // 6. 检测时间段重叠冲突
  conflicts.push(...detectTimeSlotOverlapConflicts(schedules));

  console.log('所有冲突:', conflicts);

  // 更新冲突数据
  conflictData.value = {
    hasConflicts: conflicts.length > 0,
    conflicts: conflicts,
    summary: {
      total: conflicts.length,
      critical: conflicts.filter(c => c.severity === 'critical').length,
      warning: conflicts.filter(c => c.severity === 'warning').length
    }
  };
};

// [新增] 检测医生重复排班冲突
const detectDoctorDoubleBooking = (schedules) => {
  const conflicts = [];
  const doctorScheduleMap = new Map();
  
  schedules.forEach(schedule => {
    const { date, shift, doctors } = schedule;
    const timeKey = `${date}-${shift}`;
    
    doctors.forEach(doctor => {
      const doctorKey = `${doctor.id}-${timeKey}`;
      
      if (doctorScheduleMap.has(doctorKey)) {
        const existingSchedule = doctorScheduleMap.get(doctorKey);
        conflicts.push({
          type: 'doctor_double_booking',
          severity: 'critical',
          title: '医生重复排班',
          description: `医生 ${doctor.name} 在 ${date} ${shift} 被重复排班`,
          details: [
            `医生: ${doctor.name}`,
            `时间: ${date} ${shift}`,
            `地点1: ${existingSchedule.location || '未分配'}`,
            `地点2: ${doctor.location || '未分配'}`
          ],
          doctorId: doctor.id,
          date: date,
          shift: shift
        });
      } else {
        doctorScheduleMap.set(doctorKey, { ...doctor, date, shift });
      }
    });
  });
  
  return conflicts;
};

// [新增] 检测办公室冲突 - 修改为同一日期同一时间段同一办公室被多人使用即为冲突
const detectOfficeConflicts = (schedules) => {
  const conflicts = [];
  const officeTimeSlotMap = new Map(); // 改为按日期、时间段和办公室分组
  
  console.log('开始检测办公室冲突，排班数据:', schedules);
  
  schedules.forEach(schedule => {
    const { date, shift, doctors } = schedule;
    
    console.log(`检查 ${date} ${shift} 的医生:`, doctors);
    
    doctors.forEach(doctor => {
      if (doctor.location) {
        // 改为按日期、时间段和办公室分组
        const officeDateKey = `${doctor.location}-${date}-${shift}`;
        
        console.log(`检查医生 ${doctor.name} 在办公室 ${doctor.location} 日期 ${date} 时间段 ${shift}`);
        
        if (officeTimeSlotMap.has(officeDateKey)) {
          const existingDoctors = officeTimeSlotMap.get(officeDateKey);
          
          // 检查是否已经记录了这个医生
          const alreadyRecorded = existingDoctors.some(existing => existing.id === doctor.id);
          
          if (!alreadyRecorded) {
            existingDoctors.push({ ...doctor, date, shift });
            console.log(`发现办公室冲突: ${doctor.location} 在 ${date} ${shift} 被多个医生使用`);
            
            // 为所有使用这个办公室的医生创建冲突记录
            existingDoctors.forEach(existingDoctor => {
              conflicts.push({
                type: 'office_conflict',
                severity: 'critical',
                title: '办公室冲突',
                description: `办公室 ${doctor.location} 在 ${date} ${shift} 被多个医生使用`,
                details: [
                  `办公室: ${doctor.location}`,
                  `日期: ${date}`,
                  `时间段: ${shift}`,
                  `使用医生: ${existingDoctors.map(d => d.name).join(', ')}`,
                  `建议: 每个办公室在同一时间段只能分配给一个医生`
                ],
                location: doctor.location,
                date: date,
                shift: shift,
                doctorIds: existingDoctors.map(d => d.id),
                allDoctors: existingDoctors
              });
            });
          }
        } else {
          officeTimeSlotMap.set(officeDateKey, [{ ...doctor, date, shift }]);
          console.log(`记录医生 ${doctor.name} 在办公室 ${doctor.location} 时间段 ${shift}`);
        }
      }
    });
  });
  
  console.log('办公室冲突检测完成，发现冲突:', conflicts);
  return conflicts;
};

// [新增] 检测医生跨办公室冲突
const detectDoctorMultiOfficeConflicts = (schedules) => {
  const conflicts = [];
  const doctorOfficeMap = new Map();
  
  schedules.forEach(schedule => {
    const { date, shift, doctors } = schedule;
    const timeKey = `${date}-${shift}`;
    
    doctors.forEach(doctor => {
      if (doctor.location) {
        const doctorKey = `${doctor.id}-${timeKey}`;
        
        if (doctorOfficeMap.has(doctorKey)) {
          const existingLocation = doctorOfficeMap.get(doctorKey);
          conflicts.push({
            type: 'doctor_multi_office',
            severity: 'critical',
            title: '医生跨办公室冲突',
            description: `医生 ${doctor.name} 在 ${date} ${shift} 被分配到多个办公室`,
            details: [
              `医生: ${doctor.name}`,
              `时间: ${date} ${shift}`,
              `办公室1: ${existingLocation}`,
              `办公室2: ${doctor.location}`
            ],
            doctorId: doctor.id,
            date: date,
            shift: shift,
            locations: [existingLocation, doctor.location]
          });
        } else {
          doctorOfficeMap.set(doctorKey, doctor.location);
        }
      }
    });
  });
  
  return conflicts;
};

// [新增] 检测工作时间冲突（连续工作时间过长）
const detectWorkDurationConflicts = (schedules) => {
  const conflicts = [];
  const doctorWorkMap = new Map();
  
  // 收集每个医生的工作安排
  schedules.forEach(schedule => {
    const { date, shift, doctors } = schedule;
    doctors.forEach(doctor => {
      if (!doctorWorkMap.has(doctor.id)) {
        doctorWorkMap.set(doctor.id, []);
      }
      doctorWorkMap.get(doctor.id).push({ date, shift, doctor });
    });
  });
  
  // 检查每个医生的工作安排
  doctorWorkMap.forEach((workList, doctorId) => {
    // 按日期排序
    workList.sort((a, b) => new Date(a.date) - new Date(b.date));
    
    // 检查连续工作天数
    let consecutiveDays = 1;
    let maxConsecutiveDays = 1;
    
    for (let i = 1; i < workList.length; i++) {
      const prevDate = new Date(workList[i-1].date);
      const currDate = new Date(workList[i].date);
      const dayDiff = (currDate - prevDate) / (1000 * 60 * 60 * 24);
      
      if (dayDiff === 1) {
        consecutiveDays++;
        maxConsecutiveDays = Math.max(maxConsecutiveDays, consecutiveDays);
      } else {
        consecutiveDays = 1;
      }
    }
    
    // 如果连续工作超过7天，标记为冲突
    if (maxConsecutiveDays > 7) {
      conflicts.push({
        type: 'work_duration_conflict',
        severity: 'warning',
        title: '工作时间冲突',
        description: `医生 ${workList[0].doctor.name} 连续工作 ${maxConsecutiveDays} 天，建议休息`,
        details: [
          `医生: ${workList[0].doctor.name}`,
          `连续工作天数: ${maxConsecutiveDays} 天`,
          `建议: 连续工作不应超过7天，请安排休息时间`
        ],
        doctorId: doctorId,
        consecutiveDays: maxConsecutiveDays
      });
    }
  });
  
  return conflicts;
};

// [新增] 检测休息时间冲突
const detectRestTimeConflicts = (schedules) => {
  const conflicts = [];
  return conflicts; // 暂时简化实现
};

// [新增] 检测时间段重叠冲突
const detectTimeSlotOverlapConflicts = (schedules) => {
  const conflicts = [];
  return conflicts; // 暂时简化实现
};

// [新增] 获取医生冲突样式类 - 修改为持久检查，显示所有冲突
const getDoctorConflictClass = (doctor, date, shift) => {
  const relevantConflicts = conflictData.value.conflicts.filter(conflict => {
    switch (conflict.type) {
      case 'doctor_double_booking':
      case 'doctor_multi_office':
        // 特定日期和班次的冲突
        return conflict.doctorId === doctor.id && 
               conflict.date === date && 
               conflict.shift === shift;
      case 'office_conflict':
        // 办公室冲突：只要日期匹配且医生在冲突列表中即可
        return conflict.date === date && 
               conflict.doctorIds && conflict.doctorIds.includes(doctor.id);
      case 'work_duration_conflict':
      case 'rest_time_conflict':
      case 'time_slot_overlap':
        // 全局性冲突：影响该医生的所有排班
        return conflict.doctorId === doctor.id || 
               (conflict.doctorIds && conflict.doctorIds.includes(doctor.id));
      default:
        return false;
    }
  });

  if (relevantConflicts.length > 0) {
    const hasCritical = relevantConflicts.some(c => c.severity === 'critical');
    return hasCritical ? 'conflict-error' : 'conflict-warning';
  }
  return '';
};

// [新增] 检查医生是否有冲突 - 修改为持久检查，显示所有冲突
const hasDoctorConflicts = (doctor, date, shift) => {
  const hasConflict = conflictData.value.conflicts.some(conflict => {
    switch (conflict.type) {
      case 'doctor_double_booking':
      case 'doctor_multi_office':
        // 特定日期和班次的冲突
        return conflict.doctorId === doctor.id && 
               conflict.date === date && 
               conflict.shift === shift;
      case 'office_conflict':
        // 办公室冲突：只要日期匹配且医生在冲突列表中即可
        return conflict.date === date && 
               conflict.doctorIds && conflict.doctorIds.includes(doctor.id);
      case 'work_duration_conflict':
      case 'rest_time_conflict':
      case 'time_slot_overlap':
        // 全局性冲突：影响该医生的所有排班，全部显示冲突图标
        return conflict.doctorId === doctor.id || 
               (conflict.doctorIds && conflict.doctorIds.includes(doctor.id));
      default:
        return false;
    }
  });
  
  return hasConflict;
};

// [新增] 检查时间段卡片是否匹配班次
const isTimeSlotMatchShift = (timeSlot, shift) => {
  if (!timeSlot) return true;
  
  // 获取时间段名称，支持多种字段名
  const slotName = (timeSlot.slotName || timeSlot.slot_name || '').toLowerCase();
  const shiftLower = shift.toLowerCase();
  
  // 如果时间段名称为空，默认允许
  if (!slotName) return true;
  
  // 检查时间段名称是否包含班次信息
  if (slotName.includes('上午') && shiftLower === '下午') {
    return false;
  }
  if (slotName.includes('下午') && shiftLower === '上午') {
    return false;
  }
  
  // 检查时间段名称是否包含"am"或"pm"标识
  if (slotName.includes('am') && shiftLower === '下午') {
    return false;
  }
  if (slotName.includes('pm') && shiftLower === '上午') {
    return false;
  }
  
  // 检查时间段名称是否包含"morning"或"afternoon"标识
  if (slotName.includes('morning') && shiftLower === '下午') {
    return false;
  }
  if (slotName.includes('afternoon') && shiftLower === '上午') {
    return false;
  }
  
  return true; // 默认允许，对于没有明确班次标识的时间段
};

// [新增] 获取医生冲突图标样式类
const getDoctorConflictIconClass = (doctor, date, shift) => {
  const relevantConflicts = conflictData.value.conflicts.filter(conflict => {
    switch (conflict.type) {
      case 'doctor_double_booking':
      case 'doctor_multi_office':
        // 特定日期和班次的冲突
        return conflict.doctorId === doctor.id && 
               conflict.date === date && 
               conflict.shift === shift;
      case 'office_conflict':
        // 办公室冲突：只要日期匹配且医生在冲突列表中即可
        return conflict.date === date && 
               conflict.doctorIds && conflict.doctorIds.includes(doctor.id);
      case 'work_duration_conflict':
      case 'rest_time_conflict':
      case 'time_slot_overlap':
        // 全局性冲突：影响该医生的所有排班
        return conflict.doctorId === doctor.id || 
               (conflict.doctorIds && conflict.doctorIds.includes(doctor.id));
      default:
        return false;
    }
  });

  if (relevantConflicts.length > 0) {
    const hasCritical = relevantConflicts.some(c => c.severity === 'critical');
    return hasCritical ? 'conflict-error-icon' : 'conflict-warning-icon';
  }
  return '';
};

// [新增] 显示冲突详情
const showConflictDetails = (doctor, date, shift) => {
  const relevantConflicts = conflictData.value.conflicts.filter(conflict => {
    switch (conflict.type) {
      case 'doctor_double_booking':
      case 'doctor_multi_office':
        // 特定日期和班次的冲突
        return conflict.doctorId === doctor.id && 
               conflict.date === date && 
               conflict.shift === shift;
      case 'office_conflict':
        // 办公室冲突：只要日期匹配且医生在冲突列表中即可
        return conflict.date === date && 
               conflict.doctorIds && conflict.doctorIds.includes(doctor.id);
      case 'work_duration_conflict':
      case 'rest_time_conflict':
      case 'time_slot_overlap':
        // 全局性冲突：影响该医生的所有排班
        return conflict.doctorId === doctor.id || 
               (conflict.doctorIds && conflict.doctorIds.includes(doctor.id));
      default:
        return false;
    }
  });

  if (relevantConflicts.length > 0) {
    const conflictTypes = relevantConflicts.map(c => c.title).join('、');
    const severity = relevantConflicts.some(c => c.severity === 'critical') ? 'critical' : 'warning';
    
    let message = `医生: ${doctor.name} (工号:${doctor.identifier || doctor.id})\n冲突类型: ${conflictTypes}\n\n详细信息:\n`;
    relevantConflicts.forEach(conflict => {
      message += `• ${conflict.description}\n`;
      if (conflict.details && conflict.details.length > 0) {
        conflict.details.forEach(detail => {
          message += `  - ${detail}\n`;
        });
      }
    });
    
    if (severity === 'critical') {
      ElMessage.error(message);
    } else {
      ElMessage.warning(message);
    }
  }
};

// [新增] 调试冲突函数
const debugConflicts = () => {
  console.log('=== 调试冲突信息 ===');
  console.log('当前选中的科室:', activeSub.value);
  console.log('排班数据:', scheduleData.value);
  console.log('冲突数据:', conflictData.value);
  
  if (activeSub.value && scheduleData.value[activeSub.value]) {
    const schedules = scheduleData.value[activeSub.value];
    console.log('当前科室的排班:', schedules);
    
    // 检查每个排班
    schedules.forEach((schedule, index) => {
      console.log(`排班 ${index}:`, schedule);
      if (schedule.doctors) {
        schedule.doctors.forEach((doctor, docIndex) => {
          console.log(`  医生 ${docIndex}:`, doctor);
        });
      }
    });
  }
  
  ElMessage.info('调试信息已输出到控制台，请按F12查看');
};

// [新增] 监听 activeSub 变化，自动更新日历事件
watch(activeSub, () => {
  convertScheduleToEvents();
});

// [新增] 监听 scheduleData 变化，自动更新日历事件
watch(() => scheduleData.value, () => {
  convertScheduleToEvents();
}, { deep: true });

// [新增] 单独监听 scheduleData 变化进行冲突检测，避免递归
let conflictDetectionTimeout = null;
let updateTimeout = null;
watch(() => scheduleData.value, () => {
  // 使用防抖避免频繁触发冲突检测
  if (conflictDetectionTimeout) {
    clearTimeout(conflictDetectionTimeout);
  }
  conflictDetectionTimeout = setTimeout(() => {
    console.log('排班数据发生变化，重新检测冲突...');
    detectAllConflicts();
  }, 300); // 减少到300ms防抖，提高响应速度
}, { deep: true });

// [新增] 监听时间段列变化，防抖更新
watch(() => timeSlotColumns.value, () => {
  if (updateTimeout) {
    clearTimeout(updateTimeout);
  }
  updateTimeout = setTimeout(() => {
    console.log('时间段列发生变化，触发UI更新');
    // 强制触发响应式更新
    nextTick(() => {
      console.log('UI更新完成');
    });
  }, 100); // 100ms防抖，快速响应UI变化
}, { deep: true });

// 加载科室数据
const loadDepartments = async () => {
  try {
    loadingDepartments.value = true;
    console.log('开始获取科室数据...');
    
    // 获取所有父科室
    const parentResponse = await getAllParentDepartments();
    console.log('父科室API响应:', parentResponse);
    
    if (parentResponse && Array.isArray(parentResponse)) {
      const parentDepartments = parentResponse;
      console.log('父科室数据:', parentDepartments);
      
      // 为每个父科室获取子科室
      const departmentsWithChildren = await Promise.all(
        parentDepartments.map(async (parent) => {
          try {
            const childrenResponse = await getDepartmentsByParentId(parent.parentDepartmentId);
            console.log(`父科室 ${parent.name} 的子科室响应:`, childrenResponse);
            
            const children = childrenResponse && Array.isArray(childrenResponse) ? childrenResponse : [];
            console.log(`父科室 ${parent.name} 的子科室:`, children);
            
            return {
              id: `p${parent.parentDepartmentId}`,
              name: parent.name,
              description: parent.description,
              parentDepartmentId: parent.parentDepartmentId,
              children: children.map(child => ({
                id: `s${child.departmentId}`,
                name: child.name,
                description: child.description,
                departmentId: child.departmentId,
                parentDepartmentId: child.parentDepartmentId
              }))
            };
          } catch (error) {
            console.error(`获取父科室 ${parent.name} 的子科室失败:`, error);
            return {
              id: `p${parent.parentDepartmentId}`,
              name: parent.name,
              description: parent.description,
              parentDepartmentId: parent.parentDepartmentId,
              children: []
            };
          }
        })
      );
      
      departments.value = departmentsWithChildren;
      console.log('最终科室数据结构:', departments.value);
      
      // 初始进入页面不选中任何科室，等待用户手动选择
      
    } else {
      console.error('获取父科室数据失败:', parentResponse);
      ElMessage.warning('获取科室数据失败，使用默认数据');
      loadFallbackDepartments();
    }
  } catch (error) {
    console.error('获取科室数据出错:', error);
    ElMessage.warning('网络错误，使用默认科室数据');
    loadFallbackDepartments();
  } finally {
    loadingDepartments.value = false;
  }
};

// 备用科室数据
const loadFallbackDepartments = () => {
  departments.value = [
    { id: 'p1', name: '内科', children: [
        { id: 's1-1', name: '呼吸内科' }, { id: 's1-2', name: '心血管科' }
      ]},
    { id: 'p2', name: '外科', children: [ { id: 's2-1', name: '普外科' } ]},
    { id: 'p3', name: '妇产科', children: [] },
  ];
  if (departments.value.length > 0) {
    handleParentSelect(departments.value[0].id);
  }
};

// 加载选中科室的医生数据
const loadDoctorsForDepartment = async (departmentId) => {
  if (!departmentId) {
    doctorsData.value = {};
    return;
  }

  try {
    loadingDoctors.value = true;
    
    const response = await getDoctorsByDepartmentId(departmentId);
    
    if (response && Array.isArray(response)) {
      // 转换医生数据格式，适配前端显示
      const doctors = response.map(doctor => ({
        id: doctor.doctorId || doctor.id,
        name: doctor.fullName || doctor.name,
        identifier: doctor.identifier,
        title: doctor.title || '医生',
        gender: doctor.gender || 'male', // 默认性别
        specialty: doctor.specialty || '',
        phoneNumber: doctor.phoneNumber || ''
      }));
      
      // 将医生数据存储到对应的科室ID下
      doctorsData.value[departmentId] = doctors;
      
    } else {
      console.error('获取科室医生数据失败:', response);
      doctorsData.value[departmentId] = [];
    }
  } catch (error) {
    console.error('获取科室医生数据出错:', error);
    doctorsData.value[departmentId] = [];
    ElMessage.warning('获取医生数据失败');
  } finally {
    loadingDoctors.value = false;
  }
};

// 加载选中科室的办公地点数据
const loadLocationsForDepartment = async (departmentId) => {
  if (!departmentId) {
    console.log('科室ID为空，清空地点数据');
    availableLocations.value = [];
    return;
  }

  try {
    console.log('开始加载科室地点数据，科室ID:', departmentId);
    const response = await getLocationsByDepartmentId(departmentId);
    console.log('地点API响应:', response);
    console.log('响应类型:', typeof response, '是否为数组:', Array.isArray(response));
    
    if (response && Array.isArray(response)) {
      // 直接使用后端返回的完整Location信息
      const locations = response.map(location => ({
        location_id: location.locationId, // 使用真实的数据库ID
        name: location.locationName,
        building: location.building || '门诊楼',
        floor: location.floorLevel ? `${location.floorLevel}层` : '一层',
        room_number: location.roomNumber || '001'
      }));
      
      availableLocations.value = locations;
      console.log('成功加载的地点数据:', locations);
      console.log('availableLocations.value 长度:', availableLocations.value.length);
    } else {
      console.error('获取科室办公地点数据失败:', response);
      console.error('响应不是数组或为空');
      availableLocations.value = [];
    }
  } catch (error) {
    console.error('获取科室办公地点数据出错:', error);
    availableLocations.value = [];
    ElMessage.warning('获取办公地点数据失败');
  }
};

// 从后端加载排班数据
const loadSchedulesFromBackend = async () => {
  try {
    console.log('开始从后端加载排班数据...');
    
    // 获取当前选中的科室ID
    const departmentId = activeSub.value ? activeSub.value.replace(/^[sp]/, '') : null;
    if (!departmentId) {
      console.log('没有选中科室，跳过排班数据加载');
      return;
    }
    
    // 构建查询参数
    const params = {
      departmentId: departmentId,
      startDate: getCurrentWeekStart(),
      endDate: getCurrentWeekEnd(),
      page: 0,
      size: 100
    };
    
    console.log('排班查询参数:', params);
    
    const response = await getSchedules(params);
    console.log('排班数据API响应:', response);
    
    if (response && response.content) {
      // 转换后端数据格式为前端格式
      const schedules = response.content;
      
      console.log('后端返回的排班数据:', schedules);
      console.log('当前选中的科室ID:', activeSub.value);
      
      // 🔥 关键修复：合并数据而不是替换（与月视图保持一致）
      schedules.forEach(schedule => {
        const key = `s${schedule.departmentId}`;
        console.log('处理排班记录:', schedule, '键:', key);
        
        // 确保科室数据存在
        if (!scheduleData.value[key]) {
          scheduleData.value[key] = [];
        }
        
        // 检查location是否在当前可用地点列表中
        let validLocation = null;
        if (schedule.location) {
          // 尝试在可用地点列表中找到匹配的地点
          const matchedLocation = availableLocations.value.find(loc => loc.name === schedule.location);
          if (matchedLocation) {
            validLocation = schedule.location;
            console.log(`✅ 找到匹配的地点: ${schedule.location}`);
          } else {
            console.warn(`⚠️ 排班中的地点 "${schedule.location}" 不在当前可用地点列表中`);
            console.log('当前可用地点:', availableLocations.value.map(loc => loc.name));
            validLocation = null;
            console.log(`❌ 排班地点无效，不加载该排班记录`);
          }
        }
        
        // 如果地点无效，跳过该排班记录
        if (schedule.location && !validLocation) {
          console.log(`跳过无效地点的排班记录: ${schedule.doctorName} - ${schedule.location}`);
          return; // 跳过当前排班记录
        }
        
        const doctorInfo = {
          id: schedule.doctorId,
          name: schedule.doctorName,
          identifier: schedule.doctorIdentifier || (schedule.doctorId ? schedule.doctorId.toString() : ''),
          location: validLocation
        };
        
        const shift = getShiftFromTimeSlot(schedule.slotName, schedule.startTime);
        
        // 查找是否已存在相同日期和时段的记录
        const existingIndex = scheduleData.value[key].findIndex(item => 
          item.date === schedule.scheduleDate && item.shift === shift
        );
        
        if (existingIndex >= 0) {
          // 检查医生是否已存在，避免重复
          const existingDoctors = scheduleData.value[key][existingIndex].doctors;
          const doctorExists = existingDoctors.some(d => d.id === doctorInfo.id);
          if (!doctorExists) {
            existingDoctors.push(doctorInfo);
            console.log(`✅ 添加医生到现有记录: ${doctorInfo.name} - ${schedule.scheduleDate} ${shift}`);
          } else {
            console.log(`⏭️ 医生已存在，跳过: ${doctorInfo.name} - ${schedule.scheduleDate} ${shift}`);
          }
        } else {
          // 创建新记录
          scheduleData.value[key].push({
            date: schedule.scheduleDate,
            shift: shift,
            doctors: [doctorInfo]
          });
          console.log(`✅ 创建新排班记录: ${doctorInfo.name} - ${schedule.scheduleDate} ${shift}`);
        }
      });
      
      console.log('排班数据加载完成:', scheduleData.value);
      console.log('当前选中的科室数据:', scheduleData.value[activeSub.value]);
      
      // [新增] 加载排班数据后自动填充时间段
      await autoFillTimeSlotsFromSchedules(schedules);
      
    } else {
      console.log('没有获取到排班数据');
    }
    
  } catch (error) {
    console.error('加载排班数据失败:', error);
    ElMessage.warning('加载排班数据失败');
  }
};

// 根据时间段名称或时间判断班次
const getShiftFromTimeSlot = (slotName, startTime) => {
  // 🔥 优先使用时间判断（更可靠）
  if (startTime) {
    const time = typeof startTime === 'string' ? startTime : startTime.toString();
    // 如果时间 >= 12:00，就是下午
    if (time >= '12:00') {
      console.log(`⏰ 根据时间判断为下午: ${time}`);
      return '下午';
    } else {
      console.log(`⏰ 根据时间判断为上午: ${time}`);
      return '上午';
    }
  }
  
  // 🔥 备用：使用名称判断
  if (!slotName) return '上午';
  const name = slotName.toLowerCase();
  if (name.includes('下午') || name.includes('pm') || name.includes('afternoon')) {
    console.log(`📝 根据名称判断为下午: ${slotName}`);
    return '下午';
  }
  console.log(`📝 根据名称判断为上午: ${slotName}`);
  return '上午';
};

// 获取当前周的开始日期
const getCurrentWeekStart = () => {
  // 🔥 修复：使用视图显示的周一日期，而不是当前真实日期
  const monday = new Date(currentMonday.value);
  return monday.toISOString().split('T')[0];
};

// 获取当前周的结束日期
const getCurrentWeekEnd = () => {
  // 🔥 修复：基于视图显示的周一计算周日
  const monday = new Date(currentMonday.value);
  const sunday = new Date(monday.getTime() + 6 * 24 * 60 * 60 * 1000); // 加6天
  return sunday.toISOString().split('T')[0];
};

// 保存排班到后端
const saveScheduleToBackend = async (doctor, date, shift, timeSlot, location) => {
  // 设置保存状态
  scheduleStatus.value.saving = true;
  scheduleStatus.value.error = null;
  
  try {
    // 构建排班数据
    const scheduleData = {
      doctorId: parseInt(doctor.id), // 确保是整数
      scheduleDate: date, // 日期字符串，格式：YYYY-MM-DD
      slotId: (() => {
        // 优先使用数字格式的slotId
        if (timeSlot.slotId && !isNaN(parseInt(timeSlot.slotId))) {
          return parseInt(timeSlot.slotId);
        }
        // 其次使用slot_id，但需要检查是否为有效数字
        if (timeSlot.slot_id) {
          const parsed = parseInt(timeSlot.slot_id);
          if (!isNaN(parsed)) {
            return parsed;
          }
        }
        // 如果都无效，返回默认值1
        console.warn('时间段ID无效，使用默认值1:', timeSlot);
        return 1;
      })(),
      locationId: parseInt(location?.location_id), // 确保是整数
      totalSlots: 10, // 默认总号源数
      fee: "5.00", // 使用字符串格式，后端会转换为BigDecimal
      remarks: `排班：${doctor.name} - ${timeSlot.slotName || timeSlot.slot_name} - ${location?.name || '未分配地点'}`
    };

    console.log('保存排班数据:', scheduleData);
    console.log('时间段信息调试:', {
      timeSlot: timeSlot,
      slotId: timeSlot?.slotId,
      slot_id: timeSlot?.slot_id,
      parsedSlotId: scheduleData.slotId
    });
    console.log('使用的地点信息:', {
      locationId: scheduleData.locationId,
      locationName: location?.name || '未分配地点',
      doctorName: doctor.name,
      doctorAssignedLocation: doctor.location
    });
    
    const response = await createSchedule(scheduleData);
    console.log('排班保存响应:', response);
    console.log('响应类型:', typeof response);
    console.log('响应结构:', JSON.stringify(response, null, 2));
    
    // 检查响应状态 - 支持多种响应格式
    let scheduleId = null;
    if (response) {
      // 直接响应格式
      if (response.scheduleId) {
        scheduleId = response.scheduleId;
        console.log('找到scheduleId (直接):', scheduleId);
      }
      // 嵌套data格式
      else if (response.data && response.data.scheduleId) {
        scheduleId = response.data.scheduleId;
        console.log('找到scheduleId (data):', scheduleId);
      }
      // 检查其他可能的字段名
      else if (response.id) {
        scheduleId = response.id;
        console.log('找到scheduleId (id):', scheduleId);
      }
      else if (response.data && response.data.id) {
        scheduleId = response.data.id;
        console.log('找到scheduleId (data.id):', scheduleId);
      }
    }
    
    if (scheduleId) {
      
      // 更新保存状态
      scheduleStatus.value.saving = false;
      scheduleStatus.value.lastSaved = {
        scheduleId: scheduleId,
        doctor: doctor.name,
        date: date,
        shift: shift,
        timeSlot: timeSlot.slotName || timeSlot.slot_name,
        location: location?.name || '未分配地点',
        timestamp: new Date().toLocaleString()
      };
      
      ElMessage.success({
        message: `✅ 排班保存成功！\n医生：${doctor.name}\n日期：${date} ${shift}\n时间段：${timeSlot.slotName || timeSlot.slot_name}\n地点：${location?.name || '未分配地点'}\n排班ID：${scheduleId}`,
        duration: 5000,
        showClose: true
      });
      
      // 在控制台显示详细信息
      console.log('🎉 排班创建成功！', {
        scheduleId: scheduleId,
        doctor: doctor.name,
        date: date,
        shift: shift,
        timeSlot: timeSlot.slotName || timeSlot.slot_name,
        location: location?.name || '未分配地点',
        totalSlots: 10,
        fee: 5.00
      });
      
      // [优化] 保存成功后不重新加载排班数据，直接更新前端状态
      console.log('排班保存成功，前端状态已更新');
      
      // 验证医生是否正确显示
      setTimeout(() => {
        const doctorsInShift = getDoctorsForShift(date, shift);
        console.log(`保存后 ${date} ${shift} 的医生:`, doctorsInShift);
        if (doctorsInShift.length === 0) {
          console.warn('⚠️ 保存后医生未显示，可能存在数据同步问题');
        } else {
          console.log('✅ 医生显示正常');
        }
      }, 100);
      
      return response;
    } else {
      scheduleStatus.value.saving = false;
      console.warn('排班保存响应格式异常:', response);
      ElMessage.warning({
        message: `排班保存成功，但响应格式异常\n响应内容: ${JSON.stringify(response, null, 2)}`,
        duration: 8000,
        showClose: true
      });
      return response;
    }
  } catch (error) {
    console.error('保存排班失败:', error);
    
    // 更新错误状态
    scheduleStatus.value.saving = false;
    scheduleStatus.value.error = {
      message: error.message || '未知错误',
      doctor: doctor.name,
      date: date,
      shift: shift,
      timestamp: new Date().toLocaleString()
    };
    
    // 更详细的错误提示
    let errorMessage = '排班保存失败';
    let errorDetails = '';
    
    if (error.response) {
      const responseData = error.response.data;
      console.log('后端错误响应:', responseData);
      
      if (responseData && responseData.code === '400') {
        // 处理验证错误
        if (responseData.data && typeof responseData.data === 'object') {
          const validationErrors = Object.entries(responseData.data)
            .map(([field, message]) => `${field}: ${message}`)
            .join('\n');
          errorDetails = `验证错误：\n${validationErrors}`;
        } else {
          errorDetails = responseData.msg || '参数验证失败';
        }
      } else {
        errorMessage = `服务器错误：${error.response.status} - ${responseData?.msg || error.response.statusText}`;
      }
    } else if (error.request) {
      // 网络错误
      errorMessage = '网络连接失败，请检查后端服务是否启动';
    } else {
      // 其他错误
      errorMessage = error.message || '未知错误';
    }
    
    const finalMessage = errorDetails ? `${errorMessage}\n${errorDetails}` : errorMessage;
    
    ElMessage.error({
      message: `❌ ${finalMessage}\n医生：${doctor.name}\n日期：${date} ${shift}`,
      duration: 8000,
      showClose: true
    });
    
    throw error;
  } finally {
    // 确保保存状态被重置
    scheduleStatus.value.saving = false;
  }
};

// 获取时间段数据
const loadTimeSlots = async () => {
  try {
    console.log('开始获取时间段数据...');
    const response = await getTimeSlots();
    console.log('时间段API响应:', response);
    
    // 根据后端返回格式调整解析逻辑
    if (response && (response.code === 200 || response.code === '200')) {
      timeSlots.value = response.data || [];
      console.log('时间段数据加载成功:', timeSlots.value);
    } else if (response && response.data && (response.data.code === 200 || response.data.code === '200')) {
      timeSlots.value = response.data.data || [];
      console.log('时间段数据加载成功:', timeSlots.value);
    } else {
      console.error('获取时间段数据失败:', response);
      // 使用备用数据
      loadFallbackTimeSlots();
      ElMessage.warning('使用默认时间段数据');
    }
  } catch (error) {
    console.error('获取时间段数据出错:', error);
    // 使用备用数据
    loadFallbackTimeSlots();
    ElMessage.warning('网络错误，使用默认时间段数据');
  }
};

// 跳转到自动排班页面
const goToAutoSchedule = () => {
  router.push('/scheduling/auto-schedule');
};

// [新增] 显示冲突详情对话框
const showConflictDialog = () => {
  if (conflictData.value.hasConflicts) {
    conflictDialogVisible.value = true;
    // 默认展开第一个冲突
    if (conflictData.value.conflicts.length > 0) {
      activeConflictNames.value = [0];
    }
  }
};

// [新增] 导出冲突报告
const exportConflictReport = () => {
  try {
    const report = generateConflictReport();
    const blob = new Blob([report], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const timestamp = new Date().toISOString().split('T')[0];
    link.download = `排班冲突报告_${selectedDepartmentName.value}_${timestamp}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    ElMessage.success('冲突报告已导出');
  } catch (error) {
    console.error('导出冲突报告失败:', error);
    ElMessage.error('导出失败');
  }
};

// [新增] 生成冲突报告文本
const generateConflictReport = () => {
  const lines = [];
  lines.push('=' .repeat(60));
  lines.push(`排班冲突报告`);
  lines.push(`科室: ${selectedDepartmentName.value}`);
  lines.push(`生成时间: ${new Date().toLocaleString('zh-CN')}`);
  lines.push('=' .repeat(60));
  lines.push('');
  
  lines.push(`冲突汇总:`);
  lines.push(`  总计: ${conflictData.value.summary.total} 个冲突`);
  lines.push(`  严重: ${conflictData.value.summary.critical} 个`);
  lines.push(`  警告: ${conflictData.value.summary.warning} 个`);
  lines.push('');
  lines.push('-' .repeat(60));
  lines.push('');
  
  conflictData.value.conflicts.forEach((conflict, index) => {
    lines.push(`${index + 1}. ${conflict.title} [${conflict.severity === 'critical' ? '严重' : '警告'}]`);
    lines.push(`   ${conflict.description}`);
    lines.push('');
    
    conflict.details.forEach(detail => {
      lines.push(`   ${detail}`);
    });
    
    if (conflict.allDoctors && conflict.allDoctors.length > 0) {
      lines.push(`   涉及医生: ${conflict.allDoctors.map(d => `${d.name}(${d.identifier || d.id})`).join(', ')}`);
    }
    
    lines.push('');
    lines.push('-' .repeat(60));
    lines.push('');
  });
  
  lines.push('');
  lines.push('报告结束');
  lines.push('=' .repeat(60));
  
  return lines.join('\n');
};

// [新增] 自动填充排班数据
const autoFillScheduleData = async () => {
  try {
    console.log('开始自动填充排班数据...');
    
    // 1. 获取所有排班记录
    const response = await getAllSchedules(0, 1000); // 获取前1000条记录
    console.log('所有排班数据API响应:', response);
    
    if (!response || !response.content || response.content.length === 0) {
      console.log('没有找到排班数据');
      return;
    }
    
    const allSchedules = response.content;
    console.log('获取到的排班记录数量:', allSchedules.length);
    
    // 2. 获取当前周的排班数据
    const currentWeekStart = getCurrentWeekStart();
    const currentWeekEnd = getCurrentWeekEnd();
    
    const currentWeekSchedules = allSchedules.filter(schedule => {
      const scheduleDate = schedule.scheduleDate;
      return scheduleDate >= currentWeekStart && scheduleDate <= currentWeekEnd;
    });
    
    console.log('当前周的排班记录:', currentWeekSchedules);
    
    if (currentWeekSchedules.length === 0) {
      console.log('当前周没有排班数据');
      return;
    }
    
    // 3. 自动填充医生卡片（仅当医生数据为空时）
    if (!availableDoctors.value || availableDoctors.value.length === 0) {
      await autoFillDoctors(currentWeekSchedules);
    }
    
    // 4. 自动填充地点卡片（仅当地点数据为空时）
    if (!availableLocations.value || availableLocations.value.length === 0) {
      await autoFillLocations(currentWeekSchedules);
    }
    
    // 5. 自动填充时间段卡片（仅当时间段列为空时）
    if ((!timeSlotColumns.value['上午'] || timeSlotColumns.value['上午'].length === 0) &&
        (!timeSlotColumns.value['下午'] || timeSlotColumns.value['下午'].length === 0)) {
      await autoFillTimeSlots(currentWeekSchedules);
    }
    
    ElMessage.success('排班数据自动填充完成！');
    
  } catch (error) {
    console.error('自动填充排班数据失败:', error);
    ElMessage.error('自动填充排班数据失败: ' + error.message);
  }
};

// [新增] 自动填充时间段卡片
const autoFillTimeSlots = async (schedules) => {
  console.log('开始自动填充时间段卡片...');
  
  // 按班次分组时间段
  const morningSlots = new Set();
  const afternoonSlots = new Set();
  
  schedules.forEach(schedule => {
    if (schedule.slotName) {
      const slotName = schedule.slotName.toLowerCase();
      if (slotName.includes('上午') || slotName.includes('am') || slotName.includes('morning')) {
        morningSlots.add(schedule.slotName);
      } else if (slotName.includes('下午') || slotName.includes('pm') || slotName.includes('afternoon')) {
        afternoonSlots.add(schedule.slotName);
      }
    }
  });
  
  // 填充上午时间段
  if (morningSlots.size > 0) {
    timeSlotColumns.value['上午'] = Array.from(morningSlots).map((slotName, index) => ({
      slot_id: index + 1, // 使用数字ID，从1开始
      slot_name: slotName,
      start_time: '08:00:00',
      end_time: '12:00:00'
    }));
    console.log('填充上午时间段:', timeSlotColumns.value['上午']);
  }
  
  // 填充下午时间段
  if (afternoonSlots.size > 0) {
    timeSlotColumns.value['下午'] = Array.from(afternoonSlots).map((slotName, index) => ({
      slot_id: index + 10, // 使用数字ID，从10开始，避免与上午冲突
      slot_name: slotName,
      start_time: '14:00:00',
      end_time: '18:00:00'
    }));
    console.log('填充下午时间段:', timeSlotColumns.value['下午']);
  }
};

// [新增] 自动填充医生卡片
const autoFillDoctors = async (schedules) => {
  console.log('开始自动填充医生卡片...');
  
  // 收集所有医生信息
  const doctorMap = new Map();
  
  schedules.forEach(schedule => {
    if (schedule.doctorId && schedule.doctorName) {
      const doctorKey = schedule.doctorId;
      if (!doctorMap.has(doctorKey)) {
        doctorMap.set(doctorKey, {
          id: schedule.doctorId,
          name: schedule.doctorName,
          identifier: schedule.doctorIdentifier || (schedule.doctorId ? schedule.doctorId.toString() : ''),
          title: schedule.doctorTitle || '医生',
          gender: 'male', // 默认性别
          specialty: schedule.doctorSpecialty || '',
          phoneNumber: schedule.doctorPhone || ''
        });
      }
    }
  });
  
  // 更新医生数据
  const doctors = Array.from(doctorMap.values());
  if (doctors.length > 0) {
    // 获取当前选中的科室ID
    const departmentId = activeSub.value ? activeSub.value.replace(/^[sp]/, '') : '1';
    doctorsData.value[departmentId] = doctors;
    console.log('填充医生数据:', doctors);
  }
};

// [新增] 自动填充地点卡片
const autoFillLocations = async (schedules) => {
  console.log('开始自动填充地点卡片...');
  
  // 收集所有地点信息
  const locationMap = new Map();
  
  schedules.forEach(schedule => {
    if (schedule.locationId && schedule.locationName) {
      const locationKey = schedule.locationId;
      if (!locationMap.has(locationKey)) {
        locationMap.set(locationKey, {
          location_id: schedule.locationId,
          name: schedule.locationName,
          building: schedule.locationBuilding || '门诊楼',
          floor: schedule.locationFloor ? `${schedule.locationFloor}层` : '一层',
          room_number: schedule.locationRoomNumber || '001'
        });
      }
    }
  });
  
  // 更新地点数据
  const locations = Array.from(locationMap.values());
  if (locations.length > 0) {
    availableLocations.value = locations;
    console.log('填充地点数据:', locations);
  }
};

// [新增] 测试排班创建接口
const testScheduleCreation = async () => {
  try {
    console.log('开始测试排班创建接口...');
    
    const testData = {
      doctorId: 1,
      scheduleDate: "2025-01-01",
      slotId: 1,
      locationId: 1,
      totalSlots: 10,
      fee: "5.00",
      remarks: "测试排班"
    };
    
    console.log('发送测试数据:', testData);
    const response = await createSchedule(testData);
    console.log('测试响应:', response);
    
    ElMessage.success('排班创建接口测试成功！');
  } catch (error) {
    console.error('排班创建接口测试失败:', error);
    ElMessage.error('排班创建接口测试失败: ' + error.message);
  }
};

// [新增] 从排班数据自动填充时间段（专门用于loadSchedulesFromBackend）
const autoFillTimeSlotsFromSchedules = async (schedules) => {
  console.log('🔥 从排班数据自动填充时间段...', schedules);
  
  // 🔥 使用 Map 存储完整的时间段信息（包括slotId, slotName, startTime, endTime）
  const morningSlotMap = new Map(); // key: slotId, value: {slotId, slotName, startTime, endTime}
  const afternoonSlotMap = new Map();
  
  schedules.forEach(schedule => {
    console.log('处理排班记录:', schedule);
    
    if (schedule.slotName && schedule.slotId) {
      const slotName = schedule.slotName.toLowerCase();
      const slotInfo = {
        slotId: schedule.slotId,           // 🔥 camelCase
        slot_id: schedule.slotId,          // snake_case（兼容）
        slotName: schedule.slotName,       // 🔥 camelCase
        slot_name: schedule.slotName,      // snake_case（兼容）
        startTime: schedule.startTime || '08:00:00',  // 🔥 camelCase
        start_time: schedule.startTime || '08:00:00', // snake_case（兼容）
        endTime: schedule.endTime || '12:00:00',      // 🔥 camelCase
        end_time: schedule.endTime || '12:00:00'      // snake_case（兼容）
      };
      
      if (slotName.includes('上午') || slotName.includes('am') || slotName.includes('morning') || 
          (schedule.startTime && schedule.startTime < '12:00')) {
        morningSlotMap.set(schedule.slotId, slotInfo);
        console.log('✅ 添加上午时段:', slotInfo);
      } else if (slotName.includes('下午') || slotName.includes('pm') || slotName.includes('afternoon') ||
                 (schedule.startTime && schedule.startTime >= '12:00')) {
        afternoonSlotMap.set(schedule.slotId, slotInfo);
        console.log('✅ 添加下午时段:', slotInfo);
      }
    }
  });
  
  // 填充上午时间段（保留现有的，添加新的）
  if (morningSlotMap.size > 0) {
    const existingMorning = timeSlotColumns.value['上午'] || [];
    const newMorningSlots = Array.from(morningSlotMap.values());
    
    // 合并现有和新的时间段，避免重复
    const combinedMorning = [...existingMorning];
    newMorningSlots.forEach(newSlot => {
      if (!combinedMorning.some(existing => existing.slot_id === newSlot.slot_id)) {
        combinedMorning.push(newSlot);
      }
    });
    
    timeSlotColumns.value['上午'] = combinedMorning;
    console.log('✅ 填充上午时间段:', timeSlotColumns.value['上午']);
  }
  
  // 填充下午时间段（保留现有的，添加新的）
  if (afternoonSlotMap.size > 0) {
    const existingAfternoon = timeSlotColumns.value['下午'] || [];
    const newAfternoonSlots = Array.from(afternoonSlotMap.values());
    
    // 合并现有和新的时间段，避免重复
    const combinedAfternoon = [...existingAfternoon];
    newAfternoonSlots.forEach(newSlot => {
      if (!combinedAfternoon.some(existing => existing.slot_id === newSlot.slot_id)) {
        combinedAfternoon.push(newSlot);
      }
    });
    
    timeSlotColumns.value['下午'] = combinedAfternoon;
    console.log('✅ 填充下午时间段:', timeSlotColumns.value['下午']);
  }
  
  console.log('🎉 时间段填充完成！上午:', timeSlotColumns.value['上午'], '下午:', timeSlotColumns.value['下午']);
};

// 备用时间段数据
const loadFallbackTimeSlots = () => {
  timeSlots.value = [
    { slot_id: 1, slot_name: '上午 08:00-08:30', start_time: '08:00:00', end_time: '08:30:00' },
    { slot_id: 2, slot_name: '上午 08:30-09:00', start_time: '08:30:00', end_time: '09:00:00' },
    { slot_id: 3, slot_name: '上午 09:00-09:30', start_time: '09:00:00', end_time: '09:30:00' },
    { slot_id: 4, slot_name: '上午 09:30-10:00', start_time: '09:30:00', end_time: '10:00:00' },
    { slot_id: 5, slot_name: '上午 10:00-10:30', start_time: '10:00:00', end_time: '10:30:00' },
    { slot_id: 6, slot_name: '上午 10:30-11:00', start_time: '10:30:00', end_time: '11:00:00' },
    { slot_id: 7, slot_name: '上午 11:00-11:30', start_time: '11:00:00', end_time: '11:30:00' },
    { slot_id: 8, slot_name: '上午 11:30-12:00', start_time: '11:30:00', end_time: '12:00:00' },
    { slot_id: 9, slot_name: '下午 14:00-14:30', start_time: '14:00:00', end_time: '14:30:00' },
    { slot_id: 10, slot_name: '下午 14:30-15:00', start_time: '14:30:00', end_time: '15:00:00' },
    { slot_id: 11, slot_name: '下午 15:00-15:30', start_time: '15:00:00', end_time: '15:30:00' },
    { slot_id: 12, slot_name: '下午 15:30-16:00', start_time: '15:30:00', end_time: '16:00:00' },
    { slot_id: 13, slot_name: '下午 16:00-16:30', start_time: '16:00:00', end_time: '16:30:00' },
    { slot_id: 14, slot_name: '下午 16:30-17:00', start_time: '16:30:00', end_time: '17:00:00' },
    { slot_id: 15, slot_name: '晚间 18:00-18:30', start_time: '18:00:00', end_time: '18:30:00' },
    { slot_id: 16, slot_name: '晚间 18:30-19:00', start_time: '18:30:00', end_time: '19:00:00' }
  ];
  console.log('使用备用时间段数据:', timeSlots.value);
};

onMounted(async () => {
  // 加载科室数据
  await loadDepartments();
  convertScheduleToEvents();
  // 加载时间段数据
  await loadTimeSlots();
  
  // 如果API调用失败，立即使用备用数据
    if (timeSlots.value.length === 0) {
      console.log('时间段数据为空，使用备用数据');
      loadFallbackTimeSlots();
    }
  
  // 延迟执行冲突检测，确保数据已经加载完成
  setTimeout(() => {
    detectAllConflicts();
  }, 1000);
});

</script>

<style scoped>
.schedule-dashboard {
  display: flex;
  height: calc(100vh - 50px);
  background-color: #f7fafc;
  overflow: hidden; /* 防止整个页面滚动 */
}

/* [新增] 头部控制按钮样式 */
/* 标题行样式 */
.header-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.department-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 按钮控制行 - 所有按钮排成一行 */
.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 动作按钮统一尺寸与配色区分 */
.header-controls :deep(.action-btn) {
  flex: 0 0 140px; /* 统一宽度，比例协调 */
  justify-content: center;
}
.header-controls :deep(.btn-auto) {
  background-color: #409EFF;
  border-color: #409EFF;
  color: #fff;
}
.header-controls :deep(.btn-fill) {
  background-color: #67C23A;
  border-color: #67C23A;
  color: #fff;
}
.header-controls :deep(.action-btn:hover) {
  filter: brightness(0.95);
}

.view-switcher :deep(.el-button),
.week-nav :deep(.el-button) {
  padding: 6px 12px;
}

/* [新增] 日历容器样式 */
.calendar-container {
  padding: 20px;
  min-height: 600px;
}

/* [新增] FullCalendar 自定义样式 */
.calendar-container :deep(.fc) {
  font-family: 'Microsoft YaHei', sans-serif;
}

.calendar-container :deep(.fc-toolbar-title) {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.calendar-container :deep(.fc-button) {
  background-color: #409EFF;
  border-color: #409EFF;
  text-transform: none;
  padding: 6px 12px;
}

.calendar-container :deep(.fc-button:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.calendar-container :deep(.fc-button-active) {
  background-color: #337ecc;
  border-color: #337ecc;
}

.calendar-container :deep(.fc-event) {
  cursor: pointer;
  border-radius: 4px;
  padding: 2px 4px;
  font-size: 12px;
}

.calendar-container :deep(.fc-timegrid-event:hover) {
  opacity: 0.9;
}

.calendar-container :deep(.fc-daygrid-event) {
  white-space: normal;
  min-height: 22px;
  margin-bottom: 2px;
}

.calendar-container :deep(.fc-daygrid-event .fc-event-main) {
  padding: 1px 2px;
}

.calendar-container :deep(.fc-timegrid-event) {
  border-radius: 4px;
}

.calendar-container :deep(.fc-col-header-cell) {
  background-color: #f5f7fa;
  font-weight: bold;
  padding: 10px 0;
}

.calendar-container :deep(.fc-day-today) {
  background-color: #ecf5ff !important;
}

.calendar-container :deep(.fc-timegrid-slot) {
  height: 2em;
}

.department-sidebar {
  width: 320px;
  display: flex;
  background-color: #fff;
  border-right: 1px solid #e2e8f0;
  flex-shrink: 0;
  overflow-y: auto; /* 垂直滚动 */
  max-height: calc(100vh - 50px); /* 限制最大高度 */
  scroll-behavior: smooth; /* 平滑滚动 */
  transition: all 0.3s ease;
}

/* 折叠状态 */
.department-sidebar.collapsed {
  width: 0;
  opacity: 0;
  overflow: hidden;
}

/* 折叠/展开按钮 */
.sidebar-toggle {
  position: absolute;
  left: 320px;                /* 贴在侧栏右边缘 */
  top: 50%;                   /* 垂直居中 */
  transform: translateY(-50%);
  width: 28px;
  height: 64px;
  background-color: #409EFF;
  border-radius: 0 8px 8px 0;  /* 半胶囊，贴边更自然 */
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s ease;
  color: #fff;
}

.sidebar-toggle:hover {
  background-color: #66b1ff;
}

/* 折叠状态下将按钮吸附到最左侧 */
.sidebar-toggle.collapsed {
  left: 0;
  border-radius: 8px;         /* 独立悬浮小胶囊 */
}

/* 侧边栏折叠时，右侧内容占满 */
.department-sidebar.collapsed ~ .schedule-content {
  margin-left: 0;
}

/* 自定义滚动条样式 */
.department-sidebar::-webkit-scrollbar {
  width: 6px;
}

.department-sidebar::-webkit-scrollbar-thumb {
  background-color: #d0d7de;
  border-radius: 3px;
}

.department-sidebar::-webkit-scrollbar-thumb:hover {
  background-color: #b0b7be;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  gap: 12px;
}

.loading-container .el-icon {
  font-size: 24px;
}

.no-sub-departments {
  padding: 20px;
  text-align: center;
}
.department-menu {
  width: 120px;
  border-right: none;
}
.sub-department-panel {
  flex: 1;
  padding: 8px;
  border-left: 1px solid #e2e8f0;
}
.sub-department-item {
  padding: 10px 15px;
  cursor: pointer;
  border-radius: 4px;
}
.sub-department-item:hover {
  background-color: #f5f7fa;
}
.sub-department-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: bold;
}
.schedule-content {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: scroll; /* 始终显示垂直滚动条 */
  overflow-x: hidden; /* 隐藏横向滚动 */
  min-width: 0;
  height: calc(100vh - 50px); /* 固定高度 */
  scroll-behavior: smooth; /* 平滑滚动 */
}

/* 自定义滚动条样式 */
.schedule-content::-webkit-scrollbar {
  width: 8px;
}

.schedule-content::-webkit-scrollbar-thumb {
  background-color: #d0d7de;
  border-radius: 4px;
}

.schedule-content::-webkit-scrollbar-thumb:hover {
  background-color: #b0b7be;
}

.schedule-content::-webkit-scrollbar-track {
  background-color: #f5f5f5;
}
.schedule-card {
  flex-shrink: 0;
  flex-grow: 0;
  min-height: min-content; /* 确保内容可以自然增长 */
}

/* 确保排班卡片的 body 不受高度限制 */
.schedule-card :deep(.el-card__body) {
  overflow: visible;
  height: auto;
  max-height: none;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
/* 表格容器 - 添加横向滚动 */
.table-container {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
}

/* 自定义横向滚动条样式 */
.table-container::-webkit-scrollbar {
  height: 8px;
}

.table-container::-webkit-scrollbar-thumb {
  background-color: #d0d7de;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background-color: #b0b7be;
}

.schedule-table {
  width: 100%;
  min-width: 1200px; /* 设置最小宽度，超出则滚动 */
  border-collapse: collapse;
  text-align: center;
  table-layout: auto;
}
.schedule-table th, .schedule-table td {
  border: 1px solid #ebeef5;
  padding: 8px;
  font-size: 14px;
  vertical-align: top;
}
.schedule-table th {
  background-color: #f5f7fa;
}
.shift-cell {
  min-height: 120px;
  padding: 4px;
}
.doctor-tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: flex-start;
  align-items: center;
  flex-grow: 1;
}

.doctor-card-in-table {
  width: 95%;
  padding: 8px;
  border-radius: 8px;
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  cursor: grab;
  position: relative;
  text-align: left;
}
.doctor-card-in-table:hover .remove-icon {
  display: inline-flex;
}
.doctor-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-weight: 500;
}

.doctor-avatar-small {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
}
.remove-icon {
  display: none;
  position: absolute;
  top: 4px;
  right: 4px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  color: #f56c6c;
}
.doctor-card-location {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
  position: relative; /* 为了定位清除按钮 */
  transition: background-color 0.2s;
}
.doctor-card-location:hover {
  background-color: #e9e9eb;
}

.doctor-card-location.is-set {
  color: #67c23a;
  font-weight: bold;
}
/* [新增] 清除地点按钮样式 */
.clear-location-icon {
  display: none; /* 默认隐藏 */
  cursor: pointer;
  color: #f56c6c;
  margin-left: auto; /* 推到最右边 */
  padding: 2px;
}
.doctor-card-location:hover .clear-location-icon {
  display: inline-flex; /* 悬停时显示 */
}


.bottom-panels {
  display: flex;
  gap: 16px;
  margin-top: 20px;
  flex-wrap: nowrap;
  overflow-x: auto; /* 保留横向滚动 */
  overflow-y: visible; /* 允许内容垂直增长 */
  width: 100%;
  min-height: min-content; /* 确保可以自然增长 */
}
.draggable-list-card {
  flex: 1;
  min-width: 300px;
  max-width: none;
  width: auto;
  height: auto; /* 允许高度自动调整 */
  overflow: visible; /* 允许内容溢出到外层 */
}

/* 确保 el-card 的 body 不受高度限制 */
.draggable-list-card :deep(.el-card__body) {
  overflow: visible;
  height: auto;
  max-height: none;
}
.draggable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  /* 移除max-height限制，让内容自然增长，触发外层滚动 */
  overflow-y: visible;
  overflow-x: hidden;
}

.doctor-card {
  display: flex;
  align-items: center;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: grab;
  transition: box-shadow 0.2s;
  gap: 10px;
  min-height: 50px;
  width: 100%;
}
.doctor-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}
.doctor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
}
.doctor-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.doctor-name {
  font-weight: bold;
  color: #111827;
}
.doctor-title {
  font-size: 12px;
  color: #6b7280;
}
/* 新增地点卡片样式 */
.location-list {
  gap: 10px;
}
.location-card {
  display: flex;
  align-items: center;
  background-color: #f4f4f5;
  border: 1px solid #e9e9eb;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: grab;
  transition: box-shadow 0.2s;
  width: 100%;
  min-height: 50px;
  gap: 10px;
}
.location-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}
.location-icon {
  color: #909399;
  margin-right: 12px;
}
.location-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.location-name {
  font-weight: 500;
  color: #303133;
}
.location-desc {
  font-size: 12px;
  color: #909399;
}

/* 时间段卡片样式 */
.time-slot-column {
  width: 220px;
  vertical-align: top;
  background-color: #f8f9fa;
  min-height: 120px;
}

.shift-label {
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  padding: 4px 8px;
  background-color: #e9ecef;
  border-radius: 4px;
  text-align: center;
}

.time-slot-cards {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px;
}

.time-slot-card {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: grab;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  box-sizing: border-box;
}

.time-slot-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.2);
  transform: translateY(-1px);
}

.time-slot-card:hover .remove-icon {
  display: inline-flex;
}

.time-slot-card:active {
  cursor: grabbing;
}

.time-slot-card-content {
  flex: 1;
  min-width: 0;
  padding-right: 8px;
}

.time-slot-name {
  font-size: 12px;
  font-weight: 500;
  color: #1890ff;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-slot-time {
  font-size: 10px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-slot-card .remove-icon {
  display: none;
  position: absolute;
  top: 4px;
  right: 4px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  color: #f56c6c;
  padding: 2px;
}

/* 底部面板的时间段卡片样式 */
.time-slot-list {
  gap: 10px;
}

/* 排班状态指示器样式 */
.schedule-status-indicator {
  margin: 0 20px;
  font-size: 14px;
}

.status-saving {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
}

.status-success {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
}

.status-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f56c6c;
}

.status-saving .el-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.time-slot-card {
  display: flex;
  align-items: center;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: grab;
  transition: box-shadow 0.2s;
  width: 100%;
  min-height: 50px;
  gap: 10px;
}

.time-slot-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.time-slot-icon {
  color: #0ea5e9;
  margin-right: 12px;
}

.time-slot-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.time-slot-name {
  font-weight: 500;
  color: #0c4a6e;
  font-size: 13px;
}

.time-slot-time {
  font-size: 11px;
  color: #64748b;
}

/* [新增] 批量导入样式 */
.batch-import-panel {
  margin-bottom: 16px;
  min-width: 350px;
  max-width: 400px;
  width: 380px;
}

.batch-import-content {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.template-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #e1f5fe;
}

.template-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1976d2;
  font-size: 14px;
}

.template-icon {
  font-size: 16px;
}

.upload-section {
  margin-bottom: 16px;
}

.upload-dragger {
  width: 100%;
}

.upload-content {
  padding: 20px;
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.upload-text p {
  margin: 8px 0;
  color: #606266;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.file-details {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-weight: 500;
  color: #303133;
}

.file-size {
  color: #909399;
  font-size: 12px;
}

.import-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.import-progress {
  margin-bottom: 16px;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.progress-message {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.import-result {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid;
}

.import-result.success {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.import-result.error {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 16px;
}

.success-icon {
  color: #67c23a;
}

.error-icon {
  color: #f56c6c;
}

.result-content p {
  margin: 8px 0;
  color: #606266;
}

.result-details {
  margin-top: 12px;
}

.result-details h5 {
  margin: 8px 0;
  color: #303133;
  font-size: 14px;
}

.result-details ul {
  margin: 0;
  padding-left: 20px;
}

.result-details li {
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
}

/* [新增] 冲突检测样式 */
.conflict-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-right: 16px;
}

.conflict-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fff2e8;
  border: 1px solid #f5dab1;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.conflict-summary:hover {
  background-color: #ffe7ba;
  border-color: #e6a23c;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
}

.detect-conflicts-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.conflict-summary-icon {
  font-size: 16px;
}

.critical-icon {
  color: #f56c6c;
}

.warning-icon {
  color: #e6a23c;
}

.conflict-text {
  color: #e6a23c;
  font-weight: 500;
}

.critical-count {
  color: #f56c6c;
  font-weight: 600;
}

.warning-count {
  color: #e6a23c;
  font-weight: 600;
}

/* 医生卡片冲突样式 */
.conflict-error {
  background-color: #fef0f0 !important;
  border: 2px solid #f56c6c !important;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.2);
}

.conflict-warning {
  background-color: #fdf6ec !important;
  border: 2px solid #e6a23c !important;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.2);
}

.conflict-icon {
  margin-left: 8px;
  font-size: 16px;
}

.conflict-error-icon {
  color: #f56c6c;
}

.conflict-warning-icon {
  color: #e6a23c;
}

/* 冲突卡片悬停效果 */
.doctor-card-in-table.conflict-error:hover {
  background-color: #fde2e2 !important;
  transform: scale(1.02);
  transition: all 0.2s ease;
}

.doctor-card-in-table.conflict-warning:hover {
  background-color: #fce4d6 !important;
  transform: scale(1.02);
  transition: all 0.2s ease;
}

/* 自定义事件内容样式 - 日视图/周视图 */
.calendar-container :deep(.fc-event-custom-day) {
  padding: 4px;
  font-size: 12px;
  line-height: 1.4;
}

.calendar-container :deep(.fc-event-time) {
  font-weight: 600;
  font-size: 11px;
  margin-bottom: 2px;
  color: rgba(255, 255, 255, 0.95);
}

.calendar-container :deep(.fc-event-title-day) {
  margin-bottom: 2px;
}

.calendar-container :deep(.fc-event-title-day strong) {
  font-weight: 600;
  color: #fff;
}

.calendar-container :deep(.fc-event-identifier) {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9);
  margin-left: 2px;
}

.calendar-container :deep(.fc-event-department) {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 2px;
  font-style: italic;
}

.calendar-container :deep(.fc-event-location) {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 2px;
}

.calendar-container :deep(.fc-event-location i) {
  font-size: 10px;
}

/* 自定义事件内容样式 - 月视图 */
.calendar-container :deep(.fc-event-custom-month) {
  padding: 3px 6px;
  font-size: 12px;
}

.calendar-container :deep(.fc-event-title-month) {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
  overflow: hidden;
}

.calendar-container :deep(.fc-event-title-month strong) {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}

.calendar-container :deep(.fc-event-custom-month .fc-event-identifier) {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

/* 月视图事件样式 - 白底黑字 + 彩色边框 */
.calendar-container :deep(.fc-daygrid-event.shift-morning) {
  background-color: #ffffff !important;
  border-left: 4px solid #67C23A !important;
  border-top: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
}

.calendar-container :deep(.fc-daygrid-event.shift-morning:hover) {
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  border-left-width: 5px;
  transform: translateX(-1px);
}

.calendar-container :deep(.fc-daygrid-event.shift-afternoon) {
  background-color: #ffffff !important;
  border-left: 4px solid #409EFF !important;
  border-top: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
}

.calendar-container :deep(.fc-daygrid-event.shift-afternoon:hover) {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  border-left-width: 5px;
  transform: translateX(-1px);
}

/* 日视图/周视图事件样式 - 保持彩色背景 */
.calendar-container :deep(.fc-timegrid-event.shift-morning) {
  background-color: #67C23A !important;
  border-color: #529b2e !important;
}

.calendar-container :deep(.fc-timegrid-event.shift-afternoon) {
  background-color: #409EFF !important;
  border-color: #337ecc !important;
}

/* 日视图/周视图的文字保持白色 */
.calendar-container :deep(.fc-timegrid-event .fc-event-title-day strong),
.calendar-container :deep(.fc-timegrid-event .fc-event-identifier),
.calendar-container :deep(.fc-timegrid-event .fc-event-department),
.calendar-container :deep(.fc-timegrid-event .fc-event-location) {
  color: #fff !important;
}

/* 日历事件冲突样式 */
.calendar-container :deep(.fc-event.conflict-critical) {
  border-color: #f56c6c !important;
}

.calendar-container :deep(.fc-daygrid-event.conflict-critical) {
  background-color: #FEF0F0 !important;
  border-left-color: #f56c6c !important;
}

.calendar-container :deep(.fc-timegrid-event.conflict-critical) {
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.calendar-container :deep(.fc-event.conflict-warning) {
  border-color: #e6a23c !important;
}

.calendar-container :deep(.fc-daygrid-event.conflict-warning) {
  background-color: #FDF6EC !important;
  border-left-color: #e6a23c !important;
}

.calendar-container :deep(.fc-timegrid-event.conflict-warning) {
  background-color: #e6a23c !important;
  border-color: #e6a23c !important;
}

.calendar-container :deep(.fc-event.has-conflict) {
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.6);
}

/* 时间段班次不匹配样式 */
.time-slot-mismatch {
  background-color: #fef0f0 !important;
  border: 2px solid #f56c6c !important;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.2);
}

.shift-mismatch-warning {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 2px 6px;
  background-color: #f56c6c;
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.shift-mismatch-warning .warning-icon {
  font-size: 12px;
}

/* 冲突详情对话框样式 */
.conflict-dialog :deep(.el-dialog__body) {
  padding: 20px;
  max-height: 600px;
  overflow-y: auto;
}

.conflict-summary-header {
  margin-bottom: 20px;
}

.conflict-stats {
  display: flex;
  gap: 20px;
  margin-top: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}

.stat-item.critical {
  color: #f56c6c;
}

.stat-item.warning {
  color: #e6a23c;
}

.conflict-list {
  margin-top: 16px;
}

.conflict-item {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.conflict-item.conflict-critical {
  border-left: 4px solid #f56c6c;
}

.conflict-item.conflict-warning {
  border-left: 4px solid #e6a23c;
}

.conflict-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 4px 0;
}

.conflict-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.conflict-icon.critical-icon {
  color: #f56c6c;
}

.conflict-icon.warning-icon {
  color: #e6a23c;
}

.conflict-type-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.badge-critical {
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.badge-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.conflict-desc {
  color: #606266;
  font-size: 14px;
  flex: 1;
}

.conflict-details {
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 4px;
}

.conflict-doctors {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.doctors-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.doctors-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.conflict-dialog :deep(.el-collapse-item__header) {
  padding: 12px 16px;
  background-color: #fff;
  font-size: 14px;
  border-radius: 4px;
}

.conflict-dialog :deep(.el-collapse-item__header:hover) {
  background-color: #f5f7fa;
}

.conflict-dialog :deep(.el-collapse-item__content) {
  padding: 0;
}

.conflict-dialog :deep(.el-descriptions__label) {
  font-weight: 500;
  background-color: #fafafa;
}

.conflict-dialog :deep(.el-dialog__footer) {
  padding: 15px 20px;
  border-top: 1px solid #e4e7ed;
}
</style>

