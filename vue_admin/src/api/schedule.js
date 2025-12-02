import request from '@/utils/request'

/**
 * 创建排班
 * POST /api/schedules/create
 * @param {Object} scheduleData - 排班数据
 * @param {number} scheduleData.doctorId - 医生ID
 * @param {string} scheduleData.scheduleDate - 出诊日期 (YYYY-MM-DD)
 * @param {number} scheduleData.slotId - 时间段ID
 * @param {number} scheduleData.locationId - 就诊地点ID
 * @param {number} [scheduleData.totalSlots=10] - 总号源数
 * @param {number} [scheduleData.fee=5.00] - 挂号费
 * @param {string} [scheduleData.remarks] - 备注信息
 */
export function createSchedule(scheduleData) {
    return request({
        url: '/api/schedules/create',
        method: 'post',
        data: scheduleData
    });
}

/**
 * 获取排班列表
 * GET /api/schedules
 * @param {Object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @param {number} [params.page=0] - 页码
 * @param {number} [params.size=10] - 每页大小
 */
export function getSchedules(params = {}) {
    return request({
        url: '/api/schedules',
        method: 'get',
        params: params
    });
}

/**
 * 获取排班详情
 * GET /api/schedules/{id}
 * @param {number} scheduleId - 排班ID
 */
export function getScheduleById(scheduleId) {
    return request({
        url: `/api/schedules/${scheduleId}`,
        method: 'get'
    });
}

/**
 * 更新排班
 * PUT /api/schedules/{id}
 * @param {number} scheduleId - 排班ID
 * @param {Object} updateData - 更新数据
 * @param {number} [updateData.totalSlots] - 总号源数
 * @param {number} [updateData.fee] - 挂号费
 */
export function updateSchedule(scheduleId, updateData) {
    return request({
        url: `/api/schedules/${scheduleId}`,
        method: 'put',
        data: updateData
    });
}

/**
 * 批量更新排班
 * PUT /api/schedules/batch-update
 * @param {Object} batchData - 批量更新数据
 * @param {Array} batchData.updates - 更新项列表
 * @param {number} batchData.updates[].scheduleId - 排班ID
 * @param {number} [batchData.updates[].totalSlots] - 总号源数
 * @param {number} [batchData.updates[].fee] - 挂号费
 */
export function batchUpdateSchedules(batchData) {
    return request({
        url: '/api/schedules/batch-update',
        method: 'put',
        data: batchData
    });
}

/**
 * 删除排班
 * DELETE /api/schedules/{id}
 * @param {number} scheduleId - 排班ID
 */
export function deleteSchedule(scheduleId) {
    return request({
        url: `/api/schedules/${scheduleId}`,
        method: 'delete'
    });
}

/**
 * 查询所有排班记录（支持分页参数）
 * GET /api/schedules/search
 * @param {number} [page=0] - 页码
 * @param {number} [size=100] - 每页大小
 */
export function getAllSchedules(page = 0, size = 100) {
    return request({
        url: '/api/schedules/search',
        method: 'get',
        params: { page, size }
    });
}

/**
 * 根据参数删除排班
 * DELETE /api/schedules/delete
 * @param {Object} deleteData - 删除参数
 * @param {number} deleteData.doctorId - 医生ID
 * @param {number} deleteData.slotId - 时间段ID
 * @param {number} deleteData.locationId - 地点ID
 * @param {string} deleteData.scheduleDate - 排班日期 (YYYY-MM-DD)
 */
export function deleteScheduleByParams(deleteData) {
    return request({
        url: '/api/schedules/delete',
        method: 'delete',
        data: deleteData
    });
}

/**
 * 自动生成排班
 * POST /api/schedules/auto-generate
 * @param {Object} data - 自动排班参数
 * @param {number} data.departmentId - 科室ID
 * @param {string} data.startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} data.endDate - 结束日期 (YYYY-MM-DD)
 * @param {boolean} [data.overwriteExisting=false] - 是否覆盖已有排班
 * @param {boolean} [data.previewOnly=false] - 是否仅预览
 * @param {Object} [data.rules] - 排班规则配置
 */
export function autoGenerateSchedule(data) {
    return request({
        url: '/api/schedules/auto-generate',
        method: 'post',
        data
    });
}

/**
 * 预览自动排班（不保存）
 * POST /api/schedules/auto-generate
 * @param {Object} data - 自动排班参数
 */
export function previewAutoSchedule(data) {
    return request({
        url: '/api/schedules/auto-generate',
        method: 'post',
        data: {
            ...data,
            previewOnly: true
        }
    });
}

/**
 * 获取默认排班规则
 * GET /api/schedules/auto-generate/rules
 */
export function getDefaultScheduleRules() {
    return request({
        url: '/api/schedules/auto-generate/rules',
        method: 'get'
    });
}