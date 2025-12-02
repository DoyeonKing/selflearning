<template>
  <div class="back-button" @click="goBack" role="button" aria-label="返回上一级" tabindex="0" @keydown.enter="goBack" @keydown.space.prevent="goBack">
    <img src="@/assets/doctor.jpg" alt="返回" class="back-icon" />
    <span class="back-text">返回工作台</span>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  // 医生端的所有页面都返回到医生工作台
  router.push('/doctor-dashboard');
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