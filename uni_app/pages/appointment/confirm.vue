<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">预约确认</text>
		</view>
		
		<view class="content">
			<!-- 患者信息卡片 -->
			<view class="info-card patient-card">
				<view class="card-title">
					<text>👤 患者信息</text>
				</view>
				<view class="info-content">
					<view class="info-row">
						<text class="label">姓名：</text>
						<text class="value">{{ patientInfo.name }}</text>
					</view>
					<view class="info-row">
						<text class="label">学号/工号：</text>
						<text class="value">{{ patientInfo.identifier }}</text>
					</view>
				</view>
			</view>
			
			<!-- 预约信息卡片 -->
			<view class="info-card appointment-card">
				<view class="card-title">
					<text>📅 预约信息</text>
				</view>
				<view class="info-content">
					<view class="info-row">
						<text class="label">科室：</text>
						<text class="value">{{ scheduleInfo.departmentName }}</text>
					</view>
					<view class="info-row">
						<text class="label">医生：</text>
						<text class="value">{{ scheduleInfo.doctorName }} {{ scheduleInfo.doctorTitle }}</text>
					</view>
					<view class="info-row">
						<text class="label">就诊时间：</text>
						<text class="value">{{ scheduleInfo.scheduleDate }} {{ scheduleInfo.slotName }}</text>
					</view>
					<view class="info-row">
						<text class="label">诊室：</text>
						<text class="value">{{ scheduleInfo.location }}</text>
					</view>
					<view class="info-row">
						<text class="label">挂号费用：</text>
						<text class="value price">¥{{ scheduleInfo.fee }}</text>
					</view>
				</view>
			</view>
			
			<!-- 确认按钮 -->
			<view class="confirm-section">
				<button class="confirm-btn" @click="handleConfirm">确认预约</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mockSchedules, mockPatientInfo } from '../../api/mockData.js'
	import { getScheduleById } from '../../api/schedule.js'
	import { adaptSchedule } from '../../utils/dataAdapter.js'
	
	export default {
		data() {
			return {
				scheduleId: null,
				scheduleInfo: {
					departmentName: '',
					doctorName: '',
					doctorTitle: '',
					scheduleDate: '',
					slotName: '',
					location: '',
					fee: 0
				},
				patientInfo: {
					name: '',
					identifier: ''
				}
			}
		},
		onLoad(options) {
			console.log('预约确认页加载 - options:', options)
			this.scheduleId = parseInt(options.scheduleId)
			console.log('预约确认页 - scheduleId:', this.scheduleId)
			this.loadScheduleInfo()
			this.loadPatientInfo()
		},
		methods: {
			async loadScheduleInfo() {
				try {
					console.log('加载排班信息 - scheduleId:', this.scheduleId)
					// 调用后端API获取排班详情
					const response = await getScheduleById(this.scheduleId)
					console.log('排班详情API响应:', response)
					
					// getScheduleById 返回格式：{code: '200', data: {...}} 或直接返回 ScheduleResponse
					let scheduleData = null
					
					if (response && response.code === '200' && response.data) {
						// 标准 Result 格式
						scheduleData = response.data
					} else if (response && response.scheduleId) {
						// 直接返回 ScheduleResponse 对象
						scheduleData = adaptSchedule(response)
					} else if (response && response.data) {
						// 可能已经在 data 中
						scheduleData = adaptSchedule(response.data)
					} else {
						throw new Error('返回数据格式异常')
					}
					
					console.log('处理后的排班数据:', scheduleData)
					
					if (scheduleData) {
						this.scheduleInfo = {
							departmentName: scheduleData.departmentName || '',
							doctorName: scheduleData.doctorName || '',
							doctorTitle: scheduleData.doctorTitle || '',
							scheduleDate: scheduleData.scheduleDate || '',
							slotName: scheduleData.slotName || '',
							location: scheduleData.location || '',
							fee: scheduleData.fee || 0
						}
						console.log('设置后的scheduleInfo:', this.scheduleInfo)
					} else {
						throw new Error('返回数据格式异常')
					}
				} catch (error) {
					console.error('加载排班信息失败:', error)
					// 如果后端失败，使用Mock数据作为fallback
					try {
						const allSchedules = JSON.parse(JSON.stringify(mockSchedules))
						const found = allSchedules.find(s => s.scheduleId === this.scheduleId)
						if (found) {
							this.scheduleInfo = {
								departmentName: found.departmentName || '',
								doctorName: found.doctorName || '',
								doctorTitle: found.doctorTitle || '',
								scheduleDate: found.scheduleDate || '',
								slotName: found.slotName || '',
								location: found.location || '',
								fee: found.fee || 0
							}
						} else {
							uni.showToast({
								title: '排班信息不存在',
								icon: 'none'
							})
						}
					} catch (fallbackError) {
						console.error('Fallback失败:', fallbackError)
					}
				}
			},
			
			loadPatientInfo() {
				try {
					const stored = uni.getStorageSync('patientInfo')
					if (stored) {
						this.patientInfo = stored
					} else {
						this.patientInfo = mockPatientInfo || { name: '', identifier: '' }
					}
				} catch (error) {
					console.error('加载患者信息失败:', error)
					this.patientInfo = mockPatientInfo || { name: '', identifier: '' }
				}
			},
			
			async handleConfirm() {
				// 验证数据
				if (!this.scheduleInfo.doctorName || !this.patientInfo.name) {
					uni.showToast({
						title: '信息不完整',
						icon: 'none'
					})
					return
				}
				
		// 跳转到支付页面
		uni.navigateTo({
			url: `/pages/payment/payment?scheduleId=${this.scheduleId}&fee=${this.scheduleInfo.fee}&departmentName=${encodeURIComponent(this.scheduleInfo.departmentName)}&doctorName=${encodeURIComponent(this.scheduleInfo.doctorName)}&doctorTitle=${encodeURIComponent(this.scheduleInfo.doctorTitle)}&scheduleDate=${encodeURIComponent(this.scheduleInfo.scheduleDate)}&slotName=${encodeURIComponent(this.scheduleInfo.slotName)}&location=${encodeURIComponent(this.scheduleInfo.location || '')}`
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

	.info-content {
		display: flex;
		flex-direction: column;
	}

	.info-row {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
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
		flex: 1;
	}

	.value.price {
		color: #FF6B6B;
		font-size: 32rpx;
		font-weight: 700;
	}

	.confirm-section {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		padding: 30rpx;
		background: #ffffff;
		box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.08);
	}

	.confirm-btn {
		width: 100%;
		height: 96rpx;
		background: linear-gradient(135deg, #7be6d8 0%, #4FD9C3 100%);
		border-radius: 50rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 600;
		border: none;
	}
</style>