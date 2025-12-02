<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>

    <h2>患者档案管理</h2>
    <el-card shadow="always">
      <!-- 刷新按钮 -->
      <div style="margin-bottom: 16px;">
        <el-button type="primary" @click="loadMedicalHistories" :loading="loading">
          刷新数据
        </el-button>
      </div>
      <!-- 骨架屏加载 -->
      <div v-if="loading && medicalHistories.length === 0" class="skeleton-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <!-- 数据表格 -->
      <el-table v-else :data="medicalHistories" border stripe v-loading="loading">
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="allergyHistory" label="过敏史" width="200">
          <template #default="{ row }">
            <el-input 
              v-model="row.allergyHistory" 
              type="textarea" 
              :rows="2"
              placeholder="请输入过敏史" />
          </template>
        </el-table-column>
        <el-table-column prop="pastMedicalHistory" label="既往病史" width="200">
          <template #default="{ row }">
            <el-input 
              v-model="row.pastMedicalHistory" 
              type="textarea" 
              :rows="2"
              placeholder="请输入既往病史" />
          </template>
        </el-table-column>
        <el-table-column prop="isBlacklisted" label="黑名单状态" width="120">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isBlacklisted"
              active-text="是"
              inactive-text="否" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleSave(row)"
              :loading="row.saving">
              保存
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>
  </div>
</template>

<script setup>
import BackButton from '@/components/BackButton.vue';
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMedicalHistories, updateMedicalHistory } from '@/api/user';

const medicalHistories = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 加载病历历史记录
const loadMedicalHistories = async () => {
  loading.value = true;
  try {
    const response = await getMedicalHistories(currentPage.value, pageSize.value);
    
    // 处理响应数据（request.js拦截器已返回response.data）
    if (response && response.content && Array.isArray(response.content)) {
      // 标准分页响应结构
      medicalHistories.value = response.content.map(item => ({
        ...item,
        saving: false
      }));
      total.value = response.totalElements || 0;
    } else if (Array.isArray(response)) {
      // 直接返回数组
      medicalHistories.value = response.map(item => ({
        ...item,
        saving: false
      }));
      total.value = response.length;
    } else {
      medicalHistories.value = [];
      total.value = 0;
      ElMessage.warning('暂无病历数据');
    }
    
  } catch (error) {
    console.error('加载病历历史记录失败:', error);
    ElMessage.error(error.response?.data?.message || '加载数据失败，请重试');
    medicalHistories.value = [];
  } finally {
    loading.value = false;
  }
};

// 保存病历信息
const handleSave = async (row) => {
  row.saving = true;
  try {
    if (!row.id) {
      ElMessage.error('患者ID不能为空');
      return;
    }

    // 构造更新请求数据
    const updateData = {
      allergyHistory: row.allergyHistory || '',
      pastMedicalHistory: row.pastMedicalHistory || '',
      blacklistStatus: row.isBlacklisted ? 'blacklisted' : 'normal'
    };

    await updateMedicalHistory(row.id, updateData);
    ElMessage.success(`患者 ${row.name} 的病历信息已保存`);
    
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error(error.response?.data?.message || '保存失败，请重试');
  } finally {
    row.saving = false;
  }
};

// 分页变化
const handlePageChange = (page) => {
  currentPage.value = page;
  loadMedicalHistories();
};

// 每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadMedicalHistories();
};

// 页面加载时获取数据
onMounted(() => {
  loadMedicalHistories();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.skeleton-container {
  padding: 20px;
}
</style>
