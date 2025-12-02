import axios from "axios";
import {ElMessage} from "element-plus";

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000  // 后台接口超时时间
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
  console.log('=== 请求拦截器 ===');
  console.log('请求URL:', config.baseURL + config.url);
  console.log('请求方法:', config.method);
  console.log('请求参数:', config.params);
  console.log('请求头:', config.headers);
  console.log('================');
  
  // 如果是FormData类型，不设置Content-Type，让浏览器自动设置（包含boundary）
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
    if (response?.config?.responseType === 'blob') {
      return response.data;
    }

    let res = response.data;
    
    // 处理字符串响应
    if (typeof res === 'string') {
      try {
        res = res ? JSON.parse(res) : res;
      } catch (e) {
        // 如果解析失败，可能是错误消息，直接返回
        console.warn('无法解析字符串响应:', res);
        return res;
      }
    }

    // 深拷贝处理，避免响应式污染
    try {
        if (res && typeof res === 'object') {
             res = JSON.parse(JSON.stringify(res));
        }
    } catch (e) {
        console.error("Deep copy failed in request interceptor:", e);
    }

    return res;
  },
  error => {
    console.error('请求错误:', error);
    
    if (error.response) {
      const status = error.response.status;
      const data = error.response.data;
      
      if (status === 404) {
        ElMessage.error('未找到请求接口');
      } else if (status === 500) {
        ElMessage.error('系统异常，请查看后端控制台报错');
      } else if (status >= 400 && status < 500) {
        // 客户端错误，显示后端返回的错误信息
        const errorMsg = typeof data === 'string' ? data : (data?.message || '请求参数错误');
        ElMessage.error(errorMsg);
      } else {
        ElMessage.error('服务器错误');
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接');
    } else {
      ElMessage.error(error.message || '请求失败');
    }
    
    return Promise.reject(error);
  }
)
//导出request
export default request