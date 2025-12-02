import request from '@/utils/request';

/**
 * 获取所有请假申请 (管理员用)
 */
export function getAllLeaveRequests() {
    return request({
        url: '/api/leave-requests',
        method: 'get'
    });
}

/**
 * 根据状态获取请假申请
 * @param {string} status - 状态 (PENDING/APPROVED/REJECTED)
 */
export function getLeaveRequestsByStatus(status) {
    return request({
        url: `/api/leave-requests/status/${status}`,
        method: 'get'
    });
}

/**
 * 审批休假申请
 * @param {number} requestId - 申请ID
 * @param {number} approverId - 审批人ID
 * @param {string} comments - 审批意见 (可选)
 */
export function approveLeaveRequest(requestId, approverId, comments = '') {
    return request({
        url: `/api/leave-requests/${requestId}/approve`,
        method: 'put',
        params: {
            approverId,
            comments
        }
    });
}

/**
 * 拒绝休假申请
 * @param {number} requestId - 申请ID
 * @param {number} approverId - 审批人ID
 * @param {string} comments - 拒绝理由 (可选)
 */
export function rejectLeaveRequest(requestId, approverId, comments = '') {
    return request({
        url: `/api/leave-requests/${requestId}/reject`,
        method: 'put',
        params: {
            approverId,
            comments
        }
    });
}

/**
 * 获取请假批准详情（包含受影响的排班和可用替班医生）
 * @param {number} requestId - 申请ID
 */
export function getLeaveApprovalDetail(requestId) {
    return request({
        url: `/api/leave-requests/${requestId}/approval-detail`,
        method: 'get'
    });
}

/**
 * 确认替班安排
 * @param {Object} data - 替班确认数据
 * @param {number} data.leaveRequestId - 请假申请ID
 * @param {Object} data.substitutions - 替班安排 {scheduleId: doctorId}
 */
export function confirmSubstitution(data) {
    return request({
        url: '/api/leave-requests/confirm-substitution',
        method: 'post',
        data
    });
}

/**
 * 获取医生在指定时间段的排班和请假信息
 * @param {number} doctorId - 医生ID
 * @param {string} startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} endDate - 结束日期 (YYYY-MM-DD)
 */
export function getDoctorScheduleAndLeave(doctorId, startDate, endDate) {
    return request({
        url: `/api/doctors/${doctorId}/schedule-and-leave`,
        method: 'get',
        params: {
            startDate,
            endDate
        }
    });
}
