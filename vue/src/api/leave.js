import request from '@/utils/request';

/**
 * 获取指定医生的所有休假申请记录
 * @param {number} doctorId - 医生ID
 */
export function getMyLeaveRequests(doctorId) {
    return request({
        url: `/api/leave-requests/doctor/${doctorId}`,
        method: 'get'
    });
}

/**
 * 创建新的休假申请
 * @param {Object} data - 请假申请数据
 * @param {number} data.doctorId - 医生ID
 * @param {string} data.requestType - 申请类型 (leave/schedule_change)
 * @param {string} data.startTime - 开始时间 (ISO格式)
 * @param {string} data.endTime - 结束时间 (ISO格式)
 * @param {string} data.reason - 请假事由
 * @param {string} data.proofDocumentUrl - 请假证明文件URL (可选)
 */
export function createLeaveRequest(data) {
    return request({
        url: '/api/leave-requests',
        method: 'post',
        data: data
    });
}

/**
 * 删除休假申请
 * @param {number} requestId - 申请ID
 */
export function cancelLeaveRequest(requestId) {
    return request({
        url: `/api/leave-requests/${requestId}`,
        method: 'delete'
    });
}

/**
 * 获取所有休假申请 (管理员用)
 */
export function getAllLeaveRequests() {
    return request({
        url: '/api/leave-requests',
        method: 'get'
    });
}

/**
 * 根据状态获取休假申请
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