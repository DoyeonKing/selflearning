<template>
	<view class="container">
		<!-- 头部logo区域 - 仅在登录模式显示 -->
		<view class="header" v-if="!isActivation">
			<image class="logo" src="/static/logo.png" mode="aspectFit"></image>
			<text class="app-title">"挂了吗"校医院挂号系统</text>
		</view>
		
		<!-- 表单容器：登录 / 激活 -->
		<view class="form-container">
            <!-- 主标题：根据当前模式显示不同标题 -->
            <text class="title">{{ isActivation ? '账号激活' : '患者登录' }}</text>

			<view v-if="!isActivation" class="login-section">
				<view class="input-group">
					<text class="input-label">学号</text>
					<input class="input-field" v-model="loginForm.identifier" placeholder="请输入学号" />
				</view>
				<view class="input-group">
					<text class="input-label">密码</text>
					<input class="input-field" v-model="loginForm.password" type="password" placeholder="请输入密码" />
				</view>
				<button class="submit-btn" @click="handleLogin">登 录</button>
				<view class="footer-text" @click="switchToActivation">
					<text>首次使用？点击激活账户</text>
				</view>
			</view>

			<view v-else class="activation-section">
				<!-- 激活步骤指示器 -->
            <!-- 激活步骤进度条 -->
            <view class="step-indicator">
					<view class="step" :class="{ active: activationStep >= 1, completed: activationStep > 1 }">
						<text class="step-number">1</text>
						<text class="step-text">验证信息</text>
					</view>
					<view class="step-line" :class="{ active: activationStep > 1 }"></view>
					<view class="step" :class="{ active: activationStep >= 2, completed: activationStep > 2 }">
						<text class="step-number">2</text>
						<text class="step-text">身份验证</text>
					</view>
				</view>

				<view v-if="activationStep === 1" class="step-content">
					<view class="step-title">第一步：验证初始信息</view>
					<view class="input-group">
						<text class="input-label">学号</text>
						<input class="input-field" v-model="activationForm.identifier" placeholder="请输入学号" />
					</view>
					<view class="input-group">
						<text class="input-label">初始密码</text>
						<input class="input-field" v-model="activationForm.initialPassword" type="password" placeholder="请输入初始密码" />
					</view>
					<button class="submit-btn" @click="handleActivationStep1">下一步</button>
				</view>

			<view v-if="activationStep === 2" class="step-content">
				<view class="step-title">第二步：身份验证</view>
				<view class="info-desc">
					<text>为了您的账户安全，请输入您的身份证号进行验证</text>
				</view>
				<view class="input-group">
					<text class="input-label">身份证号</text>
					<input class="input-field" v-model="activationForm.idCardInput" placeholder="请输入身份证号后6位" maxlength="6" />
				</view>
				<view class="input-group">
					<text class="input-label">新密码</text>
					<input class="input-field" v-model="activationForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" />
				</view>
				<view class="input-group">
					<text class="input-label">确认密码</text>
					<input class="input-field" v-model="activationForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
				</view>
				<button class="submit-btn" @click="handleVerification">完成激活</button>
			</view>

				<view class="footer-text" @click="switchToLogin">
					<text>返回登录</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { post } from '../../utils/request.js'
	
	export default {
		data() {
			return {
				isActivation: false,
				activationStep: 1, // 1: initial info, 2: verification, 3: set password
				loginForm: {
					identifier: '',
					password: ''
				},
				activationForm: {
					identifier: '',
					initialPassword: '',
					idCard: '',          // 从后端获取的脱敏身份证号
					idCardInput: '',     // 用户输入的身份证号后6位
					phone: '',
					newPassword: '',
					confirmPassword: ''
				}
			}
		},
		methods: {
			switchToActivation() {
				this.isActivation = true;
				this.activationStep = 1;
				// 清空激活表单
				this.activationForm = {
					identifier: '',
					initialPassword: '',
					idCard: '',
					phone: '',
					newPassword: '',
					confirmPassword: ''
				};
			},
			switchToLogin() {
				this.isActivation = false;
				// 清空登录表单
				this.loginForm = {
					identifier: '',
					password: ''
				};
			},
			
		// 患者正常登录
		async handleLogin() {
			if (!this.loginForm.identifier || !this.loginForm.password) {
				uni.showToast({ title: '请输入学号和密码', icon: 'none' });
				return;
			}
			
		const startTime = Date.now()
		console.log('[LOGIN] 开始登录请求')
		
		try {
			const response = await post('/api/auth/patient/login', {
				identifier: this.loginForm.identifier,
				password: this.loginForm.password
			}, {
				showLoading: true,
				loadingText: '登录中...',
				timeout: 30000 // 增加到30秒超时（真机调试网络可能较慢）
			});
			
			const duration = Date.now() - startTime
			console.log(`[LOGIN] 登录请求完成，耗时: ${duration}ms`)
				
				if (response.code === '200') {
					// 保存token和用户信息
					uni.setStorageSync('patientToken', response.data.token);
					
					// 适配后端返回的数据格式：data.userInfo 包含患者信息
					const userInfo = response.data.userInfo || {};
					const adaptedInfo = {
						id: userInfo.patientId,
						name: userInfo.fullName,
						identifier: userInfo.identifier
					};
					uni.setStorageSync('patientInfo', adaptedInfo);
					
					console.log('登录成功，保存的用户信息:', adaptedInfo);
					
					uni.showToast({ title: '登录成功', icon: 'success' });
					// 跳转到主页
					setTimeout(() => {
						uni.switchTab({ url: '/pages/index/index' });
					}, 1500);
				} else {
					uni.showToast({ title: response.msg || '登录失败', icon: 'none' });
				}
			} catch (error) {
				uni.hideLoading();
				console.error('登录请求失败:', error);
			}
			},
			
			// 激活第一步：验证初始登录信息
			async handleActivationStep1() {
				if (!this.activationForm.identifier || !this.activationForm.initialPassword) {
					uni.showToast({ title: '请输入学号和初始密码', icon: 'none' });
					return;
				}
				
			uni.showLoading({ title: '验证中...' });
			
			try {
				const response = await post('/api/auth/verify-patient', {
					identifier: this.activationForm.identifier,
					initialPassword: this.activationForm.initialPassword
				});
				
				uni.hideLoading();
				
				// 后端返回格式：{"message": "..."} 表示成功，{"error": "..."} 表示失败
				if (response.message) {
					// 验证成功，进入第二步
					this.activationStep = 2;
					uni.showToast({ title: response.message, icon: 'success' });
				} else if (response.error) {
					uni.showToast({ title: response.error, icon: 'none' });
				} else {
					uni.showToast({ title: '验证失败', icon: 'none' });
				}
			} catch (error) {
				uni.hideLoading();
				console.error('验证请求失败:', error);
			}
			},
			
		// 激活第二步：身份验证
		async handleVerification() {
			// 验证身份证号
			if (!this.activationForm.idCardInput) {
				uni.showToast({ title: '请输入身份证号后6位', icon: 'none' });
				return;
			}
			if (this.activationForm.idCardInput.length !== 6) {
				uni.showToast({ title: '请输入完整的身份证号后6位', icon: 'none' });
				return;
			}
			
			// 验证新密码
			if (!this.activationForm.newPassword || !this.activationForm.confirmPassword) {
				uni.showToast({ title: '请输入新密码', icon: 'none' });
				return;
			}
			if (this.activationForm.newPassword.length < 6 || this.activationForm.newPassword.length > 20) {
				uni.showToast({ title: '密码长度需在6-20位之间', icon: 'none' });
				return;
			}
			if (this.activationForm.newPassword !== this.activationForm.confirmPassword) {
				uni.showToast({ title: '两次输入的密码不一致', icon: 'none' });
				return;
			}
			
		uni.showLoading({ title: '身份验证中...' });
		
		try {
			const response = await post('/api/auth/activate-patient', {
				identifier: this.activationForm.identifier,
				idCardEnding: this.activationForm.idCardInput,  // 后端使用 idCardEnding 参数名
				newPassword: this.activationForm.newPassword,
				confirmPassword: this.activationForm.confirmPassword
			});
			
			uni.hideLoading();
			
			// 后端返回格式：{"message": "..."} 表示成功，{"error": "..."} 表示失败
			if (response.message) {
				uni.showToast({ title: response.message, icon: 'success' });
				// 返回登录界面
				setTimeout(() => {
					this.isActivation = false;
					this.activationStep = 1;
					// 清空表单
					this.activationForm = {
						identifier: '',
						initialPassword: '',
						idCard: '',
						idCardInput: '',
						phone: '',
						newPassword: '',
						confirmPassword: ''
					};
				}, 2000);
			} else if (response.error) {
				uni.showToast({ title: response.error, icon: 'none' });
			} else {
				uni.showToast({ title: '激活失败', icon: 'none' });
			}
		} catch (error) {
			uni.hideLoading();
			console.error('激活请求失败:', error);
		}
		},
			
			completeActivation() {
				// 这个方法已经合并到handleVerification中
				this.handleVerification();
			}
		}
	}
</script>

<style>
	.container {
		min-height: 100vh;
		background: #FFFFFF;
		display: flex;
		flex-direction: column;
		justify-content: flex-start;
		align-items: center;
		padding: 16rpx 20rpx 36rpx;
	}
	
	/* 头部样式 - 仅登录时显示 */
	.header {
		text-align: center;
		margin-bottom: 30rpx;
	}
	
	.logo {
		width: 96rpx;
		height: 96rpx;
		margin-bottom: 16rpx;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
		opacity: 0.95;
	}
	
	.app-title {
		display: block;
		font-size: 32rpx;
		font-weight: bold;
		color: #222222;
		margin-bottom: 8rpx;
	}
	
.app-subtitle {
    display: block;
    font-size: 24rpx;
    color: #666666;
}
	
.form-container {
    background-color: #ffffff;
    border-radius: 24rpx;
    padding: 40rpx 32rpx;
    box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.06);
    width: 100%;
    max-width: 600rpx;
}
	
.title {
    display: block;
    font-size: 44rpx;
    font-weight: bold;
    text-align: center;
    margin-bottom: 50rpx;
    color: #111111;
}
	
	/* 输入组样式 */
	.input-group {
		margin-bottom: 30rpx;
	}
	
	.input-label {
		display: block;
		font-size: 28rpx;
		color: #666666;
		margin-bottom: 15rpx;
		font-weight: 500;
	}
	
.input-field {
    width: 100%;
    height: 88rpx;
    padding: 0 30rpx;
    border-radius: 12rpx;
    background-color: #f8f9fa;
    border: 2rpx solid #e5e7eb;
    font-size: 30rpx;
    color: #111111;
    box-sizing: border-box;
    transition: all 0.2s ease;
}
	
.input-field:focus {
    border-color: #2563eb;
    background-color: #ffffff;
    box-shadow: 0 0 0 6rpx rgba(37, 99, 235, 0.08);
}
	
.submit-btn {
    width: 100%;
    background: #2563eb;
    color: #ffffff;
    border: none;
    border-radius: 12rpx;
    height: 96rpx;
    line-height: 96rpx;
    font-size: 32rpx;
    font-weight: bold;
    margin-top: 30rpx;
    box-shadow: 0 8rpx 24rpx rgba(37, 99, 235, 0.25);
    transition: all 0.2s ease;
}
	
.submit-btn:active {
    transform: translateY(2rpx);
    box-shadow: 0 4rpx 12rpx rgba(37, 99, 235, 0.35);
}
	
.footer-text {
    text-align: center;
    margin-top: 40rpx;
    color: #2563eb;
    font-size: 28rpx;
    text-decoration: underline;
}
	
	/* 激活步骤指示器 */
	.step-indicator {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 40rpx;
	}
	
	.step {
		display: flex;
		flex-direction: column;
		align-items: center;
		position: relative;
	}
	
	.step-number {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		background-color: #e9ecef;
		color: #6c757d;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 24rpx;
		font-weight: bold;
		margin-bottom: 10rpx;
		transition: all 0.3s ease;
	}
	
	.step.active .step-number {
		background-color: #667eea;
		color: #ffffff;
	}
	
	.step.completed .step-number {
		background-color: #28a745;
		color: #ffffff;
	}
	
	.step-text {
		font-size: 24rpx;
		color: #6c757d;
		transition: all 0.3s ease;
	}
	
	.step.active .step-text {
		color: #667eea;
		font-weight: bold;
	}
	
	.step.completed .step-text {
		color: #28a745;
		font-weight: bold;
	}
	
	.step-line {
		width: 100rpx;
		height: 4rpx;
		background-color: #e9ecef;
		margin: 0 20rpx;
		margin-top: -30rpx;
		transition: all 0.3s ease;
	}
	
.step-line.active {
    background-color: #2563eb;
}
	
	/* 步骤内容 */
	.step-content {
		margin-bottom: 30rpx;
	}
	
	.step-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333333;
		margin-bottom: 30rpx;
		text-align: center;
	}
	
	.verification-desc {
		text-align: center;
		margin-bottom: 30rpx;
		padding: 20rpx;
		background-color: #e8f5e8;
		border-radius: 12rpx;
		border-left: 6rpx solid #28a745;
	}
	
	.verification-desc text {
		font-size: 26rpx;
		color: #28a745;
		line-height: 1.5;
	}
	
	/* 信息提示样式 */
	.info-desc {
		text-align: center;
		margin-bottom: 30rpx;
		padding: 20rpx;
		background-color: #eff6ff;
		border-radius: 12rpx;
		border-left: 6rpx solid #2563eb;
	}
	
	.info-desc text {
		font-size: 26rpx;
		color: #2563eb;
		line-height: 1.5;
	}
	
	/* 登录和激活区域 */
	.login-section, .activation-section {
		animation: fadeIn 0.5s ease-in-out;
	}
	
	@keyframes fadeIn {
		from {
			opacity: 0;
			transform: translateY(20rpx);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}
</style>
