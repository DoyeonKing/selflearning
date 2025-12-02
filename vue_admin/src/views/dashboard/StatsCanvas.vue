<template>
  <div class="stats-canvas">
    <!-- 顶部导航 -->
    <div class="top-header">
      <div class="header-content">
        <div class="header-left">
          <h2>医院后台管理系统</h2>
        </div>
        <div class="header-right">
          <el-button type="danger" @click="handleExit" size="small">
            <el-icon><Close /></el-icon>
            退出大屏
          </el-button>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div class="back-area">
      <BackButton />
    </div>

    <!-- 顶部欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>医院运营数据中心</h1>
        <p>实时监控核心运营指标，数据驱动决策</p>
      </div>
      <img :src="doctorImage" alt="医生形象" class="banner-image">
    </div>

    <!-- Tab 切换式内容区 -->
    <div class="content-wrapper">
      <el-tabs v-model="activeTab" type="border-card" class="dashboard-tabs" @tab-change="handleTabChange">
        <!-- Tab 1: 运营总览 -->
        <el-tab-pane label="运营总览" name="overview">
          <div class="tab-content">
            <!-- 核心指标卡片 -->
            <div class="top-cards">
              <div class="stat-card stat-card-green">
                <div class="stat-icon">
                  <el-icon :size="40"><Calendar /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">今日挂号量</div>
                  <div class="stat-value">{{ mockData.overview.todayAppointments }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-blue">
                <div class="stat-icon">
                  <el-icon :size="40"><User /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">今日出诊医生</div>
                  <div class="stat-value">{{ mockData.overview.activeDoctorsToday }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-red">
                <div class="stat-icon">
                  <el-icon :size="40"><Warning /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">当前候诊人数</div>
                  <div class="stat-value">{{ mockData.overview.pendingPatients }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-purple">
                <div class="stat-icon">
                  <el-icon :size="40"><UserFilled /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">累计注册用户</div>
                  <div class="stat-value">{{ mockData.overview.totalPatients }}</div>
                </div>
              </div>
            </div>

            <!-- 图表区域 -->
            <div class="charts-grid">
              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">全院挂号趋势</div>
                </template>
                <div id="appointmentTrendChart" class="chart"></div>
              </el-card>

              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">支付状态分布</div>
                </template>
                <div id="paymentStatusChart" class="chart"></div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 医生资源分析 -->
        <el-tab-pane label="医生资源分析" name="doctors">
          <div class="tab-content">
            <!-- 关键指标卡片 -->
            <div class="top-cards">
              <div class="stat-card stat-card-blue">
                <div class="stat-icon">
                  <el-icon :size="40"><UserFilled /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">医生总数</div>
                  <div class="stat-value">{{ mockData.doctors.totalDoctors }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-orange">
                <div class="stat-icon">
                  <el-icon :size="40"><Calendar /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">今日请假人数</div>
                  <div class="stat-value">{{ mockData.doctors.todayLeaveCount }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-indigo">
                <div class="stat-icon">
                  <el-icon :size="40"><OfficeBuilding /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">科室总数</div>
                  <div class="stat-value">{{ mockData.doctors.totalDepartments }}</div>
                </div>
              </div>
            </div>

            <!-- 图表区域 -->
            <div class="charts-grid">
              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">职称分布</div>
                </template>
                <div id="titleDistributionChart" class="chart"></div>
              </el-card>

              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">科室繁忙度 Top 5</div>
                </template>
                <div id="departmentBusyChart" class="chart"></div>
              </el-card>

              <el-card class="chart-card chart-full-width">
                <template #header>
                  <div class="card-header">医生工作量 Top 5</div>
                </template>
                <div id="doctorWorkloadChart" class="chart"></div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 3: 患者群体画像 -->
        <el-tab-pane label="患者群体画像" name="patients">
          <div class="tab-content">
            <!-- 关键指标卡片 -->
            <div class="top-cards">
              <div class="stat-card stat-card-green">
                <div class="stat-icon">
                  <el-icon :size="40"><User /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">本月新增注册</div>
                  <div class="stat-value">{{ mockData.patients.monthlyNewRegistrations }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-purple">
                <div class="stat-icon">
                  <el-icon :size="40"><DataAnalysis /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">师生比例系数</div>
                  <div class="stat-value">{{ mockData.patients.studentTeacherRatio }}</div>
                </div>
              </div>

              <div class="stat-card stat-card-red">
                <div class="stat-icon">
                  <el-icon :size="40"><Warning /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-label">累计爽约次数</div>
                  <div class="stat-value">{{ mockData.patients.totalNoShows }}</div>
                </div>
              </div>
            </div>

            <!-- 图表区域 -->
            <div class="charts-grid">
              <el-card class="chart-card chart-full-width">
                <template #header>
                  <div class="card-header">用户增长趋势（近30天）</div>
                </template>
                <div id="userGrowthChart" class="chart"></div>
              </el-card>

              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">患者类型构成</div>
                </template>
                <div id="patientTypeChart" class="chart"></div>
              </el-card>

              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">就诊时段热力图</div>
                </template>
                <div id="timeSlotChart" class="chart"></div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  User,
  Warning,
  UserFilled,
  Close,
  OfficeBuilding,
  DataAnalysis
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import doctorImage from '@/assets/doctor.jpg'
import BackButton from '@/components/BackButton.vue'

const router = useRouter()
const activeTab = ref('overview')

// Mock 数据
const mockData = reactive({
  overview: {
    todayAppointments: 128,
    activeDoctorsToday: 24,
    pendingPatients: 15,
    totalPatients: 3450,
    last7DaysDates: ['11-16', '11-17', '11-18', '11-19', '11-20', '11-21', '11-22'],
    last7DaysCounts: [120, 132, 101, 134, 90, 230, 210],
    paymentStatus: [
      { name: '已支付', value: 245 },
      { name: '待支付', value: 38 },
      { name: '退款', value: 12 }
    ]
  },
  doctors: {
    totalDoctors: 156,
    todayLeaveCount: 8,
    totalDepartments: 12,
    titleDistribution: [
      { name: 'Chief Physician', value: 28 },
      { name: 'Associate Chief Physician', value: 45 },
      { name: 'Attending Physician', value: 58 },
      { name: 'Resident Physician', value: 25 }
    ],
    departmentBusy: [
      { name: 'Internal Medicine', value: 342 },
      { name: 'Surgery', value: 298 },
      { name: 'Pediatrics', value: 267 },
      { name: 'Orthopedics', value: 189 },
      { name: 'Cardiology', value: 156 }
    ],
    doctorWorkload: [
      { name: 'Dr. Zhang Wei', department: 'Internal Medicine', value: 89 },
      { name: 'Dr. Li Ming', department: 'Surgery', value: 76 },
      { name: 'Dr. Wang Fang', department: 'Pediatrics', value: 68 },
      { name: 'Dr. Liu Gang', department: 'Orthopedics', value: 54 },
      { name: 'Dr. Chen Yu', department: 'Cardiology', value: 47 }
    ]
  },
  patients: {
    monthlyNewRegistrations: 287,
    studentTeacherRatio: '4.3:1',
    totalNoShows: 156,
    last30DaysDates: Array.from({ length: 30 }, (_, i) => {
      const date = new Date()
      date.setDate(date.getDate() - (29 - i))
      return `${date.getMonth() + 1}-${date.getDate()}`
    }),
    last30DaysCounts: Array.from({ length: 30 }, () => Math.floor(Math.random() * 20) + 5),
    patientType: [
      { name: 'Student', value: 2800 },
      { name: 'Teacher', value: 650 }
    ],
    timeSlotData: [
      { time: '08:00-09:00', count: 45 },
      { time: '09:00-10:00', count: 78 },
      { time: '10:00-11:00', count: 92 },
      { time: '11:00-12:00', count: 65 },
      { time: '12:00-13:00', count: 28 },
      { time: '13:00-14:00', count: 38 },
      { time: '14:00-15:00', count: 56 },
      { time: '15:00-16:00', count: 72 },
      { time: '16:00-17:00', count: 48 }
    ]
  }
})

// ECharts 实例存储
const chartInstances = new Map()

// 初始化图表函数
const initChart = (id, option) => {
  const chartDom = document.getElementById(id)
  if (!chartDom) return null

  // 如果已存在实例，先销毁
  if (chartInstances.has(id)) {
    chartInstances.get(id).dispose()
  }

  const chart = echarts.init(chartDom)
  chart.setOption(option)
  chartInstances.set(id, chart)
  return chart
}

// Tab 1: 运营总览图表
const initOverviewCharts = () => {
  // 全院挂号趋势图
  initChart('appointmentTrendChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: mockData.overview.last7DaysDates,
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' },
      splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } }
    },
    series: [
      {
        name: '挂号量',
        type: 'line',
        smooth: true,
        data: mockData.overview.last7DaysCounts,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(79, 209, 197, 0.3)' },
            { offset: 1, color: 'rgba(79, 209, 197, 0.1)' }
          ])
        },
        lineStyle: { color: '#4FD1C5', width: 3 },
        itemStyle: { color: '#4FD1C5', borderWidth: 2, borderColor: '#fff' }
      }
    ]
  })

  // 支付状态分布图
  initChart('paymentStatusChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { color: '#4a5568', fontSize: 14 }
    },
    series: [
      {
        name: '支付状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#2d3748',
          fontSize: 14,
          fontWeight: 'bold'
        },
        data: mockData.overview.paymentStatus.map((item, index) => ({
          ...item,
          itemStyle: {
            color: [
              new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#48bb78' },
                { offset: 1, color: '#38a169' }
              ]),
              new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#f59e0b' },
                { offset: 1, color: '#d97706' }
              ]),
              new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#f56565' },
                { offset: 1, color: '#e53e3e' }
              ])
            ][index]
          }
        }))
      }
    ]
  })
}

// Tab 2: 医生资源分析图表
const initDoctorsCharts = () => {
  // 职称分布图（环形图）
  initChart('titleDistributionChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { color: '#4a5568', fontSize: 12 }
    },
    series: [
      {
        name: '职称分布',
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 12
        },
        data: mockData.doctors.titleDistribution.map((item, index) => ({
          ...item,
          itemStyle: {
            color: [
              '#667eea',
              '#764ba2',
              '#f093fb',
              '#f5576c'
            ][index]
          }
        }))
      }
    ]
  })

  // 科室繁忙度 Top 5
  initChart('departmentBusyChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' }
    },
    yAxis: {
      type: 'category',
      data: mockData.doctors.departmentBusy.map(d => d.name),
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' }
    },
    series: [
      {
        name: '已预约数',
        type: 'bar',
        data: mockData.doctors.departmentBusy.map(d => d.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#4299e1' },
            { offset: 1, color: '#3182ce' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#2d3748'
        }
      }
    ]
  })

  // 医生工作量 Top 5（横向柱状图）
  initChart('doctorWorkloadChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function (params) {
        const param = params[0]
        return `${param.name}<br/>${param.seriesName}: ${param.value}人<br/>科室: ${param.data.department}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' }
    },
    yAxis: {
      type: 'category',
      data: mockData.doctors.doctorWorkload.map(d => d.name),
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' }
    },
    series: [
      {
        name: '接诊人数',
        type: 'bar',
        data: mockData.doctors.doctorWorkload.map(d => ({
          value: d.value,
          department: d.department
        })),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#9f7aea' },
            { offset: 1, color: '#805ad5' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#2d3748'
        }
      }
    ]
  })
}

// Tab 3: 患者群体画像图表
const initPatientsCharts = () => {
  // 用户增长趋势图
  initChart('userGrowthChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: mockData.patients.last30DaysDates,
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096', rotate: 45 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' },
      splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: mockData.patients.last30DaysCounts,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.1)' }
          ])
        },
        lineStyle: { color: '#667eea', width: 3 },
        itemStyle: { color: '#667eea', borderWidth: 2, borderColor: '#fff' }
      }
    ]
  })

  // 患者类型构成图
  initChart('patientTypeChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { color: '#4a5568', fontSize: 14 }
    },
    series: [
      {
        name: '患者类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#2d3748',
          fontSize: 14,
          fontWeight: 'bold'
        },
        data: [
          {
            value: mockData.patients.patientType[0].value,
            name: '学生',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#667eea' },
                { offset: 1, color: '#764ba2' }
              ])
            }
          },
          {
            value: mockData.patients.patientType[1].value,
            name: '教职工',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#f093fb' },
                { offset: 1, color: '#f5576c' }
              ])
            }
          }
        ]
      }
    ]
  })

  // 就诊时段热力图（柱状图）
  initChart('timeSlotChart', {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: mockData.patients.timeSlotData.map(d => d.time),
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096', rotate: 45 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#4a5568' } },
      axisLabel: { color: '#718096' },
      splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } }
    },
    series: [
      {
        name: '挂号量',
        type: 'bar',
        data: mockData.patients.timeSlotData.map(d => d.count),
        itemStyle: {
          color: function (params) {
            const colors = [
              ['#4299e1', '#3182ce'],
              ['#48bb78', '#38a169'],
              ['#f59e0b', '#d97706'],
              ['#f56565', '#e53e3e']
            ]
            const colorIndex = Math.floor(params.dataIndex / 3) % colors.length
            return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: colors[colorIndex][0] },
              { offset: 1, color: colors[colorIndex][1] }
            ])
          }
        },
        label: {
          show: true,
          position: 'top',
          color: '#2d3748'
        }
      }
    ]
  })
}

// 处理 Tab 切换
const handleTabChange = (tabName) => {
  nextTick(() => {
    // 延迟初始化图表，确保 DOM 已渲染
    setTimeout(() => {
      switch (tabName) {
        case 'overview':
          initOverviewCharts()
          break
        case 'doctors':
          initDoctorsCharts()
          break
        case 'patients':
          initPatientsCharts()
          break
      }
      // 触发所有图表 resize
      resizeAllCharts()
    }, 100)
  })
}

// 处理窗口大小变化
const handleResize = () => {
  resizeAllCharts()
}

// 调整所有图表大小
const resizeAllCharts = () => {
  chartInstances.forEach((chart) => {
    if (chart && !chart.isDisposed()) {
      chart.resize()
    }
  })
}

// 退出大屏
const handleExit = () => {
  router.push('/')
}

// 使用 ResizeObserver 监听图表容器大小变化
const setupResizeObserver = () => {
  const observer = new ResizeObserver(() => {
    resizeAllCharts()
  })

  // 监听所有图表容器
  const chartIds = [
    'appointmentTrendChart',
    'paymentStatusChart',
    'titleDistributionChart',
    'departmentBusyChart',
    'doctorWorkloadChart',
    'userGrowthChart',
    'patientTypeChart',
    'timeSlotChart'
  ]

  chartIds.forEach((id) => {
    const element = document.getElementById(id)
    if (element) {
      observer.observe(element)
    }
  })

  return observer
}

let resizeObserver = null

onMounted(() => {
  // 初始化第一个 Tab 的图表
  nextTick(() => {
    setTimeout(() => {
      initOverviewCharts()
      resizeObserver = setupResizeObserver()
    }, 300)
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  // 销毁所有图表实例
  chartInstances.forEach((chart) => {
    if (chart && !chart.isDisposed()) {
      chart.dispose()
    }
  })
  chartInstances.clear()
})
</script>

<style scoped>
.stats-canvas {
  min-height: 100vh;
  background-color: #f7fafc;
  padding: 0;
}

/* 顶部导航 */
.top-header {
  background-color: #fff;
  padding: 0 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

/* 返回按钮区域 */
.back-area {
  margin: 0 24px 12px 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 顶部欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #9f7aea 0%, #667eea 100%);
  border-radius: 12px;
  padding: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  margin: 0 24px 24px 24px;
}

.banner-content h1 {
  font-size: 24px;
  margin: 0 0 8px 0;
  font-weight: bold;
}

.banner-content p {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
}

.banner-image {
  border-radius: 50%;
  object-fit: cover;
  width: 120px;
  height: 120px;
  border: 4px solid white;
}

/* 内容包装器 */
.content-wrapper {
  padding: 0 24px 24px 24px;
}

.dashboard-tabs {
  background: transparent;
}

.dashboard-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.dashboard-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 20px;
}

.tab-content {
  padding: 0;
}

/* 核心指标卡片 */
.top-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
  border-left: 4px solid;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.stat-card-green {
  border-left-color: #48bb78;
}

.stat-card-blue {
  border-left-color: #4299e1;
}

.stat-card-red {
  border-left-color: #f56565;
}

.stat-card-purple {
  border-left-color: #9f7aea;
}

.stat-card-orange {
  border-left-color: #f59e0b;
}

.stat-card-indigo {
  border-left-color: #667eea;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.stat-card-green .stat-icon {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
}

.stat-card-blue .stat-icon {
  background: linear-gradient(135deg, #4299e1 0%, #3182ce 100%);
  color: white;
}

.stat-card-red .stat-icon {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
}

.stat-card-purple .stat-icon {
  background: linear-gradient(135deg, #9f7aea 0%, #805ad5 100%);
  color: white;
}

.stat-card-orange .stat-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.stat-card-indigo .stat-icon {
  background: linear-gradient(135deg, #667eea 0%, #5a67d8 100%);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: normal;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

/* 图表区域 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-card {
  border-radius: 8px;
}

.chart-card :deep(.el-card__body) {
  padding: 20px;
}

.chart-card.chart-full-width {
  grid-column: 1 / -1;
}

.card-header {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.chart {
  width: 100%;
  height: 350px;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.chart-full-width {
    grid-column: 1;
  }
}

@media (max-width: 768px) {
  .welcome-banner {
    margin: 0 20px 20px 20px;
    padding: 20px;
    flex-direction: column;
    text-align: center;
  }

  .banner-content h1 {
    font-size: 20px;
  }

  .banner-content p {
    font-size: 14px;
  }

  .banner-image {
    width: 100px;
    height: 100px;
    margin-top: 15px;
  }

  .content-wrapper {
    padding: 0 20px 20px 20px;
  }

  .top-cards {
    grid-template-columns: 1fr;
  }

  .header-left h2 {
    font-size: 18px;
  }

  .stat-value {
    font-size: 28px;
  }
}
</style>
