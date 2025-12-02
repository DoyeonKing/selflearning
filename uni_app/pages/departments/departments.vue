<template>
	<view class="container">
		<view class="page-header">
			<text class="page-title">选择科室</text>
		</view>
		
		<view class="content">
			<!-- 加载状态 -->
			<view class="loading-container" v-if="loading">
				<text class="loading-text">加载中...</text>
			</view>
			
			<!-- 内容区域 -->
			<template v-else>
				<!-- 搜索栏 -->
				<view class="search-bar">
					<input 
						class="search-input" 
						v-model="searchKeyword" 
						placeholder="请输入科室名称" 
						@input="handleSearch"
					/>
					<text class="search-icon">🔍</text>
				</view>
				
				<!-- 科室列表（两栏布局） -->
				<view class="department-container" v-if="departments.length > 0">
					<!-- 左侧：父科室列表 -->
					<view class="parent-departments">
						<view 
							class="parent-item" 
							v-for="dept in departments" 
							:key="dept.id"
							:class="{ active: selectedParentId === dept.id }"
							@click="selectParent(dept.id)"
						>
							<text class="parent-name">{{ dept.name }}</text>
						</view>
					</view>
					
					<!-- 右侧：子科室列表 -->
					<view class="sub-departments">
						<view 
							class="sub-item" 
							v-for="subDept in currentSubDepartments" 
							:key="subDept.id"
							@click="navigateToSchedule(subDept.id, subDept.name)"
						>
							<view class="sub-info">
								<text class="sub-name">{{ subDept.name }}</text>
								<text class="sub-desc" v-if="subDept.description">{{ subDept.description }}</text>
							</view>
							<text class="arrow">></text>
						</view>
						
						<!-- 空状态 -->
						<view class="empty-state" v-if="currentSubDepartments.length === 0 && selectedParentId">
							<text class="empty-icon">🏥</text>
							<text class="empty-text">该科室暂无子科室</text>
						</view>
					</view>
				</view>
				
				<!-- 无数据状态 -->
				<view class="empty-container" v-else>
					<text class="empty-icon">🏥</text>
					<text class="empty-text">暂无科室数据</text>
				</view>
			</template>
		</view>
	</view>
</template>

<script>
	import { mockDepartments } from '../../api/mockData.js'
	import { getDepartmentTree } from '../../api/schedule.js'
	
	export default {
		data() {
			return {
				departments: [],
				selectedParentId: null,
				searchKeyword: '',
				loading: true
			}
		},
		computed: {
			currentSubDepartments() {
				if (!this.selectedParentId) return []
				const parent = this.departments.find(d => d.id === this.selectedParentId)
				return parent ? parent.children : []
			}
		},
		onLoad() {
			this.loadDepartments()
		},
		methods: {
			async loadDepartments() {
				this.loading = true
				try {
					const response = await getDepartmentTree()
					console.log('获取科室树数据:', response)
					
					let allDepartments = []
					// 后端返回的是数组格式，不是标准Result格式
					if (Array.isArray(response)) {
						allDepartments = response
					} else if (response && response.code === '200' && response.data) {
						allDepartments = response.data
					} else {
						// 如果后端失败，使用Mock数据
						console.log('使用Mock数据')
						allDepartments = JSON.parse(JSON.stringify(mockDepartments))
					}
					
					// 过滤掉不应该在患者挂号中显示的科室
					// 排除：医技科室、行政科室
					const excludedNames = ['医技科室', '行政科室']
					this.departments = allDepartments.filter(dept => {
						const name = dept.name || dept.parentDepartmentName || ''
						return !excludedNames.includes(name)
					})
					
					console.log('过滤后的科室列表:', this.departments)
					
					if (this.departments.length > 0) {
						this.selectedParentId = this.departments[0].id
					}
				} catch (error) {
					console.error('加载科室列表失败:', error)
					// 使用Mock数据作为fallback
					const allDepartments = JSON.parse(JSON.stringify(mockDepartments))
					// 同样过滤掉不应该显示的科室
					const excludedNames = ['医技科室', '行政科室']
					this.departments = allDepartments.filter(dept => {
						const name = dept.name || dept.parentDepartmentName || ''
						return !excludedNames.includes(name)
					})
					if (this.departments.length > 0) {
						this.selectedParentId = this.departments[0].id
					}
				} finally {
					this.loading = false
				}
			},
			selectParent(parentId) {
				this.selectedParentId = parentId
			},
			handleSearch() {
				// TODO: 实现搜索功能
				console.log('搜索关键词:', this.searchKeyword)
			},
			navigateToSchedule(departmentId, departmentName) {
				uni.navigateTo({
					url: `/pages/schedules/schedules?departmentId=${departmentId}&departmentName=${encodeURIComponent(departmentName)}`
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
		background: linear-gradient(135deg, #5FE0D4 0%, #4FD1C5 100%);
		padding: 40rpx 30rpx 30rpx;
	}

	.page-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.content {
		padding: 20rpx 0;
	}

	.search-bar {
		margin: 20rpx 30rpx;
		position: relative;
		background: #ffffff;
		border-radius: 50rpx;
		display: flex;
		align-items: center;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.search-input {
		flex: 1;
		padding: 24rpx 80rpx 24rpx 30rpx;
		font-size: 28rpx;
		background: transparent;
		border: none;
	}

	.search-icon {
		position: absolute;
		right: 30rpx;
		font-size: 32rpx;
		color: #718096;
	}

	.department-container {
		display: flex;
		height: calc(100vh - 300rpx);
	}

	.parent-departments {
		width: 200rpx;
		background: #f0f0f0;
		border-right: 1rpx solid #e2e8f0;
		overflow-y: auto;
	}

	.parent-item {
		padding: 32rpx 20rpx;
		text-align: center;
		border-bottom: 1rpx solid #e2e8f0;
		transition: all 0.3s ease;
	}

	.parent-item:active {
		background: #e0e0e0;
	}

	.parent-item.active {
		background: #ffffff;
		color: #4FD1C5;
		font-weight: 600;
	}

	.parent-name {
		font-size: 28rpx;
	}

	.sub-departments {
		flex: 1;
		background: #ffffff;
		overflow-y: auto;
		padding: 0 20rpx;
	}

	.sub-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 32rpx 0;
		border-bottom: 1rpx solid #f0f0f0;
		transition: all 0.3s ease;
	}

	.sub-item:active {
		background: #f8f9fa;
	}

	.sub-info {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.sub-name {
		font-size: 30rpx;
		color: #1A202C;
		font-weight: 500;
		margin-bottom: 8rpx;
	}

	.sub-desc {
		font-size: 24rpx;
		color: #718096;
	}

	.arrow {
		font-size: 32rpx;
		color: #CBD5E0;
	}

	.empty-state {
		padding: 100rpx 40rpx;
		text-align: center;
	}

	.empty-icon {
		display: block;
		font-size: 100rpx;
		margin-bottom: 30rpx;
		opacity: 0.5;
	}

	.empty-text {
		display: block;
		font-size: 28rpx;
		color: #718096;
	}

	.loading-container {
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 200rpx 0;
	}

	.loading-text {
		font-size: 28rpx;
		color: #718096;
	}

	.empty-container {
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		padding: 200rpx 40rpx;
		text-align: center;
	}
</style>
