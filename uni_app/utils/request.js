/**
 * uni-app 网络请求封装
 * 统一处理请求和响应
 */
import config from '../config/index.js'

/**
 * 获取API基础URL
 */
function getBaseURL() {
	return config.baseURL
}

/**
 * 封装的request方法
 * @param {Object} options 请求配置
 * @returns {Promise}
 */
function request(options) {
	return new Promise((resolve, reject) => {
		const fullUrl = getBaseURL() + options.url
		const startTime = Date.now() // 记录请求开始时间
		
		console.log(`[REQUEST] ${options.method || 'GET'} ${fullUrl}`)
		console.log('[REQUEST] 请求参数:', options.data || {})
		
		// 显示加载提示（可选，通过 options.showLoading 控制）
		if (options.showLoading !== false) {
			uni.showLoading({
				title: options.loadingText || '加载中...',
				mask: true
			})
		}
		
		uni.request({
			url: fullUrl,
			method: options.method || 'GET',
			data: options.data || {},
			timeout: options.timeout || 30000, // 默认30秒超时（真机调试网络可能较慢）
			header: {
				'Content-Type': 'application/json',
				// 如果有token，自动添加到请求头
				...(options.header || {}),
				...getAuthHeader()
			},
			success: (res) => {
				const duration = Date.now() - startTime
				console.log(`[RESPONSE] ${options.method || 'GET'} ${fullUrl}`)
				console.log(`[RESPONSE] 耗时: ${duration}ms`)
				console.log('[RESPONSE] 状态码:', res.statusCode)
				console.log('[RESPONSE] 响应数据:', res.data)
				
				// 隐藏加载提示
				if (options.showLoading !== false) {
					uni.hideLoading()
				}
				
				// 如果请求时间超过3秒，记录警告
				if (duration > 3000) {
					console.warn(`[PERFORMANCE] 请求耗时较长: ${duration}ms, URL: ${fullUrl}`)
				}
				
				// 统一处理响应
				if (res.statusCode === 200) {
					// 后端返回的数据格式：
					// 1. 标准格式：{ code, msg, data }
					// 2. 简单格式：{ message } 或 { error }
					// 3. Spring Boot ResponseEntity: 直接返回数据（可能是数组或对象）
					resolve(res.data)
				} else if (res.statusCode === 400) {
					// 400 Bad Request - 业务逻辑错误
					// 后端返回 {"error": "..."}
					resolve(res.data)
				} else {
					console.error('[RESPONSE] 非200状态码:', res.statusCode, res.data)
					uni.showToast({
						title: '请求失败',
						icon: 'none'
					})
					reject(res)
				}
			},
			fail: (err) => {
				const duration = Date.now() - startTime
				console.error('[REQUEST] 请求失败:', err)
				console.error('[REQUEST] 失败URL:', fullUrl)
				console.error('[REQUEST] 请求耗时:', duration + 'ms')
				
				// 隐藏加载提示（添加错误保护）
				if (options.showLoading !== false) {
					try {
						uni.hideLoading()
					} catch (e) {
						// 忽略hideLoading错误（可能toast不存在）
						console.warn('[REQUEST] hideLoading失败:', e)
					}
				}
				
				// 根据错误类型给出提示
				if (err.errMsg && err.errMsg.includes('timeout')) {
					uni.showToast({
						title: '请求超时，请检查网络',
						icon: 'none',
						duration: 2000
					})
				} else if (err.errMsg && err.errMsg.includes('CONNECTION_REFUSED')) {
					uni.showToast({
						title: '无法连接服务器，请检查网络配置',
						icon: 'none',
						duration: 2000
					})
				}
				
				reject(err)
			}
		})
	})
}

/**
 * 获取认证header
 */
function getAuthHeader() {
	const token = uni.getStorageSync('patientToken')
	if (token) {
		return {
			'Authorization': 'Bearer ' + token
		}
	}
	return {}
}

/**
 * GET请求
 */
export function get(url, data = {}, options = {}) {
	return request({
		url,
		method: 'GET',
		data,
		...options // 支持传递额外的配置选项
	})
}

/**
 * POST请求
 */
export function post(url, data = {}, options = {}) {
	return request({
		url,
		method: 'POST',
		data,
		...options // 支持传递额外的配置选项（如showLoading, timeout等）
	})
}

/**
 * PUT请求
 */
export function put(url, data = {}) {
	return request({
		url,
		method: 'PUT',
		data
	})
}

/**
 * DELETE请求
 */
export function del(url, data = {}) {
	return request({
		url,
		method: 'DELETE',
		data
	})
}

export default request

