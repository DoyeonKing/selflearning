import request from '@/utils/request';

/**
 * 获取所有症状映射
 * GET /api/symptom-mappings
 */
export function getAllSymptomMappings() {
    return request({
        url: '/api/symptom-mappings',
        method: 'get',
    });
}

/**
 * 根据科室ID获取症状映射
 * GET /api/symptom-mappings/department/{departmentId}
 */
export function getSymptomMappingsByDepartment(departmentId) {
    return request({
        url: `/api/symptom-mappings/department/${departmentId}`,
        method: 'get',
    });
}

/**
 * 创建症状映射
 * POST /api/symptom-mappings
 */
export function createSymptomMapping(data) {
    return request({
        url: '/api/symptom-mappings',
        method: 'post',
        data: data,
    });
}

/**
 * 更新症状映射
 * PUT /api/symptom-mappings/{mappingId}
 */
export function updateSymptomMapping(mappingId, data) {
    return request({
        url: `/api/symptom-mappings/${mappingId}`,
        method: 'put',
        data: data,
    });
}

/**
 * 删除症状映射
 * DELETE /api/symptom-mappings/{mappingId}
 */
export function deleteSymptomMapping(mappingId) {
    return request({
        url: `/api/symptom-mappings/${mappingId}`,
        method: 'delete',
    });
}

