<script>
	export default {
		waitlistCheckTimer: null, // 候补检查定时器
		
		onLaunch: function() {
			console.log('App Launch')
			// 启动候补通知监听
			this.startWaitlistNotificationCheck()
		},
		onShow: function() {
			console.log('App Show')
			// 应用显示时启动候补通知监听
			this.startWaitlistNotificationCheck()
		},
		onHide: function() {
			console.log('App Hide')
			// 应用隐藏时停止候补通知监听（可选）
			// this.stopWaitlistNotificationCheck()
		},
		methods: {
			// 启动候补通知检查
			startWaitlistNotificationCheck() {
				// 清除旧的定时器
				if (this.waitlistCheckTimer) {
					clearInterval(this.waitlistCheckTimer)
				}
				
				// 每30秒检查一次候补通知
				this.waitlistCheckTimer = setInterval(() => {
					this.checkWaitlistNotifications()
				}, 30000)
				
				// 立即检查一次
				this.checkWaitlistNotifications()
			},
			
			// 停止候补通知检查
			stopWaitlistNotificationCheck() {
				if (this.waitlistCheckTimer) {
					clearInterval(this.waitlistCheckTimer)
					this.waitlistCheckTimer = null
				}
			},
			
			// 检查候补通知
			async checkWaitlistNotifications() {
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (!patientInfo || !patientInfo.id) {
						return
					}
					
					// 动态导入 API
					const { getPatientWaitlist } = await import('./api/appointment.js')
					
					// 获取候补列表
					const waitlistResponse = await getPatientWaitlist(patientInfo.id)
					
					let waitlistList = []
					if (waitlistResponse && waitlistResponse.code === '200' && waitlistResponse.data) {
						waitlistList = Array.isArray(waitlistResponse.data) ? waitlistResponse.data : []
					} else if (Array.isArray(waitlistResponse)) {
						waitlistList = waitlistResponse
					}
					
					// 查找 notified 状态的候补（已通知但未支付）
					const notifiedWaitlists = waitlistList.filter(w => {
						const status = (w.status || '').toLowerCase()
						return status === 'notified'
					})
					
					// 如果有已通知的候补，显示弹窗提醒
					if (notifiedWaitlists.length > 0) {
						// 检查是否已经显示过提醒（避免重复提醒）
						const lastRemindTime = uni.getStorageSync('lastWaitlistRemindTime')
						const now = Date.now()
						
						// 如果上次提醒时间超过5分钟，或者没有记录，则显示提醒
						if (!lastRemindTime || (now - lastRemindTime) > 5 * 60 * 1000) {
							this.showWaitlistNotification(notifiedWaitlists[0])
							uni.setStorageSync('lastWaitlistRemindTime', now)
						}
					}
				} catch (error) {
					console.error('检查候补通知失败:', error)
				}
			},
			
			// 显示候补通知弹窗
			showWaitlistNotification(waitlist) {
				const waitlistId = waitlist.id || waitlist.waitlistId
				const departmentName = waitlist.departmentName || '科室'
				const doctorName = waitlist.doctorName || '医生'
				
				uni.showModal({
					title: '🔔 候补通知',
					content: `您有候补号源可用！\n${departmentName} - ${doctorName}\n请在15分钟内完成支付`,
					confirmText: '立即支付',
					cancelText: '稍后',
					success: (res) => {
						if (res.confirm) {
							// 跳转到候补详情页
							uni.navigateTo({
								url: `/pages/waitlist/waitlist-detail?waitlistId=${waitlistId}`
							})
						}
					}
				})
			}
		}
	}
</script>

<style>
	/*每个页面公共css */
</style>
