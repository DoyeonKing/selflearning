import {defineStore} from 'pinia';
import request from '@/utils/request.js'; // 您的请求工具

// Key for localStorage
const USER_SESSION_KEY = 'xm-pro-user'; // 统一的localStorage key

export const useUserStore = defineStore('user', {
    state: () => ({
        // 存储从 localStorage('xm-pro-user') 读取的基本用户信息
        loggedInUser: JSON.parse(localStorage.getItem(USER_SESSION_KEY)) || null,
        // 存储通过 API 获取的详细志愿者信息
        detailedVolunteerInfo: {
            volunteerId: '',
            username: '',
            password: '',
            name: '',
            phone: '',
            idCard: '',
            registrationTime: '',
            country: '',
            gender: '',
            ethnicity: '',
            politicalStatus: '',
            highestEducation: '',
            employmentStatus: '',
            serviceCategory: '',
            serviceArea: '',
            totalServiceHours: 0,
            volunteerComprehensiveScore: 0,
            //totalTrainingHours: 0,
            accountStatus: ''
            // ... 其他您需要的字段
        },
        // 志愿者星级
        volunteerStarLevelFromView: 0,


        isLoading: false, // 可选：用于跟踪API请求状态
        error: null,      // 可选：用于存储API请求错误
    }),

    getters: {
        // 计算属性，例如获取欢迎语中使用的名字
        displayName: (state) => state.detailedVolunteerInfo.name || state.detailedVolunteerInfo.username || state.loggedInUser?.username || '访客',
        // 是否已登录 (可以根据 loggedInUser 或 token 的存在来判断)
        isAuthenticated: (state) => !!state.loggedInUser, // 或者检查 token
        // ... 其他需要的 getters
        volunteerScore: (state) => state.detailedVolunteerInfo.volunteerComprehensiveScore,
        serviceHours: (state) => state.detailedVolunteerInfo.totalServiceHours,
        trainingHours: (state) => state.detailedVolunteerInfo.totalTrainingHours,
        // 获取星级的 Getter
        starLevel: (state) => state.volunteerStarLevelFromView,
    },

    actions: {
        // 登录成功后调用的 action
        loginSuccess(apiResponseData, basicLoginInfo) {
            // apiResponseData 是后端 /volunteer/login 返回的 res.data，它本身就应该是详细的 Volunteer 对象
            // basicLoginInfo 是 { username: 'xxx' }

            this.loggedInUser = basicLoginInfo; // 更新 Store 中的基本用户信息
            localStorage.setItem(USER_SESSION_KEY, JSON.stringify(basicLoginInfo)); // 持久化

            // 如果 apiResponseData (即后端登录接口返回的数据) 已经是详细的用户信息，
            // 并且包含了所有 detailedVolunteerInfo 需要的字段，可以直接用它更新
            if (apiResponseData && apiResponseData.username) {
                // 更新 detailedVolunteerInfo 的逻辑 (确保字段名匹配)
                this.detailedVolunteerInfo.volunteerId = apiResponseData.volunteerId || '';
                this.detailedVolunteerInfo.username = apiResponseData.username || basicLoginInfo.username;
                this.detailedVolunteerInfo.name = apiResponseData.name || '';
                this.detailedVolunteerInfo.password = apiResponseData.password || ''; // 密码不应该直接存储在 Store 中
                this.detailedVolunteerInfo.phone = apiResponseData.phone || '';
                this.detailedVolunteerInfo.idCard = apiResponseData.idCard || ''; // 实体类中是 idCard
                this.detailedVolunteerInfo.registrationTime = apiResponseData.registrationTime || '';
                this.detailedVolunteerInfo.country = apiResponseData.country || '';
                this.detailedVolunteerInfo.gender = apiResponseData.gender || '';
                this.detailedVolunteerInfo.ethnicity = apiResponseData.ethnicity || '';
                this.detailedVolunteerInfo.politicalStatus = apiResponseData.politicalStatus || '';
                this.detailedVolunteerInfo.highestEducation = apiResponseData.highestEducation || ''; // 实体类中是 highestEducation
                this.detailedVolunteerInfo.employmentStatus = apiResponseData.employmentStatus || ''; // 实体类中是 employmentStatus
                this.detailedVolunteerInfo.serviceCategory = apiResponseData.serviceCategory || '';
                this.detailedVolunteerInfo.serviceArea = apiResponseData.serviceArea || '';
                this.detailedVolunteerInfo.totalServiceHours = apiResponseData.totalServiceHours || 0;
                this.detailedVolunteerInfo.volunteerComprehensiveScore = apiResponseData.volunteerComprehensiveScore || 0;
                this.detailedVolunteerInfo.totalTrainingHours = apiResponseData.totalTrainingHours || 0;
                this.detailedVolunteerInfo.accountStatus = apiResponseData.accountStatus || '';
                console.log('Store: detailedVolunteerInfo 在 loginSuccess 后更新:', JSON.parse(JSON.stringify(this.detailedVolunteerInfo)));

                // 登录成功后，也获取星级
                this.fetchVolunteerStarLevel();
            } else {
                // 如果登录接口不返回详细信息，则依赖 fetchDetailedVolunteerInfo
                this.fetchDetailedVolunteerInfo();
                this.fetchVolunteerStarLevel();    // 也获取星级
            }
        },

        // 获取详细志愿者信息的 action
        async fetchDetailedVolunteerInfo() {
            if (!this.loggedInUser || !this.loggedInUser.username) {
                console.warn('Store: No logged-in user (username) to fetch details for.');
                // 清空详细信息，防止显示旧数据
                this.clearDetailedInfo();
                return;
            }

            this.isLoading = true;
            this.error = null;
            try {
                console.log(`Store: Fetching detailed info for username: ${this.loggedInUser.username}`);
                const res = await request.get('/volunteer/selectByUsername', { // 确保这是获取详细信息的正确API
                    params: {username: this.loggedInUser.username.trim()}
                });

                console.log('Store: API response for detailed info:', res); // 打印API返回结果，非常重要！

                if (res.code === '200' && res.data) {
                    // **关键：将API返回的所有相关字段都赋给 detailedVolunteerInfo**
                    // Object.assign(this.detailedVolunteerInfo, res.data); // 这种方式如果 res.data 多了或少了字段会有问题

                    // 更安全的做法是逐个赋值或确保 res.data 结构与 detailedVolunteerInfo 兼容
                    // 例如，如果后端返回的字段名与前端 state 中的字段名不完全一致，需要手动映射
                    this.detailedVolunteerInfo.volunteerId = res.data.volunteerId || '';
                    this.detailedVolunteerInfo.username = res.data.username || this.loggedInUser.username; // 优先用API返回的
                    this.detailedVolunteerInfo.name = res.data.name || '';
                    this.detailedVolunteerInfo.password = res.data.password || ''; // 密码不应该直接存储在 Store 中
                    this.detailedVolunteerInfo.phone = res.data.phone || '';
                    this.detailedVolunteerInfo.idCard = res.data.idCard || ''; // 注意后端是 IDCardNumber，前端是 idCard
                    this.detailedVolunteerInfo.registrationTime = res.data.registrationTime || '';
                    this.detailedVolunteerInfo.country = res.data.country || '';
                    this.detailedVolunteerInfo.gender = res.data.gender || '';
                    this.detailedVolunteerInfo.ethnicity = res.data.ethnicity || '';
                    this.detailedVolunteerInfo.politicalStatus = res.data.politicalStatus || '';
                    this.detailedVolunteerInfo.highestEducation = res.data.highestEducation || '';
                    this.detailedVolunteerInfo.employmentStatus = res.data.employmentStatus || '';
                    this.detailedVolunteerInfo.serviceCategory = res.data.serviceCategory || '';
                    this.detailedVolunteerInfo.serviceArea = res.data.serviceArea || '';
                    this.detailedVolunteerInfo.totalServiceHours = res.data.totalServiceHours || 0;
                    this.detailedVolunteerInfo.volunteerComprehensiveScore = res.data.volunteerComprehensiveScore || 0;
                    // this.detailedVolunteerInfo.totalTrainingHours = res.data.totalTrainingHours || 0; // 如果有
                    this.detailedVolunteerInfo.accountStatus = res.data.accountStatus || '';

                    console.log('Store: detailedVolunteerInfo updated:', JSON.parse(JSON.stringify(this.detailedVolunteerInfo)));
                    this.fetchVolunteerStarLevel();    // **新增/确认调用：获取星级信息**
                } else {
                    console.error('Store: Failed to fetch detailed volunteer info (API Error or no data):', res.msg);
                    this.error = res.msg || '获取详细信息失败';
                    this.clearDetailedInfo();
                }
            } catch (err) {
                console.error('Store: Exception during API call for detailed volunteer info:', err);
                this.error = '请求详细信息接口出错';
                this.clearDetailedInfo();
            } finally {
                this.isLoading = false;
            }
        },

        // --- 新增：获取志愿者星级的 Action ---
        async fetchVolunteerStarLevel() {
            const identifier = this.detailedVolunteerInfo.volunteerId;
            //|| this.loggedInUser?.username
            if (!identifier) {
                console.warn('Store: 无志愿者标识 (ID 或 username) 可获取星级。');
                this.volunteerStarLevelFromView = 0;
                return;
            }

            // 确保 identifier 是字符串并去除空格，准备用作路径参数
            const pathIdentifier = identifier.toString().trim();
            if (!pathIdentifier) { // 再次检查 trim 后是否为空
                console.warn('Store: 处理后的标识符为空，无法获取星级。');
                this.volunteerStarLevelFromView = 0;
                return;
            }


            this.isLoadingStars = true;
            try {
                console.log(`Store: 正在为标识符 ${pathIdentifier} 获取星级`);
                // *** 修改这里：使用 pathIdentifier ***
                const res = await request.get(`/volunteer/${pathIdentifier}/stars`);

                console.log('Store: 星级API响应:', res);

                if (res.code === '200' && res.data && typeof res.data.starLevel === 'number') {
                    this.volunteerStarLevelFromView = res.data.starLevel;
                    console.log('Store: volunteerStarLevelFromView 已更新:', this.volunteerStarLevelFromView);
                } else {
                    console.error('Store: 获取星级失败 (API错误或数据格式不正确):', res.msg);
                    this.volunteerStarLevelFromView = 0;
                }
            } catch (err) {
                console.error('Store: 调用星级API时发生异常:', err);
                this.volunteerStarLevelFromView = 0;
            } finally {
                this.isLoadingStars = false;
            }
        },

        // --- 【重要新增】更新志愿者信息的 Action ---
        async updateVolunteerInfo(volunteerData) {
            this.isLoading = true;
            this.error = null;
            try {
                console.log('Store: Updating volunteer info:', volunteerData);
                // 调用后端更新接口：PUT /volunteer/updateById
                const res = await request.put('/volunteer/updateById', volunteerData);
                if (res.code === '200') {
                    console.log('Store: Volunteer info updated successfully.');
                    // 更新成功后，可以考虑重新获取详细信息以确保数据最新
                    // await this.fetchDetailedVolunteerInfo(volunteerData.volunteerId); // 在 Profile.vue 中已调用，这里可以省略，避免重复请求
                } else {
                    this.error = res.msg || '更新个人信息失败';
                    console.error('Store: Failed to update volunteer info:', res.msg);
                }
                return res; // 返回后端响应，供组件判断成功或失败
            } catch (err) {
                this.error = '更新个人信息时发生错误';
                console.error('Store: Error updating volunteer info:', err);
                throw err; // 抛出错误，供组件捕获
            } finally {
                this.isLoading = false;
            }
        },

        // --- 【重要新增】修改密码的 Action ---
        async changeVolunteerPassword({volunteerId, oldPassword, newPassword}) {
            this.isLoading = true;
            this.error = null;
            try {
                console.log(`Store: Changing password for volunteer ID: ${volunteerId}`);
                // 假设后端有一个专门的修改密码接口。你的后端 `VolunteerController.java`
                // 目前没有显式定义 `/volunteer/changePassword` 接口。
                // 如果没有，你可能需要添加这个接口或通过其他方式实现（例如，通过 `updateById` 更新密码字段，但需要验证旧密码）。
                // 这里我们假设你将添加一个 POST 接口来处理密码修改。
                const res = await request.post('/volunteer/changePassword', { // 假设的接口
                    volunteerId,
                    oldPassword,
                    newPassword
                });
                if (res.code === '200') {
                    console.log('Store: Password changed successfully.');
                } else {
                    this.error = res.msg || '修改密码失败';
                    console.error('Store: Failed to change password:', res.msg);
                }
                return res;
            } catch (err) {
                this.error = '修改密码时发生错误';
                console.error('Store: Error changing password:', err);
                throw err;
            } finally {
                this.isLoading = false;
            }
        },

        // 登出 action
        logout() {
            this.loggedInUser = null;
            // 清空详细信息
            Object.keys(this.detailedVolunteerInfo).forEach(key => {
                this.detailedVolunteerInfo[key] = typeof this.detailedVolunteerInfo[key] === 'number' ? 0 : (Array.isArray(this.detailedVolunteerInfo[key]) ? [] : '');
            });
            localStorage.removeItem(USER_SESSION_KEY);
            // 还可以清除其他相关的 localStorage 项
            // 例如：localStorage.removeItem('detailed-volunteer-info'); // 如果您之前有存这个
        },

        // 应用初始化时调用，尝试从 localStorage 加载用户并获取详细信息
        // 应用初始化时调用，尝试从 localStorage 加载用户并获取详细信息
        initializeStore() {
            const storedUser = localStorage.getItem(USER_SESSION_KEY);
            if (storedUser) {
                try { // 添加 try-catch 以处理 JSON.parse 可能的错误
                    this.loggedInUser = JSON.parse(storedUser);
                    if (this.loggedInUser && this.loggedInUser.username) { // 确保 loggedInUser 和 username 都存在
                        this.fetchDetailedVolunteerInfo(); // 获取详细信息
                        //this.fetchVolunteerStarLevel();    // **新增/确认调用：获取星级信息**
                    } else {
                        // 如果 localStorage 中的数据不包含 username，视为无效用户会话
                        this.logout(); // 清理无效状态
                    }
                } catch (e) {
                    console.error("Store 初始化时解析 localStorage 用户数据错误:", e);
                    this.logout(); // 解析失败则清空，避免应用出错
                }
            }
        }
    }
});