/**
 * 患者端API接口
 * 基于后端实际的 AuthController 接口
 */
import { post, get } from '../utils/request.js'

/**
 * 患者正常登录
 * 对应后端：POST /api/auth/patient/login
 * @param {Object} data - { identifier, password }
 */
export function loginPatient(data) {
	return post('/api/auth/patient/login', data)
}

/**
 * 验证初始信息（激活第一步）
 * 对应后端：POST /api/auth/verify-patient
 * @param {Object} data - { identifier, initialPassword }
 */
export function verifyInitialLogin(data) {
	return post('/api/auth/verify-patient', data)
}

/**
 * 身份验证并激活账户（激活第二步）
 * 对应后端：POST /api/auth/activate-patient
 * @param {Object} data - { identifier, idCardEnding, newPassword, confirmPassword }
 */
export function activatePatient(data) {
	return post('/api/auth/activate-patient', data)
}

/**
 * 检查账户激活状态
 * @param {String} identifier - 学号/工号
 * 注意：此接口后端暂未实现
 */
export function checkActivationStatus(identifier) {
	return get(`/api/patient/activation-status/${identifier}`)
}

/**
 * 登出
 * 注意：此接口后端暂未实现
 */
export function logoutPatient() {
	return post('/api/patient/logout')
}
