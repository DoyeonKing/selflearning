console.log('🚀 [1/8] 开始加载 main.js...')

console.log('🚀 [2/8] 导入 Vue 和相关依赖...')
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/adminStore'

console.log('✅ [2/8] 依赖导入成功')

try {
  console.log('🚀 [3/8] 创建应用实例...')
  const app = createApp(App)
  const pinia = createPinia()
  console.log('✅ [3/8] 应用实例创建成功')
  
  console.log('🚀 [4/8] 注册插件...')
  app.use(router)
  app.use(pinia)
  app.use(ElementPlus, {
    locale: zhCn,
  })
  console.log('✅ [4/8] 插件注册成功')
  
  console.log('🚀 [5/8] 注册 ElementPlus 图标...')
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }
  console.log('✅ [5/8] 图标注册成功')
  
  // 添加全局错误处理
  app.config.errorHandler = (err, instance, info) => {
    console.error('❌ Vue全局错误:', err)
    console.error('错误信息:', info)
    console.error('组件实例:', instance)
    // 显示错误到页面
    const appEl = document.getElementById('app')
    if (appEl) {
      appEl.innerHTML = `
        <div style="padding: 20px; color: red; font-family: monospace; background: #fff; border: 2px solid red; margin: 20px;">
          <h2>Vue 应用错误</h2>
          <p><strong>错误:</strong> ${err.message}</p>
          <p><strong>位置:</strong> ${info}</p>
          <pre style="background: #f5f5f5; padding: 10px; overflow: auto;">${err.stack || '无堆栈信息'}</pre>
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
  
  console.log('🚀 [6/8] 初始化 AdminStore...')
  try {
    const adminStore = useAdminStore()
    adminStore.initializeStore()
    console.log('✅ [6/8] AdminStore 初始化成功')
  } catch (error) {
    console.error('❌ [6/8] 初始化adminStore失败:', error)
    console.error('错误堆栈:', error.stack)
  }
  
  console.log('🚀 [7/8] 挂载应用到 DOM...')
  const appEl = document.getElementById('app')
  if (!appEl) {
    throw new Error('找不到 #app 元素！请检查 index.html')
  }
  
  app.mount('#app')
  console.log('✅ [7/8] Vue应用挂载成功')
  console.log('✅ [8/8] 应用启动完成！')
  
} catch (error) {
  console.error('❌ 应用启动过程中发生错误:', error)
  console.error('错误堆栈:', error.stack)
  
  // 显示错误信息到页面
  const appEl = document.getElementById('app')
  if (appEl) {
    appEl.innerHTML = `
      <div style="padding: 20px; color: red; font-family: monospace; background: #fff; border: 2px solid red; margin: 20px;">
        <h2>❌ 应用启动失败</h2>
        <p><strong>错误信息:</strong> ${error.message}</p>
        <pre style="background: #f5f5f5; padding: 10px; overflow: auto;">${error.stack || '无堆栈信息'}</pre>
        <p style="margin-top: 20px;">请打开浏览器控制台（F12）查看详细错误信息</p>
      </div>
    `
  } else {
    document.body.innerHTML = `
      <div style="padding: 20px; color: red;">
        <h2>严重错误：找不到 #app 元素</h2>
        <p>请检查 index.html 文件是否包含 &lt;div id="app"&gt;&lt;/div&gt;</p>
      </div>
    `
  }
}
