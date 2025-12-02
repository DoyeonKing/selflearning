/**
 * 预约相关API接口
 */
import { get, post, put, del } from '../utils/request.js'
import { adaptAppointmentList, adaptAppointment, adaptAppointmentDetail, adaptWaitlistList, adaptWaitlist } from '../utils/dataAdapter.js'

/**
 * 获取患者的所有预约
 * @param {Number} patientId - 患者ID
 */
export async function getPatientAppointments(patientId) {
	const response = await get(`/api/appointments/patient/${patientId}`)
	console.log('getPatientAppointments 原始响应:', response)
	console.log('响应类型:', typeof response, '是否为数组:', Array.isArray(response))
	
	// 后端直接返回数组（ResponseEntity<List<AppointmentResponse>>）
	if (Array.isArray(response)) {
		console.log('检测到数组格式，开始适配，数组长度:', response.length)
		const adapted = adaptAppointmentList(response)
		console.log('适配后的预约列表，长度:', adapted.length)
		return {
			code: '200',
			data: adapted
		}
	}
	
	// 兼容可能的 Result 格式
	if (response && response.code === '200' && response.data) {
		console.log('检测到 Result 格式，data 类型:', typeof response.data, '是否为数组:', Array.isArray(response.data))
		const adapted = Array.isArray(response.data) ? adaptAppointmentList(response.data) : []
		return {
			...response,
			data: adapted
		}
	}
	
	console.warn('getPatientAppointments: 响应格式不匹配，返回原始响应')
	return response
}

/**
 * 获取患者即将就诊的预约
 * @param {Number} patientId - 患者ID
 */
export async function getUpcomingAppointments(patientId) {
	const response = await get(`/api/appointments/patient/${patientId}/upcoming`)
	// 后端直接返回数组，直接使用
	if (Array.isArray(response)) {
		return {
			code: '200',
			data: adaptAppointmentList(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointmentList(response.data)
		}
	}
	return response
}

/**
 * 创建预约
 * @param {Object} data - 预约数据
 */
export async function createAppointment(data) {
	const response = await post('/api/appointments', data)
	// 后端直接返回 AppointmentResponse 对象
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointment(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointment(response.data)
		}
	}
	return response
}

/**
 * 取消预约
 * @param {Number} appointmentId - 预约ID
 */
export async function cancelAppointment(appointmentId) {
	const response = await put(`/api/appointments/${appointmentId}/cancel`)
	// 后端直接返回 AppointmentResponse 对象
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointment(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointment(response.data)
		}
	}
	return response
}

/**
 * 获取预约详情
 * @param {Number} appointmentId - 预约ID
 */
export async function getAppointmentDetail(appointmentId) {
	const response = await get(`/api/appointments/${appointmentId}`)
	// 后端直接返回 AppointmentResponse 对象
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointmentDetail(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointmentDetail(response.data)
		}
	}
	return response
}

/**
 * 更新预约支付状态
 * @param {Number} appointmentId - 预约ID
 * @param {Object} data - 支付数据
 */
export async function updateAppointmentPayment(appointmentId, data) {
	const response = await put(`/api/appointments/${appointmentId}`, data)
	// 后端直接返回 AppointmentResponse 对象
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointment(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointment(response.data)
		}
	}
	return response
}

/**
 * 支付挂号费用
 * @param {Number} appointmentId - 预约ID
 * @param {Object} paymentData - 支付信息
 */
export async function payForAppointment(appointmentId, paymentData) {
	const response = await post(`/api/appointments/${appointmentId}/pay`, paymentData)
	// 后端直接返回 AppointmentResponse 对象
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointment(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointment(response.data)
		}
	}
	return response
}

/**
 * 候补相关API接口
 */

/**
 * 获取患者的所有候补记录
 * @param {Number} patientId - 患者ID
 */
export async function getPatientWaitlist(patientId) {
	const response = await get(`/api/waitlist/patient/${patientId}`)
	// 后端直接返回数组，直接使用
	if (Array.isArray(response)) {
		return {
			code: '200',
			data: adaptWaitlistList(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptWaitlistList(response.data)
		}
	}
	return response
}

/**
 * 获取候补详情
 * @param {Number} waitlistId - 候补ID
 */
export async function getWaitlistDetail(waitlistId) {
	const response = await get(`/api/waitlist/${waitlistId}`)
	// 后端直接返回 WaitlistResponse 对象
	if (response && response.waitlistId) {
		return {
			code: '200',
			data: adaptWaitlist(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptWaitlist(response.data)
		}
	}
	return response
}

/**
 * 创建候补申请
 * @param {Object} data - 候补数据
 */
export async function createWaitlist(data) {
	const response = await post('/api/waitlist', data)
	// 后端直接返回 WaitlistResponse 对象
	if (response && response.waitlistId) {
		return {
			code: '200',
			data: adaptWaitlist(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptWaitlist(response.data)
		}
	}
	return response
}

/**
 * 取消候补
 * @param {Number} waitlistId - 候补ID
 */
export async function cancelWaitlist(waitlistId) {
	const response = await put(`/api/waitlist/${waitlistId}/cancel`)
	// 后端直接返回 WaitlistResponse 对象
	if (response && response.waitlistId) {
		return {
			code: '200',
			data: adaptWaitlist(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptWaitlist(response.data)
		}
	}
	return response
}

/**
 * 支付候补费用（候补转正式预约）
 * @param {Number} waitlistId - 候补ID
 * @param {Object} paymentData - 支付信息 { paymentMethod, transactionId }
 */
export async function payForWaitlist(waitlistId, paymentData) {
	const response = await post(`/api/waitlist/${waitlistId}/pay`, paymentData)
	// 后端直接返回 AppointmentResponse 对象（候补转预约后）
	if (response && response.appointmentId) {
		return {
			code: '200',
			data: adaptAppointment(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptAppointment(response.data)
		}
	}
	return response
}

/**
 * 获取预约二维码Token
 * @param {Number} appointmentId - 预约ID
 */
export async function getAppointmentQrCode(appointmentId) {
	const response = await get(`/api/appointments/${appointmentId}/qr-code`)
	// 后端直接返回 QrCodeResponse 对象
	if (response && response.qrToken) {
		return {
			code: '200',
			data: response
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return response
	}
	return response
}

/**
 * 查询候补排队位置
 * @param {Number} waitlistId - 候补ID
 * @returns {Promise} 返回排队位置信息
 */
export async function getWaitlistPosition(waitlistId) {
	const response = await get(`/api/waitlist/${waitlistId}/position`)
	// 后端直接返回位置信息对象
	if (response && response.waitlistId) {
		return {
			code: '200',
			data: response
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return response
	}
	return response
}