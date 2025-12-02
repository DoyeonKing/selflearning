/**
 * 模拟数据文件
 * 用于演示页面功能，实际开发中应删除此文件
 */

// 模拟今日可预约数据（使用子科室ID）
export const mockTodaySchedules = [
	{
		id: 1,
		departmentId: 101,
		departmentName: '呼吸内科',
		availableSlots: 15
	},
	{
		id: 2,
		departmentId: 201,
		departmentName: '普外科',
		availableSlots: 8
	},
	{
		id: 3,
		departmentId: 401,
		departmentName: '儿科内科',
		availableSlots: 12
	},
	{
		id: 4,
		departmentId: 301,
		departmentName: '妇科',
		availableSlots: 6
	}
]

// 模拟即将就诊预约数据
export const mockUpcomingAppointment = {
	id: 1,
	scheduleTime: new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString(), // 12小时后
	departmentName: '内科',
	doctorName: '张医生',
	queueNumber: 5
}

// 模拟热门科室数据
export const mockPopularDepartments = [
	{ id: 1, name: '内科' },
	{ id: 2, name: '外科' },
	{ id: 3, name: '儿科' },
	{ id: 4, name: '妇科' },
	{ id: 5, name: '眼科' },
	{ id: 6, name: '耳鼻喉科' },
	{ id: 7, name: '皮肤科' },
	{ id: 8, name: '口腔科' }
]

// 模拟患者信息
export const mockPatientInfo = {
	id: 1,
	name: '张三',
	identifier: '2021001001'
}

// 模拟消息数据（按发送者分组）
export const mockMessages = [
	{
		id: 1,
		senderId: 'system',
		senderName: '系统通知',
		type: 'system',
		title: '系统维护通知',
		content: '系统维护通知：系统将于今晚22:00-24:00进行维护升级',
		createTime: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
		isRead: true
	},
	{
		id: 2,
		senderId: 'hospital',
		senderName: '医院公告',
		type: 'notice',
		title: '春节期间医院门诊时间调整通知',
		content: '春节期间医院门诊时间调整通知：1月24日-1月30日门诊时间调整为上午8:00-12:00，下午停诊',
		createTime: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
		isRead: false
	},
	{
		id: 3,
		senderId: 'hospital',
		senderName: '医院公告',
		type: 'notice',
		title: '冬季健康提醒',
		content: '冬季来临，请注意保暖，如有不适请及时就医。',
		createTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
		isRead: false
	},
	{
		id: 4,
		senderId: 'appointment',
		senderName: '预约中心',
		type: 'appointment',
		title: '预约成功提醒',
		content: '您的预约已成功，就诊时间：2024年1月15日 09:30，科室：内科，医生：张医生',
		createTime: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
		isRead: false
	},
	{
		id: 5,
		senderId: 'appointment',
		senderName: '预约中心',
		type: 'reminder',
		title: '就诊提醒',
		content: '您有预约即将到期，请提前15分钟到达医院',
		createTime: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
		isRead: false
	},
	{
		id: 6,
		senderId: 'cancel',
		senderName: '停诊通知',
		type: 'cancel',
		title: '内科医生停诊通知',
		content: '内科医生今日临时停诊，已自动取消预约，请重新挂号',
		createTime: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
		isRead: false
	}
]

// 模拟预约列表数据
export const mockAppointments = [
	{
		id: 1,
		scheduleId: 1,
		departmentId: 1,
		departmentName: '内科',
		doctorId: 1,
		doctorName: '张医生',
		scheduleTime: '2024-01-15T09:30:00',
		appointmentTime: '2024-01-10T10:00:00',
		status: 'confirmed', // confirmed: 已确认, completed: 已完成, cancelled: 已取消
		queueNumber: 5,
		patientName: '张三',
		patientId: 1
	},
	{
		id: 2,
		scheduleId: 2,
		departmentId: 2,
		departmentName: '外科',
		doctorId: 2,
		doctorName: '李医生',
		scheduleTime: '2024-01-20T14:00:00',
		appointmentTime: '2024-01-12T15:30:00',
		status: 'confirmed',
		queueNumber: 3,
		patientName: '张三',
		patientId: 1
	},
	{
		id: 3,
		scheduleId: 3,
		departmentId: 3,
		departmentName: '儿科',
		doctorId: 3,
		doctorName: '王医生',
		scheduleTime: '2024-01-25T10:00:00',
		appointmentTime: '2024-01-18T11:00:00',
		status: 'confirmed',
		queueNumber: 8,
		patientName: '张三',
		patientId: 1
	},
	{
		id: 4,
		scheduleId: 4,
		departmentId: 1,
		departmentName: '内科',
		doctorId: 1,
		doctorName: '张医生',
		scheduleTime: '2024-01-05T09:00:00',
		appointmentTime: '2024-01-01T10:00:00',
		status: 'completed',
		queueNumber: 2,
		patientName: '张三',
		patientId: 1
	},
	{
		id: 5,
		scheduleId: 5,
		departmentId: 4,
		departmentName: '妇科',
		doctorId: 4,
		doctorName: '赵医生',
		scheduleTime: '2024-01-08T14:30:00',
		appointmentTime: '2024-01-03T16:00:00',
		status: 'cancelled',
		queueNumber: 1,
		patientName: '张三',
		patientId: 1
	}
]

// 模拟科室列表数据（树形结构）
export const mockDepartments = [
	{
		id: 1,
		name: '内科',
		description: '内科疾病诊疗',
		children: [
			{ id: 101, name: '呼吸内科', description: '呼吸系统疾病' },
			{ id: 102, name: '心血管内科', description: '心脏血管疾病' },
			{ id: 103, name: '消化内科', description: '消化系统疾病' },
			{ id: 104, name: '内分泌科', description: '内分泌疾病' }
		]
	},
	{
		id: 2,
		name: '外科',
		description: '外科手术诊疗',
		children: [
			{ id: 201, name: '普外科', description: '普通外科手术' },
			{ id: 202, name: '骨科', description: '骨科疾病治疗' },
			{ id: 203, name: '泌尿外科', description: '泌尿系统疾病' }
		]
	},
	{
		id: 3,
		name: '妇产科',
		description: '妇科产科诊疗',
		children: [
			{ id: 301, name: '妇科', description: '妇科疾病诊疗' },
			{ id: 302, name: '产科', description: '产科服务' }
		]
	},
	{
		id: 4,
		name: '儿科',
		description: '儿科疾病诊疗',
		children: [
			{ id: 401, name: '儿科内科', description: '儿童内科疾病' },
			{ id: 402, name: '儿科外科', description: '儿童外科疾病' }
		]
	},
	{
		id: 5,
		name: '眼科',
		description: '眼科疾病诊疗',
		children: []
	},
	{
		id: 6,
		name: '耳鼻喉科',
		description: '耳鼻喉疾病诊疗',
		children: []
	},
	{
		id: 7,
		name: '皮肤科',
		description: '皮肤疾病诊疗',
		children: []
	},
	{
		id: 8,
		name: '口腔科',
		description: '口腔疾病诊疗',
		children: []
	}
]

// 模拟医生排班数据
export const mockSchedules = [
	// 呼吸内科 - 李医生
	{
		scheduleId: 1001,
		doctorId: 201,
		doctorName: '李建国',
		doctorTitle: '副主任医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-201诊室',
		fee: 20.00,
		totalSlots: 20,
		bookedSlots: 20,
		remainingSlots: 0,
		status: 'full'
	},
	{
		scheduleId: 1002,
		doctorId: 201,
		doctorName: '李建国',
		doctorTitle: '副主任医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-201诊室',
		fee: 20.00,
		totalSlots: 20,
		bookedSlots: 12,
		remainingSlots: 8,
		status: 'available'
	},
	// 心血管内科 - 张医生
	{
		scheduleId: 1003,
		doctorId: 101,
		doctorName: '张文华',
		doctorTitle: '主任医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-301诊室',
		fee: 60.00,
		totalSlots: 10,
		bookedSlots: 10,
		remainingSlots: 0,
		status: 'full'
	},
	{
		scheduleId: 1004,
		doctorId: 102,
		doctorName: '陈明军',
		doctorTitle: '副主任医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-302诊室',
		fee: 40.00,
		totalSlots: 15,
		bookedSlots: 3,
		remainingSlots: 12,
		status: 'available'
	},
	{
		scheduleId: 1005,
		doctorId: 101,
		doctorName: '张文华',
		doctorTitle: '主任医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-301诊室',
		fee: 60.00,
		totalSlots: 10,
		bookedSlots: 2,
		remainingSlots: 8,
		status: 'available'
	},
	// 消化内科 - 王医生
	{
		scheduleId: 1006,
		doctorId: 301,
		doctorName: '王丽娜',
		doctorTitle: '主治医师',
		specialty: '消化系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 103,
		departmentName: '消化内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-401诊室',
		fee: 30.00,
		totalSlots: 15,
		bookedSlots: 4,
		remainingSlots: 11,
		status: 'available'
	},
	{
		scheduleId: 1007,
		doctorId: 302,
		doctorName: '刘德华',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 103,
		departmentName: '消化内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-402诊室',
		fee: 35.00,
		totalSlots: 18,
		bookedSlots: 8,
		remainingSlots: 10,
		status: 'available'
	},
	// 妇科 - 赵医生
	{
		scheduleId: 1008,
		doctorId: 401,
		doctorName: '赵雅静',
		doctorTitle: '主任医师',
		specialty: '妇科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 301,
		departmentName: '妇科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-501诊室',
		fee: 50.00,
		totalSlots: 12,
		bookedSlots: 9,
		remainingSlots: 3,
		status: 'available'
	},
	{
		scheduleId: 1009,
		doctorId: 402,
		doctorName: '孙美玲',
		doctorTitle: '副主任医师',
		specialty: '妇科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 301,
		departmentName: '妇科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-502诊室',
		fee: 35.00,
		totalSlots: 15,
		bookedSlots: 2,
		remainingSlots: 13,
		status: 'available'
	},
	// 儿科 - 杨医生
	{
		scheduleId: 1010,
		doctorId: 501,
		doctorName: '杨晓红',
		doctorTitle: '主治医师',
		specialty: '儿童内科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 401,
		departmentName: '儿科内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-601诊室',
		fee: 25.00,
		totalSlots: 20,
		bookedSlots: 6,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1011,
		doctorId: 502,
		doctorName: '周建强',
		doctorTitle: '副主任医师',
		specialty: '儿童内科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 401,
		departmentName: '儿科内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-602诊室',
		fee: 30.00,
		totalSlots: 18,
		bookedSlots: 11,
		remainingSlots: 7,
		status: 'available'
	},
	// 普外科 - 孙医生
	{
		scheduleId: 1012,
		doctorId: 2011,
		doctorName: '孙志强',
		doctorTitle: '主任医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-701诊室',
		fee: 60.00,
		totalSlots: 10,
		bookedSlots: 10,
		remainingSlots: 0,
		status: 'full'
	},
	{
		scheduleId: 1013,
		doctorId: 2012,
		doctorName: '马国华',
		doctorTitle: '副主任医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-702诊室',
		fee: 40.00,
		totalSlots: 12,
		bookedSlots: 5,
		remainingSlots: 7,
		status: 'available'
	},
	
	// 为了更好的测试效果，为呼吸内科添加更多医生和多个日期的排班
	// 呼吸内科 - 第三个医生（明天+后天）
	{
		scheduleId: 1014,
		doctorId: 203,
		doctorName: '王秀兰',
		doctorTitle: '主治医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getDateAfterDays(1),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-203诊室',
		fee: 20.00,
		totalSlots: 20,
		bookedSlots: 3,
		remainingSlots: 17,
		status: 'available'
	},
	{
		scheduleId: 1015,
		doctorId: 203,
		doctorName: '王秀兰',
		doctorTitle: '主治医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getDateAfterDays(2),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-203诊室',
		fee: 20.00,
		totalSlots: 20,
		bookedSlots: 6,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1016,
		doctorId: 203,
		doctorName: '王秀兰',
		doctorTitle: '主治医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getDateAfterDays(3),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-203诊室',
		fee: 20.00,
		totalSlots: 20,
		bookedSlots: 1,
		remainingSlots: 19,
		status: 'available'
	},
	
	// 心血管内科 - 第三个医生（多个日期）
	{
		scheduleId: 1017,
		doctorId: 103,
		doctorName: '刘春梅',
		doctorTitle: '主治医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getDateAfterDays(1),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-303诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 2,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1018,
		doctorId: 103,
		doctorName: '刘春梅',
		doctorTitle: '主治医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getDateAfterDays(1),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-303诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 8,
		remainingSlots: 8,
		status: 'available'
	},
	{
		scheduleId: 1019,
		doctorId: 103,
		doctorName: '刘春梅',
		doctorTitle: '主治医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getDateAfterDays(4),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-303诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 12,
		remainingSlots: 4,
		status: 'available'
	},
	
	// 消化内科 - 第三个医生（多个日期）
	{
		scheduleId: 1020,
		doctorId: 303,
		doctorName: '黄晓明',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 103,
		departmentName: '消化内科',
		scheduleDate: getDateAfterDays(1),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-403诊室',
		fee: 38.00,
		totalSlots: 15,
		bookedSlots: 1,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1021,
		doctorId: 303,
		doctorName: '黄晓明',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 103,
		departmentName: '消化内科',
		scheduleDate: getDateAfterDays(5),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-403诊室',
		fee: 38.00,
		totalSlots: 15,
		bookedSlots: 7,
		remainingSlots: 8,
		status: 'available'
	},
	
	// 妇科 - 第三个医生（多个日期）
	{
		scheduleId: 1022,
		doctorId: 403,
		doctorName: '李小红',
		doctorTitle: '主治医师',
		specialty: '妇科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 301,
		departmentName: '妇科',
		scheduleDate: getDateAfterDays(1),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-503诊室',
		fee: 30.00,
		totalSlots: 18,
		bookedSlots: 4,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1023,
		doctorId: 403,
		doctorName: '李小红',
		doctorTitle: '主治医师',
		specialty: '妇科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 301,
		departmentName: '妇科',
		scheduleDate: getDateAfterDays(6),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-503诊室',
		fee: 30.00,
		totalSlots: 18,
		bookedSlots: 15,
		remainingSlots: 3,
		status: 'available'
	},
	
	// 普外科 - 第三个医生（多个日期）
	{
		scheduleId: 1024,
		doctorId: 2013,
		doctorName: '吴海军',
		doctorTitle: '主治医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getDateAfterDays(1),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-703诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 2,
		remainingSlots: 14,
		status: 'available'
	},
	{
		scheduleId: 1025,
		doctorId: 2013,
		doctorName: '吴海军',
		doctorTitle: '主治医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getDateAfterDays(1),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-703诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 9,
		remainingSlots: 7,
		status: 'available'
	},
	{
		scheduleId: 1026,
		doctorId: 2013,
		doctorName: '吴海军',
		doctorTitle: '主治医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getDateAfterDays(7),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-703诊室',
		fee: 35.00,
		totalSlots: 16,
		bookedSlots: 16,
		remainingSlots: 0,
		status: 'full'
	},
	// 更多约满的排班用于测试候补功能
	// 呼吸内科 - 王医生（明天上午已满）
	{
		scheduleId: 1031,
		doctorId: 203,
		doctorName: '王秀兰',
		doctorTitle: '主治医师',
		specialty: '呼吸系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 101,
		departmentName: '呼吸内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-203诊室',
		fee: 25.00,
		totalSlots: 15,
		bookedSlots: 15,
		remainingSlots: 0,
		status: 'full'
	},
	// 消化内科 - 黄医生（明天上午已满）
	{
		scheduleId: 1032,
		doctorId: 303,
		doctorName: '黄晓明',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 103,
		departmentName: '消化内科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-403诊室',
		fee: 40.00,
		totalSlots: 12,
		bookedSlots: 12,
		remainingSlots: 0,
		status: 'full'
	},
	// 妇科 - 李医生（明天上午已满）
	{
		scheduleId: 1033,
		doctorId: 403,
		doctorName: '李小红',
		doctorTitle: '主治医师',
		specialty: '妇科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 301,
		departmentName: '妇科',
		scheduleDate: getTomorrowDate(),
		slotName: '上午08:00-12:00',
		startTime: '08:00',
		endTime: '12:00',
		location: '门诊楼-503诊室',
		fee: 35.00,
		totalSlots: 14,
		bookedSlots: 14,
		remainingSlots: 0,
		status: 'full'
	},
	// 心血管内科 - 刘医生（明天下午已满）
	{
		scheduleId: 1034,
		doctorId: 103,
		doctorName: '刘春梅',
		doctorTitle: '主治医师',
		specialty: '心血管疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 102,
		departmentName: '心血管内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-303诊室',
		fee: 35.00,
		totalSlots: 18,
		bookedSlots: 18,
		remainingSlots: 0,
		status: 'full'
	},
	// 儿科 - 杨医生（明天下午已满）
	{
		scheduleId: 1035,
		doctorId: 501,
		doctorName: '杨晓红',
		doctorTitle: '主治医师',
		specialty: '儿童内科疾病',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 401,
		departmentName: '儿科内科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-603诊室',
		fee: 30.00,
		totalSlots: 20,
		bookedSlots: 20,
		remainingSlots: 0,
		status: 'full'
	},
	// 普外科 - 马医生（明天下午已满）
	{
		scheduleId: 1036,
		doctorId: 2012,
		doctorName: '马国华',
		doctorTitle: '副主任医师',
		specialty: '普通外科手术',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		departmentId: 201,
		departmentName: '普外科',
		scheduleDate: getTomorrowDate(),
		slotName: '下午14:00-17:30',
		startTime: '14:00',
		endTime: '17:30',
		location: '门诊楼-704诊室',
		fee: 40.00,
		totalSlots: 16,
		bookedSlots: 16,
		remainingSlots: 0,
		status: 'full'
	}
]

// 辅助函数：获取明天的日期
function getTomorrowDate() {
	const tomorrow = new Date()
	tomorrow.setDate(tomorrow.getDate() + 1)
	const year = tomorrow.getFullYear()
	const month = String(tomorrow.getMonth() + 1).padStart(2, '0')
	const day = String(tomorrow.getDate()).padStart(2, '0')
	return `${year}-${month}-${day}`
}

// 辅助函数：获取指定天数后的日期
function getDateAfterDays(days) {
	const date = new Date()
	date.setDate(date.getDate() + days)
	const year = date.getFullYear()
	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')
	return `${year}-${month}-${day}`
}

// 模拟医生详细信息数据
export const mockDoctorDetails = [
	{
		doctorId: 201,
		doctorName: '李建国',
		doctorTitle: '副主任医师',
		specialty: '呼吸系统疾病',
		departmentName: '呼吸内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事呼吸内科临床工作18年，擅长慢性阻塞性肺疾病、哮喘、肺炎等呼吸系统疾病的诊治。对疑难重症有丰富的诊疗经验。',
		education: '医学硕士，毕业于华中科技大学同济医学院',
		experience: '18年临床经验',
		awards: '市级优秀医师'
	},
	{
		doctorId: 101,
		doctorName: '张文华',
		doctorTitle: '主任医师',
		specialty: '心血管疾病',
		departmentName: '心血管内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事心血管临床工作25年，擅长冠心病、高血压、心律失常等疾病的诊断和治疗。主持完成多项省级科研项目，发表SCI论文20余篇。',
		education: '医学博士，毕业于北京医科大学',
		experience: '25年临床经验',
		awards: '省级优秀医师、科技进步二等奖'
	},
	{
		doctorId: 102,
		doctorName: '陈明军',
		doctorTitle: '副主任医师',
		specialty: '心血管疾病',
		departmentName: '心血管内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事心血管内科临床工作15年，擅长心力衰竭、心肌病、高血压等疾病的诊治。精通心脏超声检查。',
		education: '医学博士，毕业于中山大学医学院',
		experience: '15年临床经验',
		awards: '市级科技进步奖'
	},
	{
		doctorId: 301,
		doctorName: '王丽娜',
		doctorTitle: '主治医师',
		specialty: '消化系统疾病',
		departmentName: '消化内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事消化内科工作12年，擅长胃食管反流病、消化性溃疡、炎症性肠病等消化系统疾病的诊治。精通胃镜、肠镜等内镜检查。',
		education: '医学硕士，毕业于上海交通大学医学院',
		experience: '12年临床经验',
		awards: ''
	},
	{
		doctorId: 302,
		doctorName: '刘德华',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		departmentName: '消化内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事消化内科临床工作16年，擅长肝硬化、肝癌、胰腺炎等疾病的诊治。在消化道早癌筛查方面有丰富经验。',
		education: '医学博士，毕业于首都医科大学',
		experience: '16年临床经验',
		awards: '省级医师奖'
	},
	{
		doctorId: 401,
		doctorName: '赵雅静',
		doctorTitle: '主任医师',
		specialty: '妇科疾病',
		departmentName: '妇科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事妇科临床工作20年，擅长妇科肿瘤、内分泌疾病、不孕不育等疾病的诊治。熟练掌握各种妇科微创手术技术。',
		education: '医学博士，毕业于北京协和医学院',
		experience: '20年临床经验',
		awards: '省级科技进步一等奖'
	},
	{
		doctorId: 402,
		doctorName: '孙美玲',
		doctorTitle: '副主任医师',
		specialty: '妇科疾病',
		departmentName: '妇科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事妇科临床工作14年，擅长月经不调、更年期综合征、盆腔炎等疾病的诊治。专注于妇科微创手术。',
		education: '医学硕士，毕业于复旦大学医学院',
		experience: '14年临床经验',
		awards: '市级优秀医师'
	},
	{
		doctorId: 501,
		doctorName: '杨晓红',
		doctorTitle: '主治医师',
		specialty: '儿童内科疾病',
		departmentName: '儿科内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事儿科临床工作10年，擅长儿童呼吸道感染、消化系统疾病、生长发育迟缓等疾病的诊治。',
		education: '医学硕士，毕业于重庆医科大学',
		experience: '10年临床经验',
		awards: ''
	},
	{
		doctorId: 502,
		doctorName: '周建强',
		doctorTitle: '副主任医师',
		specialty: '儿童内科疾病',
		departmentName: '儿科内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事儿科临床工作18年，擅长儿童哮喘、过敏性鼻炎、湿疹等过敏性疾病以及儿童营养与生长发育。',
		education: '医学博士，毕业于中国医科大学',
		experience: '18年临床经验',
		awards: '省级优秀儿科医师'
	},
	{
		doctorId: 2011,
		doctorName: '孙志强',
		doctorTitle: '主任医师',
		specialty: '普通外科手术',
		departmentName: '普外科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事普外科临床工作22年，擅长胆囊切除、阑尾切除、疝修补等常规手术，精通腹腔镜微创手术。',
		education: '医学博士，毕业于第二军医大学',
		experience: '22年临床经验',
		awards: '省级外科手术能手'
	},
	{
		doctorId: 2012,
		doctorName: '马国华',
		doctorTitle: '副主任医师',
		specialty: '普通外科手术',
		departmentName: '普外科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事普外科临床工作15年，擅长甲状腺疾病、乳腺疾病的外科治疗，在微创手术方面经验丰富。',
		education: '医学硕士，毕业于北京大学医学部',
		experience: '15年临床经验',
		awards: '市级科技进步三等奖'
	},
	{
		doctorId: 203,
		doctorName: '王秀兰',
		doctorTitle: '主治医师',
		specialty: '呼吸系统疾病',
		departmentName: '呼吸内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事呼吸内科临床工作10年，擅长慢性咳嗽、支气管哮喘、间质性肺病等呼吸系统疾病的诊治。在呼吸内镜技术方面有较高造诣。',
		education: '医学硕士，毕业于四川大学华西医学院',
		experience: '10年临床经验',
		awards: ''
	},
	{
		doctorId: 103,
		doctorName: '刘春梅',
		doctorTitle: '主治医师',
		specialty: '心血管疾病',
		departmentName: '心血管内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事心血管内科临床工作11年，擅长冠心病、高血压、心律失常等疾病的诊治。在心脏康复方面有深入研究。',
		education: '医学硕士，毕业于中南大学湘雅医学院',
		experience: '11年临床经验',
		awards: ''
	},
	{
		doctorId: 303,
		doctorName: '黄晓明',
		doctorTitle: '副主任医师',
		specialty: '消化系统疾病',
		departmentName: '消化内科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事消化内科临床工作17年，擅长消化道肿瘤、炎症性肠病、胃肠动力疾病的诊治。精通各种内镜下治疗技术。',
		education: '医学博士，毕业于山东大学齐鲁医学院',
		experience: '17年临床经验',
		awards: '省级青年医学人才'
	},
	{
		doctorId: 403,
		doctorName: '李小红',
		doctorTitle: '主治医师',
		specialty: '妇科疾病',
		departmentName: '妇科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事妇科临床工作12年，擅长妇科炎症、月经失调、计划生育等疾病的诊治。在妇科微创技术方面经验丰富。',
		education: '医学硕士，毕业于浙江大学医学院',
		experience: '12年临床经验',
		awards: ''
	},
	{
		doctorId: 2013,
		doctorName: '吴海军',
		doctorTitle: '主治医师',
		specialty: '普通外科手术',
		departmentName: '普外科',
		photoUrl: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
		bio: '从事普外科临床工作13年，擅长消化道肿瘤、腹壁疝、急腹症等疾病的诊治。精通腹腔镜和开腹手术。',
		education: '医学硕士，毕业于天津医科大学',
		experience: '13年临床经验',
		awards: ''
	}
]

// 辅助函数：获取当前时间的ISO字符串
function getNowDate() {
	return new Date().toISOString()
}

// 辅助函数：获取N分钟后的日期
function getDateAfterMinutes(minutes) {
	const date = new Date()
	date.setMinutes(date.getMinutes() + minutes)
	return date.toISOString()
}

// 模拟候补列表数据
export const mockWaitlist = [
	{
		id: 1,
		scheduleId: 1003,
		patientId: 1,
		patientName: '张三',
		departmentId: 102,
		departmentName: '心血管内科',
		doctorId: 101,
		doctorName: '张文华',
		doctorTitle: '主任医师',
		specialty: '心血管疾病',
		scheduleDate: getTomorrowDate(),
		scheduleTime: getTomorrowDate() + 'T08:00:00',
		slotName: '上午08:00-12:00',
		location: '门诊楼-301诊室',
		fee: 60.00,
		status: 'waiting',
		queuePosition: 3,
		createdAt: getDateAfterMinutes(-30),
		notificationSentAt: null
	},
	{
		id: 2,
		scheduleId: 1012,
		patientId: 1,
		patientName: '张三',
		departmentId: 201,
		departmentName: '普外科',
		doctorId: 2011,
		doctorName: '孙志强',
		doctorTitle: '主任医师',
		specialty: '普通外科手术',
		scheduleDate: getTomorrowDate(),
		scheduleTime: getTomorrowDate() + 'T08:00:00',
		slotName: '上午08:00-12:00',
		location: '门诊楼-701诊室',
		fee: 60.00,
		status: 'notified',
		queuePosition: 1,
		createdAt: getDateAfterMinutes(-90),
		notificationSentAt: getDateAfterMinutes(-5) // 5分钟前发送的通知
	},
	{
		id: 3,
		scheduleId: 1003,
		patientId: 1,
		patientName: '张三',
		departmentId: 102,
		departmentName: '心血管内科',
		doctorId: 101,
		doctorName: '张文华',
		doctorTitle: '主任医师',
		specialty: '心血管疾病',
		scheduleDate: getDateAfterDays(7),
		scheduleTime: getDateAfterDays(7) + 'T08:00:00',
		slotName: '上午08:00-12:00',
		location: '门诊楼-301诊室',
		fee: 60.00,
		status: 'booked',
		queuePosition: 0,
		createdAt: getDateAfterMinutes(-120),
		notificationSentAt: getDateAfterMinutes(-20)
	},
	{
		id: 4,
		scheduleId: 1026,
		patientId: 1,
		patientName: '张三',
		departmentId: 201,
		departmentName: '普外科',
		doctorId: 2013,
		doctorName: '吴海军',
		doctorTitle: '主治医师',
		specialty: '普通外科手术',
		scheduleDate: getDateAfterDays(7),
		scheduleTime: getDateAfterDays(7) + 'T08:00:00',
		slotName: '上午08:00-12:00',
		location: '门诊楼-703诊室',
		fee: 35.00,
		status: 'expired',
		queuePosition: 2,
		createdAt: getDateAfterMinutes(-200),
		notificationSentAt: getDateAfterMinutes(-900) // 15分钟前（已过期）
	}
]