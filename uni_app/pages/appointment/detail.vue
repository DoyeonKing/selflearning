<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">预约详情</text>
		</view>
		
		<view class="content">
			<!-- 状态卡片 -->
			<view class="status-card">
				<view class="status-icon" :class="isExpiredStatus(appointment) ? 'expired' : appointment.status">
					<text>{{ getStatusIcon(appointment) }}</text>
				</view>
				<text class="status-text">{{ getStatusText(appointment) }}</text>
			</view>
			
			<!-- 患者信息 -->
			<view class="info-card">
				<view class="card-title">患者信息</view>
				<view class="info-row">
					<text class="label">姓名：</text>
					<text class="value">{{ appointment.patientName }}</text>
				</view>
				<view class="info-row">
					<text class="label">学号/工号：</text>
					<text class="value">{{ patientInfo.identifier }}</text>
				</view>
			</view>
			
			<!-- 预约信息 -->
			<view class="info-card">
				<view class="card-title">预约信息</view>
				<view class="info-row">
					<text class="label">科室：</text>
					<text class="value">{{ appointment.departmentName }}</text>
				</view>
				<view class="info-row">
					<text class="label">医生：</text>
					<text class="value">{{ appointment.doctorName }}</text>
				</view>
				<view class="info-row">
					<text class="label">就诊时间：</text>
					<text class="value">{{ formatDateTime(appointment.scheduleTime) }}</text>
				</view>
				<view class="info-row" v-if="getLocationName(appointment)">
					<text class="label">就诊地点：</text>
					<text class="value">{{ getLocationName(appointment) }}</text>
				</view>
				<view class="info-row" v-if="isConfirmedStatus(appointment.status) && (appointment.queueNumber || appointment.appointmentNumber)">
					<text class="label">排队号：</text>
					<text class="value queue-number">第{{ appointment.queueNumber || appointment.appointmentNumber }}号</text>
				</view>
				<!-- 过号状态显示 -->
				<view class="info-row" v-if="appointment.missedCallCount > 0">
					<text class="label">过号次数：</text>
					<text class="value missed-call-count">已过号{{ appointment.missedCallCount }}次</text>
				</view>
				<view class="info-row">
					<text class="label">预约时间：</text>
					<text class="value">{{ formatDateTime(appointment.appointmentTime) }}</text>
				</view>
			</view>
			
			<!-- 过号提示卡片（已叫号但状态已改回scheduled） -->
			<view class="missed-call-card" v-if="appointment.calledAt && appointment.status !== 'checked_in'">
				<view class="missed-call-icon">⚠️</view>
				<view class="missed-call-content">
					<text class="missed-call-title">您已过号</text>
					<text class="missed-call-desc">请重新扫码签到</text>
				</view>
			</view>
			
			<!-- 待支付提示卡片 -->
			<view class="payment-pending-card" v-if="isPendingPaymentStatus(appointment.status)">
				<view class="payment-icon">💰</view>
				<view class="payment-content">
					<text class="payment-title">待支付</text>
					<text class="payment-desc">请尽快完成支付以确认预约</text>
					<text class="payment-fee">挂号费：¥{{ appointment.fee || 0 }}</text>
					<text class="payment-deadline" v-if="appointment.paymentDeadline">
						支付截止：{{ formatDateTime(appointment.paymentDeadline) }}
					</text>
				</view>
			</view>
			
			<!-- 签到二维码（已支付且未过期状态显示，排除待支付状态） -->
			<view class="qr-code-card" v-if="isConfirmedStatus(appointment.status) && !isPendingPaymentStatus(appointment.status) && !isExpiredStatus(appointment)">
				<view class="qr-title">
					<text class="qr-icon">📱</text>
					<text class="qr-text">签到二维码</text>
					<text class="qr-refresh-tip" v-if="refreshCountdown > 0">
						{{ refreshCountdown }}秒后自动刷新
					</text>
				</view>
				<view class="qr-container">
					<image class="qr-code" :src="qrCodeUrl" mode="aspectFit" v-if="qrCodeUrl"></image>
					<view class="qr-loading" v-else>
						<text>生成二维码中...</text>
					</view>
				</view>
				<text class="qr-desc">就诊时出示此二维码进行签到</text>
				<text class="qr-tip">⚠️ 二维码每{{ refreshInterval }}秒自动刷新，请勿截图保存</text>
				<view class="qr-refresh-btn" @click="refreshQRCode">
					<text>手动刷新</text>
				</view>
			</view>
			
			<!-- 导航按钮（仅已确认且未过期状态显示） -->
			<view class="navigation-section" v-if="isConfirmedStatus(appointment.status) && !isExpiredStatus(appointment)">
				<button class="navigation-btn" @click="handleNavigation">
					<text class="nav-icon">🧭</text>
					<text>导航到诊室</text>
				</button>
			</view>
			
			<!-- 操作按钮 -->
			<view class="action-section" v-if="!isCancelledStatus(appointment.status)">
				<!-- 待支付状态：显示支付和取消按钮 -->
				<view class="button-row" v-if="isPendingPaymentStatus(appointment.status)">
					<button class="pay-btn-half" @click="handlePayment">立即支付</button>
					<button class="cancel-btn-half" @click="handleCancel">取消预约</button>
				</view>
				<!-- 其他状态：显示返回主页和取消按钮 -->
				<view class="button-row" v-else>
					<button class="home-btn" @click="handleBackToHome">返回主页</button>
					<button class="cancel-btn" v-if="canCancelAppointment(appointment.status)" @click="handleCancel">取消预约</button>
					<button class="view-btn" v-if="isCompletedStatus(appointment.status) && !isConfirmedStatus(appointment.status)" @click="handleBackToHome">查看其他预约</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAppointmentDetail, cancelAppointment, getAppointmentQrCode, payForAppointment } from '../../api/appointment.js'
	import { mockPatientInfo } from '../../api/mockData.js'
	
	export default {
	data() {
		return {
			appointmentId: null,
			appointment: {},
			patientInfo: {},
			qrCodeUrl: '',
			qrToken: '',
			paymentMethods: [
				{ value: 'wechat', name: '微信支付', icon: '💚' },
				{ value: 'alipay', name: '支付宝', icon: '🔵' },
				{ value: 'balance', name: '校园卡余额', icon: '💳' }
			],
			selectedPaymentMethod: 'wechat',
			refreshTimer: null,      // 刷新定时器
			countdownTimer: null,    // 倒计时定时器
			refreshCountdown: 0,     // 刷新倒计时
			refreshInterval: 60,     // 刷新间隔（秒）
			urlParams: {}, // 存储URL传递的参数
			loading: false
		}
	},
onLoad(options) {
	this.appointmentId = parseInt(options.appointmentId)
	// 如果URL传了参数，说明是新建的预约
	this.urlParams = {
		departmentName: options.departmentName ? decodeURIComponent(options.departmentName) : '',
		doctorName: options.doctorName ? decodeURIComponent(options.doctorName) : '',
		doctorTitle: options.doctorTitle ? decodeURIComponent(options.doctorTitle) : '',
		scheduleDate: options.scheduleDate ? decodeURIComponent(options.scheduleDate) : '',
		slotName: options.slotName ? decodeURIComponent(options.slotName) : ''
	}
	// 先加载患者信息，因为 loadAppointmentDetail 可能用到
	this.loadPatientInfo()
	this.loadAppointmentDetail()
},
onShow() {
	// 页面显示时启动自动刷新（如果预约已加载）
	if (this.appointment && this.appointment.status) {
		this.startAutoRefresh()
	}
},
onHide() {
	// 页面隐藏时停止刷新
	this.stopAutoRefresh()
},
onUnload() {
	// 页面卸载时清除定时器
	this.stopAutoRefresh()
},
		methods: {
	async loadAppointmentDetail() {
		if (!this.appointmentId) {
			// 没有预约ID时（例如演示场景），尝试使用URL参数构建基础信息
			if (this.urlParams.departmentName && this.urlParams.doctorName) {
				const now = new Date()
				let scheduleTime
				if (this.urlParams.scheduleDate) {
					scheduleTime = new Date(this.urlParams.scheduleDate + 'T12:00:00').toISOString()
				} else {
					scheduleTime = new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString()
				}
				this.appointment = {
					departmentName: this.urlParams.departmentName,
					doctorName: this.urlParams.doctorTitle ? `${this.urlParams.doctorName} ${this.urlParams.doctorTitle}` : this.urlParams.doctorName,
					scheduleTime: scheduleTime,
					appointmentTime: now.toISOString(),
					status: 'confirmed',
					queueNumber: null,
					appointmentNumber: null,
					patientName: this.patientInfo.name || '张三',
					patientId: this.patientInfo.id || 1
				}
			} else {
				uni.showToast({
					title: '缺少预约信息',
					icon: 'none'
				})
			}
			return
		}
		
		console.log('[前端] ========== 开始加载预约详情 ==========')
		console.log('[前端] 预约ID:', this.appointmentId)
		const loadStartTime = new Date().toISOString()
		console.log('[前端] 加载开始时间:', loadStartTime)
		
		this.loading = true
		try {
			const response = await getAppointmentDetail(this.appointmentId)
			console.log('[前端] 预约详情API响应:', JSON.stringify(response, null, 2))
			
			if (response && response.code === '200' && response.data) {
				this.appointment = response.data
				console.log('[前端] 预约详情数据加载成功:', {
					appointmentId: this.appointment.appointmentId,
					status: this.appointment.status,
					scheduleTime: this.appointment.scheduleTime,
					scheduleEndTime: this.appointment.scheduleEndTime,
					appointmentTime: this.appointment.appointmentTime,
					appointmentNumber: this.appointment.appointmentNumber || this.appointment.queueNumber,
					patientName: this.appointment.patientName
				})
				console.log('[前端] 预约状态检查:', {
					isConfirmedStatus: this.isConfirmedStatus(this.appointment.status),
					isCompletedStatus: this.isCompletedStatus(this.appointment.status),
					isCancelledStatus: this.isCancelledStatus(this.appointment.status),
					isExpiredStatus: this.isExpiredStatus(this.appointment)
				})
				
				// 只有已支付状态才生成二维码（排除待支付状态）
				if (this.isConfirmedStatus(this.appointment.status) && !this.isPendingPaymentStatus(this.appointment.status)) {
					console.log('[前端] 准备生成二维码并启动自动刷新')
					this.generateQRCode().then(() => {
						console.log('[前端] 二维码生成完成，启动自动刷新')
						this.startAutoRefresh()
					}).catch(error => {
						console.error('[前端] 二维码生成失败:', error)
					})
				} else {
					console.log('[前端] 当前状态不需要生成二维码 - status:', this.appointment.status)
				}
			} else {
				console.error('[前端] 预约详情加载失败 - 响应码:', response?.code, ', 响应消息:', response?.msg)
				throw new Error(response?.msg || '加载预约详情失败')
			}
		} catch (error) {
			console.error('[前端] 加载预约详情异常:', error)
			console.error('[前端] 错误详情:', {
				message: error.message,
				stack: error.stack,
				appointmentId: this.appointmentId
			})
			uni.showToast({
				title: error.message || '加载失败，请重试',
				icon: 'none'
			})
		} finally {
			this.loading = false
			const loadEndTime = new Date().toISOString()
			console.log('[前端] 预约详情加载完成 - 结束时间:', loadEndTime)
			console.log('[前端] ========== 预约详情加载结束 ==========')
		}
	},
			
			// 生成二维码
			async generateQRCode() {
				console.log('========== [前端] 开始生成二维码 ==========')
				const requestTime = new Date().toISOString()
				console.log('[前端] 请求时间:', requestTime)
				console.log('[前端] 预约ID:', this.appointmentId)
				
				if (!this.appointmentId) {
					console.warn('[前端] 预约ID为空，无法生成二维码')
					return
				}
				
				// 检查预约状态，已确认或已签到的预约都可以生成二维码
				const statusLower = (this.appointment.status || '').toLowerCase()
				const canGenerate = this.isConfirmedStatus(this.appointment.status) || statusLower === 'checked_in'
				const isExpired = this.isExpiredStatus(this.appointment)
				
				console.log('[前端] 预约状态检查:', {
					status: this.appointment.status,
					statusLower: statusLower,
					isConfirmed: this.isConfirmedStatus(this.appointment.status),
					isCheckedIn: statusLower === 'checked_in',
					canGenerate: canGenerate,
					isExpired: isExpired,
					scheduleTime: this.appointment.scheduleTime,
					scheduleEndTime: this.appointment.scheduleEndTime
				})
				
				if (!canGenerate || isExpired) {
					console.warn('[前端] 无法生成二维码 - canGenerate:', canGenerate, ', isExpired:', isExpired)
					return
				}
				
				try {
					console.log('[前端] 调用API生成二维码 - 预约ID:', this.appointmentId)
					const response = await getAppointmentQrCode(this.appointmentId)
					console.log('[前端] API响应:', JSON.stringify(response, null, 2))
					
					if (response && response.code === '200' && response.data) {
						this.qrToken = response.data.qrToken
						this.refreshInterval = response.data.refreshInterval || 60
						const expiresIn = response.data.expiresIn || 0
						const expiresInMinutes = Math.floor(expiresIn / 60)
						
						console.log('[前端] 二维码Token获取成功:', {
							qrToken: this.qrToken,
							refreshInterval: this.refreshInterval,
							expiresIn: expiresIn,
							expiresInMinutes: expiresInMinutes,
							expiresAt: new Date(Date.now() + expiresIn * 1000).toISOString()
						})
						
						// 使用在线API生成二维码图片（Token作为内容）
						// 注意：这里使用在线API生成图片，Token是从后端获取的
						this.qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(this.qrToken)}`
						console.log('[前端] 二维码图片URL已生成')
						
						// 重置倒计时
						this.refreshCountdown = this.refreshInterval
						console.log('[前端] 倒计时已重置:', this.refreshCountdown, '秒')
						console.log('[前端] 二维码将在', expiresInMinutes, '分钟后过期')
						console.log('========== [前端] 二维码生成成功 ==========')
					} else {
						console.error('[前端] 生成二维码失败 - 响应码:', response?.code, ', 响应数据:', response)
						uni.showToast({
							title: '生成二维码失败，请重试',
							icon: 'none'
						})
						// 不提供降级方案，要求用户重试
					}
				} catch (error) {
					console.error('[前端] 生成二维码异常:', error)
					console.error('[前端] 错误详情:', {
						message: error.message,
						stack: error.stack,
						appointmentId: this.appointmentId
					})
					uni.showToast({
						title: '生成二维码失败，请重试',
						icon: 'none'
					})
					// 不提供降级方案，要求用户重试
				}
			},
			
			// 手动刷新二维码
			refreshQRCode() {
				console.log('[前端] 手动刷新二维码 - 当前Token:', this.qrToken, ', 刷新间隔:', this.refreshInterval)
				const oldToken = this.qrToken
				this.qrCodeUrl = ''  // 清空旧二维码
				console.log('[前端] 旧二维码已清空，旧Token:', oldToken)
				this.generateQRCode()
			},
			
			// 启动自动刷新
			startAutoRefresh() {
				console.log('[前端] ========== 启动自动刷新 ==========')
				const isConfirmed = this.isConfirmedStatus(this.appointment.status)
				const isExpired = this.isExpiredStatus(this.appointment)
				console.log('[前端] 自动刷新检查 - 预约状态:', this.appointment.status, ', isConfirmed:', isConfirmed, ', isExpired:', isExpired)
				
				// 只有已确认且未过期的预约才启动刷新
				if (!isConfirmed || isExpired) {
					console.warn('[前端] 不满足自动刷新条件，取消启动')
					return
				}
				
				this.stopAutoRefresh()  // 先清除旧的定时器
				console.log('[前端] 旧定时器已清除')
				
				// 倒计时定时器（每秒更新）
				this.countdownTimer = setInterval(() => {
					if (this.refreshCountdown > 0) {
						this.refreshCountdown--
					} else {
						this.refreshCountdown = this.refreshInterval
					}
				}, 1000)
				console.log('[前端] 倒计时定时器已启动 - 间隔: 1秒')
				
				// 刷新定时器（每refreshInterval秒刷新一次）
				this.refreshTimer = setInterval(() => {
					console.log('[前端] ========== 自动刷新二维码 ==========')
					console.log('[前端] 自动刷新触发 - 当前时间:', new Date().toISOString(), ', 刷新间隔:', this.refreshInterval, '秒')
					this.refreshQRCode()
				}, this.refreshInterval * 1000)
				console.log('[前端] 刷新定时器已启动 - 间隔:', this.refreshInterval, '秒 (', this.refreshInterval * 1000, '毫秒)')
				console.log('[前端] ========== 自动刷新已启动 ==========')
			},
			
			// 停止自动刷新
			stopAutoRefresh() {
				console.log('[前端] ========== 停止自动刷新 ==========')
				console.log('[前端] 当前定时器状态 - refreshTimer:', this.refreshTimer, ', countdownTimer:', this.countdownTimer)
				
				if (this.refreshTimer) {
					clearInterval(this.refreshTimer)
					this.refreshTimer = null
					console.log('[前端] 刷新定时器已清除')
				}
				if (this.countdownTimer) {
					clearInterval(this.countdownTimer)
					this.countdownTimer = null
					console.log('[前端] 倒计时定时器已清除')
				}
				
				console.log('[前端] ========== 自动刷新已停止 ==========')
			},
			
			loadPatientInfo() {
				const stored = uni.getStorageSync('patientInfo')
				this.patientInfo = stored || mockPatientInfo
			},
			
			// 判断预约时间是否已过去（检查排班结束时间，而不是开始时间）
			isAppointmentTimePassed(appointment) {
				if (!appointment) return false
				
				// 优先使用排班结束时间，如果没有则使用开始时间
				const timeToCheck = appointment.scheduleEndTime || appointment.scheduleTime
				if (!timeToCheck) return false
				
				const endTime = new Date(timeToCheck)
				const now = new Date()
				
				// 检查日期是否有效
				if (isNaN(endTime.getTime())) {
					console.warn('[detail isAppointmentTimePassed] 无效的时间格式:', timeToCheck)
					return false
				}
				
				// 如果排班结束时间已经过去（至少1分钟），则认为已过去
				return endTime.getTime() < (now.getTime() - 60 * 1000)
			},
			
			// 判断是否为已过期状态（时间已过去但不是已完成状态）
			isExpiredStatus(appointment) {
				if (!appointment) return false
				// 已完成状态不算过期
				if (this.isCompletedStatus(appointment.status)) {
					return false
				}
				// 已取消状态不算过期
				if (this.isCancelledStatus(appointment.status)) {
					return false
				}
				// 检查时间是否已过去（至少1分钟）
				if (!this.isAppointmentTimePassed(appointment)) {
					return false
				}
				// 如果预约是今天创建的，且就诊时间也是今天，不显示为过期（可能是刚创建的预约）
				if (appointment.appointmentTime) {
					const appointmentDate = new Date(appointment.appointmentTime)
					const scheduleDate = appointment.scheduleTime ? new Date(appointment.scheduleTime) : null
					const now = new Date()
					
					// 如果预约是今天创建的，且就诊时间也是今天，不显示为过期
					if (scheduleDate && 
						appointmentDate.toDateString() === now.toDateString() &&
						scheduleDate.toDateString() === now.toDateString()) {
						// 检查预约创建时间和就诊时间的间隔
						const timeDiff = scheduleDate.getTime() - appointmentDate.getTime()
						// 如果预约创建时间在就诊时间之后，说明是刚创建的预约，不显示为过期
						if (timeDiff < 0) {
							return false
						}
					}
				}
				return true
			},
			
			getStatusText(appointment) {
				if (!appointment || !appointment.status) return '未知'
				
				// 如果已过期，返回"已过期"
				if (this.isExpiredStatus(appointment)) {
					return '已过期'
				}
				
				const status = appointment.status
				const statusLower = status.toLowerCase()
				const statusMap = {
					'confirmed': '已预约',
					'scheduled': '已预约',
					'checked_in': '已签到',
					'CHECKED_IN': '已签到',
					'completed': '已完成',
					'cancelled': '已取消',
					'pending': '待支付',
					'pending_payment': '待支付',
					'no_show': '爽约',
					'NO_SHOW': '爽约'
				}
				return statusMap[statusLower] || statusMap[status] || '未知'
			},
			
			getStatusIcon(appointment) {
				if (!appointment) return '❓'
				
				// 如果已过期，返回过期图标
				if (this.isExpiredStatus(appointment)) {
					return '⏰'
				}
				
				const status = appointment.status
				if (!status) return '❓'
				const statusLower = status.toLowerCase()
				const iconMap = {
					'confirmed': '✅',
					'scheduled': '✅',
					'checked_in': '📝',
					'CHECKED_IN': '📝',
					'completed': '✔️',
					'cancelled': '❌',
					'pending': '⏳',
					'pending_payment': '⏳',
					'no_show': '⚠️',
					'NO_SHOW': '⚠️'
				}
				return iconMap[statusLower] || iconMap[status] || '❓'
			},
			
			// 判断是否为已确认状态（兼容大小写）
			// 包括：confirmed, scheduled, checked_in（已支付的状态）
			// 注意：不包括 pending_payment（待支付状态不显示二维码）
			isConfirmedStatus(status) {
				if (!status) {
					console.log('[detail isConfirmedStatus] status 为空')
					return false
				}
				const statusLower = status.toLowerCase()
				const result = statusLower === 'confirmed' || 
					   statusLower === 'scheduled' || 
					   statusLower === 'checked_in'
				console.log('[detail isConfirmedStatus] 状态:', status, '转换为:', statusLower, '结果:', result)
				return result
			},
			
			// 判断是否为已完成状态
			isCompletedStatus(status) {
				if (!status) return false
				return status.toLowerCase() === 'completed'
			},
			
			// 判断是否为已取消状态
			isCancelledStatus(status) {
				if (!status) return false
				return status.toLowerCase() === 'cancelled'
			},
			
			// 判断是否为待支付状态
			isPendingStatus(status) {
				if (!status) return false
				const statusLower = status.toLowerCase()
				return statusLower === 'pending' || statusLower === 'pending_payment'
			},
			
			// 判断是否为待支付状态（用于显示支付按钮）
			isPendingPaymentStatus(status) {
				if (!status) return false
				const statusLower = status.toLowerCase()
				return statusLower === 'pending_payment'
			},
			
			// 判断是否可以取消预约（已取消状态、已签到状态和已过期的预约不能取消）
			canCancelAppointment(status) {
				if (!status || !this.appointment) return false
				
				const statusLower = (status || '').toLowerCase()
				
				// 已签到状态不能取消
				if (statusLower === 'checked_in') {
					return false
				}
				// // 已取消状态不能再次取消
				// if (statusLower === 'cancelled') {
				// 	return false
				// }
				// 已完成状态不能取消
				if (statusLower === 'completed') {
					return false
				}
				// 检查预约时间是否已过去（至少1分钟）
				if (this.appointment && this.appointment.scheduleTime) {
					const scheduleTime = new Date(this.appointment.scheduleTime)
					const now = new Date()
					// 如果就诊时间已经过去（至少1分钟），不能取消
					if (scheduleTime.getTime() < (now.getTime() - 60 * 1000)) {
						console.log('[detail canCancelAppointment] 预约时间已过去，不能取消')
						return false
					}
				}
				// 只有已预约或待支付状态可以取消
				const canCancel = statusLower === 'confirmed' || 
								  statusLower === 'scheduled' || 
								  statusLower === 'pending_payment' ||
								  statusLower === 'pending'
				console.log('[detail canCancelAppointment] 状态:', status, '可以取消:', canCancel)
				return canCancel
			},
			
			formatDateTime(dateString) {
				if (!dateString) return ''
				const date = new Date(dateString)
				const month = date.getMonth() + 1
				const day = date.getDate()
				const hours = date.getHours().toString().padStart(2, '0')
				const minutes = date.getMinutes().toString().padStart(2, '0')
				return `${month}月${day}日 ${hours}:${minutes}`
			},
			
			// 获取地点名称
			getLocationName(appointment) {
				if (!appointment) return ''
				
				// 优先从schedule.location获取
				if (appointment.schedule && appointment.schedule.location) {
					return appointment.schedule.location
				}
				
				// 如果没有，尝试从其他字段获取
				if (appointment.location) {
					return appointment.location
				}
				
				// 如果都没有，返回空字符串（不显示）
				return ''
			// 处理支付
			async handlePayment() {
				if (!this.appointmentId) {
					uni.showToast({
						title: '预约信息不完整',
						icon: 'none'
					})
					return
				}
				
				// 显示支付方式选择
				uni.showActionSheet({
					itemList: this.paymentMethods.map(m => m.icon + ' ' + m.name),
					success: async (res) => {
						const selectedMethod = this.paymentMethods[res.tapIndex]
						this.selectedPaymentMethod = selectedMethod.value
						
						// 确认支付
						uni.showModal({
							title: '确认支付',
							content: `使用${selectedMethod.name}支付 ¥${this.appointment.fee || 0}？`,
							success: async (modalRes) => {
								if (modalRes.confirm) {
									await this.processPayment()
								}
							}
						})
					}
				})
			},
			
			// 处理支付流程
			async processPayment() {
				uni.showLoading({ title: '支付中...' })
				
				try {
					console.log('开始支付，appointmentId:', this.appointmentId)
					
					const response = await payForAppointment(this.appointmentId, {
						paymentMethod: this.selectedPaymentMethod,
						transactionId: 'TXN' + Date.now()
					})
					
					console.log('支付完整响应:', JSON.stringify(response, null, 2))
					
					// 检查响应
					if (response && (response.code === '200' || response.appointmentId)) {
						uni.hideLoading()
						
						// 显示支付成功
						uni.showToast({
							title: '支付成功',
							icon: 'success',
							duration: 2000
						})
						
						// 延迟刷新页面，显示二维码
						setTimeout(() => {
							this.loadAppointmentDetail()
						}, 2000)
					} else {
						throw new Error(response?.msg || response?.message || '支付失败')
					}
				} catch (error) {
					console.error('支付失败:', error)
					uni.hideLoading()
					uni.showToast({
						title: error.message || '支付失败，请重试',
						icon: 'none',
						duration: 2000
					})
				}
			},
			
			async handleCancel() {
				// 检查是否可以取消
				if (!this.canCancelAppointment(this.appointment.status)) {
					// 检查是否是时间已过去
					if (this.appointment && this.appointment.scheduleTime) {
						const scheduleTime = new Date(this.appointment.scheduleTime)
						const now = new Date()
						if (scheduleTime <= now) {
							uni.showToast({
								title: '预约时间已过，无法取消',
								icon: 'none',
								duration: 2000
							})
							return
						}
					}
					uni.showToast({
						title: '该预约无法取消',
						icon: 'none',
						duration: 2000
					})
					return
				}
				
				uni.showModal({
					title: '确认取消',
					content: '确定要取消这个预约吗？',
					success: async (res) => {
						if (res.confirm) {
							try {
							uni.showLoading({ title: '取消中...' })
								const response = await cancelAppointment(this.appointmentId)
								console.log('取消预约响应:', response)
							
								if (response && response.code === '200') {
								uni.showToast({
									title: '预约已取消',
									icon: 'success'
								})
								
								setTimeout(() => {
									uni.navigateBack()
								}, 1500)
								} else {
									uni.showToast({
										title: response?.msg || '取消失败',
										icon: 'none'
									})
								}
							} catch (error) {
								console.error('取消预约失败:', error)
								uni.showToast({
									title: '取消失败，请重试',
									icon: 'none'
								})
							} finally {
								uni.hideLoading()
							}
						}
					}
				})
			},
			
			// 导航到诊室
			handleNavigation() {
				if (!this.appointmentId) {
					uni.showToast({
						title: '预约信息不完整',
						icon: 'none'
					})
					return
				}
				
				// 优先使用schedule中的locationId
				let locationId = null
				if (this.appointment && this.appointment.schedule && this.appointment.schedule.locationId) {
					locationId = this.appointment.schedule.locationId
				}
				
				// 如果有locationId，直接传递；否则传递appointmentId让导航页自己获取
				if (locationId) {
				uni.navigateTo({
						url: `/pages/navigation/index?locationId=${locationId}`
					})
				} else {
					// 传递appointmentId，导航页会调用API获取locationId
					uni.navigateTo({
						url: `/pages/navigation/index?appointmentId=${this.appointmentId}`
				})
				}
			},
			
			handleBackToHome() {
				// 返回主页，并触发列表页刷新
				uni.switchTab({
					url: '/pages/index/index',
					success: () => {
						// 通过事件通知列表页刷新
						uni.$emit('refreshAppointmentList')
					}
				})
			}
		}
	}
</script>

<style lang="scss">
	.container {
		min-height: 100vh;
		background-color: #f7fafc;
		padding-bottom: 120rpx;
	}

	.page-header {
		background: linear-gradient(135deg, #7be6d8 0%, #4FD9C3 100%);
		padding: 40rpx 30rpx 30rpx;
	}

	.page-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.content {
		padding: 30rpx;
	}

	.status-card {
		background: linear-gradient(135deg, #7be6d8 0%, #4FD9C3 100%);
		border-radius: 20rpx;
		padding: 60rpx 30rpx;
		margin-bottom: 20rpx;
		text-align: center;
		box-shadow: 0 4rpx 20rpx rgba(79, 209, 197, 0.3);
	}
	
	.status-card .status-icon.expired {
		color: #C2410C;
	}

	.status-icon {
		font-size: 80rpx;
		margin-bottom: 16rpx;
	}

	.status-text {
		display: block;
		font-size: 32rpx;
		color: #ffffff;
		font-weight: 600;
	}

	.info-card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.card-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #1A202C;
		margin-bottom: 24rpx;
		padding-bottom: 20rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.info-row {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.label {
		font-size: 28rpx;
		color: #718096;
		width: 160rpx;
	}

	.value {
		font-size: 28rpx;
		color: #1A202C;
		font-weight: 500;
	}

	.queue-number {
		color: #4FD9C3;
		font-weight: 700;
		font-size: 32rpx;
	}

	.qr-code-card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.qr-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
	}
	
	.qr-refresh-tip {
		font-size: 24rpx;
		color: #FFA500;
		font-weight: 600;
	}

	.qr-icon {
		font-size: 32rpx;
		margin-right: 8rpx;
	}

	.qr-text {
		font-size: 30rpx;
		font-weight: 600;
		color: #1A202C;
	}

	.qr-container {
		display: flex;
		justify-content: center;
		align-items: center;
		margin-bottom: 16rpx;
		padding: 20rpx;
		background: #f7fafc;
		border-radius: 16rpx;
	}

	.qr-code {
		width: 400rpx;
		height: 400rpx;
	}

	.qr-loading {
		padding: 60rpx;
		color: #718096;
		text-align: center;
	}
	
	.qr-desc {
		display: block;
		text-align: center;
		font-size: 24rpx;
		color: #718096;
		margin-top: 20rpx;
	}
	
	.qr-tip {
		display: block;
		text-align: center;
		font-size: 24rpx;
		color: #FF4D4F;
		font-weight: 600;
		margin-top: 12rpx;
	}
	
	.qr-refresh-btn {
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 16rpx 32rpx;
		background: #E6F7FF;
		border-radius: 50rpx;
		color: #1890FF;
		font-size: 26rpx;
		font-weight: 600;
		margin-top: 20rpx;
	}

	.navigation-section {
		margin: 20rpx 0;
		padding: 0 30rpx;
	}
	
	.navigation-btn {
		width: 100%;
		height: 96rpx;
		background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%);
		border: none;
		border-radius: 50rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
	}
	
	.nav-icon {
		font-size: 36rpx;
	}
	
	.action-section {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		padding: 30rpx;
		background: #ffffff;
		box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.08);
	}

	.button-row {
		display: flex;
		gap: 20rpx;
		width: 100%;
	}

	.home-btn {
		flex: 1;
		height: 96rpx;
		background: linear-gradient(135deg, #4FD9C3 0%, #7be6d8 100%);
		border: none;
		border-radius: 50rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 600;
	}

	.cancel-btn {
		flex: 1;
		height: 96rpx;
		background: #FFF5F5;
		border: 2rpx solid #FED7D7;
		border-radius: 50rpx;
		color: #DC2626;
		font-size: 32rpx;
		font-weight: 600;
	}

	/* 待支付状态的并排按钮 */
	.pay-btn-half {
		flex: 1;
		height: 96rpx;
		background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%);
		border: none;
		border-radius: 50rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 600;
		box-shadow: 0 4rpx 12rpx rgba(82, 196, 26, 0.3);
	}

	.pay-btn-half:active {
		opacity: 0.8;
		transform: scale(0.98);
	}

	.cancel-btn-half {
		flex: 1;
		height: 96rpx;
		background: #FFF5F5;
		border: 2rpx solid #FED7D7;
		border-radius: 50rpx;
		color: #DC2626;
		font-size: 32rpx;
		font-weight: 600;
	}

	.cancel-btn-half:active {
		opacity: 0.8;
		transform: scale(0.98);
	}

	.view-btn {
		flex: 1;
		height: 96rpx;
		background: #E6FFFA;
		border: 2rpx solid #7be6d8;
		border-radius: 50rpx;
		color: #38A2AC;
		font-size: 32rpx;
		font-weight: 600;
	}

	.missed-call-count {
		color: #FF4D4F;
		font-weight: 700;
		font-size: 28rpx;
	}

	.missed-call-card {
		background: #FFF7E6;
		border: 2rpx solid #FFD591;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.missed-call-icon {
		font-size: 48rpx;
	}

	.missed-call-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.missed-call-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #FA8C16;
	}

	.missed-call-desc {
		font-size: 26rpx;
		color: #AD6800;
		line-height: 1.5;
	}

	/* 待支付卡片样式 */
	.payment-pending-card {
		background: linear-gradient(135deg, #E6F7FF 0%, #BAE7FF 100%);
		border: 2rpx solid #91D5FF;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.payment-icon {
		font-size: 48rpx;
	}

	.payment-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.payment-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1890FF;
	}

	.payment-desc {
		font-size: 26rpx;
		color: #0050B3;
		line-height: 1.5;
	}

	.payment-fee {
		font-size: 32rpx;
		font-weight: 700;
		color: #FF4D4F;
		margin-top: 8rpx;
	}

	.payment-deadline {
		font-size: 24rpx;
		color: #FA8C16;
		margin-top: 4rpx;
	}

</style>
