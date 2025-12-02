<template>
	<scroll-view class="page-scroll" :scroll-y="true">
	<view class="container">
		<!-- 控制面板 -->
		<view class="control-panel">
			<!-- 楼层选择器 -->
			<view class="floor-selector" v-if="availableFloors.length > 1">
				<text class="info-label">当前楼层：</text>
				<view class="floor-buttons">
					<view 
						v-for="floor in availableFloors" 
						:key="floor"
						class="floor-btn"
						:class="{ 'active': currentFloor === floor }"
						@click="switchFloor(floor)"
					>
						{{ floor }}楼
					</view>
				</view>
			</view>
			
			<!-- 导航指引区域 -->
			<view class="navigation-guide" v-if="currentStep">
				<view class="guide-header">
					<text class="guide-icon">🧭</text>
					<text class="guide-title">当前指引</text>
				</view>
				<view class="guide-content">
					<text class="guide-text">{{ currentStep.instruction }}</text>
				</view>
				<view class="guide-detail" v-if="currentStep.distance > 0">
					距离：{{ Math.round(currentStep.distance) }}米 | 
					预计：{{ Math.round(currentStep.walkTime / 60) }}分钟
				</view>
				<view class="guide-actions">
					<button class="arrived-btn" @click="markAsArrived" v-if="currentStep.toNodeName">
						✅ 已到达 {{ currentStep.toNodeName }}
					</button>
				</view>
			</view>
			
			<!-- 下一步提示 -->
			<view class="next-step-hint" v-if="nextStep && nextStep.instruction">
				<text class="hint-text">💡 下一步：{{ nextStep.instruction }}</text>
			</view>
			
			<view class="info-row">
				<text class="info-label">正在前往：</text>
				<text class="info-value">{{ targetNodeName || '加载中...' }}</text>
			</view>
			<view class="control-buttons">
				<view class="control-btn" @click.stop="scanLocationCode" style="background: #52C41A;">
					<text class="btn-text">📷 扫码定位</text>
				</view>
			</view>
		</view>
		
		<!-- 地图容器 -->
		<view class="map-container">
			<!-- 背景图片（根据楼层动态切换） -->
			<image 
				v-if="showBackgroundImage"
				class="background-image" 
				:src="`/static/images/hospital_floor_${currentFloor}.jpg`"
				mode="aspectFit"
				@error="handleImageError"
				@load="handleImageLoad"
			></image>
			
			<!-- Canvas绘制层 -->
			<canvas 
				canvas-id="mapCanvas" 
				class="map-canvas"
				:style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
				@touchstart="handleTouchStart"
				@touchmove="handleTouchMove"
				@touchend="handleTouchEnd"
			></canvas>
			
			<!-- 调试信息（开发时显示） -->
			<view class="debug-info" v-if="showDebugGrid">
				<text class="debug-text">网格: {{ gridWidth }}x{{ gridHeight }}</text>
				<text class="debug-text">路径: {{ path.length }}点</text>
				<text class="debug-text">起点: {{ startNode ? startNode.x + ',' + startNode.y : '无' }}</text>
				<text class="debug-text">终点: {{ targetNode ? targetNode.x + ',' + targetNode.y : '无' }}</text>
			</view>
		</view>
		
		<!-- 地图图例 -->
		<view class="map-legend">
			<view class="legend-item">
				<view class="legend-dot current"></view>
				<text class="legend-text">当前位置</text>
			</view>
			<view class="legend-item">
				<view class="legend-dot next"></view>
				<text class="legend-text">下一步节点</text>
			</view>
			<view class="legend-item">
				<view class="legend-dot destination"></view>
				<text class="legend-text">终点/诊室</text>
			</view>
			<view class="legend-item">
				<view class="legend-line path"></view>
				<text class="legend-text">规划路线</text>
			</view>
			<view class="legend-item">
				<view class="legend-block obstacle"></view>
				<text class="legend-text">房间/墙体</text>
			</view>
		</view>
		
		<!-- 加载提示 -->
		<view class="loading-overlay" v-if="loading">
			<text class="loading-text">正在加载地图...</text>
		</view>
	</view>
	</scroll-view>
</template>

<script>
const FLOOR_LAYOUTS = {
	1: {
		corridors: [
			{ x: 6, y: 18, width: 28, height: 4, color: 'rgba(255,255,255,0.85)' },
			{ x: 18, y: 6, width: 4, height: 24, color: 'rgba(255,255,255,0.85)' }
		],
		rooms: [
			{ x: 2, y: 22, width: 10, height: 6, label: '门诊大厅' },
			{ x: 28, y: 6, width: 8, height: 6, label: '检验科' },
			{ x: 30, y: 18, width: 8, height: 6, label: '放射科' }
		]
	},
	2: {
		corridors: [
			{ x: 6, y: 18, width: 28, height: 4, color: 'rgba(255,255,255,0.85)' },
			{ x: 6, y: 10, width: 28, height: 4, color: 'rgba(255,255,255,0.85)' }
		],
		rooms: [
			{ x: 2, y: 20, width: 8, height: 6, label: '外科诊区' },
			{ x: 30, y: 20, width: 8, height: 6, label: '专科诊区' },
			{ x: 18, y: 4, width: 8, height: 6, label: 'VIP候诊区' }
		]
	},
	3: {
		corridors: [
			{ x: 6, y: 16, width: 28, height: 4, color: 'rgba(255,255,255,0.85)' }
		],
		rooms: [
			{ x: 2, y: 20, width: 10, height: 6, label: '内科病房' },
			{ x: 28, y: 20, width: 10, height: 6, label: '骨科病房' },
			{ x: 18, y: 6, width: 8, height: 6, label: '医生办公室' }
		]
	}
}

import { getMapConfig, getTargetNode, scanQRCode, calculateNavigationPath, getNextStep } from '../../api/map.js'
import { getAppointmentDetail } from '../../api/appointment.js'

// 注意：pathfinding库需要通过npm安装
// 安装命令：npm install pathfinding
// 如果uni-app不支持npm包，可以使用CDN或内联代码
// 这里我们使用简化的A*算法实现
class SimpleAStar {
	constructor(grid) {
		this.grid = grid
		this.width = grid[0].length
		this.height = grid.length
	}
	
	findPath(startX, startY, endX, endY) {
		console.log('[A*算法] 开始寻路，起点:', startX, startY, '终点:', endX, endY)
		console.log('[A*算法] 网格尺寸:', this.width, 'x', this.height)
		
		// 检查起点和终点是否可通行
		if (this.grid[startY][startX] === 1) {
			console.error('[A*算法] 起点在障碍物上')
			return []
		}
		if (this.grid[endY][endX] === 1) {
			console.error('[A*算法] 终点在障碍物上')
			return []
		}
		
		const openList = [{ x: startX, y: startY, g: 0, h: 0, f: 0, parent: null }]
		const closedList = []
		const openMap = new Map()
		openMap.set(`${startX},${startY}`, openList[0])
		
		let iterations = 0
		const maxIterations = this.width * this.height * 2 // 防止无限循环
		
		while (openList.length > 0 && iterations < maxIterations) {
			iterations++
			// 找到f值最小的节点
			let currentIndex = 0
			for (let i = 1; i < openList.length; i++) {
				if (openList[i].f < openList[currentIndex].f) {
					currentIndex = i
				}
			}
			
			const current = openList.splice(currentIndex, 1)[0]
			openMap.delete(`${current.x},${current.y}`)
			closedList.push(current)
			
			// 到达目标
			if (current.x === endX && current.y === endY) {
				const path = []
				let node = current
				while (node) {
					path.unshift({ x: node.x, y: node.y })
					node = node.parent
				}
				console.log('[A*算法] 找到路径，长度:', path.length, '迭代次数:', iterations)
				return path
			}
			
			// 检查相邻节点（只允许上下左右4个方向，禁止对角线，避免视觉上“穿墙”）
			const neighbors = [
				{ x: current.x + 1, y: current.y },
				{ x: current.x - 1, y: current.y },
				{ x: current.x, y: current.y + 1 },
				{ x: current.x, y: current.y - 1 }
			]
			
			for (const neighbor of neighbors) {
				// 边界检查
				if (neighbor.x < 0 || neighbor.x >= this.width || 
					neighbor.y < 0 || neighbor.y >= this.height) {
					continue
				}
				
				// 障碍物检查
				if (this.grid[neighbor.y][neighbor.x] === 1) {
					continue
				}
				
				// 检查是否已在closed列表
				if (closedList.some(n => n.x === neighbor.x && n.y === neighbor.y)) {
					continue
				}
				
				// 计算g值（移动成本，4方向统一为1）
				const g = current.g + 1
				
				// 计算h值（启发式距离）
				const h = Math.abs(neighbor.x - endX) + Math.abs(neighbor.y - endY)
				
				// 计算f值
				const f = g + h
				
				// 检查是否已在open列表
				const existing = openMap.get(`${neighbor.x},${neighbor.y}`)
				if (existing) {
					if (g < existing.g) {
						existing.g = g
						existing.f = f
						existing.parent = current
					}
				} else {
					const newNode = { x: neighbor.x, y: neighbor.y, g, h, f, parent: current }
					openList.push(newNode)
					openMap.set(`${neighbor.x},${neighbor.y}`, newNode)
				}
			}
		}
		
		// 未找到路径
		console.error('[A*算法] 未找到路径，迭代次数:', iterations, 'openList长度:', openList.length)
		return []
	}
}

export default {
		data() {
		return {
			locationId: null,
			appointmentId: null,
			gridWidth: 40,
			gridHeight: 30,
			gridMatrix: null,
			nodes: [],
			targetNode: null,
			startNode: null,
			path: [],
			canvasWidth: 750,
			canvasHeight: 600,
			scaleX: 1,
			scaleY: 1,
			currentPathIndex: 0,
			animationTimer: null,
			locationTimer: null, // 定位定时器
			targetNodeName: '',
			remainingDistance: 0,
			showDebugGrid: true,
			loading: true,
			// 导航指引相关
			navigationSteps: [], // 导航步骤列表（从后端API获取）
			currentStepIndex: 0, // 当前步骤索引
			currentStep: null, // 当前步骤信息
			nextStep: null, // 下一步信息
			useRealNavigation: true, // 是否使用真实导航（基于map_edges）
			imageLoaded: false,
			showBackgroundImage: false, // 默认不显示背景图片（如果图片不存在会报错）
			// 楼层相关
			currentFloor: 1, // 当前楼层
			availableFloors: [1, 2, 3], // 可用楼层列表
			floorLayouts: FLOOR_LAYOUTS,
			currentNode: null, // 当前节点信息（包含楼层）
			// 真实定位相关
			isRealTimeMode: false, // 是否开启实时定位模式
			currentLocation: null, // 当前实际位置 {x, y} (整数网格坐标)
			currentLocationFloat: null, // 当前实际位置浮点数坐标 {x, y} (用于精确绘制)
			lastGpsLocation: null, // 上次GPS坐标 {lat, lng} (用于检测位置变化)
			locationHeading: 0, // 当前朝向角度（0-360度，0度表示正北）
			locationStatus: '', // 定位状态提示
			locationAccuracy: 0, // 定位精度（米）
			locationErrorShown: false, // 是否已显示定位错误提示（避免重复提示）
			compassListener: null, // 设备方向传感器监听器
			// 演示模式相关
			isDemoMode: false, // 是否开启演示模式
			demoMoveTimer: null, // 演示移动定时器
			demoPathIndex: 0, // 演示路径索引
			enableClickToMove: false, // 是否允许点击地图移动
			// 🎯 教室演示GPS坐标配置（超小范围调试模式：约1-2平方米）
			// 
			// 📍 实际GPS坐标：东经 116°20'19" (116.338611°) 北纬 39°57'0" (39.95°)
			//   北京市海淀区
			//   根据实际定位结果调整范围
			// 
			// 💡 动态地图范围：首次GPS定位时会自动以当前GPS坐标为中心设置范围
			//   各偏移0.00001度（约1.1米），总范围约2.2米x2.2米（约5平方米）
			//   适合在座位上精细调试，GPS坐标微小变化（几厘米）就能看到箭头明显移动
			//   0.00001度 ≈ 1.1米
			//   注意：如果GPS坐标超出范围，会自动更新mapBounds
			mapBounds: null, // 初始为null，第一次GPS定位时自动设置
			mapBoundsOffset: 0.00001  // 地图范围偏移量（约1.1米）
		}
	},
	onLoad(options) {
		try {
			console.log('导航页面加载，参数:', options)
			
			// 确保停止所有演示模式和定时器（添加安全检查）
			if (typeof this.stopDemoMode === 'function') {
				this.stopDemoMode()
			}
			if (typeof this.stopLocationTracking === 'function') {
				this.stopLocationTracking()
			}
			
			// 支持两种方式：直接传locationId，或传appointmentId
			if (options && options.locationId) {
				this.locationId = parseInt(options.locationId)
				if (typeof this.loadMapData === 'function') {
					this.loadMapData()
				}
			} else if (options && options.appointmentId) {
				this.appointmentId = parseInt(options.appointmentId)
				if (typeof this.loadAppointmentAndNavigate === 'function') {
					this.loadAppointmentAndNavigate()
				}
			} else {
				uni.showToast({
					title: '缺少必要参数',
					icon: 'none'
				})
				setTimeout(() => {
					uni.navigateBack()
				}, 1500)
			}
			
			// 提示用户扫码定位
			setTimeout(() => {
				uni.showModal({
					title: '开始导航',
					content: '请点击"📷 扫码定位"按钮，扫描医院里的二维码来定位当前位置，系统将自动为您规划到目的地的路径。',
					showCancel: false,
					confirmText: '知道了'
				})
			}, 1000)
		} catch (error) {
			console.error('导航页面加载失败:', error)
			uni.showToast({
				title: '页面加载失败',
				icon: 'none'
			})
		}
	},
	onReady() {
		// 获取canvas上下文
		this.ctx = uni.createCanvasContext('mapCanvas', this)
		
		// 获取Canvas实际尺寸
		const query = uni.createSelectorQuery().in(this)
		query.select('.map-container').boundingClientRect((rect) => {
			if (rect) {
				this.canvasWidth = rect.width || 750
				this.canvasHeight = rect.height || 600
				console.log('Canvas尺寸:', this.canvasWidth, this.canvasHeight)
			}
			// 延迟绘制，确保数据已加载
			setTimeout(() => {
				if (this.gridMatrix) {
					this.drawMap()
				}
			}, 500)
		}).exec()
	},
	onUnload() {
		// 清除所有定时器（真机调试只使用GPS定位）
		if (this.animationTimer) {
			clearInterval(this.animationTimer)
			this.animationTimer = null
		}
		if (this.locationTimer) {
			clearInterval(this.locationTimer)
			this.locationTimer = null
		}
		if (this.demoMoveTimer) {
			clearInterval(this.demoMoveTimer)
			this.demoMoveTimer = null
		}
		// 停止所有模式
		this.stopDemoMode()
		this.stopLocationTracking()
		// 重置动画索引（已禁用动画模式）
		this.currentPathIndex = 0
	},
	onHide() {
		// 页面隐藏时停止定位和演示
		this.stopLocationTracking()
		this.stopDemoMode()
	},
	onShow() {
		// 页面显示时，强制停止演示模式（真机调试只使用GPS定位）
		if (this.isDemoMode || this.demoMoveTimer) {
			console.log('[页面显示] 检测到演示模式在运行，强制停止')
			this.isDemoMode = false
			this.demoPathIndex = 0
			if (this.demoMoveTimer) {
				clearInterval(this.demoMoveTimer)
				this.demoMoveTimer = null
			}
		}
	},
	methods: {
		async loadAppointmentAndNavigate() {
			try {
				const response = await getAppointmentDetail(this.appointmentId)
				console.log('预约详情响应:', JSON.stringify(response, null, 2))
				
				if (response && response.code === '200' && response.data) {
					const appointment = response.data
					console.log('预约数据:', appointment)
					console.log('schedule对象:', appointment.schedule)
					
					// 从schedule中获取locationId
					let locationId = null
					if (appointment.schedule) {
						locationId = appointment.schedule.locationId
						console.log('从schedule获取locationId:', locationId)
					}
					
					// 如果schedule中没有，尝试从其他字段获取
					if (!locationId && appointment.locationId) {
						locationId = appointment.locationId
						console.log('从appointment获取locationId:', locationId)
					}
					
					if (locationId) {
						this.locationId = locationId
						console.log('✅ 成功获取locationId:', this.locationId)
						this.loadMapData()
					} else {
						console.error('❌ 预约信息中缺少locationId')
						console.log('预约数据详情:', {
							hasSchedule: !!appointment.schedule,
							scheduleKeys: appointment.schedule ? Object.keys(appointment.schedule) : [],
							appointmentKeys: Object.keys(appointment)
						})
						uni.showModal({
							title: '缺少诊室信息',
							content: '预约信息中缺少诊室位置，无法导航。请联系管理员。',
							showCancel: false,
							success: () => {
								uni.navigateBack()
							}
						})
					}
				} else {
					throw new Error('获取预约详情失败')
				}
			} catch (error) {
				console.error('加载预约信息失败:', error)
				uni.showModal({
					title: '加载失败',
					content: '无法获取预约信息，请稍后重试',
					showCancel: false,
					success: () => {
						uni.navigateBack()
					}
				})
			}
		},
		
	async loadMapData() {
		this.loading = true
		try {
			// 获取地图配置（传入当前楼层）
			const configResponse = await getMapConfig(this.currentFloor || 1)
			console.log('地图配置响应（完整）:', JSON.stringify(configResponse, null, 2))
				console.log('响应类型:', typeof configResponse)
				console.log('响应data:', configResponse?.data)
				console.log('响应data类型:', typeof configResponse?.data)
				
				// 处理不同的响应格式
				let config = null
				if (configResponse && configResponse.code === '200') {
					// 如果data是对象且有grid属性
					if (configResponse.data && configResponse.data.grid) {
						config = configResponse.data
					} 
					// 如果data本身就是配置对象（直接返回MapConfigResponse的情况）
					else if (configResponse.data && typeof configResponse.data === 'object' && Object.keys(configResponse.data).length > 0) {
						config = configResponse.data
					}
					// 如果整个响应就是配置对象（某些情况下Spring Boot直接返回对象）
					else if (configResponse.grid) {
						config = configResponse
					}
				}
				
				if (config && config.grid) {
					this.gridWidth = config.grid.width
					this.gridHeight = config.grid.height
					this.gridMatrix = config.grid.gridMatrix
					this.nodes = config.nodes || []
					
					console.log('地图数据加载成功:', {
						gridWidth: this.gridWidth,
						gridHeight: this.gridHeight,
						gridMatrixSize: this.gridMatrix ? this.gridMatrix.length : 0,
						nodesCount: this.nodes.length,
						nodes: this.nodes
					})
					
					// 如果nodes为空或只有一个，使用默认节点
					if (!this.nodes || this.nodes.length <= 1) {
						console.warn('后端返回的nodes数据不足，使用默认节点')
						this.nodes = [
							{nodeId: 1, name: "医院大门", x: 20, y: 29, locationId: null},
							{nodeId: 2, name: "分诊台", x: 20, y: 20, locationId: null},
							{nodeId: 3, name: "电梯口", x: 35, y: 20, locationId: null},
							{nodeId: 4, name: "内科诊室", x: 5, y: 10, locationId: 1},
							{nodeId: 5, name: "外科诊室", x: 5, y: 5, locationId: 2}
						]
					}
					
					// 获取目标节点
					await this.loadTargetNode()
				} else {
					console.error('地图配置响应格式错误:', configResponse)
					// 如果API失败，使用模拟数据
					console.warn('使用模拟数据')
					this.useMockData()
				}
			} catch (error) {
				console.error('加载地图数据失败:', error)
				uni.showToast({
					title: '加载地图失败',
					icon: 'none'
				})
			} finally {
				this.loading = false
			}
		},
		
		/**
		 * 使用模拟数据（当API失败时）
		 */
		useMockData() {
			console.log('初始化模拟地图数据')
			this.gridWidth = 40
			this.gridHeight = 30
			
			// 创建网格矩阵（全部初始化为通路）
			this.gridMatrix = Array(30).fill(0).map(() => Array(40).fill(0))
			
			// 设置障碍物（与后端MapService保持一致）
			// 左侧墙壁（x=0到x=3），留出通道y=8到y=12
			for (let y = 0; y < 30; y++) {
				for (let x = 0; x < 4; x++) {
					if (y < 8 || y > 12) {
						this.gridMatrix[y][x] = 1
					}
				}
			}
			
			// 右侧墙壁（x=36到x=39），留出通道y=18到y=22
			for (let y = 0; y < 30; y++) {
				for (let x = 36; x < 40; x++) {
					if (y < 18 || y > 22) {
						this.gridMatrix[y][x] = 1
					}
				}
			}
			
			// 中间横向墙壁（y=15到y=17，x=10到x=30）
			for (let y = 15; y < 18; y++) {
				for (let x = 10; x < 31; x++) {
					this.gridMatrix[y][x] = 1
				}
			}
			
			// 上方横向墙壁（y=0到y=2）
			for (let y = 0; y < 3; y++) {
				for (let x = 0; x < 40; x++) {
					this.gridMatrix[y][x] = 1
				}
			}
			
			// 下方横向墙壁（y=27到y=29），但起点(20,29)需要可通行
			for (let y = 27; y < 30; y++) {
				for (let x = 0; x < 40; x++) {
					if (!(y === 29 && x >= 18 && x <= 22)) { // 起点周围留出通道
						this.gridMatrix[y][x] = 1
					}
				}
			}
			
			// 中间纵向墙壁（x=15到x=17，y=5到y=12）
			for (let y = 5; y < 13; y++) {
				for (let x = 15; x < 18; x++) {
					this.gridMatrix[y][x] = 1
				}
			}
			
			// 确保关键节点位置是通路
			// Node 1: 医院大门 (20, 29)
			this.gridMatrix[29][20] = 0
			this.gridMatrix[28][20] = 0
			this.gridMatrix[29][19] = 0
			this.gridMatrix[29][21] = 0
			
			// Node 2: 分诊台 (20, 20)
			this.gridMatrix[20][20] = 0
			this.gridMatrix[20][19] = 0
			this.gridMatrix[20][21] = 0
			this.gridMatrix[19][20] = 0
			this.gridMatrix[21][20] = 0
			
			// Node 3: 电梯口 (35, 20)
			this.gridMatrix[20][35] = 0
			this.gridMatrix[20][34] = 0
			this.gridMatrix[20][36] = 0
			this.gridMatrix[19][35] = 0
			this.gridMatrix[21][35] = 0
			
			// Node 4: 内科诊室 (5, 10)
			this.gridMatrix[10][5] = 0
			this.gridMatrix[10][4] = 0
			this.gridMatrix[10][6] = 0
			this.gridMatrix[9][5] = 0
			this.gridMatrix[11][5] = 0
			
			// Node 5: 外科诊室 (5, 5)
			this.gridMatrix[5][5] = 0
			this.gridMatrix[5][4] = 0
			this.gridMatrix[5][6] = 0
			this.gridMatrix[4][5] = 0
			this.gridMatrix[6][5] = 0
			
			// 确保起点到终点有通路（创建一条主要通道）
			// 从起点(20,29)向上到(20,20)，然后向左到(5,20)，再向上到(5,10)
			
			// 1. 纵向通道：从起点(20,29)向上到(20,20)
			for (let y = 20; y <= 29; y++) {
				this.gridMatrix[y][20] = 0
				this.gridMatrix[y][19] = 0 // 加宽通道
				this.gridMatrix[y][21] = 0 // 加宽通道
			}
			
			// 2. 横向通道：从(20,20)向左到(5,20)，避开中间横向墙壁
			// 中间横向墙壁在y=15-17，x=10-30，所以我们在y=20这一行是安全的
			for (let x = 5; x <= 20; x++) {
				this.gridMatrix[20][x] = 0
				this.gridMatrix[19][x] = 0 // 加宽通道
				this.gridMatrix[21][x] = 0 // 加宽通道
			}
			
			// 3. 纵向通道：从(5,20)向上到(5,10)
			for (let y = 10; y <= 20; y++) {
				this.gridMatrix[y][5] = 0
				this.gridMatrix[y][4] = 0 // 加宽通道
				this.gridMatrix[y][6] = 0 // 加宽通道
			}
			
			// 4. 确保起点(20,29)周围至少有一个方向可通行
			// 起点上方
			if (this.gridMatrix[28][20] === 1) {
				this.gridMatrix[28][20] = 0
			}
			// 起点左侧
			if (this.gridMatrix[29][19] === 1) {
				this.gridMatrix[29][19] = 0
			}
			// 起点右侧
			if (this.gridMatrix[29][21] === 1) {
				this.gridMatrix[29][21] = 0
			}
			
			console.log('通道创建完成，验证连通性:')
			console.log('起点(20,29)可通行:', this.gridMatrix[29][20] === 0)
			console.log('起点上方(20,28)可通行:', this.gridMatrix[28][20] === 0)
			console.log('分诊台(20,20)可通行:', this.gridMatrix[20][20] === 0)
			console.log('终点(5,10)可通行:', this.gridMatrix[10][5] === 0)
			
		// ⚠️ 警告：使用模拟数据（仅用于开发测试）
		console.error('⚠️ 警告：后端API失败，使用模拟数据！这不应该在生产环境出现！')
		console.error('请检查：1. 后端服务是否启动  2. 数据库是否有节点数据  3. API地址是否正确')
		
		// 最小化的模拟节点（仅用于网格测试）
		this.nodes = [
			{nodeId: 1, name: "起点（模拟）", x: 20, y: 29, locationId: null},
			{nodeId: 2, name: "中转点（模拟）", x: 20, y: 20, locationId: null}
		]
		
		console.log('⚠️ 模拟数据初始化完成（这是临时数据，不是真实诊室）')
		
		// 设置默认起点（医院大门）
		if (!this.startNode) {
			this.startNode = { x: 20, y: 29 }
		}
		
		// 如果有locationId，尝试加载目标节点
		if (this.locationId) {
			// 异步调用，但不等待结果（避免阻塞）
			this.loadTargetNode().catch(err => {
				console.warn('加载目标节点失败（使用模拟数据时）:', err)
				// 如果加载失败，使用默认目标节点
				const defaultTarget = this.nodes.find(n => n.locationId === this.locationId) || this.nodes[0]
				if (defaultTarget) {
					this.targetNode = { x: defaultTarget.x, y: defaultTarget.y }
					this.targetNodeName = defaultTarget.name
					console.log('使用默认目标节点:', this.targetNodeName)
				}
			})
		}
		},
		
		async loadTargetNode() {
			try {
				const response = await getTargetNode(this.locationId)
				console.log('目标节点响应:', response)
				
				console.log('目标节点响应（完整）:', JSON.stringify(response, null, 2))
				
				// 处理不同的响应格式
				let nodeData = null
				if (response && response.code === '200') {
					if (response.data && response.data.nodeId) {
						nodeData = response.data
					} else if (response.nodeId) {
						nodeData = response
					}
				}
				
				if (nodeData) {
					this.targetNode = {
						x: nodeData.x,
						y: nodeData.y,
						nodeId: nodeData.nodeId,
						name: nodeData.name,
						locationId: nodeData.locationId
					}
					this.targetNodeName = this.targetNode.name || '诊室'
					console.log('✅ 成功获取目标节点:', this.targetNode)
			} else {
				// 如果API失败，尝试从nodes中找到对应的节点
				console.warn('⚠️ 目标节点API失败，尝试从节点列表查找')
				const foundNode = this.nodes.find(n => n.locationId === this.locationId)
				
				if (foundNode) {
					this.targetNode = foundNode
					this.targetNodeName = this.targetNode.name || '诊室'
					console.log('✅ 从节点列表找到目标节点:', this.targetNode)
				} else {
					// ❌ 无法找到目标节点，给出明确错误提示
					console.error('❌ 无法找到locationId=' + this.locationId + '的节点')
					console.error('当前nodes列表:', this.nodes)
					
					uni.showModal({
						title: '无法找到诊室',
						content: `无法找到诊室ID为${this.locationId}的位置信息。\n\n可能原因：\n1. 数据库中该诊室未配置地图节点\n2. 后端API返回数据异常\n\n请联系管理员配置该诊室的位置信息。`,
						showCancel: false,
						confirmText: '返回',
						success: () => {
							uni.navigateBack()
						}
					})
					return // 直接返回，不继续执行
				}
			}
				
				// 不在这里设置起点！起点只能通过扫码定位获得
				// 如果已经有起点（之前扫码过），才计算路径
				if (this.startNode && this.startNode.nodeId) {
					console.log('已有起点，计算路径:', { 起点: this.startNode, 终点: this.targetNode })
					// 计算路径
					this.calculatePath()
				} else {
					console.log('等待扫码定位当前位置...')
					// 提示用户扫码
					uni.showToast({
						title: '请先扫码定位当前位置',
						icon: 'none',
						duration: 2000
					})
				}
			} catch (error) {
				console.error('加载目标节点失败:', error)
				uni.showToast({
					title: '获取目标位置失败',
					icon: 'none'
				})
			}
		},
		
		async calculatePath() {
			// 检查必要数据
			if (!this.startNode || !this.startNode.nodeId) {
				console.error('❌ 无法计算路径：起点节点ID缺失', this.startNode)
				uni.showToast({
					title: '请先扫码定位当前位置',
					icon: 'none',
					duration: 2000
				})
				return
			}
			
			if (!this.targetNode || !this.targetNode.nodeId) {
				console.error('❌ 无法计算路径：终点节点ID缺失', this.targetNode)
				// 尝试重新加载目标节点
				if (this.locationId) {
					console.log('尝试重新加载目标节点...')
					await this.loadTargetNode()
					if (!this.targetNode || !this.targetNode.nodeId) {
						uni.showToast({
							title: '无法获取目的地信息',
							icon: 'none',
							duration: 2000
						})
						return
					}
				} else {
					uni.showToast({
						title: '请先设置目的地',
						icon: 'none',
						duration: 2000
					})
					return
				}
			}
			
			// 优先使用真实导航API（基于map_edges）
			let realNavigationSucceeded = false
			if (this.useRealNavigation) {
				try {
					console.log('🧭 使用真实导航API计算路径', {
						startNodeId: this.startNode.nodeId,
						startNodeName: this.startNode.name,
						endNodeId: this.targetNode.nodeId,
						endNodeName: this.targetNode.name
					})
					
					const response = await calculateNavigationPath(
						this.startNode.nodeId,
						this.targetNode.nodeId
					)
					
					if (response && response.code === '200' && response.data) {
						const pathData = response.data
						this.navigationSteps = pathData.steps || []
						this.currentStepIndex = 0
						
						if (this.navigationSteps.length === 0) {
							console.warn('⚠️ 后端返回路径为空，改用前端网格规划')
						} else {
							// 更新当前步骤和下一步
							this.currentStep = this.navigationSteps[0]
							this.nextStep = this.navigationSteps.length > 1 ? this.navigationSteps[1] : null
							
							console.log('✅ 导航路径获取成功', {
								总步数: this.navigationSteps.length,
								当前步骤: this.currentStep.instruction,
								下一步: this.nextStep ? this.nextStep.instruction : '无',
								总距离: pathData.totalDistance,
								总时间: pathData.totalTime
							})
							
							// 更新剩余距离和时间
							this.remainingDistance = Math.round(pathData.totalDistance || 0)
							this.totalTime = pathData.totalTime || 0
							
							// 根据导航步骤生成路径点（用于绘制）
							this.path = this.generatePathFromSteps(this.navigationSteps)
							
							// 验证路径是否生成成功
							if (!this.path || this.path.length === 0) {
								console.error('❌ 路径生成失败，steps:', this.navigationSteps)
								console.error('当前nodes列表:', this.nodes.map(n => ({ nodeId: n.nodeId, name: n.name, x: n.x, y: n.y })))
								// 如果路径生成失败，至少保证起点和终点在路径中
								if (this.startNode && this.targetNode) {
									this.path = [
										{ x: this.startNode.x, y: this.startNode.y },
										{ x: this.targetNode.x, y: this.targetNode.y }
									]
									console.warn('⚠️ 使用简化路径（起点到终点直线）')
								}
							}
							
							// 如果路径中有楼层变化，切换到对应楼层
							if (this.currentStep && this.currentStep.toFloor) {
								await this.switchFloor(this.currentStep.toFloor)
							}
							
							// 绘制地图
							this.$nextTick(() => {
								this.drawMap()
							})
							
							this.updateArrowHeading()
							
							realNavigationSucceeded = true
							
							uni.showToast({
								title: `路径规划成功，共${this.navigationSteps.length}步`,
								icon: 'success',
								duration: 2000
							})
						}
					} else {
						throw new Error('后端API返回错误：' + (response?.message || '未知错误'))
					}
				} catch (error) {
					console.error('真实导航API调用失败，降级到网格路径规划:', error)
					// 降级到原来的网格路径规划
				}
			}
			
			// 如果真实导航失败或没有返回步数，降级方案：使用网格路径规划
			if (realNavigationSucceeded) {
				return
			}
			if (!this.gridMatrix || !this.startNode || !this.targetNode) {
				console.error('缺少必要数据，无法计算路径', {
					hasGridMatrix: !!this.gridMatrix,
					hasStartNode: !!this.startNode,
					hasTargetNode: !!this.targetNode,
					startNode: this.startNode,
					targetNode: this.targetNode
				})
				return
			}
			
			// 验证起点和终点坐标
			if (this.startNode.x < 0 || this.startNode.x >= this.gridWidth ||
				this.startNode.y < 0 || this.startNode.y >= this.gridHeight) {
				console.error('起点坐标超出范围:', this.startNode)
				uni.showToast({
					title: '起点坐标无效',
					icon: 'none'
				})
				return
			}
			
			if (this.targetNode.x < 0 || this.targetNode.x >= this.gridWidth ||
				this.targetNode.y < 0 || this.targetNode.y >= this.gridHeight) {
				console.error('终点坐标超出范围:', this.targetNode)
				uni.showToast({
					title: '终点坐标无效',
					icon: 'none'
				})
				return
			}
			
			// 检查起点和终点是否在障碍物上
			if (this.gridMatrix[this.startNode.y][this.startNode.x] === 1) {
				console.warn('起点在障碍物上，尝试调整到最近的可通行位置')
				// 尝试找到最近的可通行位置
				const nearby = this.findNearestWalkable(this.startNode.x, this.startNode.y)
				if (nearby) {
					this.startNode = nearby
					console.log('调整后的起点:', this.startNode)
				}
			}
			
			if (this.gridMatrix[this.targetNode.y][this.targetNode.x] === 1) {
				console.warn('终点在障碍物上，尝试调整到最近的可通行位置')
				const nearby = this.findNearestWalkable(this.targetNode.x, this.targetNode.y)
				if (nearby) {
					this.targetNode = nearby
					console.log('调整后的终点:', this.targetNode)
				}
			}
			
			console.log('开始计算路径，起点:', this.startNode, '终点:', this.targetNode)
			console.log('网格尺寸:', this.gridWidth, 'x', this.gridHeight)
			console.log('起点是否可通行:', this.gridMatrix[this.startNode.y][this.startNode.x] === 0)
			console.log('终点是否可通行:', this.gridMatrix[this.targetNode.y][this.targetNode.x] === 0)
			
			// 检查起点周围的8个方向是否可通行
			const startX = this.startNode.x
			const startY = this.startNode.y
			const directions = [
				{dx: 0, dy: -1, name: '上'},
				{dx: 0, dy: 1, name: '下'},
				{dx: -1, dy: 0, name: '左'},
				{dx: 1, dy: 0, name: '右'},
				{dx: -1, dy: -1, name: '左上'},
				{dx: 1, dy: -1, name: '右上'},
				{dx: -1, dy: 1, name: '左下'},
				{dx: 1, dy: 1, name: '右下'}
			]
			console.log('起点周围可通行方向:')
			directions.forEach(dir => {
				const nx = startX + dir.dx
				const ny = startY + dir.dy
				if (nx >= 0 && nx < this.gridWidth && ny >= 0 && ny < this.gridHeight) {
					const walkable = this.gridMatrix[ny][nx] === 0
					console.log(`  ${dir.name}(${nx},${ny}): ${walkable ? '可通行' : '障碍物'}`)
				}
			})
			
			const astar = new SimpleAStar(this.gridMatrix)
			this.path = astar.findPath(
				this.startNode.x,
				this.startNode.y,
				this.targetNode.x,
				this.targetNode.y
			)
			
			console.log('网格路径计算完成，路径长度:', this.path.length)
			
			// 如果没有导航步骤，清空指引
			if (!this.navigationSteps || this.navigationSteps.length === 0) {
				this.currentStep = null
				this.nextStep = null
			}
			
			if (this.path.length === 0) {
				console.error('路径计算失败，可能原因：')
				console.error('1. 起点和终点之间没有通路')
				console.error('2. 起点或终点被障碍物包围')
				console.error('3. 网格数据有问题')
				uni.showToast({
					title: '无法找到路径，请检查地图配置',
					icon: 'none',
					duration: 3000
				})
				return
			}
			
			// 计算剩余距离（网格单位，假设每个网格1米）
			this.remainingDistance = this.path.length - 1
			
			// 如果还没有导航步骤（比如后端路径为空时），根据路径生成一个简单的导航步骤
			if (!this.navigationSteps || this.navigationSteps.length === 0) {
				console.log('生成前端降级导航步骤', {
					startNode: this.startNode,
					targetNode: this.targetNode,
					pathLength: this.path.length
				})
				
				this.navigationSteps = [{
					fromNodeId: this.startNode.nodeId,
					fromNodeName: this.startNode.name || '当前位置',
					toNodeId: this.targetNode.nodeId,
					toNodeName: this.targetNode.name || '目的地',
					distance: this.remainingDistance,
					walkTime: this.remainingDistance * 3,
					instruction: `沿绿色路线前往【${this.targetNode.name || '目的地'}】`,
					fromFloor: this.currentFloor,
					toFloor: this.targetNode.floorLevel || this.currentFloor,
					nodeType: 'ROOM'
				}]
				this.currentStepIndex = 0
				this.currentStep = this.navigationSteps[0]
				this.nextStep = null
				
				console.log('✅ 前端降级导航步骤已生成:', this.currentStep)
			}
			
			// 绘制地图和路径
			this.$nextTick(() => {
				this.drawMap()
			})

			this.updateArrowHeading()
		},

		updateArrowHeading() {
			if (!this.path || this.path.length < 2) {
				return
			}
			
			let currentPoint = this.currentLocation
				? { x: this.currentLocation.x, y: this.currentLocation.y }
				: this.path[0]
			let index = this.path.findIndex(p => p.x === currentPoint.x && p.y === currentPoint.y)
			if (index === -1) {
				index = 0
				currentPoint = this.path[0]
			}
			
			let nextIndex = index < this.path.length - 1 ? index + 1 : index
			if (nextIndex === index && this.path.length >= 2) {
				nextIndex = this.path.length - 1
				index = this.path.length - 2
			}
			
			const fromPoint = this.path[index]
			const toPoint = this.path[nextIndex]
			const dx = toPoint.x - fromPoint.x
			const dy = toPoint.y - fromPoint.y
			
			let angle = Math.atan2(dx, -dy) * 180 / Math.PI
			if (isNaN(angle)) {
				angle = 0
			}
			this.locationHeading = (angle + 360) % 360
		},

		activateArrowMode(gridX, gridY) {
			this.isRealTimeMode = true
			this.currentLocation = { x: gridX, y: gridY }
			this.currentLocationFloat = { x: gridX + 0.5, y: gridY + 0.5 }
			// 开启指南针监听，让箭头随手机方向旋转
			this.startCompassListener()
		},
		
		drawMap() {
			console.log('[绘制地图] ========== 开始绘制地图 ==========')
			console.log('[绘制地图] 检查条件:', {
				hasCtx: !!this.ctx,
				hasGridMatrix: !!this.gridMatrix,
				isRealTimeMode: this.isRealTimeMode,
				currentLocation: this.currentLocation,
				currentLocationFloat: this.currentLocationFloat,
				startNode: this.startNode
			})
			
			if (!this.ctx) {
				console.warn('[绘制地图] ❌ Canvas上下文未初始化')
				return
			}
			
			if (!this.gridMatrix) {
				console.warn('[绘制地图] ❌ 地图数据未加载')
				return
			}
			
			console.log('[绘制地图] ✅ 条件满足，开始绘制，实时模式:', this.isRealTimeMode, '当前位置:', JSON.stringify(this.currentLocation))
			console.log('开始绘制地图，Canvas尺寸:', this.canvasWidth, this.canvasHeight)
			console.log('网格尺寸:', this.gridWidth, this.gridHeight)
			
			const ctx = this.ctx
			const cellWidth = this.canvasWidth / this.gridWidth
			const cellHeight = this.canvasHeight / this.gridHeight
			
			console.log('单元格尺寸:', cellWidth, cellHeight)
			
			// 清空画布
			ctx.clearRect(0, 0, this.canvasWidth, this.canvasHeight)
			
			// 柔和背景
			const bgGradient = ctx.createLinearGradient(0, 0, this.canvasWidth, this.canvasHeight)
			bgGradient.addColorStop(0, '#f6fbff')
			bgGradient.addColorStop(1, '#ecf7ff')
			ctx.setFillStyle(bgGradient)
			ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight)
			
			// 绘制楼层预设布局（走廊/房间）
			this.drawFloorLayout(ctx, cellWidth, cellHeight)
			
			// 可选的网格线（仅在调试模式下显示）
			if (this.showDebugGrid) {
				ctx.setStrokeStyle('rgba(0, 0, 0, 0.05)')
				ctx.setLineWidth(1)
				for (let x = 0; x <= this.gridWidth; x++) {
					const px = x * cellWidth
					ctx.beginPath()
					ctx.moveTo(px, 0)
					ctx.lineTo(px, this.canvasHeight)
					ctx.stroke()
				}
				for (let y = 0; y <= this.gridHeight; y++) {
					const py = y * cellHeight
					ctx.beginPath()
					ctx.moveTo(0, py)
					ctx.lineTo(this.canvasWidth, py)
					ctx.stroke()
				}
			}
			
			// 绘制障碍/房间区域（淡色）
			ctx.setFillStyle('rgba(255, 180, 180, 0.25)')
			for (let y = 0; y < this.gridHeight; y++) {
				for (let x = 0; x < this.gridWidth; x++) {
					if (this.gridMatrix[y][x] === 1) {
						ctx.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight)
					}
				}
			}
			
			// 只绘制从当前位置到下一步节点的路径段（简化显示，避免整条路径太乱）
			let pathToDraw = []
			
			if (this.currentStep && this.currentStep.fromNodeId && this.currentStep.toNodeId) {
				// 只绘制当前这一步的路径
				const currentStepPath = this.generatePathFromSteps([this.currentStep])
				if (currentStepPath && currentStepPath.length > 0) {
					pathToDraw = currentStepPath
					console.log('绘制当前步骤路径，点数:', pathToDraw.length, '从', this.currentStep.fromNodeName, '到', this.currentStep.toNodeName)
				}
			} else if (this.path && this.path.length > 0) {
				// 如果没有当前步骤，回退到显示完整路径（但这种情况应该很少）
				pathToDraw = this.path
				console.log('绘制完整路径，路径点数:', pathToDraw.length)
			}
			
			if (pathToDraw && pathToDraw.length > 0) {
				const gradient = ctx.createLinearGradient(0, 0, this.canvasWidth, this.canvasHeight)
				gradient.addColorStop(0, '#52C41A')
				gradient.addColorStop(0.5, '#13C2C2')
				gradient.addColorStop(1, '#1890FF')
				ctx.setStrokeStyle(gradient)
				ctx.setLineWidth(5)
				const canSetShadow = typeof ctx.setShadow === 'function'
				if (canSetShadow) {
					ctx.setShadow(0, 0, 8, 'rgba(19, 194, 194, 0.35)')
				}
				ctx.beginPath()
				
				for (let i = 0; i < pathToDraw.length; i++) {
					const point = pathToDraw[i]
					const x = point.x * cellWidth + cellWidth / 2
					const y = point.y * cellHeight + cellHeight / 2
					
					if (i === 0) {
						ctx.moveTo(x, y)
					} else {
						ctx.lineTo(x, y)
					}
				}
				
				ctx.stroke()
				if (canSetShadow) {
					ctx.setShadow(0, 0, 0, 'rgba(0,0,0,0)')
				}
			} else {
				console.warn('没有路径数据可绘制')
			}
			
			// ========= 绘制关键节点和可视化辅助元素 =========
			
			// 高亮“下一步要到达的节点”（当前指引的目标点）
			// 但不要和箭头位置重合
			if (this.currentStep && this.currentStep.toNodeId && this.nodes && this.nodes.length > 0) {
				const nextNode = this.nodes.find(n => n.nodeId === this.currentStep.toNodeId)
				if (nextNode && nextNode.x !== undefined && nextNode.y !== undefined) {
					// 检查是否和当前箭头位置重合
					let isOverlapping = false
					if (this.isRealTimeMode && this.currentLocation) {
						const dx = Math.abs(nextNode.x - this.currentLocation.x)
						const dy = Math.abs(nextNode.y - this.currentLocation.y)
						// 如果距离小于2个网格单位，认为是重合
						if (dx < 2 && dy < 2) {
							isOverlapping = true
						}
					}
					
					// 只有不重合时才画黄圈
					if (!isOverlapping) {
						const nx = nextNode.x * cellWidth + cellWidth / 2
						const ny = nextNode.y * cellHeight + cellHeight / 2
						
						// 使用黄色描边圈出下一步节点
						ctx.setStrokeStyle('#FAAD14')
						ctx.setLineWidth(3)
						ctx.beginPath()
						ctx.arc(nx, ny, 14, 0, Math.PI * 2)
						ctx.stroke()
					}
				}
			}
			
			// 在地图上标出少量关键节点名称（防止文字重叠）
			if (this.nodes && this.nodes.length > 0) {
				const focusIds = new Set()
				if (this.targetNode && this.targetNode.nodeId) focusIds.add(this.targetNode.nodeId)
				if (this.currentNode && this.currentNode.nodeId) focusIds.add(this.currentNode.nodeId)
				if (this.currentStep && this.currentStep.toNodeId) focusIds.add(this.currentStep.toNodeId)
				if (this.currentStep && this.currentStep.fromNodeId) focusIds.add(this.currentStep.fromNodeId)
				if (this.startNode && this.startNode.nodeId) focusIds.add(this.startNode.nodeId)
				
				ctx.setFontSize(11)
				
				this.nodes.forEach(node => {
					if (node.x === undefined || node.y === undefined) return
					
					const name = node.name || ''
					const isFocus = focusIds.has(node.nodeId)
					const isAnchor = !isFocus && name && (
						name.includes('门诊大厅') ||
						name.includes('分诊台') ||
						name.includes('电梯') ||
						name.includes('楼梯')
					)
					
					// 只绘制当前相关节点 + 少量核心锚点，避免全屏文字
					if (!isFocus && !isAnchor) return
					
					const cx = node.x * cellWidth + cellWidth / 2
					const cy = node.y * cellHeight + cellHeight / 2
					
					const label = name
					const textWidth = label.length * 11
					const paddingX = 6
					const paddingY = 3
					const bw = textWidth + paddingX * 2
					const bh = 18
					const bx = cx - bw / 2
					const by = cy - 24
					
					// 背景气泡
					ctx.setFillStyle(isFocus ? 'rgba(0, 0, 0, 0.75)' : 'rgba(255, 255, 255, 0.9)')
					ctx.fillRect(bx, by, bw, bh)
					
					// 文本
					ctx.setFillStyle(isFocus ? '#FFFFFF' : '#333333')
					ctx.setTextAlign('center')
					ctx.fillText(label, cx, by + bh - 5)
					ctx.setTextAlign('left')
				})
			}
			
			// 箭头将在最后绘制（在所有元素之后），确保在最上层
			
			// 绘制起点（实时定位模式下不显示起点，只显示箭头）
			if (this.startNode && !this.isRealTimeMode) {
				// 非实时定位模式：绘制蓝色圆点作为起点
				const centerX = this.startNode.x * cellWidth + cellWidth / 2
				const centerY = this.startNode.y * cellHeight + cellHeight / 2
				ctx.setFillStyle('#1890FF')
				ctx.beginPath()
				ctx.arc(centerX, centerY, 8, 0, Math.PI * 2)
				ctx.fill()
				
				ctx.setFontSize(12)
				ctx.setTextAlign('center')
				ctx.setFillStyle('#0b4991')
				ctx.fillText('起点', centerX, centerY - 12)
				ctx.setTextAlign('left')
			}
			
			// 绘制终点
			if (this.targetNode) {
				const targetX = this.targetNode.x * cellWidth + cellWidth / 2
				const targetY = this.targetNode.y * cellHeight + cellHeight / 2
				ctx.setFillStyle('#FF4D4F')
				ctx.beginPath()
				ctx.arc(
					targetX,
					targetY,
					10,
					0,
					Math.PI * 2
				)
				ctx.fill()
				
				ctx.setFontSize(12)
				ctx.setTextAlign('center')
				ctx.setFillStyle('#B50B27')
				ctx.fillText(`终点 · ${this.targetNode.name || ''}`, targetX, targetY - 14)
				ctx.setTextAlign('left')
			}
			
			// 计算到终点的距离（在绘制箭头之前）
			if (this.isRealTimeMode && this.currentLocation && this.path && this.path.length > 0) {
				// 找到当前位置在路径上的最近点
				let minDist = Infinity
				let nearestIndex = 0
				for (let i = 0; i < this.path.length; i++) {
					const dist = Math.abs(this.path[i].x - this.currentLocation.x) + 
								Math.abs(this.path[i].y - this.currentLocation.y)
					if (dist < minDist) {
						minDist = dist
						nearestIndex = i
					}
				}
				this.remainingDistance = this.path.length - nearestIndex - 1
			}
			
			// 最后绘制实时GPS定位的当前位置箭头（确保在最上层，像苹果地图一样）
			// 使用浮点数坐标绘制，让箭头平滑移动
			console.log('[绘制箭头] 检查条件:', {
				isRealTimeMode: this.isRealTimeMode,
				currentLocationFloat: this.currentLocationFloat,
				currentLocation: this.currentLocation,
				startNode: this.startNode
			})
			
			if (this.isRealTimeMode) {
				// 实时定位模式：优先使用浮点数坐标，如果没有则使用整数坐标，如果都没有则使用起点
				let locationToDraw = null
				if (this.currentLocationFloat) {
					locationToDraw = this.currentLocationFloat
					console.log('[绘制箭头] 使用浮点数坐标')
				} else if (this.currentLocation) {
					// 如果没有浮点数坐标，使用整数坐标 + 0.5（网格中心）
					locationToDraw = { x: this.currentLocation.x + 0.5, y: this.currentLocation.y + 0.5 }
					console.log('[绘制箭头] 使用整数坐标+0.5')
				} else if (this.startNode) {
					// 如果还没有GPS位置，使用起点位置（临时显示）
					locationToDraw = { x: this.startNode.x + 0.5, y: this.startNode.y + 0.5 }
					console.log('[绘制箭头] 使用起点坐标')
				}
				
				if (locationToDraw) {
					console.log('[绘制箭头] ✅ 实时定位模式，绘制箭头，位置:', JSON.stringify(locationToDraw), '朝向:', this.locationHeading.toFixed(1) + '°')
					// 实时GPS定位：绘制带方向的箭头
					// 使用浮点数坐标，让箭头平滑移动（像苹果地图一样）
					const centerX = locationToDraw.x * cellWidth
					const centerY = locationToDraw.y * cellHeight
				
				console.log('[绘制箭头] 计算的中心坐标:', { 
					centerX, 
					centerY, 
					cellWidth, 
					cellHeight,
					浮点坐标: locationToDraw,
					整数坐标: this.currentLocation
				})
				
				// 先绘制定位精度圆圈（在箭头下方）
				if (this.locationAccuracy > 0) {
					ctx.setStrokeStyle('rgba(255, 77, 79, 0.3)')
					ctx.setLineWidth(2)
					ctx.beginPath()
					// 将精度（米）转换为像素（假设1米=10像素）
					const radius = Math.min(this.locationAccuracy * 10, Math.min(cellWidth, cellHeight) * 5)
					ctx.arc(centerX, centerY, radius, 0, Math.PI * 2)
					ctx.stroke()
				}
				
				ctx.save() // 保存当前状态
				
				// 移动到中心点并旋转画布
				ctx.translate(centerX, centerY)
				// 将朝向角度转换为画布旋转角度（画布0度向右，需要转换为地图坐标系）
				// heading是0-360度，0度表示正北（向上），需要转换为画布坐标系（0度向右）
				const rotationAngle = (this.locationHeading - 90) * Math.PI / 180
				ctx.rotate(rotationAngle)
				
				// 绘制箭头（指向正上方，旋转后会指向正确方向）
				// 增大箭头尺寸，让它更明显
				const arrowSize = 20 // 从12改为20，更大更明显
				
				// 绘制箭头主体（红色三角形）
				ctx.setFillStyle('#FF4D4F') // 红色
				ctx.beginPath()
				ctx.moveTo(0, -arrowSize) // 箭头顶点（向上）
				ctx.lineTo(-arrowSize * 0.7, arrowSize * 0.4) // 左下
				ctx.lineTo(0, arrowSize * 0.2) // 中心点
				ctx.lineTo(arrowSize * 0.7, arrowSize * 0.4) // 右下
				ctx.closePath()
				ctx.fill()
				
				// 绘制箭头边框（黑色，更明显）
				ctx.setStrokeStyle('#000000')
				ctx.setLineWidth(2)
				ctx.beginPath()
				ctx.moveTo(0, -arrowSize)
				ctx.lineTo(-arrowSize * 0.7, arrowSize * 0.4)
				ctx.lineTo(0, arrowSize * 0.2)
				ctx.lineTo(arrowSize * 0.7, arrowSize * 0.4)
				ctx.closePath()
				ctx.stroke()
				
				// 绘制箭头中心圆点（白色，更明显）
				ctx.setFillStyle('#FFFFFF')
				ctx.setStrokeStyle('#000000')
				ctx.setLineWidth(1)
				ctx.beginPath()
				ctx.arc(0, 0, 6, 0, Math.PI * 2) // 从4改为6，更大
				ctx.fill()
				ctx.stroke()
				
				ctx.restore() // 恢复状态
				
				ctx.setFontSize(12)
				ctx.setTextAlign('center')
				ctx.setFillStyle('#FF4D4F')
				ctx.fillText('当前位置', centerX, centerY - arrowSize - 6)
				ctx.setTextAlign('left')
				
					console.log('[绘制箭头] 箭头绘制完成，位置:', { centerX, centerY }, '尺寸:', arrowSize, '朝向:', this.locationHeading.toFixed(1) + '°')
				} else {
					console.warn('[绘制箭头] ⚠️ 实时定位模式已开启，但还没有位置数据')
				}
			}
			
			ctx.draw(false, () => {
				console.log('Canvas绘制完成')
			})
		},
		
		drawFloorLayout(ctx, cellWidth, cellHeight) {
			if (!this.floorLayouts) return
			const layout = this.floorLayouts[this.currentFloor]
			if (!layout) return
			
			ctx.save()
			
			if (layout.corridors) {
				layout.corridors.forEach(zone => {
					ctx.setFillStyle(zone.color || 'rgba(255,255,255,0.9)')
					ctx.fillRect(
						zone.x * cellWidth,
						zone.y * cellHeight,
						zone.width * cellWidth,
						zone.height * cellHeight
					)
				})
			}
			
			if (layout.rooms) {
				layout.rooms.forEach(room => {
					ctx.setFillStyle(room.color || 'rgba(255,255,255,0.65)')
					ctx.fillRect(
						room.x * cellWidth,
						room.y * cellHeight,
						room.width * cellWidth,
						room.height * cellHeight
					)
					if (room.label) {
						ctx.setFontSize(12)
						ctx.setTextAlign('center')
						ctx.setFillStyle('rgba(0, 0, 0, 0.45)')
						ctx.fillText(
							room.label,
							(room.x + room.width / 2) * cellWidth,
							(room.y + room.height / 2) * cellHeight + 4
						)
						ctx.setTextAlign('left')
					}
				})
			}
			
			ctx.restore()
		},
		
		// 真机调试：已禁用动画模式，只使用GPS定位
		startAnimation() {
			// 已禁用，不再使用模拟动画
			console.warn('[动画] 动画模式已禁用，真机调试只使用GPS定位')
			if (this.animationTimer) {
				clearInterval(this.animationTimer)
				this.animationTimer = null
			}
			this.currentPathIndex = 0
		},
		
		restartAnimation() {
			// 已禁用，不再使用模拟动画
			if (this.animationTimer) {
				clearInterval(this.animationTimer)
				this.animationTimer = null
			}
			this.currentPathIndex = 0
		},
		
		toggleAnimation() {
			// 已禁用，不再使用模拟动画
			if (this.animationTimer) {
				clearInterval(this.animationTimer)
				this.animationTimer = null
			}
		},
		
		toggleDebugMode() {
			this.showDebugGrid = !this.showDebugGrid
			this.drawMap()
		},
		
		/**
		 * 切换演示模式 - 自动沿路径移动
		 */
		toggleDemoMode() {
			// 真机调试模式：完全禁用演示模式，只使用GPS定位
			uni.showToast({
				title: '演示模式已禁用，请使用GPS定位',
				icon: 'none',
				duration: 2000
			})
			// 强制停止演示模式
			this.isDemoMode = false
			this.demoPathIndex = 0
			if (this.demoMoveTimer) {
				clearInterval(this.demoMoveTimer)
				this.demoMoveTimer = null
			}
		},
		
		/**
		 * 开始演示模式 - 自动沿路径移动
		 */
		startDemoMode() {
			// 真机调试模式：完全禁用演示模式，只使用GPS定位
			console.warn('[演示模式] 演示模式已被禁用，真机调试只使用GPS定位')
			this.isDemoMode = false
			this.demoPathIndex = 0
			if (this.demoMoveTimer) {
				clearInterval(this.demoMoveTimer)
				this.demoMoveTimer = null
			}
			uni.showToast({
				title: '演示模式已禁用，请使用GPS定位',
				icon: 'none',
				duration: 2000
			})
			return
			
			// 以下代码已被禁用（真机调试不使用演示模式）
			/*
			// 如果正在实时定位，不允许开启演示模式
			if (this.isRealTimeMode) {
				uni.showToast({
					title: '请先停止实时定位',
					icon: 'none',
					duration: 2000
				})
				return
			}
			
			if (!this.path || this.path.length === 0) {
				uni.showToast({
					title: '请先计算路径',
					icon: 'none'
				})
				return
			}
			
			// 确保实时定位已停止（演示模式和实时定位不能同时开启）
			if (this.isRealTimeMode) {
				console.warn('[演示模式] 检测到实时定位在运行，强制停止')
				this.stopLocationTracking()
			}
			
			// 设置起点为路径第一个点
			if (this.path.length > 0) {
				this.demoPathIndex = 0
				this.currentLocation = { x: this.path[0].x, y: this.path[0].y }
				this.startNode = this.currentLocation
				// ⚠️ 重要：演示模式不应该设置 isRealTimeMode = true
				this.isRealTimeMode = false  // 演示模式不是实时定位
				this.isDemoMode = true
				this.locationStatus = '演示模式：自动移动中...'
				
				// 每1秒移动到下一个路径点
				this.demoMoveTimer = setInterval(() => {
					// 如果实时定位被开启了，立即停止演示
					if (this.isRealTimeMode) {
						console.warn('[演示模式] 检测到实时定位已开启，停止演示')
						this.stopDemoMode()
						return
					}
					
					if (this.demoPathIndex < this.path.length - 1) {
						this.demoPathIndex++
						const nextPoint = this.path[this.demoPathIndex]
						this.currentLocation = { x: nextPoint.x, y: nextPoint.y }
						this.startNode = this.currentLocation
						
						// 重新计算剩余路径
						this.calculatePath()
						this.updateRemainingDistance()
						
						// 更新状态
						const progress = Math.floor((this.demoPathIndex / this.path.length) * 100)
						this.locationStatus = `演示模式：移动中 ${progress}%`
						
						// 重绘地图
						this.drawMap()
					} else {
						// 到达终点
						this.stopDemoMode()
						uni.showToast({
							title: '已到达目的地！',
							icon: 'success',
							duration: 2000
						})
					}
				}, 1000) // 每1秒移动一步
				
				uni.showToast({
					title: '演示已开始，自动移动中',
					icon: 'success',
					duration: 2000
				})
			}
			*/
		},
		
		// 真机调试：完全禁用演示模式
		// startDemoMode() {
		// 	console.warn('[演示模式] 演示模式已被禁用，真机调试只使用GPS定位')
		// 	this.isDemoMode = false
		// 	this.demoPathIndex = 0
		// 	if (this.demoMoveTimer) {
		// 		clearInterval(this.demoMoveTimer)
		// 		this.demoMoveTimer = null
		// 	}
		// 	return
		// },
		
		/**
		 * 停止演示模式
		 */
		stopDemoMode() {
			console.log('[演示模式] 停止演示模式，当前状态:', {
				isDemoMode: this.isDemoMode,
				hasTimer: !!this.demoMoveTimer,
				timerId: this.demoMoveTimer
			})
			
			// 强制停止所有演示相关定时器
			if (this.demoMoveTimer) {
				clearInterval(this.demoMoveTimer)
				this.demoMoveTimer = null
				console.log('[演示模式] 已清除演示定时器')
			}
			
			// 重置演示状态
			this.isDemoMode = false
			this.demoPathIndex = 0
			
			// 清除演示状态提示
			if (this.locationStatus && this.locationStatus.includes('演示模式')) {
				this.locationStatus = ''
			}
			
			// 验证是否真的停止了
			if (this.isDemoMode || this.demoMoveTimer) {
				console.error('[演示模式] ⚠️ 警告：演示模式未完全停止！')
			} else {
				console.log('[演示模式] ✅ 演示模式已完全停止')
			}
		},
		
		handleImageError() {
			console.warn('背景图片加载失败，将只显示canvas绘制内容')
			this.imageLoaded = false
			this.showBackgroundImage = false // 隐藏背景图片
		},
		
		handleImageLoad() {
			console.log('背景图片加载成功')
			this.imageLoaded = true
		},
		
		handleTouchStart(e) {
			if (this.enableClickToMove) {
				this.handleMapClick(e)
			}
		},
		
		handleTouchMove(e) {
			// 可以添加触摸交互
		},
		
		handleTouchEnd(e) {
			if (this.enableClickToMove) {
				this.handleMapClick(e)
			}
		},
		
		/**
		 * 处理地图点击事件 - 设置当前位置（用于GPS信号不好时手动定位）
		 */
		handleMapClick(e) {
			if (!this.enableClickToMove) return
			
			// 如果开启了实时GPS定位，点击地图会停止GPS，改为手动模式
			if (this.isRealTimeMode) {
				this.stopLocationTracking()
			}
			
			// 获取点击位置相对于canvas的坐标
			const query = uni.createSelectorQuery().in(this)
			query.select('.map-canvas').boundingClientRect((rect) => {
				if (!rect) return
				
				const touch = e.touches ? e.touches[0] : e.changedTouches ? e.changedTouches[0] : null
				if (!touch) return
				
				// 计算点击位置相对于canvas的坐标
				const x = touch.clientX - rect.left
				const y = touch.clientY - rect.top
				
				// 转换为网格坐标
				const cellWidth = this.canvasWidth / this.gridWidth
				const cellHeight = this.canvasHeight / this.gridHeight
				const gridX = Math.floor(x / cellWidth)
				const gridY = Math.floor(y / cellHeight)
				
				// 检查是否可通行
				if (gridX >= 0 && gridX < this.gridWidth && 
					gridY >= 0 && gridY < this.gridHeight &&
					this.gridMatrix && this.gridMatrix[gridY][gridX] === 0) {
					
					// 设置当前位置（整数坐标和浮点数坐标）
					this.currentLocation = { x: gridX, y: gridY }
					// 手动点击时，浮点数坐标就是网格中心（整数坐标 + 0.5）
					this.currentLocationFloat = { x: gridX + 0.5, y: gridY + 0.5 }
					this.startNode = this.currentLocation
					
					// 查找最近的节点，显示位置名称
					const nearestNodeName = this.findNearestNodeName({ x: gridX, y: gridY })
					if (nearestNodeName) {
						this.locationStatus = `当前位置: ${nearestNodeName}`
					} else {
						this.locationStatus = `手动定位: (${gridX}, ${gridY})`
					}
					
					// 重新计算路径
					this.calculatePath()
					
					// 更新距离
					this.updateRemainingDistance()
					
					// 立即重绘
					this.drawMap()
					
					// 不显示toast，避免干扰
				} else {
					uni.showToast({
						title: '该位置不可通行',
						icon: 'none',
						duration: 1000
					})
				}
			}).exec()
		},
		
		/**
		 * 更新剩余距离
		 */
		updateRemainingDistance() {
			if (!this.path || this.path.length === 0) {
				this.remainingDistance = 0
				return
			}
			
			if (this.isRealTimeMode && this.currentLocation) {
				// 实时定位模式：找到当前位置在路径上的最近点
				let minDist = Infinity
				let nearestIndex = 0
				for (let i = 0; i < this.path.length; i++) {
					const dist = Math.abs(this.path[i].x - this.currentLocation.x) + 
								Math.abs(this.path[i].y - this.currentLocation.y)
					if (dist < minDist) {
						minDist = dist
						nearestIndex = i
					}
				}
				this.remainingDistance = this.path.length - nearestIndex - 1
			} else {
				// 默认：使用路径长度（真机调试不使用动画模式）
				this.remainingDistance = this.path.length - 1
			}
		},
		
		/**
		 * 切换点击移动模式
		 */
		toggleClickToMove() {
			this.enableClickToMove = !this.enableClickToMove
			if (this.enableClickToMove) {
				uni.showToast({
					title: '点击地图设置位置',
					icon: 'none',
					duration: 2000
				})
			}
		},
		
		/**
		 * 找到最近的可通行位置
		 */
		findNearestWalkable(x, y) {
			const maxRadius = 5 // 最大搜索半径
			for (let radius = 1; radius <= maxRadius; radius++) {
				for (let dx = -radius; dx <= radius; dx++) {
					for (let dy = -radius; dy <= radius; dy++) {
						const nx = x + dx
						const ny = y + dy
						if (nx >= 0 && nx < this.gridWidth && 
							ny >= 0 && ny < this.gridHeight &&
							this.gridMatrix[ny][nx] === 0) {
							return { x: nx, y: ny }
						}
					}
				}
			}
			return null
		},
		
		/**
		 * 根据网格坐标查找最近的节点，返回节点名称
		 * @param {Object} gridPos 网格坐标 {x, y}
		 * @returns {String} 节点名称，如果找不到则返回null
		 */
		findNearestNodeName(gridPos) {
			if (!gridPos || !this.nodes || this.nodes.length === 0) {
				return null
			}
			
			let minDist = Infinity
			let nearestNode = null
			
			for (const node of this.nodes) {
				if (node.x === undefined || node.y === undefined) continue
				
				// 计算欧几里得距离
				const dist = Math.sqrt(
					Math.pow(node.x - gridPos.x, 2) + 
					Math.pow(node.y - gridPos.y, 2)
				)
				
				if (dist < minDist) {
					minDist = dist
					nearestNode = node
				}
			}
			
			// 如果最近节点距离小于3个网格单位（约3米），认为是在该节点附近
			if (nearestNode && minDist < 3) {
				console.log('[定位] 找到最近节点:', {
					节点名称: nearestNode.name,
					节点坐标: { x: nearestNode.x, y: nearestNode.y },
					当前位置: gridPos,
					距离: minDist.toFixed(2) + '个网格单位'
				})
				return nearestNode.name
			}
			
			return null
		},
		
		/**
		 * 获取当前位置（GPS定位）
		 */
		async getCurrentLocation() {
			// 如果已经在定位中，不显示"正在定位"提示，避免闪烁
			if (!this.isRealTimeMode) {
				this.locationStatus = '正在定位...'
			}
			
			console.log('[定位] 开始获取GPS位置...', {
				isRealTimeMode: this.isRealTimeMode,
				hasTimer: !!this.locationTimer
			})
			
			try {
				// 请求定位权限
				console.log('[定位] 检查定位权限...')
				const res = await uni.getSetting()
				console.log('[定位] 权限检查结果:', res.authSetting)
				
				if (!res.authSetting['scope.userLocation']) {
					console.log('[定位] 未授权，请求授权...')
					// 未授权，请求授权
					const authRes = await uni.authorize({
						scope: 'scope.userLocation'
					})
					console.log('[定位] 授权结果:', authRes)
				} else {
					console.log('[定位] 已有定位权限')
				}
				
				// 获取当前位置 - 使用高精度定位
				console.log('[定位] 调用uni.getLocation...')
				const location = await new Promise((resolve, reject) => {
					uni.getLocation({
						type: 'gcj02', // 返回可以用于uni.openLocation的经纬度
						altitude: false,
						geocode: false,
						highAccuracyExpireTime: 10000, // 增加到10秒超时（真机可能较慢）
						highAccuracy: true, // 开启高精度定位
						timeout: 15000, // 总超时时间15秒
						success: (res) => {
							console.log('[定位] getLocation success:', res)
							resolve(res)
						},
						fail: (err) => {
							console.error('[定位] getLocation fail:', err)
							reject(err)
						}
					})
				})
				
				console.log('✅ GPS定位成功:', location)
				
				// 将GPS坐标转换为地图网格坐标（整数，用于路径规划）
				const gridPos = this.gpsToGrid(location.latitude, location.longitude)
				// 将GPS坐标转换为浮点数网格坐标（用于精确绘制箭头）
				const gridPosFloat = this.gpsToGridFloat(location.latitude, location.longitude)
				
				if (gridPos && gridPosFloat) {
					// 强制停止演示模式（GPS定位时不应该有演示模式）
					if (this.isDemoMode || this.demoMoveTimer) {
						console.warn('[GPS定位] 检测到演示模式在运行，强制停止')
						this.stopDemoMode()
					}
					
					// 检查GPS位置是否发生变化（使用GPS坐标而不是网格坐标，更精确）
					const oldGpsPos = this.lastGpsLocation
					// GPS坐标变化阈值：超小范围模式，降低到极小值（约0.001毫米）
					// 几乎任何GPS坐标变化都会被检测到，适合1-2平方米范围的精细调试
					// 注意：真实GPS在室内精度通常为3-10米，所以坐标可能一直不变
					const gpsChanged = !oldGpsPos || 
						Math.abs(oldGpsPos.lat - location.latitude) > 0.0000000001 || 
						Math.abs(oldGpsPos.lng - location.longitude) > 0.0000000001
					
					// 检查浮点数网格坐标是否发生变化（用于绘制）
					// 超小范围模式：降低阈值到0.00001，让更小的位置变化也能被检测到
					const oldFloatPos = this.currentLocationFloat
					const floatPosChanged = !oldFloatPos || 
						Math.abs(oldFloatPos.x - gridPosFloat.x) > 0.00001 || 
						Math.abs(oldFloatPos.y - gridPosFloat.y) > 0.00001
					
					// 记录GPS坐标变化（用于调试）
					if (oldGpsPos) {
						const latDiff = Math.abs(oldGpsPos.lat - location.latitude)
						const lngDiff = Math.abs(oldGpsPos.lng - location.longitude)
						const latDiffMeters = latDiff * 111000
						const lngDiffMeters = lngDiff * 111000 * Math.cos(location.latitude * Math.PI / 180)
						
						console.log('[定位] GPS坐标变化检测:', {
							旧GPS: { lat: oldGpsPos.lat, lng: oldGpsPos.lng },
							新GPS: { lat: location.latitude, lng: location.longitude },
							纬度变化: latDiffMeters.toFixed(4) + '米 (' + latDiff.toFixed(10) + '度)',
							经度变化: lngDiffMeters.toFixed(4) + '米 (' + lngDiff.toFixed(10) + '度)',
							GPS变化: gpsChanged,
							浮点坐标变化: floatPosChanged,
							旧浮点坐标: oldFloatPos,
							新浮点坐标: gridPosFloat,
							精度: location.accuracy + '米',
							提示: latDiffMeters < 0.01 && lngDiffMeters < 0.01 ? '⚠️ GPS坐标几乎没变，可能是室内GPS精度问题' : ''
						})
						
						// 如果GPS坐标几乎没变（小于1厘米），提示用户
						if (latDiffMeters < 0.01 && lngDiffMeters < 0.01 && this.isRealTimeMode) {
							console.warn('[定位] ⚠️ GPS坐标几乎没有变化，可能是：')
							console.warn('   1. 室内GPS信号不好，精度不够')
							console.warn('   2. 手机真的没移动（静止状态）')
							console.warn('   3. 建议使用"扫码定位"或"选择位置"功能')
						}
					} else {
						// 首次定位
						console.log('[定位] 首次GPS定位:', {
							GPS: { lat: location.latitude, lng: location.longitude },
							精度: location.accuracy + '米',
							网格坐标: gridPos,
							浮点坐标: gridPosFloat
						})
					}
					
					// 更新朝向：优先使用设备朝向（让箭头跟随手机转动），如果没有则使用移动方向
					let headingUpdated = false
					
					// 1. 优先使用设备朝向（location.heading），如果存在且有效
					if (location.heading !== undefined && location.heading !== null && !isNaN(location.heading)) {
						this.locationHeading = location.heading
						headingUpdated = true
						console.log('[定位] 使用设备朝向:', location.heading.toFixed(1) + '°')
					}
					// 2. 如果设备朝向不可用，且位置变化，根据移动方向计算朝向
					else if (gpsChanged && oldGpsPos) {
						// 使用GPS坐标计算朝向（更准确）
						const lat1 = oldGpsPos.lat * Math.PI / 180
						const lat2 = location.latitude * Math.PI / 180
						const dLng = (location.longitude - oldGpsPos.lng) * Math.PI / 180
						
						// 计算方位角（0度表示正北）
						const y = Math.sin(dLng) * Math.cos(lat2)
						const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
						let angle = Math.atan2(y, x) * 180 / Math.PI
						angle = (angle + 360) % 360 // 转换为0-360度
						
						this.locationHeading = angle
						headingUpdated = true
						console.log('[定位] 根据GPS移动方向计算朝向:', {
							GPS移动: {
								from: { lat: oldGpsPos.lat, lng: oldGpsPos.lng },
								to: { lat: location.latitude, lng: location.longitude }
							},
							角度: angle.toFixed(1) + '°',
							浮点网格坐标: gridPosFloat
						})
					}
					// 3. 首次定位，如果没有设备朝向，默认朝向正北
					else if (!oldGpsPos && !headingUpdated) {
						this.locationHeading = 0
						console.log('[定位] 首次定位，默认朝向正北')
					}
					// 4. 如果位置没变且没有设备朝向，保持当前朝向（不更新）
					else if (!headingUpdated) {
						console.log('[定位] 位置未变化且无设备朝向，保持当前朝向:', this.locationHeading.toFixed(1) + '°')
					}
					
					// 更新GPS坐标和网格坐标（无论是否变化都更新，确保箭头实时移动）
					// 即使GPS坐标没变，也更新浮点数坐标（可能有精度变化，让箭头位置更精确）
					this.lastGpsLocation = { lat: location.latitude, lng: location.longitude }
					this.currentLocation = gridPos // 整数坐标，用于路径规划
					this.currentLocationFloat = gridPosFloat // 浮点数坐标，用于精确绘制（总是更新，确保箭头位置精确）
					this.locationAccuracy = location.accuracy || 0
					
					// 记录坐标更新（用于调试）
					console.log('[定位] 坐标已更新:', {
						整数坐标: gridPos,
						浮点坐标: gridPosFloat,
						旧浮点坐标: oldFloatPos,
						浮点坐标变化: floatPosChanged
					})
					
					// 查找最近的节点，显示当前位置名称
					const nearestNodeName = this.findNearestNodeName(gridPos)
					if (nearestNodeName) {
						this.locationStatus = `当前位置: ${nearestNodeName} (精度: ${Math.round(location.accuracy || 0)}米)`
						console.log('[定位] 当前位置:', nearestNodeName)
					} else {
						this.locationStatus = `实时GPS定位中 (精度: ${Math.round(location.accuracy || 0)}米)`
						console.log('[定位] 未找到附近节点，显示坐标:', gridPos)
					}
					
					// 更新起点并重新计算路径
					this.startNode = gridPos
					
					// GPS位置变化时重新计算路径（使用整数网格坐标）
					if (gpsChanged) {
						console.log('📍 GPS位置已更新:', {
							旧GPS: oldGpsPos,
							新GPS: { lat: location.latitude, lng: location.longitude },
							网格坐标: gridPos,
							浮点坐标: gridPosFloat,
							精度: location.accuracy + '米',
							来源: '真实GPS定位'
						})
						
						// 重新计算路径（使用整数网格坐标）
						this.calculatePath()
						
						// 更新剩余距离
						this.updateRemainingDistance()
					}
					
					// 无论位置是否变化，都重绘地图（确保箭头位置和朝向实时更新，像苹果地图一样）
					// 使用浮点数坐标绘制，让箭头平滑移动
					if (this.isRealTimeMode) {
						console.log('[定位] 准备重绘地图:', {
							浮点坐标: gridPosFloat,
							整数坐标: gridPos,
							朝向: this.locationHeading.toFixed(1) + '°',
							GPS: { lat: location.latitude, lng: location.longitude },
							GPS变化: gpsChanged,
							浮点坐标变化: floatPosChanged,
							朝向已更新: headingUpdated,
							设备朝向: location.heading !== undefined ? location.heading : '无',
							精度: location.accuracy + '米'
						})
						// 立即重绘，不使用nextTick，让箭头实时跟随GPS移动和手机转动
						// 即使坐标没变，也强制重绘（确保箭头位置和朝向实时更新）
						// 因为浮点数坐标可能有微小变化，或者朝向可能有变化
						this.drawMap()
						console.log('[定位] ✅ 地图重绘完成，箭头位置:', JSON.stringify(gridPosFloat))
					} else {
						console.warn('[定位] ⚠️ 实时定位模式未开启，不重绘地图')
					}
				} else {
					throw new Error('坐标转换失败，可能不在医院范围内')
				}
			} catch (error) {
				console.error('❌ 定位失败:', error)
				console.error('[定位] 错误详情:', {
					errMsg: error.errMsg,
					errno: error.errno,
					error: JSON.stringify(error)
				})
				
				// 定位失败时，如果开启了实时定位模式，自动停止
				if (this.isRealTimeMode) {
					this.stopLocationTracking()
					// 只显示一次提示，避免循环
					if (!this.locationErrorShown) {
						this.locationErrorShown = true
						uni.showToast({
							title: '定位失败，已自动停止',
							icon: 'none',
							duration: 2000
						})
					}
				} else {
					this.locationStatus = '定位失败，请使用手动选择位置'
				}
				
				// 根据不同的错误类型给出提示
				if (error.errMsg) {
					if (error.errMsg.includes('auth deny') || error.errMsg.includes('permission')) {
						// 权限被拒绝
						this.stopLocationTracking()
						uni.showModal({
							title: '需要定位权限',
							content: '请在手机设置中开启定位权限，然后重新尝试',
							showCancel: true,
							confirmText: '去设置',
							success: (res) => {
								if (res.confirm) {
									uni.openSetting()
								}
							}
						})
					} else if (error.errMsg.includes('timeout')) {
						// 定位超时
						uni.showToast({
							title: '定位超时，请到室外或GPS信号好的地方',
							icon: 'none',
							duration: 3000
						})
					} else if (error.errMsg.includes('fail')) {
						// 其他定位失败
						uni.showToast({
							title: '定位失败：' + error.errMsg,
							icon: 'none',
							duration: 3000
						})
					}
				}
			}
		},
		
		/**
		 * 将GPS坐标转换为地图网格坐标（浮点数，用于精确绘制）
		 * @param {Number} lat 纬度
		 * @param {Number} lng 经度
		 * @returns {Object} {x, y} 浮点数网格坐标
		 */
		gpsToGridFloat(lat, lng) {
			// 如果mapBounds未初始化，自动以当前GPS坐标为中心设置范围
			if (!this.mapBounds) {
				console.log('[坐标转换] 首次GPS定位，自动设置地图范围，中心坐标:', { lat, lng })
				this.mapBounds = {
					minLat: lat - this.mapBoundsOffset,
					maxLat: lat + this.mapBoundsOffset,
					minLng: lng - this.mapBoundsOffset,
					maxLng: lng + this.mapBoundsOffset
				}
				console.log('[坐标转换] 地图范围已设置:', this.mapBounds)
			}
			
			// 检查GPS坐标是否超出当前地图范围（允许一定误差）
			const tolerance = 0.00001 // 允许0.00001度的误差（约1.1米）
			
			// 如果GPS坐标超出范围，自动更新mapBounds（动态扩展范围）
			if (lat < this.mapBounds.minLat - tolerance || lat > this.mapBounds.maxLat + tolerance ||
				lng < this.mapBounds.minLng - tolerance || lng > this.mapBounds.maxLng + tolerance) {
				console.log('[坐标转换] GPS坐标超出当前范围，自动扩展地图范围')
				console.log('[坐标转换] 旧范围:', JSON.stringify(this.mapBounds))
				console.log('[坐标转换] 新GPS坐标:', { lat, lng })
				
				// 动态扩展范围，以新GPS坐标为中心
				this.mapBounds = {
					minLat: lat - this.mapBoundsOffset,
					maxLat: lat + this.mapBoundsOffset,
					minLng: lng - this.mapBoundsOffset,
					maxLng: lng + this.mapBoundsOffset
				}
				console.log('[坐标转换] 新范围:', JSON.stringify(this.mapBounds))
			}
			
			// 重新计算范围（使用更新后的mapBounds）
			const latRange = this.mapBounds.maxLat - this.mapBounds.minLat
			const lngRange = this.mapBounds.maxLng - this.mapBounds.minLng
			
			// 线性映射：将GPS坐标映射到网格坐标（浮点数，保持精度）
			// 经度映射到X轴（从左到右）
			const x = ((lng - this.mapBounds.minLng) / lngRange) * this.gridWidth
			// 纬度映射到Y轴（从上到下，因为纬度越大越靠北，在屏幕上越靠上）
			const y = ((this.mapBounds.maxLat - lat) / latRange) * this.gridHeight
			
			// 确保坐标在有效范围内
			const gridX = Math.max(0, Math.min(this.gridWidth - 1, x))
			const gridY = Math.max(0, Math.min(this.gridHeight - 1, y))
			
			return { x: gridX, y: gridY }
		},
		
		/**
		 * 将GPS坐标转换为地图网格坐标（整数，用于路径规划）
		 * @param {Number} lat 纬度
		 * @param {Number} lng 经度
		 * @returns {Object} {x, y} 整数网格坐标
		 */
		gpsToGrid(lat, lng) {
			console.log('📍 GPS坐标转换:', { lat, lng, mapBounds: this.mapBounds })
			
			// 先获取浮点数坐标
			const floatPos = this.gpsToGridFloat(lat, lng)
			
			// 转换为整数坐标
			const gridX = Math.floor(floatPos.x)
			const gridY = Math.floor(floatPos.y)
			
			// 检查转换后的坐标是否可通行，如果不可通行，找最近的可通行位置
			if (this.gridMatrix && this.gridMatrix[gridY] && this.gridMatrix[gridY][gridX] === 1) {
				console.log('⚠️ 转换后的坐标在障碍物上，查找最近可通行位置')
				const walkablePos = this.findNearestWalkable(gridX, gridY)
				if (walkablePos) {
					console.log('✅ 找到可通行位置:', walkablePos)
					return walkablePos
				}
			}
			
			console.log('✅ GPS转网格坐标成功:', { 
				GPS: { lat, lng }, 
				网格: { x: gridX, y: gridY },
				浮点坐标: floatPos,
				网格尺寸: { width: this.gridWidth, height: this.gridHeight }
			})
			
		return { x: gridX, y: gridY }
	},
	
	/**
	 * 切换实时定位模式
	 */
	toggleRealTimeLocation() {
			if (this.isRealTimeMode) {
				this.stopLocationTracking()
			} else {
				this.startRealTimeLocation()
			}
		},
		
		/**
		 * 开启实时定位跟踪 - 真实GPS定位
		 */
		startRealTimeLocation() {
			console.log('[实时定位] 开始启动真实GPS定位')
			
			// 强制停止所有演示模式
			this.stopDemoMode()
			
			// 重置错误提示标志
			this.locationErrorShown = false
			
			// 立即开启实时定位模式，这样箭头会立即显示（使用起点位置）
			this.isRealTimeMode = true
			this.stopLocationTracking(true) // 先停止旧的定时器，但保持 isRealTimeMode = true
			
			// 启动设备方向传感器监听（让箭头跟随手机转动）
			this.startCompassListener()
			
			// 立即重绘地图，显示箭头（即使GPS还没定位成功）
			this.$nextTick(() => {
				this.drawMap()
			})
			
			// 先获取一次位置
			this.getCurrentLocation().then(() => {
				console.log('[实时定位] GPS定位成功，开启实时跟踪')
				
				// 再次确保演示模式已停止
				if (this.isDemoMode || this.demoMoveTimer) {
					console.warn('[实时定位] 检测到演示模式，强制停止')
					this.stopDemoMode()
				}
				
				// 每1秒更新一次位置（实时跟踪，像苹果地图一样频繁更新）
				this.locationTimer = setInterval(() => {
					// 每次更新前都检查，确保演示模式没有启动
					if (this.isDemoMode || this.demoMoveTimer) {
						console.warn('[实时定位] 检测到演示模式，停止并清除')
						this.stopDemoMode()
					}
					console.log('[实时定位] 定时器触发，更新GPS位置...')
					this.getCurrentLocation()
				}, 1000) // 1秒更新一次，像苹果地图一样实时更新
				
				uni.showToast({
					title: '实时GPS定位已开启',
					icon: 'success',
					duration: 1500
				})
			}).catch((error) => {
				// 定位失败，但保持实时定位模式开启（箭头仍然显示）
				// 定时器继续运行，如果GPS信号恢复，会自动重新定位
				this.locationStatus = '定位失败，正在重试...'
				console.error('[实时定位] 首次定位失败，但保持实时模式开启:', error)
				
				// 即使首次定位失败，也开启定时器，持续尝试定位
				this.locationTimer = setInterval(() => {
					if (this.isDemoMode || this.demoMoveTimer) {
						console.warn('[实时定位] 检测到演示模式，停止并清除')
						this.stopDemoMode()
					}
					console.log('[实时定位] 定时器触发，重试GPS定位...')
					this.getCurrentLocation().catch(err => {
						// 定位失败时不显示错误，避免频繁提示
						console.warn('[实时定位] GPS定位失败，继续重试:', err)
					})
				}, 2000) // 定位失败时，每2秒重试一次
				
				uni.showToast({
					title: '定位中，请稍候...',
					icon: 'none',
					duration: 2000
				})
			})
		},
		
		/**
		 * 停止定位跟踪
		 */
		stopLocationTracking(keepMode = false) {
			if (this.locationTimer) {
				clearInterval(this.locationTimer)
				this.locationTimer = null
			}
			// 停止设备方向传感器监听
			this.stopCompassListener()
			// 如果 keepMode 为 true，不重置 isRealTimeMode（用于重启定位时）
			if (!keepMode) {
				this.isRealTimeMode = false
				this.locationStatus = '定位已停止'
			}
			this.locationErrorShown = false // 重置错误提示标志
		},
		
		/**
		 * 启动设备方向传感器监听（让箭头跟随手机转动）
		 */
		startCompassListener() {
			console.log('[方向传感器] 启动设备方向传感器监听')
			
			// 停止旧的监听器（如果存在）
			this.stopCompassListener()
			
			// 某些平台需要显式启动指南针
			if (typeof uni.startCompass === 'function') {
				uni.startCompass({
					fail: (err) => {
						console.warn('[方向传感器] startCompass失败:', err)
					}
				})
			}
			
			// 定义方向变化回调函数（添加节流，避免更新太频繁）
			let lastHeadingUpdate = 0
			const compassCallback = (res) => {
				// res.direction: 设备方向，范围 0-360 度，0度表示正北
				if (this.isRealTimeMode && res.direction !== undefined && res.direction !== null) {
					// 节流：每100ms最多更新一次，避免重绘太频繁
					const now = Date.now()
					if (now - lastHeadingUpdate < 100) {
						return
					}
					lastHeadingUpdate = now
					
					// 更新朝向角度
					this.locationHeading = res.direction
					console.log('[方向传感器] 设备方向更新:', res.direction.toFixed(1) + '°')
					
					// 立即重绘地图，让箭头跟随手机转动
					this.drawMap()
				}
			}
			
			// 启动设备方向传感器
			this.compassListener = compassCallback
			uni.onCompassChange(compassCallback)
			
			console.log('[方向传感器] ✅ 设备方向传感器监听已启动')
		},
		
		/**
		 * 停止设备方向传感器监听
		 */
		stopCompassListener() {
			if (this.compassListener) {
				try {
					// uni.offCompassChange 需要传入回调函数
					uni.offCompassChange(this.compassListener)
					console.log('[方向传感器] 已停止设备方向传感器监听')
				} catch (e) {
					console.warn('[方向传感器] 停止监听失败:', e)
					// 如果 offCompassChange 不支持，尝试使用其他方式
					// 在某些平台，可能需要重新启动监听并立即停止
				}
				this.compassListener = null
			}
			if (typeof uni.stopCompass === 'function') {
				uni.stopCompass()
			}
		},
		
		/**
		 * 手动选择位置
		 */
		showLocationPicker() {
			console.log('[选择位置] 按钮被点击')
			console.log('[选择位置] nodes数量:', this.nodes ? this.nodes.length : 0)
			console.log('[选择位置] nodes数据:', JSON.stringify(this.nodes, null, 2))
			
		// 检查nodes是否已加载
		if (!this.nodes || this.nodes.length === 0) {
			console.error('[选择位置] ❌ 错误：nodes数据为空！无法选择位置')
			uni.showModal({
				title: '无可用位置',
				content: '地图节点数据未加载，请返回重试。\n\n如果问题持续，请联系管理员检查数据库配置。',
				showCancel: false,
				confirmText: '返回',
				success: () => {
					uni.navigateBack()
				}
			})
			return
		}
		
		// 如果只有很少的节点，给出警告
		if (this.nodes.length < 3) {
			console.warn('[选择位置] ⚠️ 警告：只有' + this.nodes.length + '个节点，数据可能不完整')
		}
			
			// 停止所有定位，确保可以操作
			this.stopLocationTracking()
			
			// 准备选项列表，确保每个节点都有name属性
			let validNodes = this.nodes.filter(n => n && n.name)
			
			// showActionSheet最多支持6个选项，如果超过6个，优先显示重要节点
			if (validNodes.length > 6) {
				console.warn('[选择位置] 节点数量超过6个，优先显示重要节点')
				// 优先显示：医院大门、分诊台、电梯口，然后是诊室（有locationId的）
				const importantNodes = validNodes.filter(n => 
					n.nodeId <= 3 || // 前3个固定节点
					n.locationId !== null // 或者是有locationId的诊室
				)
				
				// 如果重要节点不超过6个，就用重要节点
				if (importantNodes.length <= 6) {
					validNodes = importantNodes
				} else {
					// 如果重要节点也超过6个，只取前6个
					validNodes = importantNodes.slice(0, 6)
				}
			}
			
			const itemList = validNodes.map(n => n.name)
			
			console.log('[选择位置] 选项列表:', itemList)
			console.log('[选择位置] 选项数量:', itemList.length)
			
			if (itemList.length === 0) {
				uni.showToast({
					title: '没有可用的位置选项',
					icon: 'none'
				})
				return
			}
			
			// 使用showActionSheet，如果失败则使用备选方案
			uni.showActionSheet({
				itemList: itemList,
				success: (res) => {
					console.log('[选择位置] 用户选择了索引:', res.tapIndex)
					// 使用validNodes而不是this.nodes，因为可能被截断了
					const selectedNode = validNodes[res.tapIndex]
					if (selectedNode) {
						console.log('[选择位置] 选中的节点:', selectedNode)
						this.currentLocation = { x: selectedNode.x, y: selectedNode.y }
						// 手动选择时，浮点数坐标就是网格中心（整数坐标 + 0.5）
						this.currentLocationFloat = { x: selectedNode.x + 0.5, y: selectedNode.y + 0.5 }
						this.startNode = this.currentLocation
						this.locationStatus = `已选择: ${selectedNode.name}`
						
						// 重新计算路径
						this.calculatePath()
						
						// 停止实时定位
						this.stopLocationTracking()
						
						uni.showToast({
							title: `已选择${selectedNode.name}`,
							icon: 'success',
							duration: 1500
						})
					} else {
						console.error('[选择位置] 未找到选中的节点')
					}
				},
				fail: (err) => {
					console.error('[选择位置] showActionSheet失败:', err)
					// 如果showActionSheet失败，使用备选方案：显示第一个节点
					if (this.nodes && this.nodes.length > 0) {
						const firstNode = this.nodes[0]
						this.currentLocation = { x: firstNode.x, y: firstNode.y }
						// 手动选择时，浮点数坐标就是网格中心（整数坐标 + 0.5）
						this.currentLocationFloat = { x: firstNode.x + 0.5, y: firstNode.y + 0.5 }
						this.startNode = this.currentLocation
						this.locationStatus = `已选择: ${firstNode.name}`
						this.calculatePath()
						uni.showToast({
							title: `已选择${firstNode.name}`,
							icon: 'success'
						})
					}
				}
			})
		},
		
		/**
		 * 扫码定位（改进版：从后端API获取完整节点信息，包括楼层）
		 */
		async scanLocationCode() {
			try {
				const res = await uni.scanCode({
					scanType: ['qrCode', 'barCode']
				})
				
				console.log('扫码结果:', res)
				
				// 解析二维码内容（格式：HOSPITAL_NODE_{nodeId}）
				const content = res.result.trim()
				const match = content.match(/HOSPITAL_NODE[_\s]?(\d+)/i)
				
				if (!match) {
					throw new Error('二维码格式不正确，应为：HOSPITAL_NODE_数字')
				}
				
				const qrcodeContent = content
				
				// 调用后端API获取节点完整信息（包括楼层、坐标等）
				try {
					const scanResponse = await scanQRCode(qrcodeContent)
					
					console.log('后端扫码API响应:', scanResponse)
					
					if (scanResponse && scanResponse.code === '200') {
						const nodeData = scanResponse.data
						
						// 获取节点的完整信息
						const nodeId = nodeData.nodeId
						const nodeName = nodeData.nodeName
						const floorLevel = nodeData.floorLevel
						const coordinatesX = nodeData.coordinatesX
						const coordinatesY = nodeData.coordinatesY
						
						console.log('节点信息:', {
							nodeId,
							nodeName,
							floorLevel,
							coordinatesX,
							coordinatesY
						})
						
						// 转换为网格坐标（如果后端返回的是实际坐标，需要转换）
						// 这里假设后端返回的坐标就是网格坐标
						const gridX = Math.round(coordinatesX)
						const gridY = Math.round(coordinatesY)
						
						// 更新当前节点信息
						this.currentNode = {
							nodeId,
							name: nodeName,
							floorLevel,
							x: gridX,
							y: gridY
						}
						
						// 切换到对应楼层
						if (floorLevel && floorLevel !== this.currentFloor) {
							this.switchFloor(floorLevel)
						}
						
						// 更新当前位置
						this.startNode = {
							x: gridX,
							y: gridY,
							nodeId: nodeId,
							name: nodeName
						}
						this.activateArrowMode(gridX, gridY)
					
					// 更新状态
					this.locationStatus = `📍 ${nodeName} (${floorLevel}楼)`
					
					// 确保目标节点已加载，然后计算路径
					if (!this.targetNode || !this.targetNode.nodeId) {
						console.log('扫码定位：目标节点未加载，先加载目标节点')
						// 如果目标节点还没加载，先加载它
						await this.loadTargetNode()
					}
					
					// 重新计算路径（从扫码位置到目的地）
					console.log('扫码定位：开始计算路径', {
						起点: this.startNode,
						终点: this.targetNode
					})
					await this.calculatePath()
						
						uni.showToast({
							title: `定位成功：${nodeName}`,
							icon: 'success',
							duration: 2000
						})
						
					} else {
						throw new Error(scanResponse.data.message || '后端API返回错误')
					}
					
				} catch (apiError) {
					console.error('调用后端API失败，尝试本地查找:', apiError)
					
					// 如果API失败，尝试本地查找（降级方案）
					const nodeId = parseInt(match[1])
					const node = this.nodes.find(n => n.nodeId === nodeId)
					
					if (node) {
						this.startNode = {
							...node
						}
						this.activateArrowMode(node.x, node.y)
						this.locationStatus = `扫码定位: ${node.name}`
						await this.calculatePath()
						
						uni.showToast({
							title: `定位到${node.name}`,
							icon: 'success'
						})
					} else {
						throw new Error('未找到对应的位置节点')
					}
				}
				
			} catch (error) {
				console.error('扫码失败:', error)
				if (error.errMsg && !error.errMsg.includes('cancel')) {
					uni.showToast({
						title: error.message || '扫码失败',
						icon: 'none',
						duration: 2000
					})
				}
			}
		},
		
		/**
		 * 切换楼层
		 */
	async switchFloor(floorLevel) {
		if (this.currentFloor === floorLevel) {
			return
		}
		
		console.log(`切换楼层: ${this.currentFloor}楼 -> ${floorLevel}楼`)
		this.currentFloor = floorLevel
		
		// 重新加载该楼层的地图数据
		this.loading = true
		try {
			const configResponse = await getMapConfig(floorLevel)
			console.log(`获取${floorLevel}楼地图配置:`, configResponse)
			
			if (configResponse && configResponse.code === '200' && configResponse.data) {
				const config = configResponse.data
				if (config.grid) {
					this.gridMatrix = config.grid.gridMatrix
					this.nodes = config.nodes || []
					console.log(`${floorLevel}楼地图加载成功，节点数:`, this.nodes.length)
				}
			}
		} catch (error) {
			console.error(`加载${floorLevel}楼地图失败:`, error)
			uni.showToast({
				title: '切换楼层失败',
				icon: 'none'
			})
			return
		} finally {
			this.loading = false
		}
		
		// 更新背景图片路径
		this.showBackgroundImage = true
		// 背景图片路径：/static/images/hospital_floor_{floorLevel}.jpg
		
		// 重新绘制地图
		this.$nextTick(() => {
			this.drawMap()
		})
		
		uni.showToast({
			title: `已切换到${floorLevel}楼`,
			icon: 'success'
		})
	},
		
		/**
		 * 根据楼层过滤节点
		 */
		filterNodesByFloor(floorLevel) {
			// 从所有节点中筛选出当前楼层的节点
			// 注意：这里需要从后端重新加载该楼层的节点
			// 或者在前端过滤已有的nodes数组
			console.log(`过滤${floorLevel}楼的节点`)
		},
		
		/**
		 * 从导航步骤生成路径点（用于绘制）
		 */
		generatePathFromSteps(steps) {
			// 根据后端返回的steps生成路径点
			// 如果节点距离很近，直接用直线；否则用A*生成细颗粒度路径
			if (!steps || steps.length === 0) {
				return []
			}
			
			const path = []
			const astar = this.gridMatrix ? new SimpleAStar(this.gridMatrix) : null
			
			for (const step of steps) {
				const fromNode = this.nodes.find(n => n.nodeId === step.fromNodeId)
				const toNode = this.nodes.find(n => n.nodeId === step.toNodeId)
				if (!fromNode || !toNode) {
					console.warn('找不到节点:', { fromNodeId: step.fromNodeId, toNodeId: step.toNodeId })
					continue
				}
				
				// 计算节点间距离
				const dx = Math.abs(toNode.x - fromNode.x)
				const dy = Math.abs(toNode.y - fromNode.y)
				const distance = Math.sqrt(dx * dx + dy * dy)
				
				// 如果节点很近（距离<3）或没有网格数据，直接用直线
				if (distance < 3 || !astar) {
					if (path.length === 0) {
						path.push({ x: fromNode.x, y: fromNode.y })
					}
					path.push({ x: toNode.x, y: toNode.y })
					continue
				}
				
				// 否则用A*生成细颗粒度路径
				const segment = astar.findPath(fromNode.x, fromNode.y, toNode.x, toNode.y)
				if (!segment || segment.length === 0) {
					// A*失败，回退到直线
					if (path.length === 0) {
						path.push({ x: fromNode.x, y: fromNode.y })
					}
					path.push({ x: toNode.x, y: toNode.y })
					continue
				}
				
				// 首段保留起点，后续段去掉第一个点，避免重复
				if (path.length === 0) {
					path.push(...segment)
				} else {
					path.push(...segment.slice(1))
				}
			}
			
			return path
		},
		
		/**
		 * 更新到下一步（当患者到达某个节点时调用）
		 */
		moveToNextStep() {
			if (this.currentStepIndex < this.navigationSteps.length - 1) {
				this.currentStepIndex++
				this.currentStep = this.navigationSteps[this.currentStepIndex]
				this.nextStep = this.currentStepIndex < this.navigationSteps.length - 1 
					? this.navigationSteps[this.currentStepIndex + 1] 
					: null
				
				// 如果下一步有楼层变化，切换楼层
				if (this.currentStep && this.currentStep.toFloor) {
					this.switchFloor(this.currentStep.toFloor)
				}
				
				// 更新起点为当前到达的节点，并确保箭头模式保持激活
				if (this.currentStep && this.currentStep.toNodeId) {
					const toNode = this.nodes.find(n => n.nodeId === this.currentStep.toNodeId)
					if (toNode) {
						this.startNode = {
							x: toNode.x,
							y: toNode.y,
							nodeId: toNode.nodeId,
							name: toNode.name
						}
						
						// 更新箭头位置到新节点，并确保箭头模式保持激活
						this.activateArrowMode(toNode.x, toNode.y)
						
						// 同时更新currentNode信息
						this.currentNode = {
							nodeId: toNode.nodeId,
							name: toNode.name,
							floorLevel: toNode.floorLevel || this.currentFloor,
							x: toNode.x,
							y: toNode.y
						}
						
						console.log('✅ 已到达节点，箭头移动到新位置:', {
							节点: toNode.name,
							坐标: `(${toNode.x}, ${toNode.y})`,
							箭头模式: this.isRealTimeMode,
							指南针监听: !!this.compassListener
						})
					}
				}
				
				// 重新计算剩余距离
				let remaining = 0
				for (let i = this.currentStepIndex; i < this.navigationSteps.length; i++) {
					remaining += this.navigationSteps[i].distance || 0
				}
				this.remainingDistance = Math.round(remaining)
				
				// 重新生成路径（从新位置到终点）
				if (this.navigationSteps && this.navigationSteps.length > 0) {
					// 只取剩余的步骤
					const remainingSteps = this.navigationSteps.slice(this.currentStepIndex)
					this.path = this.generatePathFromSteps(remainingSteps)
					
					// 如果路径生成失败，至少保证起点和终点
					if (!this.path || this.path.length === 0) {
						if (this.startNode && this.targetNode) {
							this.path = [
								{ x: this.startNode.x, y: this.startNode.y },
								{ x: this.targetNode.x, y: this.targetNode.y }
							]
						}
					}
				}
				
				// 重新绘制地图
				this.$nextTick(() => {
					this.drawMap()
				})

				// 更新箭头朝向（指向下一步）
				this.updateArrowHeading()
			} else {
				// 到达终点
				this.currentStep = null
				this.nextStep = null
				uni.showToast({
					title: '🎉 已到达目的地！',
					icon: 'success',
					duration: 2000
				})
			}
		},
		
		/**
		 * 标记已到达当前节点
		 */
		markAsArrived() {
			if (!this.currentStep) {
				return
			}
			
			uni.showModal({
				title: '确认到达',
				content: `您已到达 ${this.currentStep.toNodeName} 吗？`,
				success: (res) => {
					if (res.confirm) {
						this.moveToNextStep()
					}
				}
			})
		},
		
		/**
		 * 处理位置更新（用于真实GPS定位）
		 */
		processLocationUpdate(location) {
			// 将GPS坐标转换为地图网格坐标（整数，用于路径规划）
			const gridPos = this.gpsToGrid(location.latitude, location.longitude)
			// 将GPS坐标转换为浮点数网格坐标（用于精确绘制箭头）
			const gridPosFloat = this.gpsToGridFloat(location.latitude, location.longitude)
			
			if (gridPos && gridPosFloat) {
				// 检查GPS位置是否发生变化
				// 超小范围模式：降低阈值到极小值（约0.001毫米），几乎任何变化都会被检测
				// 注意：真实GPS在室内精度通常为3-10米，所以坐标可能一直不变
				const oldGpsPos = this.lastGpsLocation
				const gpsChanged = !oldGpsPos || 
					Math.abs(oldGpsPos.lat - location.latitude) > 0.0000000001 || 
					Math.abs(oldGpsPos.lng - location.longitude) > 0.0000000001
				
				// 检查浮点数网格坐标是否发生变化
				// 超小范围模式：降低阈值到极小的值，让任何位置变化都能被检测到
				const oldFloatPos = this.currentLocationFloat
				const floatPosChanged = !oldFloatPos || 
					Math.abs(oldFloatPos.x - gridPosFloat.x) > 0.000001 || 
					Math.abs(oldFloatPos.y - gridPosFloat.y) > 0.000001
				
				// 记录浮点数坐标变化（用于调试）
				if (oldFloatPos) {
					const xDiff = Math.abs(oldFloatPos.x - gridPosFloat.x)
					const yDiff = Math.abs(oldFloatPos.y - gridPosFloat.y)
					console.log('[位置更新] 浮点数坐标变化:', {
						旧坐标: oldFloatPos,
						新坐标: gridPosFloat,
						X变化: xDiff.toFixed(6),
						Y变化: yDiff.toFixed(6),
						变化: floatPosChanged,
						像素变化X: (xDiff * (this.canvasWidth / this.gridWidth)).toFixed(2) + 'px',
						像素变化Y: (yDiff * (this.canvasHeight / this.gridHeight)).toFixed(2) + 'px'
					})
				}
				
				// 更新朝向：优先使用设备朝向或模拟朝向
				if (location.heading !== undefined && location.heading !== null && !isNaN(location.heading)) {
					this.locationHeading = location.heading
				} else if (gpsChanged && oldGpsPos) {
					// 根据移动方向计算朝向
					const lat1 = oldGpsPos.lat * Math.PI / 180
					const lat2 = location.latitude * Math.PI / 180
					const dLng = (location.longitude - oldGpsPos.lng) * Math.PI / 180
					const y = Math.sin(dLng) * Math.cos(lat2)
					const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
					let angle = Math.atan2(y, x) * 180 / Math.PI
					angle = (angle + 360) % 360
					this.locationHeading = angle
				}
				
				// 更新GPS坐标和网格坐标（无论是否变化都更新，确保箭头位置实时更新）
				this.lastGpsLocation = { lat: location.latitude, lng: location.longitude }
				this.currentLocation = gridPos
				this.currentLocationFloat = gridPosFloat // 总是更新浮点数坐标，确保箭头位置精确
				this.locationAccuracy = location.accuracy || 0
				
				// 更新起点
				this.startNode = gridPos
				
				// 记录坐标更新（用于调试）
				console.log('[位置更新] 坐标已更新:', {
					整数坐标: gridPos,
					浮点坐标: gridPosFloat,
					旧浮点坐标: oldFloatPos,
					浮点坐标变化: floatPosChanged,
					GPS变化: gpsChanged,
					原始GPS: { lat: location.latitude, lng: location.longitude },
					旧GPS: oldGpsPos,
					GPS差异: oldGpsPos ? {
						lat: (location.latitude - oldGpsPos.lat).toFixed(12),
						lng: (location.longitude - oldGpsPos.lng).toFixed(12)
					} : '首次定位'
				})
				
				// GPS位置变化时重新计算路径
				if (gpsChanged) {
					this.calculatePath()
					this.updateRemainingDistance()
				}
				
				// 无论位置是否变化，都强制重绘地图（确保箭头位置和朝向实时更新）
				if (this.isRealTimeMode) {
					console.log('[位置更新] 强制重绘地图，箭头位置:', JSON.stringify(gridPosFloat), '朝向:', this.locationHeading.toFixed(1) + '°')
					// 立即重绘，不使用nextTick，让箭头实时移动
					this.drawMap()
				} else {
					console.warn('[位置更新] ⚠️ 实时定位模式未开启，不重绘地图')
				}
			}
		},
		
	}
}
</script>

<style lang="scss" scoped>
.page-scroll {
	height: 100vh;
}

.container {
	width: 100%;
	/* 使用最小高度而不是固定高度，让页面可以滚动 */
	min-height: 100vh;
	background-color: #f7fafc;
	display: flex;
	flex-direction: column;
}

.control-panel {
	background: #ffffff;
	padding: 30rpx;
	border-bottom: 1rpx solid #e5e5e5;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
}

.info-row {
	display: flex;
	align-items: center;
	margin-bottom: 16rpx;
	
	&:last-child {
		margin-bottom: 0;
	}
}

.info-label {
	font-size: 28rpx;
	color: #666;
	margin-right: 12rpx;
}

.info-value {
	font-size: 30rpx;
	color: #333;
	font-weight: 600;
}

.floor-selector {
	display: flex;
	align-items: center;
	margin-bottom: 16rpx;
	padding: 16rpx;
	background: #f5f5f5;
	border-radius: 8rpx;
}

.floor-buttons {
	display: flex;
	gap: 12rpx;
	flex: 1;
}

.floor-btn {
	padding: 12rpx 24rpx;
	background: #fff;
	border: 2rpx solid #ddd;
	border-radius: 8rpx;
	font-size: 28rpx;
	color: #666;
	text-align: center;
	transition: all 0.3s;
}

.floor-btn.active {
	background: #1890ff;
	border-color: #1890ff;
	color: #fff;
	font-weight: 600;
}

.floor-label {
	position: absolute;
	top: 20rpx;
	right: 20rpx;
	background: rgba(24, 144, 255, 0.9);
	padding: 12rpx 24rpx;
	border-radius: 20rpx;
	z-index: 10;
}

.floor-text {
	font-size: 28rpx;
	color: #fff;
	font-weight: 600;
}

.control-buttons {
	display: flex;
	gap: 16rpx;
	margin-top: 20rpx;
	flex-wrap: wrap;
	position: relative;
	z-index: 100; /* 确保按钮区域在最上层 */
}

.demo-btn {
	flex: 1;
	min-width: 200rpx;
	padding: 20rpx 24rpx;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-radius: 12rpx;
	text-align: center;
	box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.4);
}

.demo-text {
	font-size: 30rpx;
	color: #ffffff;
	font-weight: 600;
}

.debug-toggle {
	flex: 1;
	min-width: 200rpx;
	padding: 16rpx 24rpx;
	background: #E6F7FF;
	border-radius: 8rpx;
	text-align: center;
}

.debug-text {
	font-size: 26rpx;
	color: #1890FF;
}

.control-btn {
	flex: 1;
	min-width: 150rpx;
	padding: 16rpx 24rpx;
	background: #52C41A;
	border-radius: 8rpx;
	text-align: center;
	transition: all 0.3s;
	position: relative;
	z-index: 100; /* 确保按钮在最上层，可以点击 */
	cursor: pointer; /* 确保可以点击 */
	-webkit-tap-highlight-color: transparent; /* 移除点击高亮 */
	user-select: none; /* 防止文本选择 */
	
	&.active {
		background: #FFA500;
		box-shadow: 0 4rpx 12rpx rgba(255, 165, 0, 0.4);
	}
	
	/* 确保按钮内容可以点击 */
	.btn-text {
		pointer-events: none; /* 让点击事件穿透到按钮本身 */
	}
}

.control-btn.primary {
	background: #1890FF;
}

.btn-text {
	font-size: 26rpx;
	color: #ffffff;
	font-weight: 600;
}

.location-status {
	margin-top: 16rpx;
	padding: 12rpx 20rpx;
	background: #FFF7E6;
	border-radius: 8rpx;
	border-left: 4rpx solid #FFA500;
}

.status-text {
	font-size: 24rpx;
	color: #AD6800;
}

.map-container {
	width: 100%;
	height: 900rpx;
	position: relative;
	overflow: hidden;
	background-image: linear-gradient(135deg, #eef5ff 0%, #dfefff 100%);
	border-radius: 24rpx;
	box-shadow: 0 24rpx 48rpx rgba(42, 130, 228, 0.12);
	margin-top: 24rpx;
}

.background-image {
	width: 100%;
	height: 100%;
	position: absolute;
	top: 0;
	left: 0;
	opacity: 0.6;
	z-index: 1;
}

.map-canvas {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	z-index: 10;
	background-color: rgba(240, 240, 240, 0.25);
}

.map-legend {
	margin: 16rpx 0 0;
	padding: 16rpx 20rpx;
	background: #ffffff;
	border-radius: 14rpx;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.legend-item {
	display: flex;
	align-items: center;
	gap: 8rpx;
	min-width: 45%;
}

.legend-text {
	font-size: 24rpx;
	color: #4a4a4a;
}

.legend-dot {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
}

.legend-dot.current {
	background: #FF4D4F;
}

.legend-dot.next {
	border: 3rpx solid #FAAD14;
	width: 18rpx;
	height: 18rpx;
	border-radius: 50%;
}

.legend-dot.destination {
	background: #FF7875;
	box-shadow: 0 0 6rpx rgba(255, 120, 117, 0.6);
}

.legend-line.path {
	width: 32rpx;
	height: 4rpx;
	border-radius: 4rpx;
	background: linear-gradient(90deg, #52C41A, #13C2C2);
}

.legend-block.obstacle {
	width: 20rpx;
	height: 14rpx;
	border-radius: 2rpx;
	background: rgba(255, 153, 153, 0.45);
}

.floor-label {
	position: absolute;
	top: 20rpx;
	right: 20rpx;
	background: rgba(24, 144, 255, 0.9);
	padding: 12rpx 24rpx;
	border-radius: 20rpx;
	z-index: 20;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
}

.floor-text {
	font-size: 28rpx;
	color: #fff;
	font-weight: 600;
}

.debug-info {
	position: absolute;
	bottom: 20rpx;
	left: 20rpx;
	background: rgba(0, 0, 0, 0.6);
	padding: 16rpx;
	border-radius: 8rpx;
	z-index: 20;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.debug-text {
	font-size: 22rpx;
	color: #ffffff;
}

.loading-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(255, 255, 255, 0.9);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}

.loading-text {
	font-size: 32rpx;
	color: #666;
}

.navigation-guide {
	margin-top: 20rpx;
	padding: 24rpx;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-radius: 16rpx;
	box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.3);
}

.guide-header {
	display: flex;
	align-items: center;
	margin-bottom: 16rpx;
}

.guide-icon {
	font-size: 36rpx;
	margin-right: 12rpx;
}

.guide-title {
	font-size: 32rpx;
	color: #ffffff;
	font-weight: 600;
}

.guide-content {
	margin-bottom: 12rpx;
}

.guide-text {
	font-size: 30rpx;
	color: #ffffff;
	line-height: 1.6;
	font-weight: 500;
}

.guide-detail {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.9);
	margin-bottom: 16rpx;
}

.guide-actions {
	margin-top: 16rpx;
}

.arrived-btn {
	width: 100%;
	padding: 16rpx;
	background: rgba(255, 255, 255, 0.2);
	border: 2rpx solid rgba(255, 255, 255, 0.5);
	border-radius: 12rpx;
	color: #ffffff;
	font-size: 28rpx;
	font-weight: 600;
}

.next-step-hint {
	margin-top: 16rpx;
	padding: 16rpx 20rpx;
	background: #FFF7E6;
	border-left: 4rpx solid #FFA500;
	border-radius: 8rpx;
}

.hint-text {
	font-size: 26rpx;
	color: #AD6800;
	line-height: 1.5;
}
</style>

