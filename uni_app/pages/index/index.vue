<template>
	<view class="container">
		<!-- 顶部用户信息区域 -->
		<view class="header">
			<view class="header-bg"></view>
			<view class="user-info">
				<view class="logo-wrapper">
					<image class="logo" src="/static/logo.png" mode="aspectFit"></image>
				</view>
				<view class="user-details">
					<text class="greeting">您好，</text>
					<text class="user-name">{{ patientInfo.name || '患者' }}</text>
					<view class="user-id-wrapper">
						<text class="user-id">{{ displayIdentifier }}</text>
						<text class="eye-icon" @click.stop="toggleIdentifierMask">👁️</text>
					</view>
				</view>
				<view class="notification-bell" @click="navigateToMessages">
					<text class="bell-icon">🔔</text>
					<view class="notification-badge" v-if="unreadCount > 0">
						<text class="badge-text">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 主要功能入口（2x2网格布局） -->
		<view class="main-functions">
			<view class="function-card card-1" @click="navigateToDepartments">
				<view class="icon-wrapper">
					<view class="function-icon">🏥</view>
				</view>
				<text class="function-title">预约挂号</text>
			</view>
			
			<view class="function-card card-2" @click="navigateToMyAppointments">
				<view class="icon-wrapper">
					<view class="function-icon">📅</view>
				</view>
				<text class="function-title">我的预约</text>
			</view>
			
			<view class="function-card card-3" @click="navigateToProfile">
				<view class="icon-wrapper">
					<view class="function-icon">👤</view>
				</view>
				<text class="function-title">个人中心</text>
			</view>
			
			<view class="function-card card-4" @click="showContactInfo">
				<view class="icon-wrapper">
					<view class="function-icon">📞</view>
				</view>
				<text class="function-title">联系我们</text>
			</view>
		</view>

		<!-- 即将就诊提醒卡片（简化版） -->
		<view class="appointment-card" v-if="upcomingAppointment && isWithin24Hours">
			<view class="appointment-icon">🔔</view>
			<view class="appointment-content">
				<text class="appointment-title">即将就诊</text>
				<text class="appointment-info-text">{{ formatTime(upcomingAppointment.scheduleTime) }} · {{ upcomingAppointment.departmentName }} · {{ upcomingAppointment.doctorName }}</text>
			</view>
			<text class="appointment-number">#{{ upcomingAppointment.queueNumber }}</text>
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

		<!-- 合并的信息卡片：今日可预约 + 热门科室 -->
		<view class="info-card">
			<view class="card-header">
				<text class="card-title">今日可预约</text>
				<text class="view-all" @click="navigateToDepartments">查看全部 ></text>
			</view>
			<!-- 骨架屏 -->
			<view class="skeleton-container" v-if="loading && todaySchedules.length === 0">
				<view class="skeleton-item" v-for="i in 4" :key="i" :style="{ animationDelay: `${(i - 1) * 100}ms` }"></view>
			</view>
			<!-- 数据列表 -->
			<view class="schedule-grid" v-else-if="todaySchedules.length > 0">
				<view 
					class="schedule-item" 
					v-for="schedule in todaySchedules.slice(0, 4)" 
					:key="schedule.id"
					@click="navigateToDepartmentSchedule(schedule.departmentId)"
				>
					<text class="dept-name">{{ schedule.departmentName }}</text>
					<text class="available-count">还剩 {{ schedule.availableSlots }} 号</text>
				</view>
			</view>
			<!-- 空状态 -->
			<view class="empty-state" v-else>
				<text class="empty-icon">🩺</text>
				<text class="empty-text">今日号源已约满，明日 8:00 放号</text>
				<view class="empty-btn" @click="navigateToDepartments">
					<text class="empty-btn-text">去挂号</text>
				</view>
			</view>
			<view class="card-divider"></view>
			<view class="card-header">
				<text class="card-title">热门科室</text>
			</view>
			<!-- 热门科室骨架屏 -->
			<view class="skeleton-tags" v-if="loading && popularDepartments.length === 0">
				<view class="skeleton-tag" v-for="i in 6" :key="i" :style="{ animationDelay: `${(i - 1) * 200}ms` }"></view>
			</view>
			<!-- 热门科室列表 -->
			<view v-else-if="popularDepartments.length > 0">
				<view class="department-tags" :class="{ 'tags-collapsed': !showAllDepartments }">
					<view 
						class="department-tag" 
						v-for="dept in popularDepartments" 
						:key="dept.id"
						@click="navigateToDepartments"
					>
						<text class="tag-text">{{ dept.name }}</text>
					</view>
				</view>
				<view class="more-btn" @click="toggleDepartments" v-if="popularDepartments.length > 4">
					<text class="more-btn-text">{{ showAllDepartments ? '收起 ∧' : '更多 ∨' }}</text>
				</view>
			</view>
			<!-- 热门科室空状态 -->
			<view class="empty-state-small" v-else>
				<text class="empty-icon-small">🏥</text>
				<text class="empty-text-small">暂无热门科室</text>
				<view class="empty-btn-small" @click="navigateToDepartments">
					<text class="empty-btn-text-small">去挂号</text>
				</view>
			</view>
		</view>

		<!-- 加载状态 -->
		<view class="loading" v-if="loading">
			<text class="loading-text">加载中...</text>
		</view>
	</view>
</template>

<script>
	import { mockTodaySchedules, mockPopularDepartments, mockPatientInfo } from '../../api/mockData.js'
	import { getTodaySchedules, getPopularDepartments, getDepartmentTree } from '../../api/schedule.js'
	import { getUpcomingAppointments, getPatientWaitlist } from '../../api/appointment.js'
	
	export default {
		data() {
			return {
				loading: false,
				hasNetworkError: false,
				isRefreshing: false,
				patientInfo: {
					name: '张三',
					identifier: '2021001001'
				},
				todaySchedules: [],
				upcomingAppointment: null,
				popularDepartments: [],
				waitlistCount: 0,
				unreadCount: 0,
				showAllDepartments: false,
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
			},
			isWithin24Hours() {
				if (!this.upcomingAppointment) return false
				const now = new Date()
				
				// 优先使用排班结束时间判断：只要排班结束时间还没到，就显示
				if (this.upcomingAppointment.scheduleEndTime) {
					const scheduleEndTime = new Date(this.upcomingAppointment.scheduleEndTime)
					// 如果排班结束时间还没到，就显示
					if (scheduleEndTime > now) {
						console.log('[首页] isWithin24Hours检查 - 排班结束时间未到:', {
							scheduleEndTime: this.upcomingAppointment.scheduleEndTime,
							now: now.toISOString(),
							diff: scheduleEndTime - now
						})
						return true
					} else {
						console.log('[首页] isWithin24Hours检查 - 排班结束时间已过:', {
							scheduleEndTime: this.upcomingAppointment.scheduleEndTime,
							now: now.toISOString()
						})
						return false
					}
				}
				
				// 如果没有排班结束时间，使用开始时间判断（兼容旧数据）
				if (this.upcomingAppointment.scheduleTime) {
					const scheduleTime = new Date(this.upcomingAppointment.scheduleTime)
					// 如果开始时间在未来24小时内，也显示
					const diff = scheduleTime - now
					const result = diff > 0 && diff <= 24 * 60 * 60 * 1000
					console.log('[首页] isWithin24Hours检查 - 使用开始时间:', {
						scheduleTime: this.upcomingAppointment.scheduleTime,
						now: now.toISOString(),
						diff: diff,
						result: result
					})
					return result
				}
				
				return false
			}
		},
		onLoad() {
			this.checkLoginStatus()
			this.loadPageData()
		},
		onShow() {
			// 页面显示时先重置候补数量，避免显示旧数据
			this.$set(this, 'waitlistCount', 0)
			// 页面显示时刷新数据
			this.loadPageData()
			// 同步待就诊数量到storage
			if (this.upcomingAppointment) {
				uni.setStorageSync('upcomingAppointment', this.upcomingAppointment)
			} else {
				uni.removeStorageSync('upcomingAppointment')
			}
		},
		onPullDownRefresh() {
			// 下拉刷新
			this.isRefreshing = true
			this.loadPageData()
			this.isRefreshing = false
			uni.stopPullDownRefresh()
		},
		methods: {
			// 检查登录状态
			checkLoginStatus() {
				const patientInfo = uni.getStorageSync('patientInfo')
				
				console.log('从Storage读取的patientInfo:', patientInfo)
				
				// 如果没有登录信息，使用模拟数据（仅用于演示）
				if (!patientInfo) {
					console.log('使用模拟数据演示页面功能')
					this.patientInfo = mockPatientInfo
					return true
				}
				
				console.log('使用登录数据，患者信息:', patientInfo)
				this.patientInfo = patientInfo
				return true
			},
			
			// 加载页面数据
			async loadPageData() {
				// 先检查登录状态
				this.checkLoginStatus()
				
				this.loading = true
				this.hasNetworkError = false
				
				// 加载今日排班（调用真实API）
				try {
					const today = new Date()
					const startDate = this.formatDate(today)
					const endDate = startDate // 只查询今日
					
					console.log('首页 - 加载今日排班:', { startDate, endDate })
					const scheduleResponse = await getTodaySchedules(startDate, endDate)
					console.log('首页 - 今日排班响应:', scheduleResponse)
					
					if (scheduleResponse && scheduleResponse.code === '200' && scheduleResponse.data) {
						// 按科室分组，统计每个科室的可用号源总数
						const departmentMap = new Map()
						
						scheduleResponse.data.forEach(schedule => {
							if (!schedule.departmentId || !schedule.departmentName) return
							
							const deptId = schedule.departmentId
							const remainingSlots = schedule.remainingSlots || 0
							
							if (departmentMap.has(deptId)) {
								const existing = departmentMap.get(deptId)
								existing.availableSlots += remainingSlots
							} else {
								departmentMap.set(deptId, {
									id: deptId,
									departmentId: deptId,
									departmentName: schedule.departmentName,
									availableSlots: remainingSlots
								})
							}
						})
						
						// 转换为数组，按可用号源数量降序排列，取前4个
						this.todaySchedules = Array.from(departmentMap.values())
							.filter(dept => dept.availableSlots > 0) // 只显示有号源的科室
							.sort((a, b) => b.availableSlots - a.availableSlots)
							.slice(0, 4)
						
						console.log('首页 - 处理后的今日可预约:', this.todaySchedules)
					} else {
						// API失败时使用mock数据
						console.warn('首页 - 今日排班API失败，使用mock数据')
						this.todaySchedules = JSON.parse(JSON.stringify(mockTodaySchedules))
					}
				} catch (error) {
					console.error('首页 - 加载今日排班失败:', error)
					// 出错时使用mock数据
					this.todaySchedules = JSON.parse(JSON.stringify(mockTodaySchedules))
				}
				
				// 加载热门科室（调用真实API）
				try {
					console.log('首页 - 加载热门科室')
					const popularResponse = await getPopularDepartments()
					console.log('首页 - 热门科室响应:', popularResponse)
					
					const excludedNames = ['医技科室', '行政科室'] // 排除不应该显示的科室
					
					if (popularResponse && popularResponse.code === '200' && popularResponse.data) {
						// 如果返回的是数组
						const deptList = Array.isArray(popularResponse.data) 
							? popularResponse.data
							: [popularResponse.data]
						
						this.popularDepartments = deptList
							.filter(dept => {
								const name = dept.name || dept.departmentName || ''
								return !excludedNames.includes(name)
							})
							.map(dept => ({
								id: dept.departmentId || dept.id,
								name: dept.name || dept.departmentName
							}))
					} else if (Array.isArray(popularResponse)) {
						// 直接返回数组
						this.popularDepartments = popularResponse
							.filter(dept => {
								const name = dept.name || dept.departmentName || ''
								return !excludedNames.includes(name)
							})
							.map(dept => ({
								id: dept.departmentId || dept.id,
								name: dept.name || dept.departmentName
							}))
					} else {
						// 如果后端没有popular接口，使用科室树获取父科室作为热门科室
						console.log('首页 - 热门科室接口不可用，使用科室树获取父科室')
						try {
							const treeResponse = await getDepartmentTree()
							const excludedNames = ['医技科室', '行政科室'] // 排除不应该显示的科室
							
							if (Array.isArray(treeResponse)) {
								// 取前6个父科室作为热门科室（DepartmentTreeDTO 结构：id, name, type, children）
								this.popularDepartments = treeResponse
									.filter(item => {
										// 只取父科室，并排除医技科室和行政科室
										const isParent = item.type === 'parent' || !item.type
										const name = item.name || ''
										return isParent && !excludedNames.includes(name)
									})
									.slice(0, 6)
									.map(parent => ({
										id: parent.id,
										name: parent.name
									}))
							} else if (treeResponse && Array.isArray(treeResponse.data)) {
								this.popularDepartments = treeResponse.data
									.filter(item => {
										const isParent = item.type === 'parent' || !item.type
										const name = item.name || ''
										return isParent && !excludedNames.includes(name)
									})
									.slice(0, 6)
									.map(parent => ({
										id: parent.id,
										name: parent.name
									}))
							} else {
								// 都失败时使用mock数据
								this.popularDepartments = JSON.parse(JSON.stringify(mockPopularDepartments))
							}
						} catch (treeError) {
							console.error('首页 - 加载科室树失败:', treeError)
							this.popularDepartments = JSON.parse(JSON.stringify(mockPopularDepartments))
						}
					}
				} catch (error) {
					console.error('首页 - 加载热门科室失败:', error)
					// 出错时使用mock数据
					this.popularDepartments = JSON.parse(JSON.stringify(mockPopularDepartments))
				}
				
				// 加载未读通知数量
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (patientInfo && patientInfo.id) {
						const { getUnreadCount } = await import('../../api/notification.js')
						const count = await getUnreadCount(patientInfo.id, 'patient')
						this.unreadCount = count || 0
					} else {
						this.unreadCount = 0
					}
				} catch (error) {
					console.error('加载未读通知数量失败:', error)
					this.unreadCount = 0
				}
				
				// 加载即将就诊的预约（调用真实API）
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (patientInfo && patientInfo.id) {
						// 获取即将就诊的预约
						const appointmentResponse = await getUpcomingAppointments(patientInfo.id)
						console.log('[首页] 即将就诊预约响应:', appointmentResponse)
						if (appointmentResponse && appointmentResponse.code === '200' && appointmentResponse.data) {
							const now = new Date()
							console.log('[首页] 当前时间:', now.toISOString())
							
							// 过滤掉已取消和已完成的预约
							// 注意：不过滤时间已过去的预约，因为只要排班结束时间还没到，就算"即将就诊"
							const validAppointments = appointmentResponse.data.filter(apt => {
								// 过滤已取消的预约
								if (apt.status === 'cancelled' || apt.status === 'CANCELLED') {
									console.log('[首页] 过滤已取消的预约:', apt.appointmentId, apt.scheduleTime)
									return false
								}
								// 过滤已完成的预约
								if (apt.status === 'completed' || apt.status === 'COMPLETED') {
									console.log('[首页] 过滤已完成的预约:', apt.appointmentId, apt.scheduleTime)
									return false
								}
								
								// 检查排班结束时间：如果排班结束时间已过，则过滤掉
								if (apt.scheduleEndTime) {
									const scheduleEndTime = new Date(apt.scheduleEndTime)
									if (scheduleEndTime <= now) {
										console.log('[首页] 过滤排班结束时间已过的预约:', {
											id: apt.appointmentId,
											scheduleTime: apt.scheduleTime,
											scheduleEndTime: apt.scheduleEndTime,
											now: now.toISOString()
										})
										return false
									}
								} else if (apt.scheduleTime) {
									// 如果没有排班结束时间，使用开始时间+默认时长（比如4小时）来判断
									// 或者直接不过滤，让后端查询逻辑处理
									console.log('[首页] 预约没有排班结束时间，使用开始时间:', apt.appointmentId, apt.scheduleTime)
								}
								
								return true
							})
							
							console.log('[首页] 过滤后的有效预约数量:', validAppointments.length)
							console.log('[首页] 有效预约列表:', validAppointments.map(apt => ({
								id: apt.appointmentId,
								scheduleTime: apt.scheduleTime,
								scheduleEndTime: apt.scheduleEndTime,
								status: apt.status
							})))
							
							// 按就诊时间升序排序，取最早的预约
							if (validAppointments.length > 0) {
								validAppointments.sort((a, b) => {
									if (!a.scheduleTime && !b.scheduleTime) return 0
									if (!a.scheduleTime) return 1
									if (!b.scheduleTime) return -1
									const timeA = new Date(a.scheduleTime).getTime()
									const timeB = new Date(b.scheduleTime).getTime()
									return timeA - timeB // 升序：最早的在前
								})
								
								this.upcomingAppointment = validAppointments[0]
								console.log('[首页] 最终选择的即将就诊预约:', {
									id: this.upcomingAppointment.appointmentId,
									scheduleTime: this.upcomingAppointment.scheduleTime,
									scheduleEndTime: this.upcomingAppointment.scheduleEndTime,
									status: this.upcomingAppointment.status
								})
							} else {
								this.upcomingAppointment = null
								console.log('[首页] 没有有效的即将就诊预约')
							}
						} else {
							this.upcomingAppointment = null
							console.log('[首页] API响应无效，清空即将就诊预约')
						}
						
						// 获取候补数量（只统计等待中和已通知的候补）
						const waitlistResponse = await getPatientWaitlist(patientInfo.id)
						console.log('首页 - 候补列表响应:', waitlistResponse)
						console.log('首页 - 候补列表响应类型:', typeof waitlistResponse, '是否为数组:', Array.isArray(waitlistResponse))
						
						// 先重置为0，避免使用旧数据，并强制更新视图
						this.$set(this, 'waitlistCount', 0)
						
						if (waitlistResponse && waitlistResponse.code === '200' && waitlistResponse.data) {
							const waitlistData = waitlistResponse.data
							console.log('首页 - 候补数据:', waitlistData)
							console.log('首页 - 候补数据长度:', Array.isArray(waitlistData) ? waitlistData.length : 0)
							
							// 确保是数组
							const waitlistArray = Array.isArray(waitlistData) ? waitlistData : []
							
							// 过滤状态：只统计 waiting（等待中）和 notified（已通知）的候补
							// 排除 expired（已过期）、booked（已预约）、cancelled（已取消）等状态
							const validCount = waitlistArray.filter(w => {
								const status = (w.status || '').toLowerCase()
								return status === 'waiting' || status === 'notified'
							}).length
							this.$set(this, 'waitlistCount', validCount)
							
							console.log('首页 - 候补数量统计:', {
								总数: waitlistArray.length,
								有效候补: validCount,
								更新后的waitlistCount: this.waitlistCount,
								所有状态: waitlistArray.map(w => w.status),
								所有候补项: waitlistArray.map(w => ({
									id: w.id || w.waitlistId,
									status: w.status,
									departmentName: w.departmentName
								}))
							})
							
							// 强制更新视图
							this.$nextTick(() => {
								this.$forceUpdate()
								console.log('首页 - $nextTick后waitlistCount:', this.waitlistCount)
							})
						} else if (Array.isArray(waitlistResponse)) {
							// 如果直接返回数组
							console.log('首页 - 候补数据直接是数组:', waitlistResponse)
							const validCount = waitlistResponse.filter(w => {
								const status = (w.status || '').toLowerCase()
								return status === 'waiting' || status === 'notified'
							}).length
							this.$set(this, 'waitlistCount', validCount)
							console.log('首页 - 候补数量统计（直接数组）:', {
								总数: waitlistResponse.length,
								有效候补: validCount,
								更新后的waitlistCount: this.waitlistCount
							})
							// 强制更新视图
							this.$nextTick(() => {
								this.$forceUpdate()
								console.log('首页 - $nextTick后waitlistCount（直接数组）:', this.waitlistCount)
							})
						} else {
							console.log('首页 - 候补数据格式异常，设置为0')
							this.$set(this, 'waitlistCount', 0)
							this.$nextTick(() => {
								this.$forceUpdate()
								console.log('首页 - $nextTick后waitlistCount（异常）:', this.waitlistCount)
							})
						}
					} else {
						this.upcomingAppointment = null
						this.$set(this, 'waitlistCount', 0)
					}
				} catch (error) {
					console.error('加载预约/候补数据失败:', error)
					this.upcomingAppointment = null
					this.$set(this, 'waitlistCount', 0)
				}
				
				this.loading = false
			},
			
			// 格式化日期为 YYYY-MM-DD
			formatDate(date) {
				const year = date.getFullYear()
				const month = String(date.getMonth() + 1).padStart(2, '0')
				const day = String(date.getDate()).padStart(2, '0')
				return `${year}-${month}-${day}`
			},
			
			// 导航到消息中心
			navigateToMessages() {
				uni.switchTab({
					url: '/pages/messages/messages'
				})
			},
			
			// 格式化时间
			formatTime(timeString) {
				if (!timeString) return ''
				const date = new Date(timeString)
				const month = date.getMonth() + 1
				const day = date.getDate()
				const hours = date.getHours().toString().padStart(2, '0')
				const minutes = date.getMinutes().toString().padStart(2, '0')
				return month + '月' + day + '日 ' + hours + ':' + minutes
			},
			
			// 导航到科室列表
			navigateToDepartments() {
				uni.navigateTo({
					url: '/pages/departments/departments'
				})
			},
			
			// 导航到我的预约
			navigateToMyAppointments() {
				uni.switchTab({
					url: '/pages/appointments/appointments'
				})
			},
			
			// 导航到个人中心
			navigateToProfile() {
				uni.switchTab({
					url: '/pages/profile/profile'
				})
			},
			
			// 导航到候补列表（优先跳转到 notified 状态的候补详情）
			async navigateToWaitlist() {
				try {
					const patientInfo = uni.getStorageSync('patientInfo')
					if (!patientInfo || !patientInfo.id) {
						// 未登录，直接跳转到候补列表
						uni.navigateTo({
							url: '/pages/waitlist/waitlist'
						})
						return
					}
					
					// 获取候补列表
					const { getPatientWaitlist } = await import('../../api/appointment.js')
					const waitlistResponse = await getPatientWaitlist(patientInfo.id)
					
					let waitlistList = []
					if (waitlistResponse && waitlistResponse.code === '200' && waitlistResponse.data) {
						waitlistList = Array.isArray(waitlistResponse.data) ? waitlistResponse.data : []
					} else if (Array.isArray(waitlistResponse)) {
						waitlistList = waitlistResponse
					}
					
					// 优先查找 notified 状态的候补
					const notifiedWaitlist = waitlistList.find(w => {
						const status = (w.status || '').toLowerCase()
						return status === 'notified'
					})
					
					if (notifiedWaitlist) {
						// 有已通知的候补，跳转到详情页
						const waitlistId = notifiedWaitlist.id || notifiedWaitlist.waitlistId
						uni.navigateTo({
							url: `/pages/waitlist/waitlist-detail?waitlistId=${waitlistId}`
						})
					} else {
						// 没有已通知的候补，跳转到候补列表
						uni.navigateTo({
							url: '/pages/waitlist/waitlist'
						})
					}
				} catch (error) {
					console.error('获取候补列表失败:', error)
					// 出错时跳转到候补列表
					uni.navigateTo({
						url: '/pages/waitlist/waitlist'
					})
				}
			},
			
			// 显示联系方式
			showContactInfo() {
				uni.showActionSheet({
					itemList: ['客服电话', '紧急求助', '医院地址', '更多信息'],
					success: (res) => {
						switch(res.tapIndex) {
							case 0:
								// 客服电话
								uni.makePhoneCall({
									phoneNumber: '400-123-4567',
									fail: () => {
										uni.showModal({
											title: '客服电话',
											content: '400-123-4567\n工作时间：周一至周日 8:00-18:00',
											showCancel: false,
											confirmText: '知道了'
										})
									}
								})
								break
							case 1:
								// 紧急求助
								uni.showModal({
									title: '紧急求助',
									content: '如有紧急情况，请拨打120急救电话\n或直接前往医院急诊科',
									confirmText: '拨打120',
									cancelText: '取消',
									success: (modalRes) => {
										if (modalRes.confirm) {
											uni.makePhoneCall({
												phoneNumber: '120',
												fail: () => {
													uni.showToast({
														title: '请手动拨打120',
														icon: 'none',
														duration: 2000
													})
												}
											})
										}
									}
								})
								break
							case 2:
								// 医院地址
								uni.showModal({
									title: '医院地址',
									content: 'XX大学校医院\n地址：XX市XX区XX路XX号\n邮编：100000',
									showCancel: false,
									confirmText: '知道了'
								})
								break
							case 3:
								// 更多信息
								uni.showModal({
									title: '联系我们',
									content: '客服电话：400-123-4567\n工作时间：周一至周日 8:00-18:00\n邮箱：service@hospital.edu.cn\n地址：XX市XX区XX路XX号',
									showCancel: false,
									confirmText: '知道了'
								})
								break
						}
					}
				})
			},
			
			// 导航到科室排班
			navigateToDepartmentSchedule(departmentId) {
				// 查找对应的子科室
				const schedule = this.todaySchedules.find(s => s.departmentId === departmentId)
				const departmentName = schedule ? schedule.departmentName : '科室'
				
				uni.navigateTo({
					url: `/pages/schedules/schedules?departmentId=${departmentId}&departmentName=${encodeURIComponent(departmentName)}`
				})
			},
			
			// 切换热门科室展开/收起
			toggleDepartments() {
				this.showAllDepartments = !this.showAllDepartments
			},
			
			// 切换学号脱敏显示
			toggleIdentifierMask() {
				this.identifierMasked = !this.identifierMasked
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

	/* 头部用户信息区域 */
	.header {
		position: relative;
		padding: 40rpx 30rpx 35rpx;
		overflow: hidden;
	}

	.header-bg {
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		opacity: 0.95;
	}

	.header-bg::after {
		content: '';
		position: absolute;
		top: -50%;
		right: -20%;
		width: 400rpx;
		height: 400rpx;
		background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%);
		border-radius: 50%;
	}

	.user-info {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
	}

	.logo-wrapper {
		width: 90rpx;
		height: 90rpx;
		background: rgba(255, 255, 255, 0.25);
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		backdrop-filter: blur(10rpx);
		margin-right: 24rpx;
		padding: 8rpx;
	}

	.logo {
		width: 100%;
		height: 100%;
		border-radius: 16rpx;
	}

	.user-details {
		flex: 1;
	}

	.greeting {
		display: block;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.9);
		margin-bottom: 4rpx;
	}

	.user-name {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		margin-bottom: 6rpx;
		text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
	}

	.user-id-wrapper {
		display: flex;
		align-items: center;
		gap: 8rpx;
	}
	
	.user-id {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.8);
	}
	
	.eye-icon {
		font-size: 20rpx;
		opacity: 0.7;
	}

	/* 消息通知图标 */
	.notification-bell {
		position: relative;
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.25);
		border-radius: 50%;
		backdrop-filter: blur(10rpx);
		margin-left: 20rpx;
		transition: all 0.3s ease;
	}

	.notification-bell:active {
		transform: scale(0.9);
		background: rgba(255, 255, 255, 0.35);
	}

	.bell-icon {
		font-size: 36rpx;
	}

	.notification-badge {
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
		box-shadow: 0 2rpx 8rpx rgba(255, 107, 107, 0.4);
	}

	.badge-text {
		font-size: 20rpx;
		color: #ffffff;
		font-weight: 700;
		line-height: 1;
	}

	/* 主要功能入口（2x2网格布局） */
	.main-functions {
		padding: 30rpx 30rpx 25rpx;
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 20rpx;
	}

	.function-card {
		background: #ffffff;
		border-radius: 20rpx;
		padding: 32rpx 20rpx;
		text-align: center;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
		position: relative;
		overflow: hidden;
	}

	.function-card::before {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		opacity: 0;
		transition: opacity 0.3s ease;
	}

	.function-card:active {
		transform: translateY(-4rpx);
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
	}

	.function-card:active::before {
		opacity: 1;
	}

	/* 无障碍支持 */
	.function-card:focus {
		outline: 2rpx solid $color-primary;
		outline-offset: 2rpx;
	}

	.card-1 .icon-wrapper {
		background-color: #E6FFFA;
	}

	.card-2 .icon-wrapper {
		background-color: #EBF4FF;
	}

	.card-3 .icon-wrapper {
		background-color: #F0F9FF;
	}

	.card-4 .icon-wrapper {
		background-color: #FEF3C7;
	}

	.icon-wrapper {
		width: 88rpx;
		height: 88rpx;
		margin: 0 auto 16rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
	}

	.function-icon {
		font-size: 44rpx;
		color: $color-primary;
	}

	.function-title {
		display: block;
		font-size: 28rpx;
		font-weight: 600;
		color: #2D3748;
	}

	/* 信息卡片通用样式 */
	.info-card {
		background: #ffffff;
		margin: 0 30rpx 20rpx;
		border-radius: 24rpx;
		padding: 28rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
	}

	.card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 20rpx;
	}

	.card-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1A202C;
		position: relative;
		padding-left: 12rpx;
	}

	.card-title::before {
		content: '';
		position: absolute;
		left: 0;
		top: 50%;
		transform: translateY(-50%);
		width: 4rpx;
		height: 24rpx;
		background: linear-gradient(180deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 2rpx;
	}

	.view-all {
		font-size: 24rpx;
		color: $color-primary;
		font-weight: 500;
	}

	.card-divider {
		height: 1rpx;
		background: linear-gradient(90deg, transparent 0%, #E2E8F0 50%, transparent 100%);
		margin: 24rpx 0;
	}

	/* 今日可预约网格样式 */
	.schedule-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 16rpx;
	}

	.schedule-item {
		background: linear-gradient(135deg, #F7FAFC 0%, #EDF2F7 100%);
		border-radius: 16rpx;
		padding: 20rpx 18rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		border: 1rpx solid #E2E8F0;
		transition: all 0.3s ease;
	}

	.schedule-item:active {
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		transform: translateY(-2rpx);
		box-shadow: 0 6rpx 16rpx rgba(79, 209, 197, 0.3);
	}

	.schedule-item:active .dept-name,
	.schedule-item:active .available-count {
		color: #ffffff;
	}

	.dept-name {
		font-size: 28rpx;
		color: #2D3748;
		font-weight: 600;
		margin-bottom: 10rpx;
		transition: color 0.3s ease;
	}

	.available-count {
		font-size: 24rpx;
		color: $color-primary;
		font-weight: 700;
		transition: color 0.3s ease;
	}

	.department-tags {
		display: flex;
		gap: 12rpx;
		padding: 4rpx 0;
		flex-wrap: wrap;
	}
	
	.department-tags.tags-collapsed {
		max-height: 120rpx;
		overflow: hidden;
	}
	
	.department-tag {
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 16rpx;
		padding: 6rpx 16rpx;
		white-space: nowrap;
		transition: all 0.3s ease;
		box-shadow: 0 4rpx 12rpx rgba(79, 209, 197, 0.25);
	}

	.department-tag:active {
		transform: translateY(-2rpx) scale(0.98);
		box-shadow: 0 6rpx 16rpx rgba(79, 209, 197, 0.35);
	}
	
	.more-btn {
		margin-top: 16rpx;
		text-align: center;
		padding: 12rpx;
	}
	
	.more-btn-text {
		font-size: 24rpx;
		color: $color-primary;
		font-weight: 500;
	}

	.tag-text {
		font-size: 22rpx;
		color: #ffffff;
		font-weight: 600;
	}

	/* 即将就诊提醒卡片 */
	.appointment-card {
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		margin: 0 30rpx 20rpx;
		border-radius: 24rpx;
		padding: 24rpx 28rpx;
		display: flex;
		align-items: center;
		box-shadow: 0 8rpx 24rpx rgba(79, 209, 197, 0.3);
		position: relative;
		overflow: hidden;
	}

	.appointment-card::before {
		content: '';
		position: absolute;
		top: -50%;
		right: -20%;
		width: 300rpx;
		height: 300rpx;
		background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%);
		border-radius: 50%;
	}

	.appointment-icon {
		font-size: 40rpx;
		margin-right: 20rpx;
		position: relative;
		z-index: 1;
	}

	.appointment-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		position: relative;
		z-index: 1;
	}

	.appointment-title {
		font-size: 28rpx;
		color: #ffffff;
		font-weight: 700;
		margin-bottom: 8rpx;
	}

	.appointment-info-text {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.95);
		line-height: 1.4;
	}

	.appointment-number {
		font-size: 36rpx;
		color: #ffffff;
		font-weight: 800;
		position: relative;
		z-index: 1;
	}

	/* 候补提醒卡片样式 */
	.waitlist-card {
		background: linear-gradient(135deg, rgba(255, 165, 0, 0.15) 0%, rgba(255, 165, 0, 0.05) 100%);
		border: 2rpx solid rgba(255, 165, 0, 0.3);
		margin: 20rpx 30rpx;
		padding: 24rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		box-shadow: 0 4rpx 20rpx rgba(255, 165, 0, 0.2);
		transition: all 0.3s ease;
	}

	.waitlist-card:active {
		transform: scale(0.98);
	}

	.waitlist-icon {
		font-size: 56rpx;
		margin-right: 20rpx;
	}

	.waitlist-content {
		flex: 1;
	}

	.waitlist-title {
		display: block;
		font-size: 32rpx;
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

	/* 加载状态 */
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
		backdrop-filter: blur(10rpx);
	}

	.loading-text {
		font-size: 28rpx;
	}

	/* 骨架屏样式 */
	.skeleton-container {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 16rpx;
	}

	.skeleton-item {
		background: linear-gradient(90deg, #F7FAFC 25%, #EDF2F7 50%, #F7FAFC 75%);
		background-size: 200% 100%;
		border-radius: 16rpx;
		padding: 20rpx 18rpx;
		height: 100rpx;
		animation: skeleton-loading 1.5s ease-in-out infinite;
		opacity: 0;
		animation-fill-mode: both;
	}

	.skeleton-tags {
		display: flex;
		gap: 14rpx;
		padding: 6rpx 0;
	}

	.skeleton-tag {
		background: linear-gradient(90deg, #F7FAFC 25%, #EDF2F7 50%, #F7FAFC 75%);
		background-size: 200% 100%;
		border-radius: 24rpx;
		padding: 14rpx 24rpx;
		height: 60rpx;
		width: 120rpx;
		animation: skeleton-loading 1.5s ease-in-out infinite;
		opacity: 0;
		animation-fill-mode: both;
	}

	@keyframes skeleton-loading {
		0% {
			background-position: 200% 0;
		}
		100% {
			background-position: -200% 0;
		}
	}

	/* 空状态样式 */
	.empty-state {
		padding: 60rpx 20rpx;
		text-align: center;
	}

	.empty-icon {
		display: block;
		font-size: 80rpx;
		margin-bottom: 20rpx;
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

	.empty-state-small {
		padding: 30rpx 20rpx;
		text-align: center;
	}
	
	.empty-icon-small {
		display: block;
		font-size: 60rpx;
		margin-bottom: 16rpx;
		opacity: 0.5;
	}

	.empty-text-small {
		font-size: 24rpx;
		color: #A0AEC0;
		margin-bottom: 20rpx;
	}
	
	.empty-btn-small {
		margin-top: 20rpx;
		padding: 12rpx 40rpx;
		background: linear-gradient(135deg, lighten($color-primary, 10%) 0%, $color-primary 100%);
		border-radius: 20rpx;
		display: inline-block;
	}
	
	.empty-btn-text-small {
		font-size: 24rpx;
		color: #ffffff;
		font-weight: 600;
	}

	/* 响应式设计 */
	@media screen and (min-width: 768px) {
		.container {
			max-width: 750rpx;
			margin: 0 auto;
		}
	}

	/* 触摸目标大小优化 */
	.function-card,
	.schedule-item,
	.department-tag {
		min-height: 88rpx;
	}

	/* 字体大小优化 */
	.function-title,
	.card-title,
	.dept-name {
		font-size: 28rpx;
	}

	/* 对比度优化 */
	.view-all,
	.available-count {
		color: $color-primary;
		font-weight: 600;
	}
</style>
