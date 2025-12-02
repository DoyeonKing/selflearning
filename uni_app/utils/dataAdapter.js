/**
 * 数据适配工具函数
 * 用于将后端返回的数据格式转换为前端需要的扁平格式
 */

/**
 * 转换后端预约列表数据为前端格式
 * @param {Array} backendData - 后端返回的预约列表
 * @returns {Array} 前端格式的预约列表
 */
export function adaptAppointmentList(backendData) {
	if (!Array.isArray(backendData)) {
		console.warn('adaptAppointmentList: 输入不是数组:', backendData)
		return []
	}
	
	console.log('adaptAppointmentList: 开始适配，输入数组长度:', backendData.length)
	
	const adapted = backendData.map(adaptAppointment).filter(item => item !== null)
	
	console.log('adaptAppointmentList: 适配完成，有效数据数量:', adapted.length)
	
	return adapted
}

/**
 * 转换单个预约数据
 * @param {Object} appointment - 后端预约对象
 * @returns {Object} 前端格式的预约对象
 */
export function adaptAppointment(appointment) {
	if (!appointment) {
		console.warn('adaptAppointment: appointment 为空')
		return null
	}
	
	console.log('adaptAppointment 原始数据:', JSON.stringify(appointment, null, 2))
	
	// 如果数据已经是扁平格式（Mock数据），直接返回
	if (appointment.departmentName && !appointment.schedule) {
		console.log('adaptAppointment: 已经是扁平格式，直接返回')
		return appointment
	}
	
	// 后端嵌套格式，需要转换
	const schedule = appointment.schedule || {}
	const patient = appointment.patient || {}
	
	console.log('adaptAppointment 嵌套数据:', {
		schedule: schedule,
		patient: patient,
		status: appointment.status
	})
	
	// 状态映射（兼容大小写）
	const statusMap = {
		// 后端状态 -> 前端状态
		'scheduled': 'confirmed',
		'SCHEDULED': 'confirmed',
		'confirmed': 'confirmed',
		'CONFIRMED': 'confirmed',
		'checked_in': 'checked_in',
		'CHECKED_IN': 'checked_in',
		'completed': 'completed',
		'COMPLETED': 'completed',
		'cancelled': 'cancelled',
		'CANCELLED': 'cancelled',
		'no_show': 'cancelled',
		'NO_SHOW': 'cancelled',
		'pending_payment': 'pending_payment',
		'PENDING_PAYMENT': 'pending_payment',
		'pending': 'pending',
		'PENDING': 'pending'
	}
	
	// 处理状态：后端可能返回枚举值（字符串）或枚举对象
	let statusValue = appointment.status
	if (typeof statusValue === 'object' && statusValue !== null) {
		// 如果是枚举对象，获取其字符串值
		statusValue = statusValue.name || statusValue.toString()
	}
	const finalStatus = statusMap[statusValue] || statusMap[statusValue?.toLowerCase()] || statusValue?.toLowerCase() || 'pending'
	
	const scheduleTime = schedule.scheduleDate ? 
		(schedule.scheduleDate + 'T' + formatTime(schedule.startTime)) : ''
	const scheduleEndTime = schedule.scheduleDate && schedule.endTime ? 
		(schedule.scheduleDate + 'T' + formatTime(schedule.endTime)) : ''
	
	console.log('[数据适配] schedule原始数据:', {
		scheduleDate: schedule.scheduleDate,
		startTime: schedule.startTime,
		endTime: schedule.endTime,
		startTimeType: typeof schedule.startTime,
		endTimeType: typeof schedule.endTime
	})
	console.log('[数据适配] 格式化后的时间:', {
		scheduleTime: scheduleTime,
		scheduleEndTime: scheduleEndTime
	})
	
	const adapted = {
		id: appointment.appointmentId,
		appointmentId: appointment.appointmentId,
		scheduleId: schedule.scheduleId,
		departmentId: schedule.departmentId,
		departmentName: schedule.departmentName || '',
		doctorId: schedule.doctorId,
		doctorName: schedule.doctorName || '',
		doctorTitle: schedule.doctorTitle || '',
		scheduleTime: scheduleTime,
		scheduleEndTime: scheduleEndTime,
		appointmentTime: appointment.createdAt || '',
		status: finalStatus,
		queueNumber: appointment.appointmentNumber,
		appointmentNumber: appointment.appointmentNumber,
		patientName: patient.fullName || patient.name || '',
		patientId: patient.patientId || patient.id,
		// 直接添加location字段，方便访问
		location: schedule.location || '',
		locationId: schedule.locationId || null,
		fee: parseFloat(schedule.fee || 0),
		paymentDeadline: appointment.paymentDeadline || '',
		// 保留schedule对象以便访问locationId等字段
		schedule: {
			scheduleId: schedule.scheduleId,
			locationId: schedule.locationId,
			location: schedule.location || '',
			departmentId: schedule.departmentId,
			departmentName: schedule.departmentName,
			doctorId: schedule.doctorId,
			doctorName: schedule.doctorName,
			doctorTitle: schedule.doctorTitle,
			scheduleDate: schedule.scheduleDate,
			slotName: schedule.slotName,
			startTime: schedule.startTime,
			endTime: schedule.endTime,
			fee: schedule.fee
		}
	}
	
	console.log('[数据适配] 适配后数据:', JSON.stringify(adapted, null, 2))
	console.log('[数据适配] 适配后字段检查:', {
		id: adapted.id,
		appointmentId: adapted.appointmentId,
		departmentName: adapted.departmentName,
		doctorName: adapted.doctorName,
		status: adapted.status,
		scheduleTime: adapted.scheduleTime,
		scheduleEndTime: adapted.scheduleEndTime,
		hasScheduleEndTime: !!adapted.scheduleEndTime,
		hasDepartmentName: !!adapted.departmentName,
		hasDoctorName: !!adapted.doctorName
	})
	
	return adapted
}

/**
 * 转换后端排班数据为前端格式
 * @param {Object|Array} backendData - 后端返回的排班数据
 * @returns {Object|Array} 前端格式的排班数据
 */
export function adaptSchedule(backendData) {
	if (!backendData) return null
	
	// 如果是数组
	if (Array.isArray(backendData)) {
		return backendData.map(adaptSingleSchedule)
	}
	
	return adaptSingleSchedule(backendData)
}

/**
 * 转换单个排班数据
 * @param {Object} schedule - 后端排班对象
 * @returns {Object} 前端格式的排班对象
 */
function adaptSingleSchedule(schedule) {
	if (!schedule) return null
	
	// 如果已经是扁平格式，直接返回
	if (schedule.departmentName && !schedule.doctor) {
		return schedule
	}
	
	// 后端嵌套格式，需要转换
	// 注意：这里假设后端会返回 doctor, department, slot, location 等关联数据
	
	// 获取默认头像
	const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
	const photoUrl = schedule.doctorPhotoUrl || schedule.photoUrl || ''
	
	return {
		scheduleId: schedule.scheduleId,
		doctorId: schedule.doctorId,
		doctorName: schedule.doctorName || '',
		doctorTitle: schedule.doctorTitle || '',
		specialty: schedule.doctorSpecialty || schedule.specialty || '',
		photoUrl: photoUrl || defaultAvatar,
		departmentId: schedule.departmentId,
		departmentName: schedule.departmentName || '',
		scheduleDate: schedule.scheduleDate,
		slotName: schedule.slotName || '',
		startTime: schedule.startTime,
		endTime: schedule.endTime,
		location: schedule.location || schedule.locationName || '',
		locationId: schedule.locationId || null,
		fee: parseFloat(schedule.fee || 0),
		totalSlots: schedule.totalSlots || 0,
		bookedSlots: schedule.bookedSlots || 0,
		remainingSlots: (schedule.totalSlots || 0) - (schedule.bookedSlots || 0),
		status: schedule.status
	}
}

/**
 * 转换预约详情数据
 */
export function adaptAppointmentDetail(backendData) {
	return adaptAppointment(backendData)
}

/**
 * 转换排班详情数据
 */
export function adaptScheduleDetail(backendData) {
	return adaptSchedule(backendData)
}

/**
 * 转换后端候补列表数据为前端格式
 * @param {Array} backendData - 后端返回的候补列表
 * @returns {Array} 前端格式的候补列表
 */
export function adaptWaitlistList(backendData) {
	if (!Array.isArray(backendData)) {
		return []
	}
	
	return backendData.map(adaptWaitlist)
}

/**
 * 转换单个候补数据
 * @param {Object} waitlist - 后端候补对象
 * @returns {Object} 前端格式的候补对象
 */
export function adaptWaitlist(waitlist) {
	if (!waitlist) return null
	
	// 如果数据已经是扁平格式（Mock数据），直接返回
	if (waitlist.departmentName && !waitlist.schedule) {
		return waitlist
	}
	
	// 后端嵌套格式，需要转换
	const schedule = waitlist.schedule || {}
	const patient = waitlist.patient || {}
	
	// 状态映射
	const statusMap = {
		'PENDING': 'waiting',
		'waiting': 'waiting',
		'notified': 'notified',
		'FULFILLED': 'booked',
		'booked': 'booked',
		'expired': 'expired',
		'REJECTED': 'cancelled',
		'cancelled': 'cancelled'
	}
	
	return {
		id: waitlist.waitlistId,
		waitlistId: waitlist.waitlistId,
		scheduleId: schedule.scheduleId,
		departmentId: schedule.departmentId,
		departmentName: schedule.departmentName || '',
		doctorId: schedule.doctorId,
		doctorName: schedule.doctorName || '',
		doctorTitle: schedule.doctorTitle || '',
		scheduleDate: schedule.scheduleDate || '',
		scheduleTime: schedule.scheduleDate ? 
			(schedule.scheduleDate + 'T' + formatTime(schedule.startTime)) : '',
		slotName: schedule.slotName || '',
		location: schedule.location || '',
		fee: parseFloat(schedule.fee || 0),
		status: statusMap[waitlist.status] || waitlist.status,
		queuePosition: waitlist.queuePosition || 0,
		notificationSentAt: waitlist.notificationSentAt || '',
		createdAt: waitlist.createdAt || '',
		patientName: patient.fullName || '',
		patientId: patient.patientId
	}
}

/**
 * 格式化时间为 HH:mm:ss 格式
 * @param {String|LocalTime} time - 时间
 * @returns {String} 格式化的时间字符串
 */
function formatTime(time) {
	if (!time) return '09:00:00'
	
	// 如果已经是字符串格式，直接返回
	if (typeof time === 'string') {
		// 如果只有 HH:mm，补上 :00
		if (time.match(/^\d{2}:\d{2}$/)) {
			return time + ':00'
		}
		return time
	}
	
	// 如果是 LocalTime 对象（来自后端），转换为字符串
	return time.toString()
}

