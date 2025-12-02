import request from '@/utils/request';

/**
 * 根据科室ID获取地点列表
 * GET /api/locations/department/{departmentId}
 * @param {number} departmentId - 科室ID
 */
export function getLocationsByDepartmentId(departmentId) {
    return request({
        url: `/api/locations/department/${departmentId}`,
        method: 'get',
    });
}

/**
 * 根据科室ID获取地点名称列表
 * GET /api/locations/department/{departmentId}/names
 * @param {number} departmentId - 科室ID
 */
export function getLocationNamesByDepartmentId(departmentId) {
    return request({
        url: `/api/locations/department/${departmentId}/names`,
        method: 'get',
    });
}

/**
 * 为指定科室添加新地点
 * POST /api/locations/department/{departmentId}
 * @param {number} departmentId - 科室ID
 * @param {object} locationData - 地点信息
 */
export function addLocationToDepartment(departmentId, locationData) {
    return request({
        url: `/api/locations/department/${departmentId}`,
        method: 'post',
        data: locationData,
    });
}

/**
 * 删除指定地点
 * DELETE /api/locations/{locationId}
 * @param {number} locationId - 地点ID
 */
export function deleteLocation(locationId) {
    return request({
        url: `/api/locations/${locationId}`,
        method: 'delete',
    });
}

/**
 * 获取所有未分配科室的地点
 * GET /api/locations/unassigned
 */
export function getUnassignedLocations() {
    return request({
        url: '/api/locations/unassigned',
        method: 'get',
    });
}

/**
 * 批量将地点分配到指定科室
 * POST /api/locations/department/{departmentId}/batch-assign
 * @param {number} departmentId - 科室ID
 * @param {Array<number>} locationIds - 地点ID列表
 */
export function batchAssignLocationsToDepartment(departmentId, locationIds) {
    return request({
        url: `/api/locations/department/${departmentId}/batch-assign`,
        method: 'post',
        data: locationIds,
    });
}

/**
 * 将地点从科室中移除
 * DELETE /api/locations/department/{departmentId}/location/{locationId}
 * @param {number} departmentId - 科室ID
 * @param {number} locationId - 地点ID
 */
export function removeLocationFromDepartment(departmentId, locationId) {
    return request({
        url: `/api/locations/department/${departmentId}/location/${locationId}`,
        method: 'delete',
    });
}
