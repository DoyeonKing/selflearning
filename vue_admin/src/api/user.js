import request from '@/utils/request';

/**
 * 创建新用户（患者/医生/管理员）
 * POST /api/users
 * @param {Object} userData - 用户创建请求数据
 * @returns {Promise}
 */
export function createUser(userData) {
    return request({
        url: '/api/users',
        method: 'post',
        data: userData
    });
}

/**
 * 批量导入用户
 * POST /api/users/import
 * @param {File} file - Excel文件
 * @returns {Promise}
 */
export function importUsers(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    return request({
        url: '/api/users/import',
        method: 'post',
        data: formData
        // 不要手动设置Content-Type，让浏览器自动设置（会包含boundary）
    });
}

/**
 * 搜索医生信息（分页）
 * GET /api/users/doctorInfo
 * @param {Object} query - 查询参数
 * @param {String} query.id - 工号（可选）
 * @param {String} query.name - 姓名（可选）
 * @param {Number} query.departmentId - 科室ID（可选）
 * @param {Number} query.page - 页码（默认1）
 * @param {Number} query.pageSize - 每页数量（默认10）
 * @returns {Promise}
 */
export function searchDoctors(query) {
    const params = {
        id: query.id,
        name: query.name,
        departmentId: query.departmentId,
        page: query.page || 1,
        pageSize: query.pageSize || 10
    };
    
    return request({
        url: '/api/users/doctorInfo',
        method: 'get',
        params: params
    });
}

/**
 * 搜索患者信息（分页，固定每页10条）
 * GET /api/users/patientInfo
 * @param {Object} query - 查询参数
 * @param {String} query.id - 学号（可选）
 * @param {String} query.name - 姓名（可选）
 * @param {Number} query.page - 页码（默认1）
 * @returns {Promise}
 */
export function searchPatients(query) {
    const params = {
        id: query.id,
        name: query.name,
        page: query.page || 1
    };
    
    return request({
        url: '/api/users/patientInfo',
        method: 'get',
        params: params
    });
}

/**
 * 更新用户信息
 * PUT /api/users/{id}
 * @param {Number} id - 用户ID
 * @param {Object} userData - 用户更新请求数据
 * @returns {Promise}
 */
export function updateUser(id, userData) {
    return request({
        url: `/api/users/${id}`,
        method: 'put',
        data: userData
    });
}

/**
 * 更新用户病历信息
 * PUT /api/users/{id}/medical-history
 * @param {Number} id - 患者ID
 * @param {Object} medicalData - 病历更新请求数据
 * @returns {Promise}
 */
export function updateMedicalHistory(id, medicalData) {
    return request({
        url: `/api/users/${id}/medical-history`,
        method: 'put',
        data: medicalData
    });
}

/**
 * 获取病历历史记录（分页）
 * GET /api/users/medical-history
 * @param {Number} page - 页码（默认1）
 * @param {Number} pageSize - 每页数量（默认10）
 * @returns {Promise}
 */
export function getMedicalHistories(page = 1, pageSize = 10) {
    const params = {
        page: page,
        pageSize: pageSize
    };
    
    return request({
        url: '/api/users/medical-history',
        method: 'get',
        params: params
    });
}

/**
 * 删除用户（软删除）
 * DELETE /api/users/{id}?role=DOCTOR
 */
export function deleteUser(id, role) {
    return request({
        url: `/api/users/${id}`,
        method: 'delete',
        params: { role }
    });
}

