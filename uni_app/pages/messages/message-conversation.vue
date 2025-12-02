<template>
	<view class="conversation-container">
		<!-- 对话头 -->
		<view class="conversation-header">
			<text class="sender-name">{{ conversation.senderName }}</text>
			<text class="unread-count" v-if="unreadCount > 0">{{ unreadCount }}条未读</text>
		</view>
		
		<!-- 消息列表 -->
		<scroll-view 
			class="messages-scroll" 
			scroll-y 
			:scroll-top="scrollTop"
			@scrolltolower="loadMoreMessages"
		>
			<!-- 有通知时显示 -->
			<view 
				class="message-bubble" 
				v-for="notification in conversation.messages" 
				:key="notification.notificationId || notification.id"
				:class="{ 'unread': notification.status === 'unread', 'clickable': isClickableNotification(notification) }"
				@click="handleNotificationClick(notification)"
			>
				<view class="message-time">{{ formatTime(notification.sentAt || notification.createTime) }}</view>
				<view class="message-content-wrapper">
					<view class="message-title">{{ notification.title }}</view>
					<text class="message-text">{{ notification.content }}</text>
					<!-- 候补通知显示操作提示 -->
					<view class="action-hint" v-if="notification.type === 'waitlist_available'">
						<text class="hint-text">点击查看详情并支付</text>
					</view>
				</view>
			</view>
			
			<!-- 空状态 -->
			<view class="empty-state" v-if="conversation.messages.length === 0">
				<text class="empty-icon">📭</text>
				<text class="empty-text">暂无通知</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getUserNotifications, markAsRead, markAllAsRead } from '../../api/notification.js'
	
	export default {
		data() {
			return {
				conversation: {
					senderId: '',
					senderName: '',
					messages: []
				},
				unreadCount: 0,
				scrollTop: 0,
				notificationType: '' // 通知类型
			}
		},
		onLoad(options) {
			// 从路由参数获取通知类型（senderId实际上是通知类型）
			const notificationType = decodeURIComponent(options.senderId || '')
			this.notificationType = notificationType
			// 加载对话内容
			this.loadConversation(notificationType)
		},
		onShow() {
			// 标记所有消息为已读
			this.markAllAsRead()
		},
		methods: {
			async loadConversation(notificationType) {
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (!patientInfo || !patientInfo.id) {
						uni.showToast({
							title: '请先登录',
							icon: 'none'
						})
						return
					}
					
					// 从全局存储获取通知列表，如果没有则调用API
					let allNotifications = uni.getStorageSync('allNotifications') || []
					
					// 如果存储中没有数据，调用API获取
					if (allNotifications.length === 0) {
						const notifications = await getUserNotifications(patientInfo.id, 'patient')
						if (Array.isArray(notifications)) {
							allNotifications = notifications
						} else if (notifications && notifications.data && Array.isArray(notifications.data)) {
							allNotifications = notifications.data
						}
						uni.setStorageSync('allNotifications', allNotifications)
					}
					
					// 筛选出该类型的通知
					const notifications = allNotifications.filter(notif => notif.type === notificationType)
					
					if (notifications.length === 0) {
						uni.showToast({
							title: '暂无通知',
							icon: 'none'
						})
						this.conversation = {
							senderId: notificationType,
							senderName: this.getTypeName(notificationType),
							messages: []
						}
						return
					}
					
					// 按时间排序（最新的在前）
					const sortedNotifications = notifications.sort((a, b) => {
						const timeA = new Date(a.sentAt || a.createTime || 0)
						const timeB = new Date(b.sentAt || b.createTime || 0)
						return timeB - timeA // 降序，最新的在前
					})
					
					this.conversation = {
						senderId: notificationType,
						senderName: this.getTypeName(notificationType),
						messages: sortedNotifications
					}
					
					this.unreadCount = notifications.filter(notif => notif.status === 'unread').length
				} catch (error) {
					console.error('加载通知详情失败:', error)
					uni.showToast({
						title: '加载失败，请重试',
						icon: 'none'
					})
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
			
			// 判断通知是否可点击
			isClickableNotification(notification) {
				return notification.type === 'waitlist_available' && notification.waitlistId
			},
			
			// 处理通知点击
			handleNotificationClick(notification) {
				if (!this.isClickableNotification(notification)) {
					return
				}
				
				// 候补通知：跳转到候补详情页
				if (notification.type === 'waitlist_available' && notification.waitlistId) {
					const waitlistId = notification.waitlistId
					uni.navigateTo({
						url: `/pages/waitlist/waitlist-detail?waitlistId=${waitlistId}`,
						success: () => {
							// 标记该通知为已读
							this.markNotificationAsRead(notification)
						}
					})
				}
			},
			
			// 标记单个通知为已读
			async markNotificationAsRead(notification) {
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (patientInfo && patientInfo.id && notification.notificationId) {
						await markAsRead(notification.notificationId)
						// 更新本地状态
						notification.status = 'read'
						this.unreadCount = Math.max(0, this.unreadCount - 1)
					}
				} catch (error) {
					console.error('标记通知为已读失败:', error)
				}
			},
			
			async markAllAsRead() {
				// 调用API标记该类型的所有通知为已读
				if (this.conversation.messages && this.conversation.messages.length > 0) {
					try {
						const patientInfo = uni.getStorageSync('patientInfo')
						if (patientInfo && patientInfo.id) {
							// 标记该类型的所有未读通知为已读
							const unreadNotifications = this.conversation.messages.filter(
								notif => notif.status === 'unread'
							)
							
							// 批量标记为已读
							for (const notification of unreadNotifications) {
								try {
									await markAsRead(notification.notificationId || notification.id)
									notification.status = 'read'
								} catch (error) {
									console.error('标记通知已读失败:', error)
								}
							}
							
							this.unreadCount = 0
							
							// 更新全局存储
							const allNotifications = uni.getStorageSync('allNotifications') || []
							allNotifications.forEach(notif => {
								if (notif.type === this.notificationType && notif.status === 'unread') {
									notif.status = 'read'
								}
							})
							uni.setStorageSync('allNotifications', allNotifications)
						}
					} catch (error) {
						console.error('标记已读失败:', error)
					}
				}
			},
			
			loadMoreMessages() {
				// TODO: 加载更多历史消息
			},
			
			formatTime(timeString) {
				if (!timeString) return ''
				const date = new Date(timeString)
				const now = new Date()
				const diff = now - date
				const days = Math.floor(diff / (1000 * 60 * 60 * 24))
				
				if (days === 0) {
					return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
				} else if (days === 1) {
					return '昨天'
				} else if (days < 7) {
					return `${days}天前`
				} else {
					return `${date.getMonth() + 1}月${date.getDate()}日`
				}
			}
		}
	}
</script>

<style lang="scss">
	.conversation-container {
		min-height: 100vh;
		background-color: #f7fafc;
		display: flex;
		flex-direction: column;
	}
	
	.conversation-header {
		background: #ffffff;
		padding: 30rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		border-bottom: 1rpx solid #e2e8f0;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}
	
	.sender-name {
		font-size: 32rpx;
		font-weight: 600;
		color: #1A202C;
	}
	
	.unread-count {
		font-size: 24rpx;
		color: #FF6B6B;
		font-weight: 500;
	}
	
	.messages-scroll {
		flex: 1;
		padding: 30rpx;
	}
	
	.message-bubble {
		margin-bottom: 30rpx;
		padding: 24rpx;
		background: #ffffff;
		border-radius: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
	}
	
	.message-bubble.unread {
		background: linear-gradient(135deg, #ffffff 0%, #F0FDFA 100%);
		border-left: 4rpx solid $color-primary;
	}
	
	.message-bubble.clickable {
		cursor: pointer;
		transition: all 0.3s ease;
		
		&:active {
			transform: scale(0.98);
			box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.1);
		}
	}
	
	.action-hint {
		margin-top: 12rpx;
		padding: 12rpx;
		background: #FFF7E6;
		border-radius: 8rpx;
		border-left: 3rpx solid #FFA500;
	}
	
	.hint-text {
		font-size: 24rpx;
		color: #FF8C00;
		font-weight: 500;
	}
	
	.message-time {
		font-size: 22rpx;
		color: #A0AEC0;
		margin-bottom: 12rpx;
	}
	
	.message-content-wrapper {
		display: flex;
		flex-direction: column;
	}
	
	.message-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1A202C;
		margin-bottom: 8rpx;
	}
	
	.message-text {
		font-size: 26rpx;
		color: #718096;
		line-height: 1.6;
		white-space: pre-wrap;
		word-break: break-word;
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
	}
</style>

