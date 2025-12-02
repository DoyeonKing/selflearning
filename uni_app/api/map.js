/**
 * 地图相关API
 */
import { get } from '../utils/request.js'

/**
 * 获取地图配置（支持多楼层）
 * @param {Number} floorLevel 楼层号（默认1楼）
 * @returns {Promise} 地图配置数据（包含网格和所有节点）
 */
export async function getMapConfig(floorLevel = 1) {
	return await get(`/api/map/config?floorLevel=${floorLevel}`)
}

/**
 * 根据诊室ID获取目标节点坐标
 * @param {Number} locationId 诊室ID
 * @returns {Promise} 目标节点信息
 */
export async function getTargetNode(locationId) {
	return await get(`/api/map/target/${locationId}`)
}

/**
 * 根据二维码内容扫码定位
 * @param {String} qrcodeContent 二维码内容（如：HOSPITAL_NODE_1）
 * @returns {Promise} 节点信息（包含楼层、坐标等）
 */
export async function scanQRCode(qrcodeContent) {
	return await get(`/api/qrcode/scan/${encodeURIComponent(qrcodeContent)}`)
}

/**
 * 计算导航路径（基于真实路径规划）
 * @param {Number} startNodeId 起点节点ID
 * @param {Number} endNodeId 终点节点ID
 * @returns {Promise} 导航路径（包含步骤、距离、时间）
 */
export async function calculateNavigationPath(startNodeId, endNodeId) {
	return await get(`/api/navigation/path?startNodeId=${startNodeId}&endNodeId=${endNodeId}`)
}

/**
 * 获取下一步指引
 * @param {Number} currentNodeId 当前节点ID
 * @param {Number} targetNodeId 目标节点ID
 * @returns {Promise} 下一步指引
 */
export async function getNextStep(currentNodeId, targetNodeId) {
	return await get(`/api/navigation/next?currentNodeId=${currentNodeId}&targetNodeId=${targetNodeId}`)
}
