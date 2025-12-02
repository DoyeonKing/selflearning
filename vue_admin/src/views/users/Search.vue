<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <h2>用户信息搜索</h2>
    <el-card shadow="always">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="学号/工号"><el-input v-model="searchForm.id" clearable placeholder="请输入学号/工号" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="searchForm.name" clearable placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" clearable placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="患者" value="PATIENT" />
            <el-option label="医生" value="DOCTOR" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 提示信息 -->
      <el-alert
        v-if="searchPerformed && filteredUsers.length === 0"
        title="未找到符合条件的用户"
        type="info"
        :closable="false"
        style="margin-bottom: 16px;">
      </el-alert>

      <el-table :data="filteredUsers" border stripe v-loading="loading">
        <el-table-column prop="id" label="学号/工号" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'PATIENT' ? 'success' : 'primary'">
              {{ row.role === 'PATIENT' ? '患者' : '医生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="id_card" label="身份证号" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="其他信息">
          <template #default="{ row }">
            <span v-if="row.departmentName">科室: {{ row.departmentName }}</span>
            <span v-if="row.patientType">类型: {{ row.patientType }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>
  </div>
</template>

<script setup>
import BackButton from '@/components/BackButton.vue';
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { searchDoctors, searchPatients } from '@/api/user';

const searchForm = reactive({ id: '', name: '', role: '' });
const filteredUsers = ref([]);
const loading = ref(false);
const searchPerformed = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 搜索用户
const handleSearch = async () => {
  loading.value = true;
  searchPerformed.value = true;

  try {
    let allResults = [];
    let totalCount = 0;

    // 根据选择的角色决定调用哪个接口
    if (searchForm.role === '' || searchForm.role === 'DOCTOR') {
      // 搜索医生
      const doctorResponse = await searchDoctors({
        id: searchForm.id,
        name: searchForm.name,
        page: currentPage.value,
        pageSize: pageSize.value
      });
      
      // 处理医生数据
      if (doctorResponse.content && doctorResponse.content.length > 0) {
        const doctors = doctorResponse.content.map(item => {
          const doctorDetail = item.userDetails;
          return {
            id: doctorDetail.identifier || doctorDetail.id,
            name: doctorDetail.fullName || doctorDetail.name,
            role: 'DOCTOR',
            id_card: doctorDetail.idCardNumber || '-',
            phone: formatPhoneNumber(doctorDetail.phoneNumber || doctorDetail.phone),
            status: doctorDetail.status || '-',
            departmentName: doctorDetail.departmentName || '-'
          };
        });
        allResults.push(...doctors);
      }
      
      // 累加医生总数
      totalCount += doctorResponse.totalElements || 0;
    }

    if (searchForm.role === '' || searchForm.role === 'PATIENT') {
      // 搜索患者
      const patientResponse = await searchPatients({
        id: searchForm.id,
        name: searchForm.name,
        page: currentPage.value
      });
      
      // 处理患者数据
      if (patientResponse.content && patientResponse.content.length > 0) {
        const patients = patientResponse.content.map(item => {
          const patientDetail = item.userDetails;
          return {
            id: patientDetail.identifier || patientDetail.id,
            name: patientDetail.fullName || patientDetail.name,
            role: 'PATIENT',
            id_card: patientDetail.idCardNumber || patientDetail.id_card || '-',
            phone: formatPhoneNumber(patientDetail.phoneNumber || patientDetail.phone),
            status: patientDetail.status || '-',
            patientType: patientDetail.patientType || '-'
          };
        });
        allResults.push(...patients);
      }
      
      // 累加患者总数
      totalCount += patientResponse.totalElements || 0;
    }

    filteredUsers.value = allResults;
    total.value = totalCount;

    if (allResults.length === 0) {
      ElMessage.info('未找到符合条件的用户');
    }

  } catch (error) {
    console.error('搜索失败:', error);
    ElMessage.error(error.response?.data?.message || '搜索失败，请重试');
    filteredUsers.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

// 重置搜索
const resetSearch = () => {
  searchForm.id = '';
  searchForm.name = '';
  searchForm.role = '';
  filteredUsers.value = [];
  searchPerformed.value = false;
  currentPage.value = 1;
  total.value = 0;
};

// 分页变化
const handlePageChange = (page) => {
  currentPage.value = page;
  handleSearch();
};

// 格式化手机号，去掉+86前缀
const formatPhoneNumber = (phone) => {
  if (!phone) return '-';
  // 去掉+86前缀
  return phone.replace(/^\+86/, '');
};

// 页面初始化时自动加载数据
onMounted(() => {
  handleSearch();
});
</script>

<style scoped>.app-container{padding:20px;}.search-form{margin-bottom:20px;}</style>
