# 白屏问题排查指南

## 已修复的问题

1. ✅ ElementPlus CSS 导入已恢复
2. ✅ 路由重复定义已修复
3. ✅ adminStore 未定义变量已修复
4. ✅ adminStore 初始化时机已优化
5. ✅ 添加了全局错误处理

## 排查步骤

### 1. 检查浏览器控制台
按 F12 打开开发者工具，查看 Console 标签页是否有错误信息。

常见错误：
- `Failed to resolve module` - 模块导入错误
- `Cannot read property` - 对象属性访问错误
- `Uncaught TypeError` - 类型错误

### 2. 检查网络请求
在 Network 标签页中查看：
- 是否有 404 错误（资源文件未找到）
- 是否有 500 错误（服务器错误）
- 静态资源是否正常加载

### 3. 清除缓存
- 按 Ctrl+Shift+Delete 清除浏览器缓存
- 或者在开发者工具中右键刷新按钮，选择"清空缓存并硬性重新加载"

### 4. 检查路由
访问 `http://localhost:5173/login` 直接进入登录页，看是否能正常显示。

### 5. 检查依赖
确保所有依赖已正确安装：
```bash
cd vue_admin
npm install
```

### 6. 重启开发服务器
```bash
# 停止当前服务器（Ctrl+C）
# 重新启动
npm run dev
```

## 常见问题解决方案

### 问题：控制台显示 "Cannot find module"
**解决**：检查导入路径是否正确，文件是否存在

### 问题：控制台显示 "Pinia store not found"
**解决**：确保 pinia 已正确注册（已在 main.js 中修复）

### 问题：页面完全空白，控制台无错误
**解决**：
1. 检查 `index.html` 中是否有 `<div id="app"></div>`
2. 检查路由配置是否正确
3. 检查是否有组件渲染错误

## 调试信息

应用启动时会在控制台输出：
- ✅ AdminStore 初始化成功
- ✅ Vue应用挂载成功

如果看到 ❌ 标记，说明对应步骤失败，请查看详细错误信息。



