import request from '@/utils/request';

/**
 * 医生登录接口
 * @param {Object} data - 包含 identifier 和 password
 */
export function doctorLogin(data) {
    // 注意：在 DoctorLogin.vue 中，我们*绕过*了这个API调用
    // 但保留这个文件是为了 store 能正常导入
    return request({
        url: '/api/doctor/login', // 假设的后端登录接口
        method: 'post',
        data: data
    });
}