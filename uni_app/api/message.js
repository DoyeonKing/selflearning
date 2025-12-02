/**
 * 消息相关API接口
 */
import { get, post, put } from '../utils/request.js'

/**
 * 获取患者消息列表
 * @param {Number} patientId - 患者ID
 */
export function getMessages(patientId) {
	return get(`/api/messages/patient/${patientId}`)
}

/**
 * 获取未读消息数量
 * @param {Number} patientId - 患者ID
 */
export function getUnreadCount(patientId) {
	return get(`/api/messages/patient/${patientId}/unread-count`)
}

/**
 * 标记消息为已读
 * @param {Number} messageId - 消息ID
 */
export function markMessageAsRead(messageId) {
	return put(`/api/messages/${messageId}/read`)
}

/**
 * 标记所有消息为已读
 * @param {Number} patientId - 患者ID
 */
export function markAllAsRead(patientId) {
	return put(`/api/messages/patient/${patientId}/read-all`)
}

/**
 * 删除消息
 * @param {Number} messageId - 消息ID
 */
export function deleteMessage(messageId) {
	return post(`/api/messages/${messageId}/delete`)
}
