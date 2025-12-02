<template>
  <div class="debug-page">
    <h1>调试页面</h1>
    <div class="debug-info">
      <h2>登录状态</h2>
      <p>是否已登录: {{ adminStore.isAuthenticated ? '是' : '否' }}</p>
      <p>管理员信息: {{ JSON.stringify(adminStore.loggedInAdminBasicInfo, null, 2) }}</p>
      <p>详细信息: {{ JSON.stringify(adminStore.detailedAdminInfo, null, 2) }}</p>
      
      <h2>当前路由</h2>
      <p>路径: {{ $route.path }}</p>
      <p>名称: {{ $route.name }}</p>
      <p>Meta: {{ JSON.stringify($route.meta, null, 2) }}</p>
      
      <h2>操作</h2>
      <el-button @click="testLogin">测试登录</el-button>
      <el-button @click="testLogout">测试登出</el-button>
      <el-button @click="goToAdmin">跳转到管理页面</el-button>
    </div>
  </div>
</template>

<script setup>
import { useAdminStore } from '@/stores/adminStore'
import { useRouter } from 'vue-router'

const adminStore = useAdminStore()
const router = useRouter()

const testLogin = () => {
  adminStore.loginSuccess({
    adminId: 'test',
    name: '测试管理员',
    permissionLevel: 'super'
  }, {
    adminId: 'test',
    token: 'test-token'
  })
  console.log('测试登录完成')
}

const testLogout = () => {
  adminStore.logout()
  console.log('测试登出完成')
}

const goToAdmin = () => {
  router.push('/admin')
}
</script>

<style scoped>
.debug-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.debug-info {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.debug-info h2 {
  color: #333;
  margin-top: 20px;
}

.debug-info p {
  background: white;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  margin: 10px 0;
}

.el-button {
  margin: 5px;
}
</style>



