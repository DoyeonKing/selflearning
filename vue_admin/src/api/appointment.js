import request from '@/utils/request';

/**
 * 患者扫码签到
 * @param {Object} data - { qrToken }
 */
export async function checkInAppointment(data) {
    return await request({
        url: '/api/appointments/check-in',
        method: 'POST',
        data
    });
}

/**
 * 获取预约二维码Token（用于测试）
 * @param {Number} appointmentId - 预约ID
 */
export async function getAppointmentQrCode(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/qr-code`,
        method: 'GET'
    });
}

/**
 * 清除预约签到时间（管理员功能）
 * @param {Number} appointmentId - 预约ID
 */
export async function clearCheckIn(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/check-in`,
        method: 'DELETE'
    });
}

/**
 * 获取叫号队列（已签到但未就诊的预约列表）
 * @param {Number} scheduleId - 排班ID
 */
export async function getCallQueue(scheduleId) {
    return await request({
        url: `/api/appointments/schedule/${scheduleId}/call-queue`,
        method: 'GET'
    });
}

/**
 * 获取下一个应该叫号的预约
 * @param {Number} scheduleId - 排班ID
 */
export async function getNextAppointmentToCall(scheduleId) {
    return await request({
        url: `/api/appointments/schedule/${scheduleId}/next-to-call`,
        method: 'GET'
    });
}

/**
 * 执行叫号
 * @param {Number} appointmentId - 预约ID
 */
export async function callAppointment(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/call`,
        method: 'POST'
    });
}

/**
 * 标记过号（仅标记，不重新排队）
 * @param {Number} appointmentId - 预约ID
 */
export async function markMissedCall(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/mark-missed`,
        method: 'POST'
    });
}

/**
 * 过号后重新签到（患者重新扫码后调用）
 * @param {Number} appointmentId - 预约ID
 */
export async function recheckInAfterMissedCall(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/recheck-in`,
        method: 'POST'
    });
}

/**
 * 标记就诊完成（会自动叫号下一位）
 * @param {Number} appointmentId - 预约ID
 */
export async function completeAppointment(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}/complete`,
        method: 'POST'
    });
}

/**
 * 现场挂号（分诊台辅助患者挂号）
 * @param {Object} data - { patientId, scheduleId }
 */
export async function createWalkInAppointment(data) {
    return await request({
        url: '/api/appointments/walk-in',
        method: 'POST',
        data
    });
}

/**
 * 退款接口（管理员功能）
 * @param {Number} appointmentId - 预约ID
 */
export async function refundAppointment(appointmentId) {
    return await request({
        url: `/api/admin/appointments/${appointmentId}/refund`,
        method: 'POST'
    });
}

/**
 * 获取患者的所有预约
 * @param {Number} patientId - 患者ID
 */
export async function getPatientAppointments(patientId) {
    return await request({
        url: `/api/appointments/patient/${patientId}`,
        method: 'GET'
    });
}

/**
 * 获取预约列表（管理员功能，用于搜索）
 * @param {Object} params - 查询参数
 * @param {String} params.patientName - 患者姓名（可选）
 * @param {Number} params.appointmentId - 预约ID（可选）
 * @param {String} params.paymentStatus - 支付状态（可选）
 * @param {Number} params.page - 页码（可选）
 * @param {Number} params.size - 每页数量（可选）
 */
export async function searchAppointments(params = {}) {
    return await request({
        url: '/api/appointments',
        method: 'GET',
        params
    });
}

/**
 * 获取预约详情
 * @param {Number} appointmentId - 预约ID
 */
export async function getAppointmentDetail(appointmentId) {
    return await request({
        url: `/api/appointments/${appointmentId}`,
        method: 'GET'
    });
}

/**
 * 支付预约费用（管理员功能）
 * @param {Number} appointmentId - 预约ID
 * @param {Object} paymentData - 支付数据
 * @param {String} paymentData.paymentMethod - 支付方式：cash/wechat/alipay/card
 * @param {String} paymentData.transactionId - 交易流水号（可选）
 * @param {Number} paymentData.amount - 实收金额（可选）
 * @param {String} paymentData.remark - 备注（可选）
 */
export async function payForAppointment(appointmentId, paymentData) {
    return await request({
        url: `/api/appointments/${appointmentId}/pay`,
        method: 'POST',
        data: {
            paymentStatus: 'paid',
            paymentMethod: paymentData.paymentMethod,
            transactionId: paymentData.transactionId,
            amount: paymentData.amount,
            remark: paymentData.remark
        }
    });
}

