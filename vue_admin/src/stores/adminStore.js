import {defineStore} from 'pinia';
import request from '@/utils/request.js'; // 您的请求工具

// Key for localStorage
const ADMIN_SESSION_KEY = 'xm-pro-admin'; // 管理员会话的localStorage key

export const useAdminStore = defineStore('admin', {
    state: () => ({
        // 存储从 localStorage 读取的基本管理员登录信息
        loggedInAdminBasicInfo: JSON.parse(localStorage.getItem(ADMIN_SESSION_KEY)) || null,
        // 存储通过 API 获取的详细管理员信息 (严格对应 tbl_Administrator 实体)
        detailedAdminInfo: {
            adminId: '',
            name: '',
            gender: '',
            idCardNumber: '',
            phoneNumber: '',
            password: '', // 按照您的要求，保留密码字段，但请注意安全风险
            serviceArea: '',
            currentPosition: '',
            permissionLevel: ''
            // 如果 Administrator.java 中有 permissionScope 且数据库有对应列，也应在此处添加
            // permissionScope: '',
        },
        isLoading: false, // 可选：用于跟踪API请求状态
        error: null,      // 可选：用于存储API请求错误
    }),

    getters: {
        // 显示名称
        displayName: (state) => state.detailedAdminInfo.name || state.loggedInAdminBasicInfo?.name || state.loggedInAdminBasicInfo?.adminId || '管理员',
        // 是否已登录
        isAuthenticated: (state) => !!state.loggedInAdminBasicInfo,
        // 获取管理员ID
        currentAdminId: (state) => state.detailedAdminInfo.adminId || state.loggedInAdminBasicInfo?.adminId,
        // 获取权限等级
        adminPermissionLevel: (state) => state.detailedAdminInfo.permissionLevel || state.loggedInAdminBasicInfo?.permissionLevel,
        // 其他需要的 getters
    },

    actions: {
        // 登录成功后调用的 action
        loginSuccess(apiResponseData, basicLoginInfoFromLogin) {
            // apiResponseData 是后端 /administrator/login 返回的 res.data (完整的管理员对象)
            // basicLoginInfoFromLogin 应该包含用于登录的标识符, e.g., { adminId: 'input_value' } or { name: 'input_value' }

            this.loggedInAdminBasicInfo = basicLoginInfoFromLogin; // 更新 Store 中的基本管理员信息
            localStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(basicLoginInfoFromLogin)); // 持久化
            
            // 确保状态立即更新
            console.log('AdminStore: 登录状态已更新', this.loggedInAdminBasicInfo);

            if (apiResponseData && (apiResponseData.adminId || apiResponseData.name)) { // 确保API响应中至少有一个有效标识
                // 更新 detailedAdminInfo 的逻辑 (确保字段名匹配)
                this.detailedAdminInfo.adminId = apiResponseData.adminId || '';
                this.detailedAdminInfo.name = apiResponseData.name || '';
                this.detailedAdminInfo.gender = apiResponseData.gender || '';
                this.detailedAdminInfo.idCardNumber = apiResponseData.idCardNumber || '';
                this.detailedAdminInfo.phoneNumber = apiResponseData.phoneNumber || '';
                this.detailedAdminInfo.password = apiResponseData.password || ''; // 存储密码
                this.detailedAdminInfo.serviceArea = apiResponseData.serviceArea || '';
                this.detailedAdminInfo.currentPosition = apiResponseData.currentPosition || '';
                this.detailedAdminInfo.permissionLevel = apiResponseData.permissionLevel || '';
                // if ('permissionScope' in apiResponseData) { // 如果实体类和后端响应中有此字段
                //    this.detailedAdminInfo.permissionScope = apiResponseData.permissionScope || '';
                // }
                console.log('AdminStore: detailedAdminInfo 在 loginSuccess 后更新:', JSON.parse(JSON.stringify(this.detailedAdminInfo)));
            } else {
                // 如果登录接口不返回详细信息，或者返回的信息不包含关键标识，则依赖 fetchDetailedAdminInfo
                console.warn('AdminStore (loginSuccess): API响应不完整或缺少关键标识，将尝试获取详细信息。');
                this.fetchDetailedAdminInfo();
            }
        },

        // 清空详细管理员信息的辅助方法
        clearDetailedAdminInfo() {
            this.detailedAdminInfo = {
                adminId: '',
                name: '',
                gender: '',
                idCardNumber: '',
                phoneNumber: '',
                password: '', // 清空密码
                serviceArea: '',
                currentPosition: '',
                permissionLevel: ''
                // permissionScope: '',
            };
            console.log('AdminStore: detailedAdminInfo cleared.');
        },

        // 获取详细管理员信息的 action
        async fetchDetailedAdminInfo() {
            // 优先使用 loggedInAdminBasicInfo 中的 adminId
            const adminIdToFetch = this.loggedInAdminBasicInfo?.adminId;
            // 如果 adminId 不可用，可以考虑使用 name 作为备用查询条件（如果后端支持）
            const adminNameToFetch = this.loggedInAdminBasicInfo?.name;
            let identifierForAPI = null;
            let endpoint = '';

            if (adminIdToFetch && typeof adminIdToFetch === 'string' && adminIdToFetch.trim() !== '') {
                identifierForAPI = adminIdToFetch.trim();
                endpoint = `/administrator/selectByAdminId/${identifierForAPI}`;
                console.log(`AdminStore: 正在为 adminId: ${identifierForAPI} 获取详细信息`);
            } else if (adminNameToFetch && typeof adminNameToFetch === 'string' && adminNameToFetch.trim() !== '') {
                 // 假设后端支持按 name 查询，如果不支持，此分支无效或需要调整
                identifierForAPI = adminNameToFetch.trim();
                endpoint = `/administrator/selectByName/${identifierForAPI}`; // 假设的端点
                console.warn(`AdminStore: adminId 无效，尝试使用 name: ${identifierForAPI} 获取详细信息。请确保后端支持此查询。`);
            } else {
                console.warn(`AdminStore: 没有有效管理员标识 (adminId 或 name) 来获取详细信息。`);
                this.clearDetailedAdminInfo(); // 没有有效标识，清空详细信息
                return;
            }

            this.isLoading = true;
            this.error = null;
            try {
                const res = await request.get(endpoint);
                console.log('AdminStore: 详细信息API响应:', res);

                if (res.code === '200' && res.data) {
                    this.detailedAdminInfo.adminId = res.data.adminId || this.loggedInAdminBasicInfo?.adminId || '';
                    this.detailedAdminInfo.name = res.data.name || this.loggedInAdminBasicInfo?.name || '';
                    this.detailedAdminInfo.gender = res.data.gender || '';
                    this.detailedAdminInfo.idCardNumber = res.data.idCardNumber || '';
                    this.detailedAdminInfo.phoneNumber = res.data.phoneNumber || '';
                    this.detailedAdminInfo.password = res.data.password || ''; // 存储密码
                    this.detailedAdminInfo.serviceArea = res.data.serviceArea || '';
                    this.detailedAdminInfo.currentPosition = res.data.currentPosition || '';
                    this.detailedAdminInfo.permissionLevel = res.data.permissionLevel || '';
                    // if ('permissionScope' in res.data) {
                    //    this.detailedAdminInfo.permissionScope = res.data.permissionScope || '';
                    // }

                    // 补充/更新 loggedInAdminBasicInfo
                    if (this.loggedInAdminBasicInfo) {
                        this.loggedInAdminBasicInfo.adminId = this.detailedAdminInfo.adminId || this.loggedInAdminBasicInfo.adminId;
                        this.loggedInAdminBasicInfo.name = this.detailedAdminInfo.name || this.loggedInAdminBasicInfo.name;
                        this.loggedInAdminBasicInfo.permissionLevel = this.detailedAdminInfo.permissionLevel || this.loggedInAdminBasicInfo.permissionLevel;
                        localStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(this.loggedInAdminBasicInfo));
                    } else if (this.detailedAdminInfo.adminId) { // 如果之前 basic info 为空，但获取到了详细信息
                        this.loggedInAdminBasicInfo = {
                            adminId: this.detailedAdminInfo.adminId,
                            name: this.detailedAdminInfo.name,
                            permissionLevel: this.detailedAdminInfo.permissionLevel
                        };
                        localStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(this.loggedInAdminBasicInfo));
                    }

                    console.log('AdminStore: detailedAdminInfo 更新完毕:', JSON.parse(JSON.stringify(this.detailedAdminInfo)));
                } else {
                    console.error('AdminStore: 获取管理员详细信息失败 (API错误或无数据):', res.msg);
                    this.error = res.msg || '获取管理员详细信息失败';
                }
            } catch (err) {
                console.error('AdminStore: 调用管理员详细信息API时发生异常:', err);
                this.error = '请求管理员详细信息接口出错';
            } finally {
                this.isLoading = false;
            }
        },

        // 更新管理员信息的 Action
        async updateAdminInfo(adminData) {
            if (!adminData || !adminData.adminId) {
                this.error = '更新管理员信息失败：管理员ID缺失';
                console.error('AdminStore: adminId 是更新操作所必需的。');
                return {code: '400', msg: '管理员ID是必须的'};
            }
            this.isLoading = true;
            this.error = null;
            try {
                console.log('AdminStore: 正在更新管理员信息:', adminData);
                // 确保不发送密码字段，除非后端允许通过此接口更新密码
                const payload = { ...adminData };
                delete payload.password; // 通常不通过通用更新接口修改密码

                const res = await request.put('/administrator/updateInfo', payload);
                if (res.code === '200') {
                    console.log('AdminStore: 管理员信息更新成功。');
                    await this.fetchDetailedAdminInfo(); // 重新获取最新信息
                } else {
                    this.error = res.msg || '更新管理员信息失败';
                    console.error('AdminStore: 更新管理员信息失败:', res.msg);
                }
                return res;
            } catch (err) {
                this.error = '更新管理员信息时发生错误';
                console.error('AdminStore: 更新管理员信息时出错:', err);
                throw err;
            } finally {
                this.isLoading = false;
            }
        },

        // 修改管理员密码的 Action
        // 修改管理员密码
        async changeAdminPassword({ adminId, oldPassword, newPassword }) {
            if (!adminId || !oldPassword || !newPassword) {
                 this.error = '修改密码失败：参数不完整';
                 return { code: '400', msg: '管理员ID、原密码和新密码均不能为空' };
            }
            this.isLoading = true;
            this.error = null;
            try {
                const res = await request.post('/administrator/changePassword', {
                    adminId,
                    oldPassword,
                    newPassword
                });
                if (res.code === '200') {
                    console.log('AdminStore: Password changed successfully.');
                    // 密码修改成功后，可以考虑清除旧的会话或提示用户重新登录
                } else {
                    this.error = res.msg || '修改密码失败';
                }
                return res; // 将后端的完整响应返回给调用方
            } catch (err) {
                this.error = '修改密码时发生网络或系统错误';
                console.error('AdminStore: Error changing password:', err);
                throw err; // 让调用方也能捕获到原始错误
            } finally {
                this.isLoading = false;
            }
        },


        // 登出 action
        logout() {
            this.loggedInAdminBasicInfo = null;
            this.clearDetailedAdminInfo();
            localStorage.removeItem(ADMIN_SESSION_KEY);
            console.log('AdminStore: 已登出, 会话已清除。');
        },

        // 应用初始化时调用
        initializeStore() {
            const storedAdmin = localStorage.getItem(ADMIN_SESSION_KEY);
            if (storedAdmin) {
                try {
                    this.loggedInAdminBasicInfo = JSON.parse(storedAdmin);
                    // 确保 loggedInAdminBasicInfo 和其 adminId/name 有效才调用 fetch
                    if (this.loggedInAdminBasicInfo &&
                        ((this.loggedInAdminBasicInfo.adminId && typeof this.loggedInAdminBasicInfo.adminId === 'string' && this.loggedInAdminBasicInfo.adminId.trim() !== '') ||
                         (this.loggedInAdminBasicInfo.name && typeof this.loggedInAdminBasicInfo.name === 'string' && this.loggedInAdminBasicInfo.name.trim() !== ''))) {
                        this.fetchDetailedAdminInfo();
                    } else if (this.loggedInAdminBasicInfo) {
                        // 有基础信息但没有有效的 adminId 或 name
                        console.warn('AdminStore (initializeStore): 已加载基础管理员信息，但无有效 adminId 或 name 获取详细信息。');
                    } else {
                        this.logout(); // 如果解析出的信息无效
                    }
                } catch (e) {
                    console.error("AdminStore 初始化时解析 localStorage 管理员数据错误:", e);
                    this.logout();
                }
            }
        }
    }
});
