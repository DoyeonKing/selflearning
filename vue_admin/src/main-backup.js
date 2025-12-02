import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// 如果您正在使用CDN引入，请删除下面一行。
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
//import './assets/index.scss'
import { useAdminStore } from '@/stores/adminStore'

console.log('🚀 开始初始化应用...')

const app = createApp(App)
const pinia = createPinia()

console.log('✅ App 和 Pinia 实例创建成功')

app.use(router)
app.use(pinia)
app.use(ElementPlus, {
    locale: zhCn,
})

console.log('✅ Router, Pinia, ElementPlus 已注册')

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

console.log('✅ ElementPlus 图标已注册')

// 添加全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('❌ Vue全局错误:', err)
  console.error('错误信息:', info)
  console.error('组件实例:', instance)
  // 显示错误到页面
  const appEl = document.getElementById('app')
  if (appEl) {
    appEl.innerHTML = `
      <div style="padding: 20px; color: red; font-family: monospace;">
        <h2>Vue 应用错误</h2>
        <p><strong>错误:</strong> ${err.message}</p>
        <p><strong>位置:</strong> ${info}</p>
        <pre>${err.stack}</pre>
      </div>
    `
  }
}

// 添加未捕获的Promise错误处理
window.addEventListener('unhandledrejection', (event) => {
  console.error('❌ 未处理的Promise拒绝:', event.reason)
  console.error('Promise错误堆栈:', event.reason?.stack)
  event.preventDefault()
})

// 初始化adminStore（必须在pinia注册之后）
try {
  console.log('🔄 开始初始化 AdminStore...')
  const adminStore = useAdminStore()
  adminStore.initializeStore()
  console.log('✅ AdminStore 初始化成功')
} catch (error) {
  console.error('❌ 初始化adminStore失败:', error)
  console.error('错误堆栈:', error.stack)
}

// 挂载应用
try {
  console.log('🔄 开始挂载应用...')
  app.mount('#app')
  console.log('✅ Vue应用挂载成功')
} catch (error) {
  console.error('❌ Vue应用挂载失败:', error)
  console.error('错误堆栈:', error.stack)
  // 显示错误信息到页面
  const appEl = document.getElementById('app')
  if (appEl) {
    appEl.innerHTML = `
      <div style="padding: 20px; color: red; font-family: monospace;">
        <h2>应用启动失败</h2>
        <p><strong>错误信息:</strong> ${error.message}</p>
        <pre>${error.stack}</pre>
        <p>请打开浏览器控制台查看详细错误信息</p>
      </div>
    `
  }
}



