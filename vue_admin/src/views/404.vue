<template>
  <div>
    <img src="@/assets/404.png" alt="">

    <div>
      <el-button type="info" style="width: 50px;height: 20px" @click="router.push('/loginform')"></el-button>
    </div>




  </div>
</template>
<script setup lang="ts">
import router from "@/router/index.js";
</script>