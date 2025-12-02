import { defineStore } from 'pinia';
import request from '@/utils/request.js';
// 【新增】导入 auth.js (即使是空的，也需要导入)
import { doctorLogin } from '@/api/auth';

// Key for localStorage
const DOCTOR_SESSION_KEY = 'xm-pro-doctor'; // 基础会话信息的key
const DETAILED_INFO_KEY = 'xm-pro-doctor-detail'; // 详细信息的key

export const useDoctorStore = defineStore('doctor', {
    state: () => ({
        // 1. 基础信息 (Token, ID)
        // 从 localStorage 读取，防止刷新丢失
        loggedInDoctorBasicInfo: JSON.parse(localStorage.getItem(DOCTOR_SESSION_KEY)) || null,

        // 2. 详细信息 (姓名、科室、头像、擅长、简介等)
        // 从 localStorage 读取，防止刷新丢失。如果没读到，给定默认空对象结构
        detailedDoctorInfo: JSON.parse(localStorage.getItem(DETAILED_INFO_KEY)) || {
            doctorId: '',
            name: '',
            department: '',
            position: '',
            phone: '',
            username: '',
            specialty: '',
            bio: '',
            photoUrl: '',
        },
        isLoading: false,
        error: null,
    }),

    getters: {
        // isAuthenticated: 检查 token 是否存在
        isAuthenticated: (state) => !!state.loggedInDoctorBasicInfo?.token,

        // 显示名称: 优先显示真实姓名，没有则显示工号
        displayName: (state) => state.detailedDoctorInfo.name || state.loggedInDoctorBasicInfo?.name || state.loggedInDoctorBasicInfo?.identifier || '医生',

        // 获取医生ID
        currentDoctorId: (state) => state.detailedDoctorInfo.doctorId || state.loggedInDoctorBasicInfo?.identifier || '1',

        // 获取科室信息
        currentDepartment: (state) => state.detailedDoctorInfo.department,

        // 获取职位信息
        currentPosition: (state) => state.detailedDoctorInfo.position,
    },

    actions: {
        // 登录成功后调用的 action
        loginSuccess(apiResponseData, basicLoginInfoFromLogin) {
            // 从 API 响应中提取详细信息
            const info = apiResponseData.doctorInfo || {};

            // 1. 更新 State 中的详细信息
            this.detailedDoctorInfo = {
                doctorId: String(info.doctorId || ''),
                name: info.name || '',
                department: info.department || '',
                position: info.position || '',
                phone: info.phone || '',
                username: info.username || basicLoginInfoFromLogin.identifier || '',
                specialty: info.specialty || '',
                bio: info.bio || '',
                photoUrl: info.photoUrl || '',
            };

            // 2. 更新 State 中的基础信息
            const basicInfo = {
                identifier: basicLoginInfoFromLogin.identifier,
                token: basicLoginInfoFromLogin.token || '',
                loginTime: new Date().toISOString(),
                doctorId: String(info.doctorId || ''),
                name: info.name || ''
            };
            this.loggedInDoctorBasicInfo = basicInfo;

            // 3. 持久化存储到 localStorage (存两份，确保刷新后数据还在)
            localStorage.setItem(DOCTOR_SESSION_KEY, JSON.stringify(basicInfo));
            localStorage.setItem(DETAILED_INFO_KEY, JSON.stringify(this.detailedDoctorInfo));

            console.log('DoctorStore: 登录状态已更新并缓存');
            this.error = null;
        },

        // 登出 action
        logout() {
            // 清除 localStorage
            localStorage.removeItem(DOCTOR_SESSION_KEY);
            localStorage.removeItem(DETAILED_INFO_KEY);

            // 重置 State
            this.loggedInDoctorBasicInfo = null;
            this.detailedDoctorInfo = {
                doctorId: '',
                name: '',
                department: '',
                position: '',
                phone: '',
                username: '',
                specialty: '',
                bio: '',
                photoUrl: '',
            };
            this.error = null;
        },

        // 获取详细医生信息（根据工号获取）
        async fetchDetailedDoctorInfo() {
            if (!this.isAuthenticated) {
                this.error = '未登录';
                return;
            }

            this.isLoading = true;
            this.error = null;

            try {
                // 获取工号
                const identifier = this.loggedInDoctorBasicInfo?.identifier || this.detailedDoctorInfo?.username;
                if (!identifier) {
                    console.warn('无法获取医生工号，跳过获取详细信息');
                    this.isLoading = false;
                    return;
                }

                // 直接使用 doctorId 或 identifier 获取信息（避免 /api/doctors/info 路由冲突）
                let response = null;
                const doctorId = this.detailedDoctorInfo?.doctorId;
                
                if (doctorId) {
                    // 优先使用 doctorId 获取完整信息（包含 specialty 和 bio）
                    console.log('使用 /api/doctors/{doctorId} 获取完整信息');
                    try {
                        response = await request({
                            url: `/api/doctors/${doctorId}`,
                            method: 'GET',
                            headers: {
                                'Authorization': `Bearer ${this.loggedInDoctorBasicInfo.token}`
                            }
                        });
                    } catch (idError) {
                        // 如果 doctorId 接口失败，使用 identifier 接口（但只返回部分信息）
                        console.log('doctorId 接口失败，使用 /api/doctors/identifier/{identifier}');
                        response = await request({
                            url: `/api/doctors/identifier/${identifier}`,
                            method: 'GET',
                            headers: {
                                'Authorization': `Bearer ${this.loggedInDoctorBasicInfo.token}`
                            }
                        });
                    }
                } else {
                    // 没有 doctorId，使用 identifier 接口
                    console.log('使用 /api/doctors/identifier/{identifier}');
                    response = await request({
                        url: `/api/doctors/identifier/${identifier}`,
                        method: 'GET',
                        headers: {
                            'Authorization': `Bearer ${this.loggedInDoctorBasicInfo.token}`
                        }
                    });
                }

                // 处理不同的响应格式
                let doctorData = null;
                if (response.code === '200' || response.code === 200) {
                    doctorData = response.data || {};
                } else if (response.fullName || response.doctorId || response.identifier) {
                    // 直接返回 DoctorResponse 对象或 Map
                    doctorData = response;
                }

                if (doctorData) {
                    console.log('获取到的医生数据:', doctorData);
                    // 更新 State (合并新数据)
                    this.detailedDoctorInfo = {
                        ...this.detailedDoctorInfo,
                        // 映射后端字段 (fullName -> name, phoneNumber -> phone 等)
                        name: doctorData.fullName !== undefined ? doctorData.fullName : this.detailedDoctorInfo.name,
                        department: doctorData.department?.name || doctorData.departmentName || this.detailedDoctorInfo.department,
                        position: doctorData.title !== undefined ? doctorData.title : this.detailedDoctorInfo.position,
                        phone: doctorData.phoneNumber !== undefined ? doctorData.phoneNumber : this.detailedDoctorInfo.phone,
                        specialty: doctorData.specialty !== undefined ? (doctorData.specialty || '') : this.detailedDoctorInfo.specialty,
                        bio: doctorData.bio !== undefined ? (doctorData.bio || '') : this.detailedDoctorInfo.bio,
                        photoUrl: doctorData.photoUrl !== undefined ? doctorData.photoUrl : this.detailedDoctorInfo.photoUrl,
                        username: doctorData.identifier || doctorData.username || this.detailedDoctorInfo.username || identifier
                    };
                    // 更新缓存
                    localStorage.setItem(DETAILED_INFO_KEY, JSON.stringify(this.detailedDoctorInfo));
                    console.log('医生信息已更新:', this.detailedDoctorInfo);
                } else {
                    console.warn('无法解析医生数据:', response);
                }
            } catch (error) {
                console.error('获取医生详细信息失败:', error);
                this.error = error.message || '获取医生信息失败';
            } finally {
                this.isLoading = false;
            }
        },

        // 更新医生信息（调用 /api/doctors/info）
        // updateData 格式: { phoneNumber, specialty, bio, avatarFile(File对象) }
        async updateDoctorInfo(updateData) {
            if (!this.isAuthenticated) {
                this.error = '未登录';
                return false;
            }

            this.isLoading = true;
            this.error = null;

            try {
                // 获取工号（identifier）
                const identifier = this.loggedInDoctorBasicInfo?.identifier || this.detailedDoctorInfo?.username;
                if (!identifier) {
                    this.error = '无法获取医生工号';
                    return false;
                }

                // 构建 FormData
                const formData = new FormData();
                
                // 直接添加参数作为 FormData 字段（后端使用 @RequestParam 接收）
                formData.append('identifier', identifier);
                
                if (updateData.phoneNumber) {
                    formData.append('phoneNumber', updateData.phoneNumber);
                }
                
                if (updateData.specialty) {
                    formData.append('specialty', updateData.specialty);
                }
                
                if (updateData.bio) {
                    formData.append('bio', updateData.bio);
                }
                
                // 如果有头像文件，添加到 FormData
                if (updateData.avatarFile instanceof File) {
                    formData.append('avatarFile', updateData.avatarFile, updateData.avatarFile.name);
                    console.log('头像文件已添加到 FormData:', {
                        name: updateData.avatarFile.name,
                        size: updateData.avatarFile.size,
                        type: updateData.avatarFile.type
                    });
                } else {
                    console.log('没有头像文件需要上传');
                }
                
                console.log('准备上传的数据:', {
                    identifier: identifier,
                    phoneNumber: updateData.phoneNumber || null,
                    specialty: updateData.specialty || null,
                    bio: updateData.bio || null,
                    hasAvatarFile: updateData.avatarFile instanceof File,
                    avatarFileName: updateData.avatarFile instanceof File ? updateData.avatarFile.name : null,
                    avatarFileSize: updateData.avatarFile instanceof File ? updateData.avatarFile.size : null
                });

                // 打印 FormData 内容（用于调试）
                console.log('FormData entries:');
                for (let pair of formData.entries()) {
                    console.log(pair[0], pair[1]);
                }

                const response = await request({
                    url: '/api/doctors/info',
                    method: 'PUT',
                    data: formData,
                    headers: {
                        'Authorization': `Bearer ${this.loggedInDoctorBasicInfo.token}`
                        // 注意：不设置 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
                    }
                });

                // 处理不同的响应格式
                let doctorData = null;
                if (response.code === '200' || response.code === 200) {
                    doctorData = response.data || {};
                } else if (response.fullName || response.doctorId || response.identifier) {
                    // 直接返回 DoctorResponse 对象（后端使用 ResponseEntity.ok(response)）
                    doctorData = response;
                }

                if (doctorData) {
                    console.log('更新医生信息返回的数据:', doctorData);
                    console.log('photoUrl 字段:', doctorData.photoUrl);
                    // 更新本地状态（使用后端返回的最新数据）
                    this.detailedDoctorInfo = {
                        ...this.detailedDoctorInfo,
                        phone: doctorData.phoneNumber !== undefined ? doctorData.phoneNumber : this.detailedDoctorInfo.phone,
                        specialty: doctorData.specialty !== undefined ? doctorData.specialty : this.detailedDoctorInfo.specialty,
                        bio: doctorData.bio !== undefined ? doctorData.bio : this.detailedDoctorInfo.bio,
                        photoUrl: doctorData.photoUrl !== undefined && doctorData.photoUrl !== null ? doctorData.photoUrl : this.detailedDoctorInfo.photoUrl,
                        name: doctorData.fullName !== undefined ? doctorData.fullName : this.detailedDoctorInfo.name,
                        department: doctorData.department?.name || doctorData.departmentName || this.detailedDoctorInfo.department,
                        position: doctorData.title !== undefined ? doctorData.title : this.detailedDoctorInfo.position
                    };
                    console.log('更新后的 photoUrl:', this.detailedDoctorInfo.photoUrl);
                    // 更新缓存
                    localStorage.setItem(DETAILED_INFO_KEY, JSON.stringify(this.detailedDoctorInfo));
                    return true;
                } else {
                    this.error = response.msg || response.message || '更新医生信息失败';
                    return false;
                }
            } catch (error) {
                console.error('更新医生信息失败:', error);
                console.error('错误详情:', {
                    message: error.message,
                    response: error.response?.data,
                    status: error.response?.status,
                    statusText: error.response?.statusText,
                    headers: error.response?.headers,
                    config: {
                        url: error.config?.url,
                        method: error.config?.method,
                        headers: error.config?.headers
                    }
                });
                
                // 提取错误信息
                let errorMsg = '网络错误，请稍后重试';
                if (error.response?.data) {
                    const errorData = error.response.data;
                    if (typeof errorData === 'string') {
                        try {
                            const parsed = JSON.parse(errorData);
                            errorMsg = parsed.error || parsed.message || errorData;
                        } catch {
                            errorMsg = errorData;
                        }
                    } else if (errorData.error) {
                        errorMsg = errorData.error;
                    } else if (errorData.message) {
                        errorMsg = errorData.message;
                    } else if (errorData.msg) {
                        errorMsg = errorData.msg;
                    }
                } else if (error.message) {
                    errorMsg = error.message;
                }
                
                // 如果是 400 错误，可能是文件格式或大小问题
                if (error.response?.status === 400) {
                    errorMsg = '请求参数错误: ' + errorMsg;
                } else if (error.response?.status === 413) {
                    errorMsg = '文件太大，请选择小于 2MB 的图片';
                } else if (error.response?.status === 415) {
                    errorMsg = '不支持的文件格式，请选择 JPG 或 PNG 格式';
                }
                
                this.error = errorMsg;
                return false;
            } finally {
                this.isLoading = false;
            }
        },

        // 修改密码
        async changePassword(oldPassword, newPassword, confirmPassword) {
            if (!this.isAuthenticated) {
                this.error = '未登录';
                return false;
            }

            this.isLoading = true;
            this.error = null;

            try {
                // 获取医生工号
                const identifier = this.loggedInDoctorBasicInfo?.identifier || this.detailedDoctorInfo?.username;
                if (!identifier) {
                    this.error = '无法获取医生工号';
                    return false;
                }

                const response = await request({
                    url: '/api/doctors/change-password',
                    method: 'POST',
                    data: {
                        identifier: identifier,
                        oldPassword: oldPassword,
                        newPassword: newPassword,
                        confirmPassword: confirmPassword
                    },
                    headers: {
                        'Authorization': `Bearer ${this.loggedInDoctorBasicInfo.token}`,
                        'Content-Type': 'application/json'
                    }
                });

                if (response.code === '200' || response.code === 200 || response.message) {
                    return true;
                } else {
                    this.error = response.msg || response.error || '修改密码失败';
                    return false;
                }
            } catch (error) {
                console.error('修改密码失败:', error);
                // 提取错误信息
                if (error.response?.data) {
                    const errorData = error.response.data;
                    if (typeof errorData === 'string') {
                        try {
                            const parsed = JSON.parse(errorData);
                            this.error = parsed.error || parsed.message || errorData;
                        } catch {
                            this.error = errorData;
                        }
                    } else if (errorData.error) {
                        this.error = errorData.error;
                    } else if (errorData.message) {
                        this.error = errorData.message;
                    } else if (errorData.msg) {
                        this.error = errorData.msg;
                    } else {
                        this.error = '修改密码失败';
                    }
                } else {
                    this.error = error.message || '网络错误，请稍后重试';
                }
                return false;
            } finally {
                this.isLoading = false;
            }
        }
    }
});