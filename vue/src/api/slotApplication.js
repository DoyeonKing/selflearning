import request from '@/utils/request'

/**
 * 创建加号申请
 * @param {Object} data - 申请数据
 */
export function createSlotApplication(data) {
    return request({
        url: '/api/slot-applications',
        method: 'post',
        data
    })
}

/**
 * 获取医生的所有加号申请
 * @param {number} doctorId - 医生ID
 */
export function getSlotApplicationsByDoctor(doctorId) {
    return request({
        url: `/api/slot-applications/doctor/${doctorId}`,
        method: 'get'
    })
}

/**
 * 获取医生的指定状态的加号申请
 * @param {number} doctorId - 医生ID
 * @param {string} status - 状态
 */
export function getSlotApplicationsByDoctorAndStatus(doctorId, status) {
    return request({
        url: `/api/slot-applications/doctor/${doctorId}/status/${status}`,
        method: 'get'
    })
}

/**
 * 根据ID获取加号申请
 * @param {number} id - 申请ID
 */
export function getSlotApplicationById(id) {
    return request({
        url: `/api/slot-applications/${id}`,
        method: 'get'
    })
}

/**
 * 更新加号申请（审批）
 * @param {number} id - 申请ID
 * @param {Object} data - 更新数据
 */
export function updateSlotApplication(id, data) {
    return request({
        url: `/api/slot-applications/${id}`,
        method: 'put',
        data
    })
}

/**
 * 取消加号申请
 * @param {number} id - 申请ID
 * @param {number} doctorId - 医生ID
 */
export function cancelSlotApplication(id, doctorId) {
    return request({
        url: `/api/slot-applications/${id}/cancel`,
        method: 'put',
        params: { doctorId }
    })
}

/**
 * 获取所有加号申请（管理员用）
 */
export function getAllSlotApplications() {
    return request({
        url: '/api/slot-applications',
        method: 'get'
    })
}

/**
 * 根据状态获取加号申请（管理员用）
 * @param {string} status - 状态
 */
export function getSlotApplicationsByStatus(status) {
    return request({
        url: `/api/slot-applications/status/${status}`,
        method: 'get'
    })
}
