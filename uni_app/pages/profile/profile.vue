<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">个人中心</text>
		</view>
		
		<view class="content">
			<!-- 用户信息卡片 -->
			<view class="user-card">
				<view class="user-info">
					<view class="avatar-wrapper">
						<text class="avatar">👤</text>
					</view>
					<view class="user-details">
						<text class="user-name">{{ patientInfo.name || '患者' }}</text>
						<view class="user-id-wrapper">
							<text class="user-id">{{ displayIdentifier }}</text>
							<text class="eye-icon" @click.stop="toggleIdentifierMask">👁️</text>
						</view>
					</view>
				</view>
			</view>
			
		<!-- 待就诊卡片 -->
		<view class="upcoming-card" v-if="upcomingAppointment" @click="navigateToAppointments">
			<view class="upcoming-icon">🩺</view>
			<view class="upcoming-content">
				<text class="upcoming-title">待就诊</text>
				<text class="upcoming-info">{{ formatAppointmentTime(upcomingAppointment.scheduleTime) }} · {{ upcomingAppointment.departmentName }}</text>
			</view>
			<text class="upcoming-arrow">></text>
		</view>

		<!-- 候补提醒卡片 -->
		<view class="waitlist-card" v-if="waitlistCount > 0" @click="navigateToWaitlist">
			<view class="waitlist-icon">⏳</view>
			<view class="waitlist-content">
				<text class="waitlist-title">我的候补</text>
				<text class="waitlist-info">您有 {{ waitlistCount }} 个候补记录</text>
			</view>
			<text class="waitlist-arrow">></text>
		</view>

		<!-- 功能列表 -->
		<view class="menu-list">
			<view class="menu-item" @click="navigateToMyAppointments">
				<text class="menu-icon">📅</text>
				<text class="menu-text">我的预约</text>
				<text class="menu-arrow">></text>
			</view>
			<view class="menu-item" @click="navigateToWaitlistList">
				<text class="menu-icon">⏳</text>
				<text class="menu-text">我的候补</text>
				<text class="menu-arrow">></text>
			</view>
			<view class="menu-item" @click="navigateToEditProfile">
				<text class="menu-icon">📝</text>
				<text class="menu-text">编辑资料</text>
				<text class="menu-arrow">></text>
			</view>
			<view class="menu-item" @click="navigateToSettings">
				<text class="menu-icon">⚙️</text>
				<text class="menu-text">设置</text>
				<text class="menu-arrow">></text>
			</view>
			<view class="menu-item" @click="showAbout">
				<text class="menu-icon">ℹ️</text>
				<text class="menu-text">关于我们</text>
				<text class="menu-arrow">></text>
			</view>
		</view>

		<!-- 退出登录 -->
		<view class="logout-btn" @click="handleLogout">
			<text class="logout-text">退出登录</text>
		</view>
	</view>
</view>
</template>

<script>
	import { getPatientWaitlist } from '../../api/appointment.js'
	
	export default {
		data() {
			return {
				patientInfo: {
					name: '张三',
					identifier: '2021001001'
				},
				upcomingAppointment: null,
				waitlistCount: 0,
				identifierMasked: true
			}
		},
		computed: {
			displayIdentifier() {
				if (!this.patientInfo.identifier) return '学号/工号'
				if (this.identifierMasked && this.patientInfo.identifier.length >= 8) {
					const len = this.patientInfo.identifier.length
					return this.patientInfo.identifier.substring(0, 4) + '****' + this.patientInfo.identifier.substring(len - 2)
				}
				return this.patientInfo.identifier
			}
		},
		onLoad() {
			this.loadPatientInfo()
			this.loadUpcomingCount()
			this.loadWaitlistCount()
		},
		onShow() {
			// 页面显示时先重置候补数量，避免显示旧数据
			this.$set(this, 'waitlistCount', 0)
			// 页面显示时刷新数据
			this.loadPatientInfo()
			this.loadUpcomingCount()
			this.loadWaitlistCount()
		},
		methods: {
			loadPatientInfo() {
				const patientInfo = uni.getStorageSync('patientInfo')
				if (patientInfo) {
					this.patientInfo = patientInfo
				} else {
					// 如果没有登录信息，使用模拟数据
					this.patientInfo = {
						name: '张三',
						identifier: '2021001001'
					}
				}
			},
			navigateToEditProfile() {
				uni.showToast({
					title: '编辑资料功能开发中',
					icon: 'none',
					duration: 2000
				})
			},
			navigateToSettings() {
				uni.showToast({
					title: '设置功能开发中',
					icon: 'none',
					duration: 2000
				})
			},
			showAbout() {
				uni.showModal({
					title: '关于我们',
					content: 'XX大学校医院\n地址：XX市XX区XX路XX号\n总机：0512-66666666\n急诊：0512-66666120\n\n门诊时间\n工作日 8:00-11:30 / 14:00-17:30\n周末仅上午',
					showCancel: false,
					confirmText: '知道了'
				})
			},
			loadUpcomingCount() {
				const upcomingAppointment = uni.getStorageSync('upcomingAppointment')
				if (upcomingAppointment) {
					this.upcomingAppointment = upcomingAppointment
				} else {
					this.upcomingAppointment = null
				}
			},
			formatAppointmentTime(timeString) {
				if (!timeString) return ''
				const date = new Date(timeString)
				const month = date.getMonth() + 1
				const day = date.getDate()
				const hours = date.getHours().toString().padStart(2, '0')
				const minutes = date.getMinutes().toString().padStart(2, '0')
				return month + '月' + day + '日 ' + hours + ':' + minutes
			},
			navigateToAppointments() {
				uni.switchTab({
					url: '/pages/appointments/appointments'
				})
			},
			async loadWaitlistCount() {
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (!patientInfo || !patientInfo.id) {
						console.log('个人中心 - 未登录，候补数量设为0')
						this.$set(this, 'waitlistCount', 0)
						return
					}
					
					// 先重置为0，避免使用旧数据
					this.$set(this, 'waitlistCount', 0)
					
					const waitlistResponse = await getPatientWaitlist(patientInfo.id)
					console.log('个人中心 - 候补列表响应:', waitlistResponse)
					
					if (waitlistResponse && waitlistResponse.code === '200' && waitlistResponse.data) {
						const waitlistData = waitlistResponse.data
						console.log('个人中心 - 候补数据:', waitlistData)
						
						// 确保是数组
						const waitlistArray = Array.isArray(waitlistData) ? waitlistData : []
						
						// 过滤状态：只统计 waiting（等待中）和 notified（已通知）的候补
						const validCount = waitlistArray.filter(w => {
							const status = (w.status || '').toLowerCase()
							return status === 'waiting' || status === 'notified'
						}).length
						
						this.$set(this, 'waitlistCount', validCount)
						console.log('个人中心 - 候补数量统计:', {
							总数: waitlistArray.length,
							有效候补: validCount,
							更新后的waitlistCount: this.waitlistCount
						})
						
						// 强制更新视图
						this.$nextTick(() => {
							this.$forceUpdate()
							console.log('个人中心 - $nextTick后waitlistCount:', this.waitlistCount)
						})
					} else if (Array.isArray(waitlistResponse)) {
						// 如果直接返回数组
						const validCount = waitlistResponse.filter(w => {
							const status = (w.status || '').toLowerCase()
							return status === 'waiting' || status === 'notified'
						}).length
						this.$set(this, 'waitlistCount', validCount)
						console.log('个人中心 - 候补数量统计（直接数组）:', {
							总数: waitlistResponse.length,
							有效候补: validCount
						})
						this.$nextTick(() => {
							this.$forceUpdate()
						})
					} else {
						console.log('个人中心 - 候补数据格式异常，设置为0')
						this.$set(this, 'waitlistCount', 0)
						this.$nextTick(() => {
							this.$forceUpdate()
						})
					}
				} catch (error) {
					console.error('个人中心 - 加载候补数量失败:', error)
					this.$set(this, 'waitlistCount', 0)
					this.$nextTick(() => {
						this.$forceUpdate()
					})
				}
			},
			navigateToWaitlist() {
				uni.navigateTo({
					url: '/pages/waitlist/waitlist'
				})
			},
			navigateToWaitlistList() {
				uni.navigateTo({
					url: '/pages/waitlist/waitlist'
				})
			},
			navigateToMyAppointments() {
				uni.switchTab({
					url: '/pages/appointments/appointments'
				})
			},
			toggleIdentifierMask() {
				this.identifierMasked = !this.identifierMasked
			},
			handleLogout() {
				uni.showModal({
					title: '提示',
					content: '确定要退出登录吗？',
					success: (res) => {
						if (res.confirm) {
							uni.removeStorageSync('patientToken')
							uni.removeStorageSync('patientInfo')
							uni.reLaunch({
								url: '/pages/login/patient-login'
							})
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
		padding: 30rpx;
	}

	.user-card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 40rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
	}

	.user-info {
		display: flex;
		align-items: center;
	}

	.avatar-wrapper {
		width: 120rpx;
		height: 120rpx;
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 30rpx;
	}

	.avatar {
		font-size: 60rpx;
	}

	.user-details {
		flex: 1;
	}

	.user-name {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #1A202C;
		margin-bottom: 12rpx;
	}

	.user-id-wrapper {
		display: flex;
		align-items: center;
		gap: 8rpx;
	}
	
	.user-id {
		font-size: 26rpx;
		color: #718096;
	}
	
	.eye-icon {
		font-size: 20rpx;
		opacity: 0.7;
	}
	
	.upcoming-card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 24rpx 30rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
		display: flex;
		align-items: center;
		transition: all 0.3s ease;
	}
	
	.upcoming-card:active {
		transform: translateY(-2rpx);
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.12);
	}
	
	.upcoming-icon {
		font-size: 40rpx;
		margin-right: 20rpx;
	}
	
	.upcoming-content {
		flex: 1;
	}
	
	.upcoming-title {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1A202C;
		margin-bottom: 8rpx;
	}
	
	.upcoming-info {
		display: block;
		font-size: 24rpx;
		color: #718096;
	}
	
	.upcoming-arrow {
		font-size: 36rpx;
		color: #A0AEC0;
		font-weight: bold;
	}

	/* 候补提醒卡片样式 */
	.waitlist-card {
		background: linear-gradient(135deg, rgba(255, 165, 0, 0.15) 0%, rgba(255, 165, 0, 0.05) 100%);
		border: 2rpx solid rgba(255, 165, 0, 0.3);
		border-radius: 20rpx;
		padding: 24rpx 30rpx;
		margin-bottom: 30rpx;
		display: flex;
		align-items: center;
		box-shadow: 0 4rpx 20rpx rgba(255, 165, 0, 0.2);
		transition: all 0.3s ease;
	}

	.waitlist-card:active {
		transform: translateY(-2rpx);
		box-shadow: 0 6rpx 24rpx rgba(255, 165, 0, 0.3);
	}

	.waitlist-icon {
		font-size: 40rpx;
		margin-right: 20rpx;
	}

	.waitlist-content {
		flex: 1;
	}

	.waitlist-title {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1A202C;
		margin-bottom: 8rpx;
	}

	.waitlist-info {
		display: block;
		font-size: 24rpx;
		color: #718096;
	}

	.waitlist-arrow {
		font-size: 36rpx;
		color: #A0AEC0;
		font-weight: bold;
	}

	.menu-list {
		background: #ffffff;
		border-radius: 20rpx;
		margin-bottom: 30rpx;
		overflow: hidden;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
	}

	.menu-item {
		display: flex;
		align-items: center;
		padding: 32rpx 30rpx;
		border-bottom: 1rpx solid #F0F0F0;
		transition: all 0.3s ease;
	}

	.menu-item:last-child {
		border-bottom: none;
	}

	.menu-item:active {
		background: #F8F9FA;
	}

	.menu-icon {
		font-size: 40rpx;
		margin-right: 24rpx;
	}

	.menu-text {
		flex: 1;
		font-size: 30rpx;
		color: #1A202C;
	}

	.menu-arrow {
		font-size: 32rpx;
		color: #CBD5E0;
	}

	.logout-btn {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 32rpx;
		text-align: center;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
	}

	.logout-text {
		font-size: 32rpx;
		color: #FF6B6B;
		font-weight: 600;
	}
</style>