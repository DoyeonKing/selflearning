import request from '@/utils/request'

/**
 * 获取所有时间段列表
 * GET /api/timeslots
 * @returns {Promise} 返回时间段列表
 */
export function getTimeSlots() {
  return request({
    url: '/api/timeslots',
    method: 'get'
  })
}

/**
 * 根据ID获取时间段详情
 * GET /api/timeslots/{id}
 * @param {Number} id - 时间段ID
 * @returns {Promise} 返回时间段详情
 */
export function getTimeSlotById(id) {
  return request({
    url: `/api/timeslots/${id}`,
    method: 'get'
  })
}

/**
 * 创建新时间段
 * POST /api/timeslots
 * @param {Object} timeSlotData - 时间段数据
 * @param {String} timeSlotData.slotName - 时间段名称
 * @param {String} timeSlotData.startTime - 开始时间
 * @param {String} timeSlotData.endTime - 结束时间
 * @param {String} timeSlotData.period - 时段（上午/下午）
 * @returns {Promise} 返回创建的时间段信息
 */
export function createTimeSlot(timeSlotData) {
  return request({
    url: '/api/timeslots',
    method: 'post',
    data: timeSlotData
  })
}

/**
 * 更新时间段
 * PUT /api/timeslots/{id}
 * @param {Number} id - 时间段ID
 * @param {Object} timeSlotData - 时间段数据
 * @returns {Promise} 返回更新后的时间段信息
 */
export function updateTimeSlot(id, timeSlotData) {
  return request({
    url: `/api/timeslots/${id}`,
    method: 'put',
    data: timeSlotData
  })
}

/**
 * 删除时间段
 * DELETE /api/timeslots/{id}
 * @param {Number} id - 时间段ID
 * @returns {Promise} 返回删除结果
 */
export function deleteTimeSlot(id) {
  return request({
    url: `/api/timeslots/${id}`,
    method: 'delete'
  })
}
