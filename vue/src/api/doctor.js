import request from '@/utils/request';

/**
 * 获取当前登录医生的详细个人资料
 * (后端根据Token确定是哪个医生)
 */
export function getDoctorInfo() {
    return request({
        url: '/api/doctors/info', // 【已修改】使用您指定的接口
        method: 'get'
    });
}

/**
 * 更新当前登录医生的个人资料
 * @param {Object} data - 包含要更新字段的对象
 * (e.g., { phoneNumber, specialty, bio, photoUrl })
 */
export function updateDoctorInfo(data) {
    return request({
        url: '/api/doctors/info', // 【已修改】使用您指定的接口
        method: 'put',
        data: data
    });
}