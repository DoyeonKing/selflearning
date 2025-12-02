# 患者端 API 使用说明

## 目录结构

```
uni_app/
├── api/              # API接口定义
│   └── patient.js    # 患者端接口
├── utils/            # 工具函数
│   └── request.js    # 请求封装
├── config/           # 配置文件
│   └── index.js      # 应用配置
└── pages/
    └── login/
        └── patient-login.vue  # 患者登录页面
```

## 配置说明

### 1. 修改API地址

如需修改API服务器地址，请编辑 `config/index.js`：

```javascript
// 开发环境
const development = {
	baseURL: 'http://localhost:8080',  // 修改这里
}

// 生产环境
const production = {
	baseURL: 'https://your-production-api.com',  // 修改这里
}
```

## API 接口列表

### 1. 患者登录
```javascript
import { loginPatient } from '@/api/patient.js'

loginPatient({
  identifier: '学号',
  password: '密码'
}).then(res => {
  if (res.code === '200') {
    // 登录成功
    const token = res.data.token
    const patientInfo = res.data.patientInfo
  }
})
```

### 2. 验证初始登录（激活第一步）
```javascript
import { verifyInitialLogin } from '@/api/patient.js'

verifyInitialLogin({
  identifier: '学号',
  password: '初始密码'
}).then(res => {
  if (res.code === '200') {
    // 验证成功
    const idCard = res.data.idCard  // 脱敏的身份证号
    const phone = res.data.phone
  }
})
```

### 3. 身份验证（激活第二步）
```javascript
import { verifyIdentity } from '@/api/patient.js'

verifyIdentity({
  identifier: '学号',
  idCardLast6: '身份证后6位',
  newPassword: '新密码'
}).then(res => {
  if (res.code === '200') {
    // 激活成功
  }
})
```

### 4. 检查账户激活状态
```javascript
import { checkActivationStatus } from '@/api/patient.js'

checkActivationStatus('学号').then(res => {
  if (res.code === '200') {
    const isActivated = res.data.isActivated
  }
})
```

### 5. 登出
```javascript
import { logoutPatient } from '@/api/patient.js'

logoutPatient().then(res => {
  if (res.code === '200') {
    // 登出成功
  }
})
```

## 响应格式

所有接口统一响应格式：

```javascript
{
  code: "200",      // 状态码：200-成功，其他-失败
  msg: "请求成功",   // 响应消息
  data: {}         // 响应数据（可选）
}
```

## Token 管理

登录成功后，token会自动保存到本地存储：

```javascript
// 获取token
const token = uni.getStorageSync('patientToken')

// 获取用户信息
const patientInfo = uni.getStorageSync('patientInfo')

// 清除token（登出时）
uni.removeStorageSync('patientToken')
uni.removeStorageSync('patientInfo')
```

## 错误处理

所有网络请求的错误已在 `utils/request.js` 中统一处理：
- 网络错误会自动显示提示
- HTTP状态码异常会自动处理
- 业务逻辑错误由各接口自行处理

## 添加新接口

1. 在 `api/patient.js` 中添加新的接口方法：

```javascript
export function newApiMethod(data) {
  return post('/api/patient/new-endpoint', data)
}
```

2. 在页面中使用：

```javascript
import { newApiMethod } from '@/api/patient.js'

newApiMethod(data).then(res => {
  // 处理响应
})
```

## 注意事项

1. 所有接口调用都是异步的，使用 `async/await` 或 `.then()` 处理
2. Token会自动添加到请求头，无需手动设置
3. 请求失败时会自动显示错误提示
4. 生产环境记得修改 `config/index.js` 中的API地址

