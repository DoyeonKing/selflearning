<template>
	<view class="container">
		<!-- 对话列表 -->
		<view class="conversation-list" v-if="conversationList.length > 0">
			<view 
				class="conversation-item" 
				v-for="(conversation, index) in conversationList" 
				:key="conversation.senderId"
				@click="handleConversationClick(conversation)"
			>
				<view class="conversation-avatar">
					<text class="avatar-icon">{{ getMessageIcon(conversation.icon) }}</text>
					<view class="unread-badge" v-if="conversation.unreadCount > 0">
						<text class="badge-text">{{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}</text>
					</view>
				</view>
				<view class="conversation-content">
					<view class="conversation-header">
						<text class="sender-name">{{ conversation.senderName }}</text>
						<text class="conversation-time">{{ formatTime(conversation.latestMessage.createTime) }}</text>
					</view>
					<view class="conversation-footer">
						<text class="latest-message">{{ conversation.latestMessage.content }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 空状态 -->
		<view class="empty-state" v-if="!loading && conversationList.length === 0">
			<text class="empty-icon">💊</text>
			<text class="empty-text">暂无消息</text>
			<view class="empty-btn" @click="navigateToDepartments">
				<text class="empty-btn-text">去挂号</text>
			</view>
		</view>

		<!-- 加载状态 -->
		<view class="loading" v-if="loading">
			<text class="loading-text">加载中...</text>
		</view>
	</view>
</template>

<script>
	import { getUserNotifications, markAsRead, markAllAsRead } from '../../api/notification.js'

	export default {
		data() {
			return {
				loading: false,
				isIOS: false,
				buttonWidth: 0, // 按钮宽度（px）
				notificationList: [],
				conversationList: [],
				swipeOffset: {},
				touchStartX: 0,
				touchStartIndex: -1
			}
		},
		onLoad() {
			// 检测iOS平台
			const systemInfo = uni.getSystemInfoSync()
			this.isIOS = systemInfo.platform === 'ios'
			// 计算按钮宽度（rpx转px）
			this.buttonWidth = uni.upx2px(130)
			// 页面加载时直接显示已有数据，然后尝试加载新数据
			this.loadMessages()
		},
		onShow() {
			// 页面显示时刷新消息列表
			this.loadMessages()
			// 重置滑动状态
			this.swipeOffset = {}
			this.touchStartIndex = -1
		},
		onPullDownRefresh() {
			this.loadMessages()
			uni.stopPullDownRefresh()
		},
		methods: {
		// 加载通知列表 - 按类型分组
		async loadMessages() {
			this.loading = true
			try {
				const patientInfo = uni.getStorageSync('patientInfo')
				if (!patientInfo || !patientInfo.id) {
					console.warn('未登录，无法加载通知')
					this.conversationList = []
					this.loading = false
					return
				}
				
				// 调用通知API
				const notifications = await getUserNotifications(patientInfo.id, 'patient')
				console.log('获取到的通知列表:', notifications)
				
				// 处理响应格式（可能是数组或包装格式）
				let notificationList = []
				if (Array.isArray(notifications)) {
					notificationList = notifications
				} else if (notifications && notifications.data && Array.isArray(notifications.data)) {
					notificationList = notifications.data
				}
				
				this.notificationList = notificationList
				
				// 保存到全局存储
				uni.setStorageSync('allNotifications', notificationList)
				
				// 按通知类型分组
				const conversationMap = {}
				notificationList.forEach(notification => {
					const type = notification.type || 'system_notice'
					const typeName = this.getTypeName(type)
					
					if (!conversationMap[type]) {
						conversationMap[type] = {
							senderId: type,
							senderName: typeName,
							icon: type,
							latestMessage: {
								content: notification.content,
								createTime: notification.sentAt
							},
							unreadCount: 0,
							messages: []
						}
					}
					conversationMap[type].messages.push(notification)
					if (notification.status === 'unread') {
						conversationMap[type].unreadCount++
					}
					// 更新最新消息
					const sentAt = new Date(notification.sentAt)
					const latestSentAt = new Date(conversationMap[type].latestMessage.createTime)
					if (sentAt > latestSentAt) {
						conversationMap[type].latestMessage = {
							content: notification.content,
							createTime: notification.sentAt
						}
					}
				})
				
				// 转换为数组并排序（最新消息在前）
				this.conversationList = Object.values(conversationMap).sort((a, b) => {
					return new Date(b.latestMessage.createTime) - new Date(a.latestMessage.createTime)
				})
			} catch (error) {
				console.error('加载通知列表失败:', error)
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				})
				this.conversationList = []
			} finally {
				this.loading = false
			}
		},
		
		// 获取通知类型名称
		getTypeName(type) {
			const typeMap = {
				'payment_success': '支付通知',
				'appointment_reminder': '预约提醒',
				'cancellation': '取消通知',
				'waitlist_available': '候补通知',
				'schedule_change': '排班变更',
				'system_notice': '系统通知'
			}
			return typeMap[type] || '系统通知'
		},
		
		// 处理对话点击
		handleConversationClick(conversation) {
			console.log('点击对话', conversation)
			// 检查是否有效
			if (!conversation || !conversation.senderId) {
				uni.showToast({
					title: '对话数据错误',
					icon: 'none'
				})
				return
			}
			
			// 跳转到对话详情页
			const url = `/pages/messages/message-conversation?senderId=${encodeURIComponent(conversation.senderId)}`
			console.log('跳转URL', url)
			
			uni.navigateTo({
				url: url,
				success: () => {
					console.log('跳转成功')
				},
				fail: (err) => {
					console.error('跳转失败', err)
					uni.showToast({
						title: '跳转失败',
						icon: 'none'
					})
				}
			})
		},
		
		// 获取消息图标
		getMessageIcon(type) {
			const icons = {
				'payment_success': '💰',
				'appointment_reminder': '📅',
				'cancellation': '🚫',
				'waitlist_available': '⏳',
				'schedule_change': '📢',
				'system_notice': '🔔',
				'appointment': '📅',
				'cancel': '🚫',
				'system': '🔔',
				'notice': '📢',
				'reminder': '⏰'
			}
			return icons[type] || '📩'
		},
		
		// 格式化时间
		formatTime(timeString) {
			if (!timeString) return ''
			const date = new Date(timeString)
			const now = new Date()
			const diff = now - date
			const days = Math.floor(diff / (1000 * 60 * 60 * 24))
			
			if (days === 0) {
				// 今天
				return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
			} else if (days === 1) {
				// 昨天
				return '昨天'
			} else if (days < 7) {
				// 一周内
				return `${days}天前`
			} else {
				// 更早
				return `${date.getMonth() + 1}月${date.getDate()}日`
			}
		},
		
		// 导航到科室列表
		navigateToDepartments() {
			uni.navigateTo({
				url: '/pages/departments/departments'
			})
		}
	}
}
</script>

<style lang="scss">
	.container {
		min-height: 100vh;
		background-color: #f7fafc;
		padding-bottom: 30rpx;
	}

	.conversation-list {
		padding: 20rpx 30rpx;
		width: 100%;
		box-sizing: border-box;
	}
	
	.conversation-item {
		background: #ffffff;
		border-radius: 16rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
		display: flex;
		align-items: center;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
		transition: all 0.3s ease;
	}
	
	.conversation-item:active {
		transform: scale(0.98);
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
	}
	
	.conversation-avatar {
		position: relative;
		width: 88rpx;
		height: 88rpx;
		margin-right: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background-color: #E6FFFA;
		border-radius: 20rpx;
		box-shadow: 0 4rpx 12rpx rgba(79, 209, 197, 0.2);
	}
	
	.avatar-icon {
		font-size: 44rpx;
	}
	
	.unread-badge {
		position: absolute;
		top: -4rpx;
		right: -4rpx;
		background: #FF6B6B;
		border-radius: 20rpx;
		min-width: 32rpx;
		height: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0 8rpx;
		border: 2rpx solid #ffffff;
	}
	
	.badge-text {
		font-size: 20rpx;
		color: #ffffff;
		font-weight: 700;
	}
	
	.conversation-content {
		flex: 1;
		display: flex;
		flex-direction: column;
	}
	
	.conversation-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 8rpx;
	}
	
	.sender-name {
		font-size: 30rpx;
		font-weight: 600;
		color: #1A202C;
	}
	
	.conversation-time {
		font-size: 22rpx;
		color: #A0AEC0;
	}
	
	.conversation-footer {
		display: flex;
		align-items: center;
	}
	
	.latest-message {
		font-size: 26rpx;
		color: #718096;
		line-height: 1.4;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		overflow: hidden;
	}

	.empty-state {
		padding: 120rpx 40rpx;
		text-align: center;
	}

	.empty-icon {
		display: block;
		font-size: 120rpx;
		margin-bottom: 30rpx;
		opacity: 0.5;
	}

	.empty-text {
		display: block;
		font-size: 28rpx;
		color: #718096;
		margin-bottom: 24rpx;
	}
	
	.empty-btn {
		margin-top: 24rpx;
		padding: 16rpx 48rpx;
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 24rpx;
		display: inline-block;
	}
	
	.empty-btn-text {
		font-size: 28rpx;
		color: #ffffff;
		font-weight: 600;
	}

	.loading {
		position: fixed;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		background: rgba(0, 0, 0, 0.75);
		color: #ffffff;
		padding: 24rpx 48rpx;
		border-radius: 16rpx;
		z-index: 9999;
	}

	.loading-text {
		font-size: 28rpx;
	}
</style>