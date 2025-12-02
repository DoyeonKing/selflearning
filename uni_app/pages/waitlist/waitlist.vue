<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">我的候补</text>
		</view>
		
		<view class="content">
			<!-- 候补列表 -->
			<view class="waitlist-list" v-if="waitlistList.length > 0">
				<view 
					class="waitlist-item" 
					v-for="waitlist in waitlistList" 
					:key="waitlist.id"
					:class="waitlist.status"
					@click="navigateToDetail(waitlist.id)"
				>
				<view class="waitlist-header">
					<view class="department-info">
						<text class="department-name">{{ waitlist.departmentName }}</text>
						<text class="doctor-name">{{ waitlist.doctorName }}</text>
					</view>
					<view class="status-badge-wrapper">
						<view class="status-badge" :class="waitlist.status">
							<text class="status-text">{{ getStatusText(waitlist.status) }}</text>
						</view>
					</view>
				</view>
				
				<view class="waitlist-content">
					<view class="info-row">
						<text class="info-label">🕐 就诊时间：</text>
						<text class="info-value">{{ formatDateTime(waitlist.scheduleTime) }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">📍 诊室：</text>
						<text class="info-value">{{ waitlist.location }}</text>
					</view>
					
					<!-- 排队位置 -->
					<view class="info-row" v-if="waitlist.status === 'waiting' || waitlist.status === 'WAITING'">
						<text class="info-label">👥 排队位置：</text>
						<text class="info-value queue-position">第{{ waitlist.queuePosition }}位</text>
					</view>
					
					<!-- 通知时间和倒计时 -->
					<view class="info-row" v-if="waitlist.status === 'notified' || waitlist.status === 'NOTIFIED'">
						<text class="info-label">🔔 通知时间：</text>
						<text class="info-value">{{ formatDateTime(waitlist.notificationSentAt) }}</text>
					</view>
					<view class="countdown-wrapper" v-if="waitlist.status === 'notified' || waitlist.status === 'NOTIFIED'">
						<text class="countdown-label">剩余支付时间：</text>
						<text class="countdown-value" :class="{ 'urgent': waitlist.remainingSeconds < 300 }">
							{{ formatCountdown(waitlist.remainingSeconds) }}
						</text>
					</view>
					
					<!-- 候补时间 -->
					<view class="info-row">
						<text class="info-label">📅 申请时间：</text>
						<text class="info-value">{{ formatDateTime(waitlist.createdAt) }}</text>
					</view>
				</view>
				
				<!-- 操作按钮 -->
				<view class="waitlist-actions" v-if="waitlist.status === 'notified' || waitlist.status === 'NOTIFIED'">
					<view class="action-btn payment-btn" @click.stop="navigateToPayment(waitlist)">
						<text class="btn-text">立即支付</text>
					</view>
				</view>
				<view class="waitlist-actions" v-if="waitlist.status === 'waiting' || waitlist.status === 'WAITING'">
					<view class="action-btn cancel-btn" @click.stop="handleCancel(waitlist.id)">
						<text class="btn-text">取消候补</text>
					</view>
				</view>
				
				<!-- 15分钟倒计时提示 -->
				<view class="urgent-notice" v-if="waitlist.status === 'notified' || waitlist.status === 'NOTIFIED'">
					<text class="notice-text">⏰ 请在15分钟内完成支付，超时自动取消</text>
				</view>
			</view>
		</view>
		
		<!-- 空状态 -->
		<view class="empty-state" v-else>
			<text class="empty-icon">⏳</text>
			<text class="empty-text">暂无候补记录</text>
			<text class="empty-desc">当号源已满时，您可以申请候补排队</text>
			<view class="empty-btn" @click="navigateToDepartments">
				<text class="empty-btn-text">去挂号</text>
			</view>
		</view>
	</view>
</view>
</template>

<script>
	import { getPatientWaitlist, cancelWaitlist } from '../../api/appointment.js'
	
	export default {
		data() {
			return {
				waitlistList: [],
				countdownTimer: null,
				refreshTimer: null, // 定期刷新定时器
				loading: false
			}
		},
		onLoad() {
			this.loadWaitlist()
		},
		onShow() {
			// 页面显示时刷新数据
			this.loadWaitlist()
			// 启动定期刷新（每30秒）
			this.startAutoRefresh()
		},
		onHide() {
			// 页面隐藏时停止定期刷新
			this.stopAutoRefresh()
		},
		onUnload() {
			// 页面卸载时清除所有定时器
			if (this.countdownTimer) {
				clearInterval(this.countdownTimer)
			}
			this.stopAutoRefresh()
		},
		onPullDownRefresh() {
			// 下拉刷新
			this.loadWaitlist().finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		methods: {
			// 加载候补列表
			async loadWaitlist() {
				this.loading = true
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (!patientInfo || !patientInfo.id) {
						uni.showToast({
							title: '请先登录',
							icon: 'none'
						})
						this.loading = false
						return
					}
					
					const response = await getPatientWaitlist(patientInfo.id)
					console.log('候补列表响应:', response)
					
					if (response && response.code === '200' && response.data) {
						console.log('候补列表数据:', response.data)
						console.log('候补列表数据长度:', response.data.length)
						
						// 计算倒计时
						const now = new Date()
						this.waitlistList = response.data.map(item => {
							console.log('处理候补项:', {
								id: item.id,
								status: item.status,
								departmentName: item.departmentName,
								doctorName: item.doctorName
							})
							
							// 兼容大小写状态值
							const status = item.status || ''
							const isNotified = status === 'NOTIFIED' || status === 'notified'
							
							if (isNotified && item.notificationSentAt) {
								const notificationTime = new Date(item.notificationSentAt)
								const elapsedSeconds = Math.floor((now - notificationTime) / 1000)
								const remainingSeconds = Math.max(0, 15 * 60 - elapsedSeconds) // 15分钟 = 900秒
								
								// 如果倒计时结束，更新状态
								if (remainingSeconds === 0) {
									item.status = 'expired'
								}
								
								return {
									...item,
									remainingSeconds
								}
							}
							return item
						})
						
						console.log('处理后的候补列表:', this.waitlistList)
						console.log('候补列表长度:', this.waitlistList.length)
						
						// 启动倒计时
						this.startCountdown()
					} else {
						uni.showToast({
							title: response?.msg || '加载失败',
							icon: 'none'
						})
						this.waitlistList = []
					}
				} catch (error) {
					console.error('加载候补列表失败:', error)
					uni.showToast({
						title: '加载失败，请重试',
						icon: 'none'
					})
					this.waitlistList = []
				} finally {
					this.loading = false
				}
			},
			
			// 启动倒计时
			startCountdown() {
				if (this.countdownTimer) {
					clearInterval(this.countdownTimer)
				}
				
				this.countdownTimer = setInterval(() => {
					this.waitlistList = this.waitlistList.map(item => {
						// 兼容大小写状态值
						const status = item.status || ''
						const isNotified = status === 'NOTIFIED' || status === 'notified'
						
						if (isNotified && item.remainingSeconds > 0) {
							return {
								...item,
								remainingSeconds: item.remainingSeconds - 1
							}
						}
						return item
					})
					
					// 检查是否有倒计时结束的候补
					const hasExpired = this.waitlistList.some(item => {
						const status = item.status || ''
						const isNotified = status === 'NOTIFIED' || status === 'notified'
						return isNotified && item.remainingSeconds === 0
					})
					if (hasExpired) {
						this.loadWaitlist() // 重新加载数据
					}
				}, 1000)
			},
			
			// 启动定期刷新
			startAutoRefresh() {
				this.stopAutoRefresh() // 先清除旧的定时器
				// 每30秒刷新一次数据
				this.refreshTimer = setInterval(() => {
					this.loadWaitlist()
				}, 30000)
			},
			
			// 停止定期刷新
			stopAutoRefresh() {
				if (this.refreshTimer) {
					clearInterval(this.refreshTimer)
					this.refreshTimer = null
				}
			},
			
			// 获取状态文本
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
			
			// 获取状态样式类
			getStatusClass(status) {
				const classMap = {
					'waiting': 'waiting',
					'notified': 'notified',
					'booked': 'booked',
					'expired': 'expired',
					'cancelled': 'cancelled'
				}
				return classMap[status] || ''
			},
			
			// 获取状态徽章样式类
			getStatusBadgeClass(status) {
				const classMap = {
					'waiting': 'waiting-badge',
					'notified': 'notified-badge',
					'booked': 'booked-badge',
					'expired': 'expired-badge',
					'cancelled': 'cancelled-badge'
				}
				return classMap[status] || ''
			},
			
			// 格式化日期时间
			formatDateTime(dateString) {
				if (!dateString) return ''
				const date = new Date(dateString)
				const month = date.getMonth() + 1
				const day = date.getDate()
				const hours = date.getHours().toString().padStart(2, '0')
				const minutes = date.getMinutes().toString().padStart(2, '0')
				return month + '月' + day + '日 ' + hours + ':' + minutes
			},
			
			// 格式化倒计时
			formatCountdown(seconds) {
				const mins = Math.floor(seconds / 60)
				const secs = seconds % 60
				return `${mins}分${secs}秒`
			},
			
			// 导航到详情
			navigateToDetail(waitlistId) {
				uni.navigateTo({
					url: `/pages/waitlist/waitlist-detail?waitlistId=${waitlistId}`
				})
			},
			
			// 导航到支付
			navigateToPayment(waitlist) {
				uni.navigateTo({
					url: `/pages/payment/payment?waitlistId=${waitlist.id}&fee=${waitlist.fee}&departmentName=${encodeURIComponent(waitlist.departmentName)}&doctorName=${encodeURIComponent(waitlist.doctorName)}&slotName=${encodeURIComponent(waitlist.slotName || '')}`
				})
			},
			
			// 导航到科室列表
			navigateToDepartments() {
				uni.navigateTo({
					url: '/pages/departments/departments'
				})
			},
			
			// 取消候补
			async handleCancel(waitlistId) {
				uni.showModal({
					title: '取消候补',
					content: '确定要取消候补吗？',
					success: async (res) => {
						if (res.confirm) {
							try {
								uni.showLoading({ title: '取消中...' })
								const response = await cancelWaitlist(waitlistId)
								console.log('取消候补响应:', response)
								
								if (response && response.code === '200') {
							uni.showToast({
								title: '取消成功',
								icon: 'success'
							})
							this.loadWaitlist()
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
	.container {
		min-height: 100vh;
		background-color: #f7fafc;
	}

	.page-header {
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
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

	.waitlist-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}

	.waitlist-item {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 30rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
		transition: all 0.3s ease;
		position: relative;
		overflow: hidden;
		
		&.notified {
			border: 2rpx solid #FFA500;
			background: linear-gradient(135deg, rgba(255, 165, 0, 0.05) 0%, #ffffff 100%);
		}
		
		&.booked {
			opacity: 0.8;
		}
		
		&.expired {
			opacity: 0.7;
		}
		
		&.cancelled {
			opacity: 0.6;
		}
	}

	.waitlist-header {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-bottom: 20rpx;
		padding-bottom: 20rpx;
		border-bottom: 1rpx solid #F0F0F0;
	}

	.department-info {
		flex: 1;
	}

	.department-name {
		display: block;
		font-size: 32rpx;
		font-weight: 700;
		color: #1A202C;
		margin-bottom: 8rpx;
	}

	.doctor-name {
		display: block;
		font-size: 26rpx;
		color: #718096;
	}

	.status-badge-wrapper {
		margin-left: 20rpx;
	}

	.status-badge {
		padding: 8rpx 16rpx;
		border-radius: 20rpx;
		font-size: 24rpx;
		font-weight: 600;
		
		&.waiting-badge {
			background: #E6F7FF;
			color: #1890FF;
		}
		
		&.notified-badge {
			background: #FFF7E6;
			color: #FFA500;
		}
		
		&.booked-badge {
			background: #F6FFED;
			color: #52C41A;
		}
		
		&.expired-badge {
			background: #FFF2F0;
			color: #FF4D4F;
		}
		
		&.cancelled-badge {
			background: #F0F0F0;
			color: #A0AEC0;
		}
	}

	.status-text {
		font-size: 24rpx;
		font-weight: 600;
	}

	.waitlist-content {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
		margin-bottom: 20rpx;
	}

	.info-row {
		display: flex;
		align-items: center;
	}

	.info-label {
		font-size: 26rpx;
		color: #718096;
		margin-right: 12rpx;
		white-space: nowrap;
	}

	.info-value {
		font-size: 26rpx;
		color: #1A202C;
		flex: 1;
	}

	.queue-position {
		color: $color-primary;
		font-weight: 600;
		font-size: 28rpx;
	}

	.countdown-wrapper {
		display: flex;
		align-items: center;
		margin-top: 8rpx;
		padding: 16rpx;
		background: #FFF7E6;
		border-radius: 12rpx;
		border: 1rpx solid #FFE7BA;
	}

	.countdown-label {
		font-size: 24rpx;
		color: #FFA500;
		margin-right: 12rpx;
		font-weight: 600;
	}

	.countdown-value {
		font-size: 32rpx;
		font-weight: 700;
		color: #FFA500;
		
		&.urgent {
			color: #FF4D4F;
			animation: blink 1s infinite;
		}
	}

	@keyframes blink {
		0%, 100% { 
			opacity: 1; 
			transform: scale(1);
		}
		50% { 
			opacity: 0.7; 
			transform: scale(1.05);
		}
	}

	.waitlist-actions {
		display: flex;
		justify-content: flex-end;
		margin-top: 20rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #F0F0F0;
	}

	.action-btn {
		padding: 16rpx 48rpx;
		border-radius: 50rpx;
		font-size: 28rpx;
		font-weight: 600;
		
		&.payment-btn {
			background: linear-gradient(135deg, #FFA500 0%, #FF8C00 100%);
			color: #ffffff;
		}
		
		&.cancel-btn {
			background: #F0F0F0;
			color: #718096;
		}
	}

	.btn-text {
		font-size: 28rpx;
		font-weight: 600;
	}

	.urgent-notice {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 16rpx;
		margin-top: 20rpx;
		background: #FFF2F0;
		border-radius: 12rpx;
		border: 1rpx solid #FFCCC7;
	}

	.notice-text {
		font-size: 24rpx;
		color: #FF4D4F;
		font-weight: 600;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 40rpx;
		text-align: center;
	}

	.empty-icon {
		font-size: 120rpx;
		margin-bottom: 30rpx;
		opacity: 0.5;
	}

	.empty-text {
		font-size: 32rpx;
		color: #718096;
		margin-bottom: 16rpx;
		font-weight: 600;
	}

	.empty-desc {
		font-size: 26rpx;
		color: #A0AEC0;
		margin-bottom: 40rpx;
	}

	.empty-btn {
		padding: 20rpx 60rpx;
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 50rpx;
	}

	.empty-btn-text {
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
	}
</style>
