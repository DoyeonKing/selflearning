<template>
  <div class="back-button" @click="goBack" role="button" aria-label="返回上一级" tabindex="0" @keydown.enter="goBack" @keydown.space.prevent="goBack">
    <img :src="icon" alt="返回" class="back-icon" />
    <span class="back-text">返回</span>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import icon from '@/assets/dog1.jpg'

const router = useRouter()

function goBack() {
  // 获取当前路由信息
  const currentRoute = router.currentRoute.value;
  
  // 根据当前页面确定返回路径，避免循环跳转
  if (currentRoute.path.includes('/create')) {
    // 创建页面返回到对应的列表页面
    if (currentRoute.path.includes('/regulations/create')) {
      router.push('/regulations');
    } else if (currentRoute.path.includes('/departments/create')) {
      router.push('/departments');
    } else if (currentRoute.path.includes('/users/create')) {
      router.push('/users');
    } else {
      router.push('/');
    }
  } else if (currentRoute.path.includes('/edit')) {
    // 编辑页面返回到对应的列表页面
    if (currentRoute.path.includes('/regulations/edit')) {
      router.push('/regulations');
    } else if (currentRoute.path.includes('/users/edit')) {
      router.push('/users');
    } else {
      router.push('/');
    }
  } else if (currentRoute.path.includes('/members')) {
    // 成员页面返回到科室列表
    router.push('/departments');
  } else if (currentRoute.path.includes('/import')) {
    // 导入页面返回到用户列表
    router.push('/users');
  } else if (currentRoute.path.includes('/search')) {
    // 搜索页面返回到用户列表
    router.push('/users');
  } else if (currentRoute.path.includes('/history')) {
    // 历史页面返回到用户列表
    router.push('/users');
  } else if (currentRoute.path.includes('/scheduling')) {
    // 排班相关页面返回到首页
    router.push('/');
  } else {
    // 其他情况返回到首页
    router.push('/');
  }
}
</script>

<style scoped>
.back-button {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
}
.back-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.back-text {
  margin-left: 8px;
  font-size: 14px;
  color: #303133;
}
</style>


