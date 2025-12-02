import axios from "axios";
import {ElMessage} from "element-plus";

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000  // 后台接口超时时间
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
  // 如果是 FormData，让浏览器自动设置 Content-Type（包含 boundary）
  if (!(config.data instanceof FormData)) {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
  }
  return config
}, error => {
  return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
  response => {
    let res = response.data;
    // 兼容服务端返回的字符串数据
    if (typeof res === 'string') {
      res = res ? JSON.parse(res) : res
    }
    return res;
  },
  error => {
    console.error('请求错误详情:', error)
    
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
      return Promise.reject(error)
    }
    
    if (error.response.status === 404) {
      ElMessage.error('未找到请求接口: ' + error.config.url)
    } else if (error.response.status === 500) {
      ElMessage.error('系统异常，请查看后端控制台报错')
    } else if (error.response.status === 400) {
      // 处理 400 错误，可能包含后端返回的错误信息
      const errorData = error.response.data
      if (typeof errorData === 'string') {
        try {
          const parsed = JSON.parse(errorData)
          ElMessage.error(parsed.error || '请求参数错误')
        } catch {
          ElMessage.error(errorData || '请求参数错误')
        }
      } else if (errorData && errorData.error) {
        ElMessage.error(errorData.error)
      } else {
        ElMessage.error('请求参数错误')
      }
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)
//导出request
export default request