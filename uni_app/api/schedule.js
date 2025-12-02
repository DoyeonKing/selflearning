/**
 * 排班相关API接口
 */
import { get, post } from '../utils/request.js'
import { adaptSchedule } from '../utils/dataAdapter.js'

/**
 * 获取今日可预约排班信息
 * @param {String} startDate - 开始日期 (YYYY-MM-DD)
 * @param {String} endDate - 结束日期 (YYYY-MM-DD)
 */
export async function getTodaySchedules(startDate, endDate) {
	const response = await get('/api/schedules', {
		startDate,
		endDate
	})
	// 后端返回 Page<ScheduleResponse> 格式，提取 content
	if (response.content && Array.isArray(response.content)) {
		return {
			code: '200',
			data: adaptSchedule(response.content),
			totalElements: response.totalElements,
			totalPages: response.totalPages
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptSchedule(response.data)
		}
	}
	// 兼容直接返回数组的情况
	if (Array.isArray(response)) {
		return {
			code: '200',
			data: adaptSchedule(response)
		}
	}
	return response
}

/**
 * 获取科室树形结构
 */
export function getDepartmentTree() {
	return get('/api/departments/tree')
}

/**
 * 获取科室列表（分页）
 */
export function getDepartments() {
	return get('/api/departments')
}

/**
 * 获取热门科室
 */
export function getPopularDepartments() {
	return get('/api/departments/popular')
}

/**
 * 根据科室ID获取排班信息
 * @param {Number} departmentId - 科室ID
 * @param {String} startDate - 开始日期 (可选)
 * @param {String} endDate - 结束日期 (可选)
 */
export async function getSchedulesByDepartment(departmentId, startDate, endDate) {
	// 后端返回 Page<ScheduleResponse>，需要提取 content
	const response = await get('/api/schedules', {
		departmentId,
		startDate,
		endDate
	})
	
	console.log('获取科室排班响应:', response)
	
	// 处理返回的 Page 格式：Spring Boot 的 Page 格式
	if (response.content && Array.isArray(response.content)) {
		return {
			code: '200',
			data: adaptSchedule(response.content),
			totalElements: response.totalElements,
			totalPages: response.totalPages
		}
	}
	
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptSchedule(response.data)
		}
	}
	
	// 兼容直接返回数组的情况
	if (Array.isArray(response)) {
		return {
			code: '200',
			data: adaptSchedule(response)
		}
	}
	
	// 都不匹配，返回原数据
	return response
}

/**
 * 获取时间段列表
 * @param {Number} scheduleId - 排班ID
 */
export async function getTimeSlots(scheduleId) {
	const response = await get(`/api/timeslots/schedule/${scheduleId}`)
	// 后端可能返回数组或 Result 格式
	if (Array.isArray(response)) {
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
 * 根据科室ID获取医生列表
 * @param {Number} departmentId - 科室ID
 */
export async function getDoctorsByDepartment(departmentId) {
	const response = await get(`/api/departments/${departmentId}/doctors`)
	// 后端可能返回数组或对象
	if (Array.isArray(response)) {
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
 * 获取单个医生详细信息
 * @param {Number} doctorId - 医生ID
 */
export async function getDoctorById(doctorId) {
	const response = await get(`/api/doctors/${doctorId}`)
	console.log('getDoctorById 原始响应:', response)
	
	// 如果返回的是错误字符串
	if (typeof response === 'string') {
		throw new Error(response)
	}
	
	// 格式1: 后端直接返回 DoctorResponse 对象（通过 ResponseEntity）
	if (response && (response.doctorId || response.fullName)) {
		return {
			code: '200',
			data: response
		}
	}
	
	// 格式2: 已经包装成 { code: '200', data: DoctorResponse }
	if (response && response.code === '200' && response.data) {
		return response
	}
	
	// 格式3: 直接返回 DoctorResponse（没有包装）
	if (response && response.doctorId) {
		return {
			code: '200',
			data: response
		}
	}
	
	// 如果都不匹配，返回原响应（可能是错误）
	return response
}

/**
 * 获取单个排班详情
 * @param {Number} scheduleId - 排班ID
 */
export async function getScheduleById(scheduleId) {
	const response = await get(`/api/schedules/${scheduleId}`)
	// 后端直接返回 ScheduleResponse 对象
	if (response && response.scheduleId) {
		return {
			code: '200',
			data: adaptSchedule(response)
		}
	}
	// 兼容可能的 Result 格式
	if (response.code === '200' && response.data) {
		return {
			...response,
			data: adaptSchedule(response.data)
		}
	}
	return response
}

