<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <el-card shadow="always">
      <template #header>
        <div class="card-header-title">
          <span>挂号工时 & 门诊人次</span>
          <div>
            <el-button :loading="loading" type="primary" @click="loadAndCompute">查询</el-button>
            <el-button @click="exportCsv" :disabled="!rows.length">导出 CSV</el-button>
            <el-button :loading="downloading" @click="exportExcel" :disabled="!rows.length">导出 Excel</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="logic-hint"
        title="计算逻辑提示"
        description="仅统计状态为 completed 且存在 check_in_time 的号源；同一医生同一天按时间排序，空档 ≥ 90 分钟自动切成多段；每段工时 = 首诊至末诊跨度并统一 +0.5h 缓冲，夜班判定基于 18:00 时间阈值。"
        show-icon
      />

      <el-form :inline="true" :model="filters" class="search-form">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            range-separator="至"
            :unlink-panels="true"
          />
        </el-form-item>

        <el-form-item label="上级科室">
          <el-select v-model="filters.parentDeptId" placeholder="选择上级科室" style="width: 220px" @change="onParentChange">
            <el-option v-for="pd in parentDepartments" :key="pd.value" :label="pd.label" :value="pd.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="科室">
          <el-select v-model="filters.deptId" placeholder="选择科室" style="width: 220px" @change="onDeptChange">
            <el-option v-for="d in subDepartments" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="医生">
          <el-select v-model="filters.doctorId" placeholder="全部医生" clearable filterable style="width: 220px">
            <el-option v-for="doc in doctors" :key="doc.value" :label="doc.label" :value="doc.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" border style="width: 100%; margin-top: 8px;">
        <el-table-column prop="doctorName" label="医生" min-width="140" fixed="left" />
        <el-table-column prop="departmentName" label="所属科室" min-width="160" />
        <el-table-column prop="workDate" label="日期" width="120" />
        <el-table-column prop="segmentLabel" label="班段" width="100" />
        <el-table-column label="首诊时间" width="170">
          <template #default="scope">
            {{ scope.row.firstCallDisplay || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="末诊时间" width="170">
          <template #default="scope">
            {{ scope.row.lastEndDisplay || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="rawHours" label="原始工时(h)" width="130">
          <template #default="scope">
            {{ Number(scope.row.rawHours).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="regHours" label="挂号工时(h)" width="140">
          <template #default="scope">
            {{ Number(scope.row.regHours).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="visitCount" label="门诊人次" width="110" />
        <el-table-column label="夜班" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.nightFlag ? 'danger' : 'info'" size="small">
              {{ scope.row.nightFlag ? '夜班' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="locations" label="诊室/地点" min-width="160" />
        <el-table-column prop="performancePoints" label="绩效点数" width="120">
          <template #default="scope">
            {{ Number(scope.row.performancePoints).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="stats-bar" v-if="rows.length" style="margin-top: 12px; display: flex; gap: 16px; color: #606266;">
        <div>统计医生数：{{ statDistinctDoctors }}</div>
        <div>门诊人次：{{ statTotalVisits }}</div>
        <div>挂号工时合计：{{ statTotalRegHours }} 小时</div>
        <div>夜班班段：{{ statNightSegments }}</div>
      </div>
    </el-card>
  </div>
  
</template>

<script setup>
import BackButton from '@/components/BackButton.vue';
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getAllParentDepartments, getDepartmentsByParentId, getDoctorsByDepartmentId } from '@/api/department';
import { getRegistrationHours, exportRegistrationHoursExcel } from '@/api/report';

const loading = ref(false);
const downloading = ref(false);

const parentDepartments = ref([]); // [{ label, value }]
const subDepartments = ref([]); // [{ label, value }]
const doctors = ref([]); // [{ label, value }]

const filters = ref({
  dateRange: [],
  parentDeptId: null,
  deptId: null,
  doctorId: null,
});

const rows = ref([]);

const onParentChange = async () => {
  filters.value.deptId = null;
  subDepartments.value = [];
  doctors.value = [];
  if (!filters.value.parentDeptId) return;
  try {
    const children = await getDepartmentsByParentId(filters.value.parentDeptId);
    subDepartments.value = Array.isArray(children)
      ? children.map(c => ({
          label: c.name || c.label || '',
          value: c.departmentId ?? c.id ?? c.value,
        })).filter(c => c.value != null)
      : [];
  } catch (e) {
    ElMessage.error('加载子科室失败');
  }
};

const onDeptChange = async () => {
  filters.value.doctorId = null;
  doctors.value = [];
  if (!filters.value.deptId) return;
  try {
    const list = await getDoctorsByDepartmentId(filters.value.deptId);
    doctors.value = Array.isArray(list)
      ? list.map(d => ({
          label: d.fullName || d.name || d.label || d.identifier || '',
          value: d.doctorId ?? d.id ?? d.value ?? d.identifier,
        })).filter(d => d.value != null)
      : [];
  } catch (e) {
    ElMessage.error('加载医生列表失败');
  }
};

const statDistinctDoctors = computed(() => new Set(rows.value.map(r => r.doctorId)).size);
const statTotalVisits = computed(() => rows.value.reduce((s, r) => s + (r.visitCount || 0), 0));
const statTotalRegHours = computed(() => rows.value
  .reduce((s, r) => s + (Number(r.regHours) || 0), 0)
  .toFixed(2)
);
const statNightSegments = computed(() => rows.value.filter(r => r.nightFlag).length);

const loadAndCompute = async () => {
  if (!filters.value.dateRange || filters.value.dateRange.length !== 2) {
    ElMessage.warning('请选择日期范围');
    return;
  }
  if (!filters.value.deptId) {
    ElMessage.warning('请选择科室');
    return;
  }

  loading.value = true;
  try {
    const [startDate, endDate] = filters.value.dateRange;
    const backend = await getRegistrationHours({
      departmentId: filters.value.deptId,
      startDate,
      endDate,
      doctorId: filters.value.doctorId || undefined,
    });
    rows.value = Array.isArray(backend)
      ? backend.map(enhanceRow)
      : [];
    printDebugInfo(rows.value);
  } catch (e) {
    ElMessage.error('加载或计算工时失败');
  } finally {
    loading.value = false;
  }
};

const enhanceRow = (row) => {
  const regHours = Number(row.regHours ?? 0);
  const rawHours = Number(row.rawHours ?? 0);
  const performancePoints = Number(row.performancePoints ?? 0);
  return {
    ...row,
    regHours,
    rawHours,
    performancePoints,
    firstCallDisplay: formatDateTime(row.firstCallTime),
    lastEndDisplay: formatDateTime(row.lastEndTime),
  };
};

const exportCsv = () => {
  if (!rows.value.length) return;
  const headers = ['医生', '科室', '日期', '班段', '首诊时间', '末诊时间', '原始工时(h)', '挂号工时(h)', '门诊人次', '夜班标记', '诊室/地点', '绩效点数'];
  const dataLines = rows.value.map(r => [
    wrapCsv(r.doctorName),
    wrapCsv(r.departmentName || ''),
    wrapCsv(r.workDate || ''),
    wrapCsv(r.segmentLabel || ''),
    wrapCsv(r.firstCallDisplay || ''),
    wrapCsv(r.lastEndDisplay || ''),
    String(Number(r.rawHours).toFixed(2)),
    String(Number(r.regHours).toFixed(2)),
    String(r.visitCount || 0),
    r.nightFlag ? '夜班' : '否',
    wrapCsv(r.locations || ''),
    String(Number(r.performancePoints).toFixed(2)),
  ].join(','));
  const csv = [headers.join(','), ...dataLines].join('\n');
  const blob = new Blob(["\uFEFF" + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `医生工时统计-${Date.now()}.csv`;
  a.click();
  URL.revokeObjectURL(url);
};

const exportExcel = async () => {
  if (!rows.value.length || !filters.value.dateRange?.length) return;
  downloading.value = true;
  try {
    const [startDate, endDate] = filters.value.dateRange;
    const blobData = await exportRegistrationHoursExcel({
      departmentId: filters.value.deptId,
      startDate,
      endDate,
      doctorId: filters.value.doctorId || undefined,
    });
    const blob = blobData instanceof Blob
      ? blobData
      : new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `挂号工时-${startDate}-${endDate}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    ElMessage.error('导出 Excel 失败');
  } finally {
    downloading.value = false;
  }
};

const wrapCsv = (text) => {
  if (text == null) return '';
  const s = String(text).replaceAll('"', '""');
  if (/,|\n|\r|"/.test(s)) return `"${s}"`;
  return s;
};

const formatDateTime = (value) => {
  if (!value) return '';
  if (typeof value === 'string' && value.includes(' ') && !value.includes('T')) {
    // 后端直接返回 "yyyy-MM-dd HH:mm:ss" 字符串时，保持原样避免再次触发时区换算
    return value;
  }
  const normalized = typeof value === 'string' ? value.replace(' ', 'T') : value;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value.replace('T', ' ') : '';
  }
  const yyyy = date.getFullYear();
  const MM = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${yyyy}-${MM}-${dd} ${hh}:${mm}`;
};

const printDebugInfo = (data) => {
  console.group('[工时调试] 计算完成');
  console.log('样本数量:', data.length);
  console.log('规则: completed + check_in_time, 班段空档≥90min切段, raw=跨度, reg=raw+0.5h, 夜班≥18:00');
  console.table(
    data.map(item => ({
      医生: item.doctorName,
      日期: item.workDate,
      班段: item.segmentLabel,
      首诊: item.firstCallDisplay,
      末诊: item.lastEndDisplay,
      rawHours: item.rawHours,
      regHours: item.regHours,
      visitCount: item.visitCount,
      night: item.nightFlag,
    }))
  );
  console.groupEnd();
};

onMounted(async () => {
  try {
    const parents = await getAllParentDepartments();
    parentDepartments.value = Array.isArray(parents)
      ? parents.map(p => ({
          label: p.name || p.label || '',
          value: p.parentDepartmentId ?? p.id ?? p.value,
        })).filter(p => p.value != null)
      : [];
  } catch (e) {
    // 忽略
  }
});
</script>

<style scoped>
.app-container { padding: 16px; }
.card-header-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form { margin-bottom: 8px; }
</style>


