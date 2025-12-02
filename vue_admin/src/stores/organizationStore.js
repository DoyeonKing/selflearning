import {defineStore} from 'pinia';
import request from '@/utils/request.js'; // 您的请求工具

const ORG_SESSION_KEY = 'xm-pro-organization'; // 统一的localStorage key

export const useOrganizationStore = defineStore('organization', {
    state: () => ({
        // 存储从 localStorage('xm-pro-organization') 读取的基本组织信息 (例如登录用户名，不含密码)
        loggedInOrganizationBasicInfo: JSON.parse(localStorage.getItem(ORG_SESSION_KEY)) || null,
        // 存储通过 API 获取的详细组织信息 (严格对应 Organization.java 实体属性名)
        detailedOrganizationInfo: {
            orgId: '',
            orgName: '',
            orgLoginUserName: '',
            // orgLoginPassword: '', // 通常不在前端状态中存储密码，或设置为null
            contactPersonPhone: '',
            serviceRegion: '',
            orgScale: 0,
            orgRating: 0.0,
            orgAccountStatus: '',
            totalServiceHours: 0,
            activityCount: 0,
            trainingCount: 0
        },
        isLoading: false, // 可选：用于跟踪API请求状态
        error: null,      // 可选：用于存储API请求错误
    }),

    getters: {
        // 计算属性，例如获取欢迎语中使用的名字
        displayName: (state) => state.detailedOrganizationInfo.orgName || state.detailedOrganizationInfo.orgLoginUserName || state.loggedInOrganizationBasicInfo?.orgLoginUserName || '组织用户',
        // 是否已登录 (可以根据 loggedInOrganizationBasicInfo 的存在来判断)
        isAuthenticated: (state) => !!state.loggedInOrganizationBasicInfo,
        // 获取当前登录组织的ID
        currentOrganizationId: (state) => state.detailedOrganizationInfo.orgId,
        // 获取组织评分
        rating: (state) => state.detailedOrganizationInfo.orgRating,
        // 其他需要的 getters
    },

    actions: {
        /**
         * 登录成功后调用的 action
         * @param {object} apiResponseData - 后端 /organization/login 返回的 res.data，应包含完整的组织信息 (不含密码)
         * @param {object} basicLoginInfo - 登录时用户输入的基本信息，如 { orgLoginUserName: 'xxx' }
         */
        loginSuccess(apiResponseData, basicLoginInfo) {
            this.loggedInOrganizationBasicInfo = basicLoginInfo; // 更新 Store 中的基本组织信息
            localStorage.setItem(ORG_SESSION_KEY, JSON.stringify(basicLoginInfo)); // 持久化

            // 如果 apiResponseData (即后端登录接口返回的数据) 已经是详细的组织信息，
            // 并且包含了所有 detailedOrganizationInfo 需要的字段，可以直接用它更新
            if (apiResponseData && apiResponseData.orgLoginUserName) {
                this.detailedOrganizationInfo.orgId = apiResponseData.orgId || '';
                this.detailedOrganizationInfo.orgName = apiResponseData.orgName || '';
                this.detailedOrganizationInfo.orgLoginUserName = apiResponseData.orgLoginUserName || basicLoginInfo.orgLoginUserName;
                this.detailedOrganizationInfo.contactPersonPhone = apiResponseData.contactPersonPhone || '';
                this.detailedOrganizationInfo.serviceRegion = apiResponseData.serviceRegion || '';
                this.detailedOrganizationInfo.orgScale = Number(apiResponseData.orgScale) || 0;
                this.detailedOrganizationInfo.orgRating = Number(apiResponseData.orgRating) || 0.0;
                this.detailedOrganizationInfo.orgAccountStatus = apiResponseData.orgAccountStatus || '';
                this.detailedOrganizationInfo.totalServiceHours = Number(apiResponseData.totalServiceHours) || 0;
                this.detailedOrganizationInfo.activityCount = Number(apiResponseData.activityCount) || 0;
                this.detailedOrganizationInfo.trainingCount = Number(apiResponseData.trainingCount) || 0;

                console.log('Store (Organization): detailedOrganizationInfo 在 loginSuccess 后更新:', JSON.parse(JSON.stringify(this.detailedOrganizationInfo)));
            } else {
                // 如果登录接口不返回详细信息，则依赖 fetchDetailedOrganizationInfo
                console.warn('Store (Organization) loginSuccess: API响应不完整或缺少关键标识，将尝试获取详细信息。');
                this.fetchDetailedOrganizationInfo();
            }
        },

        // 清空详细组织信息的辅助方法
        clearDetailedOrganizationInfo() {
            this.detailedOrganizationInfo = {
                orgId: '',
                orgName: '',
                orgLoginUserName: '',
                contactPersonPhone: '',
                serviceRegion: '',
                orgScale: 0,
                orgRating: 0.0,
                orgAccountStatus: '',
                totalServiceHours: 0,
                activityCount: 0,
                trainingCount: 0,
            };
            console.log('Store (Organization): detailedOrganizationInfo cleared.');
        },

        /**
         * 获取详细组织信息的 action
         * 通常在页面刷新或首次加载时调用，以确保组织信息完整
         */
        async fetchDetailedOrganizationInfo() {
            if (!this.loggedInOrganizationBasicInfo || !this.loggedInOrganizationBasicInfo.orgLoginUserName) {
                console.warn('Store (Organization): No logged-in organization (orgLoginUserName) to fetch details for.');
                this.clearDetailedOrganizationInfo();
                return;
            }

            this.isLoading = true;
            this.error = null;
            try {
                console.log(`Store (Organization): Fetching detailed info for orgLoginUserName: ${this.loggedInOrganizationBasicInfo.orgLoginUserName}`);
                const res = await request.get('/organization/selectByOrgLoginUserName', {
                    params: { orgLoginUserName: this.loggedInOrganizationBasicInfo.orgLoginUserName.trim() }
                });

                console.log('Store (Organization): API response for detailed info (fetchDetailedOrganizationInfo):', res); // 关键日志

                if (res.code === '200' && res.data) {
                    this.detailedOrganizationInfo.orgId = res.data.orgId || '';
                    this.detailedOrganizationInfo.orgName = res.data.orgName || '';
                    this.detailedOrganizationInfo.orgLoginUserName = res.data.orgLoginUserName || this.loggedInOrganizationBasicInfo.orgLoginUserName;
                    this.detailedOrganizationInfo.contactPersonPhone = res.data.contactPersonPhone || '';
                    this.detailedOrganizationInfo.serviceRegion = res.data.serviceRegion || '';
                    this.detailedOrganizationInfo.orgScale = Number(res.data.orgScale) || 0;
                    this.detailedOrganizationInfo.orgRating = Number(res.data.orgRating) || 0.0;
                    this.detailedOrganizationInfo.orgAccountStatus = res.data.orgAccountStatus || '';
                    this.detailedOrganizationInfo.totalServiceHours = Number(res.data.totalServiceHours) || 0;
                    this.detailedOrganizationInfo.activityCount = Number(res.data.activityCount) || 0;
                    this.detailedOrganizationInfo.trainingCount = Number(res.data.trainingCount) || 0;
                    console.log('Store (Organization): detailedOrganizationInfo updated (from fetchDetailedOrganizationInfo):', JSON.parse(JSON.stringify(this.detailedOrganizationInfo)));
                } else {
                    console.error('Store (Organization): Failed to fetch detailed organization info (API Error or no data):', res.msg);
                    this.error = res.msg || '获取组织详细信息失败';
                    this.clearDetailedOrganizationInfo();
                }
            } catch (err) {
                console.error('Store (Organization): Exception during API call for detailed organization info:', err);
                this.error = '请求组织详细信息接口出错';
                this.clearDetailedOrganizationInfo();
            } finally {
                this.isLoading = false;
            }
        },

        /**
         * 更新组织信息的 Action
         * @param {object} organizationData - 包含要更新字段的对象
         * @returns {object} 后端响应
         */
        async updateOrganizationInfo(organizationData) {
            if (!organizationData || !organizationData.orgId) {
                this.error = '更新组织信息失败：组织ID缺失';
                console.error('Store (Organization): orgId is required for update.');
                return {code: '400', msg: '组织ID是必须的'};
            }
            this.isLoading = true;
            this.error = null;
            try {
                console.log('Store (Organization): Updating organization info:', organizationData);
                // 后端 updateByOrgId 接口的 @RequestBody Organization entity 可以智能处理null值，
                // 但如果需要确保某些字段不被覆盖为null，可以在前端过滤或根据后端字段定义来决定。
                // 确保传递给后端的数据字段名与后端实体类 Organization.java 严格对应。
                const payload = {
                    ...organizationData,
                    // 确保数值类型正确转换
                    orgScale: organizationData.orgScale != null ? Number(organizationData.orgScale) : undefined,
                    orgRating: organizationData.orgRating != null ? Number(organizationData.orgRating) : undefined,
                    totalServiceHours: organizationData.totalServiceHours != null ? Number(organizationData.totalServiceHours) : undefined,
                    activityCount: organizationData.activityCount != null ? Number(organizationData.activityCount) : undefined,
                    trainingCount: organizationData.trainingCount != null ? Number(organizationData.trainingCount) : undefined,
                };

                const res = await request.put('/organization/updateByOrgId', payload);
                if (res.code === '200') {
                    console.log('Store (Organization): Organization info updated successfully.');
                    // 更新成功后，重新获取详细信息以确保本地状态最新
                    await this.fetchDetailedOrganizationInfo();
                } else {
                    this.error = res.msg || '更新组织信息失败';
                    console.error('Store (Organization): Failed to update organization info:', res.msg);
                }
                return res; // 返回后端响应，供调用方判断成功或失败
            } catch (err) {
                this.error = '更新组织信息时发生错误';
                console.error('Store (Organization): Error updating organization info:', err);
                // 尝试从错误对象中提取后端可能返回的错误信息
                if (err.response && err.response.data) {
                    return err.response.data; // 返回后端错误信息结构
                }
                throw err; // 抛出原始错误，供组件捕获
            } finally {
                this.isLoading = false;
            }
        },

        /**
         * 修改组织密码的 Action
         * @param {object} payload - 包含 orgId, oldPassword, newPassword 的对象
         * @returns {object} 后端响应
         */
        async changeOrganizationPassword({ orgId, oldPassword, newPassword }) {
            if (!orgId || !oldPassword || !newPassword) {
                this.error = '修改密码失败：参数不完整';
                return {code: '400', msg: '组织ID、旧密码和新密码均不能为空'};
            }
            this.isLoading = true;
            this.error = null;
            try {
                console.log(`Store (Organization): Changing password for organization ID: ${orgId}`);
                const res = await request.post('/organization/changePassword', {
                    orgId,
                    oldPassword,
                    newPassword
                });
                if (res.code === '200') {
                    console.log('Store (Organization): Password changed successfully.');
                } else {
                    this.error = res.msg || '修改密码失败';
                    console.error('Store (Organization): Failed to change password:', res.msg);
                }
                return res;
            } catch (err) {
                this.error = '修改密码时发生错误';
                console.error('Store (Organization): Error changing password:', err);
                // 尝试从错误对象中提取后端可能返回的错误信息
                if (err.response && err.response.data) {
                    return err.response.data; // 返回后端错误信息结构
                }
                throw err; // 抛出原始错误，供组件捕获
            } finally {
                this.isLoading = false;
            }
        },

        /**
         * 登出 action
         * 清除所有组织相关状态和 localStorage
         */
        logout() {
            this.loggedInOrganizationBasicInfo = null;
            this.clearDetailedOrganizationInfo(); // 清空详细信息
            localStorage.removeItem(ORG_SESSION_KEY); // 移除 localStorage 中的会话信息
            console.log('Store (Organization): Logged out, session cleared.');
        },

        /**
         * 应用初始化时调用，尝试从 localStorage 加载用户并获取详细信息
         * 确保应用刷新后仍能保持登录状态和获取最新组织信息
         */
        initializeStore() {
            const storedOrgUser = localStorage.getItem(ORG_SESSION_KEY);
            console.log('initializeStore - storedOrgUser:', storedOrgUser); // 关键日志

            if (storedOrgUser) {
                try {
                    this.loggedInOrganizationBasicInfo = JSON.parse(storedOrgUser);
                    console.log('initializeStore - parsed loggedInOrganizationBasicInfo:', this.loggedInOrganizationBasicInfo); // 关键日志

                    // 确保 loggedInOrganizationBasicInfo 和其 orgLoginUserName 有效才调用 fetch
                    if (this.loggedInOrganizationBasicInfo &&
                        typeof this.loggedInOrganizationBasicInfo.orgLoginUserName === 'string' &&
                        this.loggedInOrganizationBasicInfo.orgLoginUserName.trim() !== '') {
                        this.fetchDetailedOrganizationInfo();
                    } else {
                        // 如果 localStorage 中的数据不包含有效的 orgLoginUserName，视为无效会话
                        console.warn('Store (Organization) initializeStore: 已加载基础组织信息，但无有效 orgLoginUserName 获取详细信息。');
                        this.logout(); // 清理无效状态
                    }
                } catch (e) {
                    console.error("Store (Organization) 初始化时解析 localStorage 用户数据错误:", e);
                    this.logout(); // 解析失败则清空，避免应用出错
                }
            } else {
                console.log('initializeStore - No stored organization user found in localStorage.');
            }
        }
    }
});