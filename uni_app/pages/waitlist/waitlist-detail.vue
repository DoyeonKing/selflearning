<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">候补详情</text>
		</view>
		
		<view class="content">
			<!-- 状态卡片 -->
			<view class="card status-card" :class="waitlist.status">
				<text class="status-icon">{{ getStatusIcon(waitlist.status) }}</text>
				<text class="status-text">{{ getStatusText(waitlist.status) }}</text>
			</view>
			
			<!-- 候补信息 -->
			<view class="card detail-card">
				<view class="info-row">
					<text class="label">候补编号：</text>
					<text class="value">WB{{ waitlist.id.toString().padStart(6, '0') }}</text>
				</view>
				<view class="info-row">
					<text class="label">科室：</text>
					<text class="value">{{ waitlist.departmentName }}</text>
				</view>
				<view class="info-row">
					<text class="label">医生：</text>
					<text class="value">{{ waitlist.doctorName }} ({{ waitlist.doctorTitle }})</text>
				</view>
				<view class="info-row">
					<text class="label">就诊时间：</text>
					<text class="value">{{ formatDateTime(waitlist.scheduleTime) }}</text>
				</view>
				<view class="info-row">
					<text class="label">就诊时段：</text>
					<text class="value">{{ waitlist.slotName }}</text>
				</view>
				<view class="info-row">
					<text class="label">诊室：</text>
					<text class="value">{{ waitlist.location }}</text>
				</view>
				<view class="info-row">
					<text class="label">挂号费用：</text>
					<text class="value fee-value">¥{{ waitlist.fee ? waitlist.fee.toFixed(2) : 'N/A' }}</text>
				</view>
				
				<!-- 排队位置 -->
				<view class="info-row" v-if="waitlist.status === 'waiting'">
					<text class="label">排队位置：</text>
					<text class="value queue-number">第{{ waitlist.queuePosition }}位</text>
				</view>
				
				<!-- 通知时间 -->
				<view class="info-row" v-if="waitlist.status === 'notified'">
					<text class="label">通知时间：</text>
					<text class="value">{{ formatDateTime(waitlist.notificationSentAt) }}</text>
				</view>
				
				<!-- 申请时间 -->
				<view class="info-row">
					<text class="label">申请时间：</text>
					<text class="value">{{ formatDateTime(waitlist.createdAt) }}</text>
				</view>
			</view>
			
			<!-- 倒计时提示 -->
			<view class="countdown-notice" v-if="waitlist.status === 'notified'">
				<view class="notice-header">
					<text class="notice-icon">⏰</text>
					<text class="notice-title">支付倒计时</text>
				</view>
				<view class="countdown-display" :class="{ 'urgent': remainingSeconds < 300 }">
					<text class="countdown-text">{{ formatCountdown(remainingSeconds) }}</text>
				</view>
				<text class="notice-desc">请在15分钟内完成支付，超时自动取消候补</text>
			</view>
			
			<!-- 温馨提示 -->
			<view class="tips-card" v-if="waitlist.status === 'waiting'">
				<text class="tips-title">💡 温馨提示</text>
				<text class="tips-content">候补成功后，系统将通过短信通知您，请保持手机畅通。收到通知后请在15分钟内完成支付，否则将自动取消。</text>
			</view>
			
			<view class="tips-card" v-if="waitlist.status === 'NOTIFIED' || waitlist.status === 'notified'">
				<text class="tips-title">🔔 重要提醒</text>
				<text class="tips-content">您已收到候补通知，请立即完成支付。支付成功后，候补将转为正式预约。</text>
			</view>
			
			<!-- 操作按钮 -->
			<view class="action-section">
				<button class="action-btn payment-btn" v-if="waitlist.status === 'NOTIFIED' || waitlist.status === 'notified'" @click="navigateToPayment">立即支付</button>
				<button class="action-btn cancel-btn" v-if="waitlist.status === 'WAITING' || waitlist.status === 'waiting'" @click="handleCancel">取消候补</button>
				<button class="action-btn disabled-btn" v-else-if="waitlist.status !== 'NOTIFIED' && waitlist.status !== 'notified' && waitlist.status !== 'WAITING' && waitlist.status !== 'waiting'" disabled>
					{{ getStatusText(waitlist.status) }}
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { getWaitlistDetail, cancelWaitlist } from '../../api/appointment.js'
	
	export default {
		data() {
			return {
				waitlistId: null,
				waitlist: {
					id: 0,
				status: 'waiting',
				departmentName: '',
				doctorName: '',
				doctorTitle: '',
				scheduleTime: '',
				slotName: '',
				location: '',
				fee: 0,
				queuePosition: 0,
				notificationSentAt: '',
				createdAt: ''
			},
				countdownTimer: null,
				remainingSeconds: 0,
				loading: false
			}
		},
		onLoad(options) {
			console.log('候补详情页加载，参数:', options)
			this.waitlistId = parseInt(options.waitlistId)
			console.log('候补ID:', this.waitlistId)
			this.loadWaitlistDetail()
		},
		onShow() {
			this.loadWaitlistDetail()
		},
		onUnload() {
			if (this.countdownTimer) {
				clearInterval(this.countdownTimer)
			}
		},
		methods: {
			async loadWaitlistDetail() {
				this.loading = true
				try {
					const response = await getWaitlistDetail(this.waitlistId)
					console.log('候补详情响应:', response)
					
					if (response && response.code === '200' && response.data) {
						this.waitlist = response.data
					
						// 计算倒计时（兼容大小写状态值）
						const status = this.waitlist.status || ''
						const isNotified = status === 'NOTIFIED' || status === 'notified'
						
						if (isNotified && this.waitlist.notificationSentAt) {
							const now = new Date()
							const notificationTime = new Date(this.waitlist.notificationSentAt)
							const elapsedSeconds = Math.floor((now - notificationTime) / 1000)
							this.remainingSeconds = Math.max(0, 15 * 60 - elapsedSeconds) // 15分钟 = 900秒
							
							// 如果倒计时结束，更新状态
							if (this.remainingSeconds === 0) {
								this.waitlist.status = 'expired'
							}
						} else {
							this.remainingSeconds = 0
						}
					
						// 启动倒计时
						this.startCountdown()
					} else {
						uni.showToast({
							title: response?.msg || '加载失败',
							icon: 'none'
						})
					}
				} catch (error) {
					console.error('加载候补详情失败:', error)
					uni.showToast({
						title: '加载失败，请重试',
						icon: 'none'
					})
				} finally {
					this.loading = false
				}
			},
			
			startCountdown() {
				if (this.countdownTimer) {
					clearInterval(this.countdownTimer)
				}
				
				// 兼容大小写状态值
				const status = this.waitlist.status || ''
				const isNotified = status === 'NOTIFIED' || status === 'notified'
				
				if (isNotified) {
					this.countdownTimer = setInterval(() => {
						if (this.remainingSeconds > 0) {
							this.remainingSeconds--
						} else {
							// 倒计时结束，重新加载数据
							clearInterval(this.countdownTimer)
							this.countdownTimer = null
							this.loadWaitlistDetail()
						}
					}, 1000)
				}
			},
			
			getStatusText(status) {
				const statusMap = {
					'WAITING': '候补中',
					'NOTIFIED': '待支付',
					'FULFILLED': '已转预约',
					'EXPIRED': '已过期',
					'REJECTED': '已拒绝',
					'CANCELLED': '已取消',
					// 兼容旧格式
					'waiting': '候补中',
					'notified': '待支付',
					'booked': '已转预约',
					'expired': '已过期',
					'cancelled': '已取消'
				}
				return statusMap[status] || '未知'
			},
			
			getStatusIcon(status) {
				const iconMap = {
					'WAITING': '⏳',
					'NOTIFIED': '🔔',
					'FULFILLED': '✅',
					'EXPIRED': '❌',
					'CANCELLED': '🚫',
					// 兼容旧格式
					'waiting': '⏳',
					'notified': '🔔',
					'booked': '✅',
					'expired': '❌',
					'cancelled': '🚫'
				}
				return iconMap[status] || '❓'
			},
			
			formatDateTime(dateString) {
				if (!dateString) return ''
				try {
					const date = new Date(dateString)
					const month = date.getMonth() + 1
					const day = date.getDate()
					const hours = date.getHours().toString().padStart(2, '0')
					const minutes = date.getMinutes().toString().padStart(2, '0')
					return month + '月' + day + '日 ' + hours + ':' + minutes
				} catch (e) {
					return dateString
				}
			},
			
			formatCountdown(seconds) {
				const mins = Math.floor(seconds / 60)
				const secs = seconds % 60
				return `${mins}分${secs}秒`
			},
			
			navigateToPayment() {
				uni.navigateTo({
					url: `/pages/payment/payment?waitlistId=${this.waitlist.id}&fee=${this.waitlist.fee}&departmentName=${encodeURIComponent(this.waitlist.departmentName)}&doctorName=${encodeURIComponent(this.waitlist.doctorName)}&slotName=${encodeURIComponent(this.waitlist.slotName)}`
				})
			},
			
			async handleCancel() {
				uni.showModal({
					title: '取消候补',
					content: '确定要取消候补吗？',
					success: async (res) => {
						if (res.confirm) {
							try {
								const response = await cancelWaitlist(this.waitlistId)
								if (response && response.code === '200' && response.data) {
							uni.showToast({
								title: '取消成功',
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
								console.error('取消候补失败:', error)
								uni.showToast({
									title: '取消失败，请重试',
									icon: 'none'
								})
							}
						}
					}
				})
			}
		}
	}
</script>

<style lang="scss">
	/* 定义颜色变量 */
	$color-primary: #4FD9C3;
	
	.container {
		min-height: 100vh;
		background-color: #f7fafc;
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
		padding: 20rpx;
	}

	.card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.status-card {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 60rpx 30rpx;
		border: 2rpx solid transparent;
		
		&.waiting {
			background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, #ffffff 100%);
			border-color: rgba(24, 144, 255, 0.3);
		}
		
		&.notified {
			background: linear-gradient(135deg, rgba(255, 165, 0, 0.1) 0%, #ffffff 100%);
			border-color: rgba(255, 165, 0, 0.3);
		}
		
		&.booked {
			background: linear-gradient(135deg, rgba(82, 196, 26, 0.1) 0%, #ffffff 100%);
			border-color: rgba(82, 196, 26, 0.3);
		}
		
		&.expired {
			background: linear-gradient(135deg, rgba(255, 77, 79, 0.1) 0%, #ffffff 100%);
			border-color: rgba(255, 77, 79, 0.3);
		}
		
		&.cancelled {
			background: #F0F0F0;
			border-color: #A0AEC0;
		}
	}

	.status-icon {
		font-size: 80rpx;
		margin-right: 20rpx;
	}

	.status-text {
		font-size: 36rpx;
		font-weight: 700;
		color: #1A202C;
	}

	.detail-card {
		line-height: 1.8;
	}

	.info-row {
		display: flex;
		align-items: flex-start;
		margin-bottom: 20rpx;
		
		&:last-child {
			margin-bottom: 0;
		}
	}

	.label {
		font-size: 26rpx;
		color: #718096;
		margin-right: 12rpx;
		white-space: nowrap;
		width: 160rpx;
	}

	.value {
		font-size: 26rpx;
		color: #1A202C;
		flex: 1;
		font-weight: 500;
	}

	.queue-number {
		color: #4FD9C3;
		font-weight: 700;
		font-size: 32rpx;
	}

	.fee-value {
		color: #FF6B6B;
		font-weight: 700;
		font-size: 32rpx;
	}

	.countdown-notice {
		background: linear-gradient(135deg, rgba(255, 165, 0, 0.1) 0%, rgba(255, 165, 0, 0.05) 100%);
		border: 2rpx solid rgba(255, 165, 0, 0.3);
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
	}

	.notice-header {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
	}

	.notice-icon {
		font-size: 40rpx;
		margin-right: 12rpx;
	}

	.notice-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #FFA500;
	}

	.countdown-display {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 30rpx;
		background: #ffffff;
		border-radius: 16rpx;
		margin-bottom: 20rpx;
		border: 2rpx solid #FFE7BA;
		
		&.urgent {
			border-color: #FFCCC7;
			background: #FFF2F0;
			
			.countdown-text {
				color: #FF4D4F;
				animation: blink 1s infinite;
			}
		}
	}

	.countdown-text {
		font-size: 64rpx;
		font-weight: 700;
		color: #FFA500;
		font-family: 'Courier New', monospace;
	}

	@keyframes blink {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.5; }
	}

	.notice-desc {
		display: block;
		font-size: 24rpx;
		color: #FF8C00;
		text-align: center;
		font-weight: 600;
	}

	.tips-card {
		background: #F0FDF4;
		border: 1rpx solid #BBF7D0;
		border-radius: 16rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
	}

	.tips-title {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #16A34A;
		margin-bottom: 16rpx;
	}

	.tips-content {
		display: block;
		font-size: 26rpx;
		color: #166534;
		line-height: 1.6;
	}

	.action-section {
		margin-top: 40rpx;
		padding: 20rpx 0;
	}

	.action-btn {
		width: 100%;
		padding: 24rpx;
		border-radius: 50rpx;
		font-size: 32rpx;
		font-weight: 700;
		border: none;
		
		&.payment-btn {
			background: linear-gradient(135deg, #FFA500 0%, #FF8C00 100%);
			color: #ffffff;
		}
		
		&.cancel-btn {
			background: #F0F0F0;
			color: #718096;
		}
		
		&.disabled-btn {
			background: #E2E8F0;
			color: #A0AEC0;
		}
	}
</style>