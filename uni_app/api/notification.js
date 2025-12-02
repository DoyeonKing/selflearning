/**
 * 通知相关API接口
 */
import { get, put, del } from '../utils/request.js'

/**
 * 获取用户通知列表
 * @param {Number} userId - 用户ID
 * @param {String} userType - 用户类型 ('patient', 'doctor', 'admin')
 */
export async function getUserNotifications(userId, userType = 'patient') {
	return get(`/api/notifications/user/${userId}?userType=${userType}`)
}

/**
 * 获取未读通知列表
 * @param {Number} userId - 用户ID
 * @param {String} userType - 用户类型 ('patient', 'doctor', 'admin')
 */
export async function getUnreadNotifications(userId, userType = 'patient') {
	return get(`/api/notifications/user/${userId}/unread?userType=${userType}`)
}

/**
 * 获取未读通知数量
 * @param {Number} userId - 用户ID
 * @param {String} userType - 用户类型 ('patient', 'doctor', 'admin')
 */
export async function getUnreadCount(userId, userType = 'patient') {
	try {
		const response = await get(`/api/notifications/user/${userId}/unread/count?userType=${userType}`)
		// 后端返回格式：{ userId, userType, unreadCount }
		return response.unreadCount || 0
	} catch (error) {
		console.error('获取未读通知数量失败:', error)
		return 0
	}
}

/**
 * 标记通知为已读
 * @param {Number} notificationId - 通知ID
 */
export async function markAsRead(notificationId) {
	return put(`/api/notifications/${notificationId}/read`)
}

/**
 * 标记所有通知为已读
 * @param {Number} userId - 用户ID
 * @param {String} userType - 用户类型 ('patient', 'doctor', 'admin')
 */
export async function markAllAsRead(userId, userType = 'patient') {
	return put(`/api/notifications/user/${userId}/read-all?userType=${userType}`)
}

/**
 * 删除通知
 * @param {Number} notificationId - 通知ID
 */
export async function deleteNotification(notificationId) {
	return del(`/api/notifications/${notificationId}`)
}

