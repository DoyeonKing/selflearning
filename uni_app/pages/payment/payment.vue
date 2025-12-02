<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">支付挂号费</text>
		</view>
		
		<view class="content">
			<!-- 费用详情卡片 -->
			<view class="amount-card">
				<text class="amount-label">挂号费用</text>
				<text class="amount-value">¥{{ fee }}</text>
			</view>
			
			<!-- 预约信息 -->
			<view class="info-card">
				<view class="card-title">预约信息</view>
				<view class="info-row">
					<text class="label">科室：</text>
					<text class="value">{{ departmentName }}</text>
				</view>
			<view class="info-row">
				<text class="label">医生：</text>
				<text class="value">{{ doctorName }} {{ doctorTitle }}</text>
			</view>
			<view class="info-row">
				<text class="label">就诊时间：</text>
				<text class="value">{{ scheduleDate }} {{ slotName }}</text>
			</view>
			<view class="info-row" v-if="location">
				<text class="label">就诊地点：</text>
				<text class="value">{{ location }}</text>
			</view>
			</view>
			
			<!-- 支付方式 -->
			<view class="payment-methods">
				<text class="methods-title">选择支付方式</text>
				<view 
					class="method-item" 
					v-for="method in paymentMethods" 
					:key="method.value"
					:class="{ active: selectedMethod === method.value }"
					@click="selectMethod(method.value)"
				>
					<text class="method-icon">{{ method.icon }}</text>
					<text class="method-name">{{ method.name }}</text>
					<view class="method-radio" v-if="selectedMethod === method.value">
						<text class="radio-icon">✓</text>
					</view>
				</view>
			</view>
			
			<!-- 支付按钮 -->
			<view class="payment-section">
				<view class="total-info">
					<text class="total-label">实付：</text>
					<text class="total-value">¥{{ fee }}</text>
				</view>
				<button class="pay-btn" @click="handlePayment">立即支付</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mockSchedules, mockPatientInfo } from '../../api/mockData.js'
	
	export default {
		data() {
			return {
				scheduleId: null,
				waitlistId: null, // 候补ID
				fee: 0,
				departmentName: '',
				doctorName: '',
				doctorTitle: '',
				scheduleDate: '',
				slotName: '',
				location: '', // 就诊地点
				patientInfo: {},
				appointmentId: null, // 预约ID
				isWaitlist: false, // 是否为候补支付
				appointmentCreated: false, // 标记是否已创建预约，防止重复创建
				selectedMethod: 'wechat',
				paymentMethods: [
					{ value: 'wechat', name: '微信支付', icon: '💚' },
					{ value: 'alipay', name: '支付宝', icon: '🔵' },
					{ value: 'balance', name: '校园卡余额', icon: '💳' }
				]
			}
		},
		onLoad(options) {
			this.scheduleId = options.scheduleId ? parseInt(options.scheduleId) : null
			this.waitlistId = options.waitlistId ? parseInt(options.waitlistId) : null
			this.fee = parseFloat(options.fee || 0)
			this.departmentName = decodeURIComponent(options.departmentName || '')
			this.doctorName = decodeURIComponent(options.doctorName || '')
			this.doctorTitle = decodeURIComponent(options.doctorTitle || '')
			this.scheduleDate = decodeURIComponent(options.scheduleDate || '')
			this.slotName = decodeURIComponent(options.slotName || '')
			this.location = decodeURIComponent(options.location || '')
			this.isWaitlist = !!this.waitlistId
			this.loadPatientInfo()
			this.createAppointment()
		},
		methods: {
			loadPatientInfo() {
				const stored = uni.getStorageSync('patientInfo')
				this.patientInfo = stored || mockPatientInfo
				console.log('加载患者信息:', this.patientInfo)
				
				// 如果没有患者ID，提示用户
				if (!this.patientInfo || !this.patientInfo.id) {
					console.warn('警告：患者ID不存在，可能导致创建预约失败')
					uni.showToast({
						title: '请先登录',
						icon: 'none',
						duration: 2000
					})
				}
			},
			
			selectMethod(method) {
				this.selectedMethod = method
			},
			
			async createAppointment() {
				// 防止重复创建
				if (this.appointmentCreated) {
					console.log('预约已创建，跳过重复创建')
					return
				}
				
				try {
					if (this.isWaitlist) {
						// 候补支付：使用候补ID
						this.appointmentId = this.waitlistId
						this.appointmentCreated = true
						console.log('候补支付，使用waitlistId:', this.appointmentId)
					} else {
						// 检查必要参数
						if (!this.scheduleId) {
							throw new Error('排班ID不能为空')
						}
						if (!this.patientInfo || !this.patientInfo.id) {
							throw new Error('患者信息不完整，请先登录')
						}
						
						console.log('准备创建预约，参数:', {
							scheduleId: this.scheduleId,
							patientId: this.patientInfo.id
						})
						
						// 动态导入 appointment API
						const { createAppointment } = await import('../../api/appointment.js')
						
						// 创建预约（状态为待支付）
						const response = await createAppointment({
							scheduleId: this.scheduleId,
							patientId: this.patientInfo.id
						})
						
						console.log('创建预约完整响应:', JSON.stringify(response, null, 2))
						
						// 处理不同的响应格式
						let appointmentData = null
						if (response && response.code === '200' && response.data) {
							appointmentData = response.data
						} else if (response && response.appointmentId) {
							// 直接返回 AppointmentResponse
							appointmentData = response
						} else if (response && response.data && response.data.appointmentId) {
							appointmentData = response.data
						}
						
						if (appointmentData) {
							this.appointmentId = appointmentData.appointmentId || appointmentData.id
							this.appointmentCreated = true
							console.log('预约创建成功，appointmentId:', this.appointmentId)
						} else {
							console.error('创建预约响应格式异常:', response)
							// 检查是否是重复预约错误
							const errorMsg = response?.msg || response?.message || ''
							if (errorMsg.includes('already has an appointment')) {
								// 如果是重复预约，尝试获取已存在的预约
								console.log('检测到重复预约，尝试获取已存在的预约')
								// 这里可以调用获取预约列表接口，找到对应的预约
								// 暂时提示用户
								throw new Error('您已预约过该排班，请前往预约列表查看')
							} else {
								throw new Error(errorMsg || '创建预约失败：响应格式异常')
							}
						}
					}
				} catch (error) {
					console.error('创建预约失败，详细信息:', error)
					console.error('错误堆栈:', error.stack)
					
					// 如果是重复预约错误，检查是否有已取消的预约，如果有则恢复预约
					const errorMessage = error.message || ''
					if (errorMessage.includes('already has an appointment') || errorMessage.includes('已预约')) {
						// 检查是否有该排班的已取消预约
						try {
							const appointmentApi = await import('../../api/appointment.js')
							const appointmentsResponse = await appointmentApi.getPatientAppointments(this.patientInfo.id)
							const appointments = appointmentsResponse?.data || appointmentsResponse || []
							
							// 查找该排班的预约（包括已取消的）
							const existingAppointment = appointments.find(apt => 
								apt.scheduleId === this.scheduleId
							)
							
							if (existingAppointment) {
								// 找到该排班的预约，检查状态
								console.log('找到该排班的预约:', existingAppointment)
								const appointmentId = existingAppointment.appointmentId || existingAppointment.id
								const currentStatus = existingAppointment.status
								
								// 如果是已取消状态，尝试恢复
								if (currentStatus === 'cancelled' || currentStatus === 'CANCELLED') {
									try {
										uni.showLoading({ title: '处理中...' })
										
										// 更新预约状态：从 cancelled 改为 scheduled（已预约，待支付）
										// 注意：数据库字段可能不支持 PENDING_PAYMENT，所以使用 scheduled
										const updateResponse = await appointmentApi.updateAppointmentPayment(
											appointmentId,
											{
												status: 'scheduled',
												paymentStatus: 'unpaid'
											}
										)
										
										console.log('更新预约完整响应:', JSON.stringify(updateResponse, null, 2))
										
										uni.hideLoading()
										
										// 处理不同的响应格式
										let updatedAppointment = null
										if (updateResponse && updateResponse.code === '200' && updateResponse.data) {
											updatedAppointment = updateResponse.data
										} else if (updateResponse && updateResponse.appointmentId) {
											updatedAppointment = updateResponse
										} else if (updateResponse && updateResponse.data && updateResponse.data.appointmentId) {
											updatedAppointment = updateResponse.data
										}
										
										if (updatedAppointment) {
											// 恢复成功，使用恢复后的预约ID
											this.appointmentId = updatedAppointment.appointmentId || updatedAppointment.id || appointmentId
											this.appointmentCreated = true
											console.log('预约恢复成功，appointmentId:', this.appointmentId)
											// 不显示提示，和正常挂号一样
										} else {
											console.error('更新预约响应格式异常，完整响应:', updateResponse)
											const errorMsg = updateResponse?.msg || updateResponse?.message || updateResponse?.error || '恢复预约失败'
											throw new Error(errorMsg)
										}
									} catch (restoreError) {
										uni.hideLoading()
										console.error('恢复预约失败，错误对象:', restoreError)
										console.error('恢复预约失败，错误消息:', restoreError.message)
										console.error('恢复预约失败，错误堆栈:', restoreError.stack)
										
										// 提取错误信息
										let errorMsg = restoreError.message || ''
										if (restoreError.response) {
											errorMsg = restoreError.response.msg || restoreError.response.message || errorMsg
										}
										
										uni.showModal({
											title: '预约失败',
											content: errorMsg || '无法恢复预约，请稍后再试或联系客服处理。',
											showCancel: true,
											confirmText: '查看预约',
											cancelText: '返回',
											success: (res) => {
												if (res.confirm) {
													uni.switchTab({
														url: '/pages/appointments/appointments'
													})
												} else {
													uni.navigateBack()
												}
											}
										})
									}
								} else {
									// 不是已取消状态，说明是有效的重复预约
									uni.showModal({
										title: '预约提示',
										content: '您已预约过该排班，请前往预约列表查看',
										showCancel: false,
										success: () => {
											uni.switchTab({
												url: '/pages/appointments/appointments'
											})
										}
									})
								}
							} else {
								// 没有已取消的预约，说明是有效的重复预约
								uni.showModal({
									title: '预约提示',
									content: '您已预约过该排班，请前往预约列表查看',
									showCancel: false,
									success: () => {
										uni.switchTab({
											url: '/pages/appointments/appointments'
										})
									}
								})
							}
						} catch (checkError) {
							console.error('检查预约列表失败:', checkError)
							// 如果检查失败，使用默认提示
							uni.showModal({
								title: '预约提示',
								content: '您已预约过该排班，请前往预约列表查看',
								showCancel: false,
								success: () => {
									uni.switchTab({
										url: '/pages/appointments/appointments'
									})
								}
							})
						}
					} else {
						// 检查是否是时间过期错误
						const errorMessage = error.message || ''
						let displayMessage = errorMessage
						
						if (errorMessage.includes('Cannot book past or ongoing schedules') || 
						    errorMessage.includes('past or ongoing') ||
						    errorMessage.includes('不能预约过去') ||
						    errorMessage.includes('时间已过期')) {
							displayMessage = '该排班时间已过期，无法预约。请选择其他时间段的排班。'
						} else if (errorMessage.includes('No available slots')) {
							displayMessage = '该排班号源已满，无法预约。'
						} else if (errorMessage.includes('not active')) {
							displayMessage = '该排班不可预约。'
						}
						
						uni.showModal({
							title: '预约失败',
							content: displayMessage,
							showCancel: false,
							success: () => {
								uni.navigateBack()
							}
						})
					}
				}
			},
			
			async handlePayment() {
				if (!this.appointmentId) {
					uni.showToast({
						title: '请先创建预约',
						icon: 'none'
					})
					return
				}
				
				uni.showLoading({ title: '支付中...' })
				
				try {
					console.log('开始支付，appointmentId:', this.appointmentId, 'isWaitlist:', this.isWaitlist)
					
					// 动态导入 appointment API
					const appointmentApi = await import('../../api/appointment.js')
					
					if (this.isWaitlist) {
						// 候补支付：调用 payForWaitlist
						console.log('调用 payForWaitlist，waitlistId:', this.waitlistId)
						const response = await appointmentApi.payForWaitlist(this.waitlistId, {
							paymentMethod: this.selectedMethod,
							transactionId: 'TXN' + Date.now()
						})
						
						console.log('候补支付完整响应:', JSON.stringify(response, null, 2))
						
						if (response && response.code === '200' && response.data) {
							this.appointmentId = response.data.appointmentId || response.data.id
							console.log('候补支付成功，新的appointmentId:', this.appointmentId)
						} else {
							throw new Error(response?.msg || response?.message || '支付失败')
						}
					} else {
						// 普通预约支付：调用 payForAppointment
						console.log('调用 payForAppointment，appointmentId:', this.appointmentId)
						const response = await appointmentApi.payForAppointment(this.appointmentId, {
							paymentMethod: this.selectedMethod,
							transactionId: 'TXN' + Date.now()
						})
						
						console.log('预约支付完整响应:', JSON.stringify(response, null, 2))
						
						// 检查响应
						if (response && response.code === '200') {
							console.log('支付成功')
						} else if (response && response.appointmentId) {
							// 直接返回 AppointmentResponse，也认为是成功
							console.log('支付成功（直接返回）')
						} else {
							throw new Error(response?.msg || response?.message || '支付失败')
						}
					}
					
					uni.hideLoading()
					
					// 显示支付成功
					uni.showToast({
						title: '支付成功',
						icon: 'success',
						duration: 2000
					})
					
					// 跳转到预约详情页面，显示二维码
					setTimeout(() => {
						console.log('跳转到预约详情，appointmentId:', this.appointmentId)
						uni.redirectTo({
							url: `/pages/appointment/detail?appointmentId=${this.appointmentId}&departmentName=${encodeURIComponent(this.departmentName)}&doctorName=${encodeURIComponent(this.doctorName)}&doctorTitle=${encodeURIComponent(this.doctorTitle)}&scheduleDate=${encodeURIComponent(this.scheduleDate)}&slotName=${encodeURIComponent(this.slotName)}`
						})
					}, 2000)
				} catch (error) {
					uni.hideLoading()
					console.error('支付失败，详细信息:', error)
					console.error('错误堆栈:', error.stack)
					uni.showToast({
						title: error.message || '支付失败，请重试',
						icon: 'none',
						duration: 3000
					})
				}
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

	.amount-card {
		background: linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%);
		border-radius: 20rpx;
		padding: 60rpx 30rpx;
		margin-bottom: 20rpx;
		text-align: center;
		box-shadow: 0 4rpx 20rpx rgba(255, 107, 107, 0.3);
	}

	.amount-label {
		display: block;
		font-size: 28rpx;
		color: rgba(255, 255, 255, 0.9);
		margin-bottom: 16rpx;
	}

	.amount-value {
		display: block;
		font-size: 72rpx;
		font-weight: 700;
		color: #ffffff;
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
		margin-bottom: 20rpx;
	}

	.info-row:last-child {
		margin-bottom: 0;
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

	.payment-methods {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.methods-title {
		display: block;
		font-size: 30rpx;
		font-weight: 600;
		color: #1A202C;
		margin-bottom: 24rpx;
	}

	.method-item {
		display: flex;
		align-items: center;
		padding: 24rpx;
		margin-bottom: 12rpx;
		border-radius: 16rpx;
		border: 2rpx solid #E2E8F0;
		transition: all 0.3s ease;
	}

	.method-item.active {
		border-color: #4FD9C3;
		background: #F0FDFC;
	}

	.method-icon {
		font-size: 40rpx;
		margin-right: 20rpx;
	}

	.method-name {
		flex: 1;
		font-size: 28rpx;
		color: #1A202C;
		font-weight: 500;
	}

	.method-radio {
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		background: #4FD9C3;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.radio-icon {
		font-size: 28rpx;
		color: #ffffff;
		font-weight: 700;
	}

	.payment-section {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		padding: 30rpx;
		background: #ffffff;
		box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.08);
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.total-info {
		display: flex;
		align-items: baseline;
		margin-right: 20rpx;
	}

	.total-label {
		font-size: 28rpx;
		color: #718096;
	}

	.total-value {
		font-size: 40rpx;
		color: #FF6B6B;
		font-weight: 700;
		margin-left: 8rpx;
	}

	.pay-btn {
		flex: 1;
		height: 96rpx;
		background: linear-gradient(135deg, #7be6d8 0%, #4FD9C3 100%);
		border-radius: 50rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 600;
		border: none;
	}
</style>
