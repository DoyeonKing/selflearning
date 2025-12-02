<template>
  <div class="module-dashboard">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>专业的账户管理</h1>
        <p>安全、高效地维护系统用户数据</p>
      </div>
      <!-- 使用本地图片 -->
      <img :src="doctorImage" alt="医生卡通形象" class="banner-image">
    </div>

    <div class="module-header">
      <h2>功能导航</h2>
      <p>请选择下面的功能进行操作。</p>
    </div>

    <div class="feature-grid">
      <router-link to="/users/create" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper"><el-icon :size="32"><Plus /></el-icon></div><h3>创建账户</h3></div>
      </router-link>
      <!-- 批量导入患者 -->
      <router-link to="/users/import-patient" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper" style="background-color: #e6f7ff; color: #1890ff;"><el-icon :size="32"><Upload /></el-icon></div><h3>批量导入患者</h3><p style="font-size: 12px; color: #999; margin: 4px 0 0;">支持学生/教师/职工</p></div>
      </router-link>
      <!-- 批量导入医生 -->
      <router-link to="/users/import-doctor" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper" style="background-color: #fef4e6; color: #f59e0b;"><el-icon :size="32"><Upload /></el-icon></div><h3>批量导入医生</h3><p style="font-size: 12px; color: #999; margin: 4px 0 0;">支持职称/专长</p></div>
      </router-link>
      <router-link to="/users/search" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper"><el-icon :size="32"><Search /></el-icon></div><h3>用户信息搜索</h3></div>
      </router-link>
      <router-link to="/users/edit" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper"><el-icon :size="32"><Edit /></el-icon></div><h3>编辑账户</h3></div>
      </router-link>
      <router-link to="/users/history" class="feature-card-link">
        <div class="feature-card"><div class="icon-wrapper"><el-icon :size="32"><Memo /></el-icon></div><h3>用户病史修改</h3></div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { Plus, Search, Edit, Memo, Upload } from '@element-plus/icons-vue';
// 导入本地图片
import doctorImage from '@/assets/doctor.jpg';
import BackButton from '@/components/BackButton.vue';
</script>

<style scoped>
.module-dashboard {
  padding: 24px;
  background-color: #f7fafc;
}

.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  margin-bottom: 24px;
}
.banner-content h1 {
  font-size: 24px;
  margin: 0 0 8px 0;
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

.module-header {
  margin-bottom: 24px;
}
.module-header h2 {
  font-size: 22px;
  color: #2d3748;
}
.module-header p {
  font-size: 16px;
  color: #718096;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}
.feature-card-link { text-decoration: none; }
.feature-card {
  background-color: white;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
}
.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}
.icon-wrapper {
  width: 64px; height: 64px;
  border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  margin: 0 auto 16px;
  background-color: #edf2f7;
  color: #4a5568;
}
.feature-card h3 {
  font-size: 16px;
  color: #2d3748;
  margin: 0;
}
</style>

