import request from '@/utils/request';

/**
 * 获取子科室分页列表
 * GET /api/departments
 * 接收查询参数 queryDTO，返回 PageDepartmentResponseDTO
 */
export function getDepartmentPage(query) {
    const params = {
        name: query.name,
        description: query.description,
        parentDepartmentId: query.parentDepartmentId,
        page: query.page !== undefined ? query.page : 0, // 直接使用传入的页码，不进行减1操作
        size: query.size,
        sortBy: query.sortBy,
        sortOrder: query.sortOrder === 'descending' ? 'desc' : 'asc',
    };

    return request({
        url: '/api/departments',
        method: 'get',
        params: params,
    });
}

/**
 * 获取所有父科室列表
 * GET /api/departments/parents
 */
export function getAllParentDepartments() {
    return request({
        url: '/api/departments/parents',
        method: 'get',
    });
}

/**
 * 根据父科室ID获取子科室列表
 * GET /api/departments/parent/{parentId}
 */
export function getDepartmentsByParentId(parentId) {
    return request({
        url: `/api/departments/parent/${parentId}`,
        method: 'get',
    });
}

/**
 * 新增子科室信息
 * POST /api/departments
 * 接收 DepartmentDTO (包含 name, parentDepartmentName, description)
 */
export function createDepartment(departmentData) {
    return request({
        url: '/api/departments',
        method: 'post',
        data: departmentData,
    });
}

/**
 * 获取所有科室列表（不分页）
 * GET /api/departments
 * 用于下拉选择等场景
 */
export function getAllDepartments() {
    return request({
        url: '/api/departments',
        method: 'get',
        params: {
            page: 0,
            size: 1000 // 获取大量数据，相当于获取全部
        }
    });
}

/**
 * 获取指定科室下的所有医生列表
 * GET /api/departments/{departmentId}/doctors
 * 返回 DepartmentDoctorsResponseDTO
 */
export function getDepartmentDoctors(departmentId) {
    return request({
        url: `/api/departments/${departmentId}/doctors`,
        method: 'get',
    });
}

/**
 * 为指定科室添加新成员
 * POST /api/departments/{departmentId}/members
 * @param {string|number} departmentId - 科室的ID
 * @param {object} memberData - 要添加的成员信息 (例如 { identifier, fullName, title })
 */
export function addDepartmentMember(departmentId, memberData) {
    return request({
        url: `/api/departments/${departmentId}/members`,
        method: 'post',
        data: memberData, // 将成员信息放在请求体中
    });
}

/**
 * 从指定科室删除一个成员
 * DELETE /api/departments/{departmentId}/members/{identifier}
 * @param {string|number} departmentId - 科室的ID
 * @param {string} memberIdentifier - 要删除的成员的ID (医生工号)
 */
export function deleteDepartmentMember(departmentId, memberIdentifier) {
    return request({
        url: `/api/departments/${departmentId}/members/${memberIdentifier}`,
        method: 'delete',
    });
}

/**
 * 更新科室信息
 * PUT /api/departments/description
 * 接收 DepartmentUpdateDTO (包含 departmentId, name, description, parentDepartmentName)
 */
export function updateDepartmentDescription(departmentData) {
    return request({
        url: '/api/departments/description',
        method: 'put',
        data: departmentData,
    });
}

/**
 * 删除科室
 * DELETE /api/departments/{name}
 * @param {string} departmentName - 要删除的科室名称
 */
export function deleteDepartmentByName(departmentName) {
    return request({
        url: `/api/departments/${departmentName}`,
        method: 'delete',
    });
}

/**
 * 根据ID获取科室详情
 * GET /api/departments/{id}
 * @param {number} id - 科室ID
 */
export function getDepartmentById(id) {
    return request({
        url: `/api/departments/${id}`,
        method: 'get',
    });
}

/**
 * 获取科室树形结构数据（排除ID=999的未分配科室）
 * GET /api/departments/tree
 */
export function getDepartmentTree() {
    return request({
        url: '/api/departments/tree',
        method: 'get',
    });
}

/**
 * 根据科室ID获取该科室下的医生列表
 * GET /api/departments/{departmentId}/doctors
 * @param {number} departmentId - 科室ID
 */
export function getDoctorsByDepartmentId(departmentId) {
    return request({
        url: `/api/departments/${departmentId}/doctors`,
        method: 'get',
    });
}

/**
 * 获取所有未分配科室的医生（department_id = 999）
 * GET /api/departments/unassigned-doctors
 */
export function getUnassignedDoctors() {
    return request({
        url: '/api/departments/unassigned-doctors',
        method: 'get',
    });
}

/**
 * 批量将医生添加到指定科室
 * POST /api/departments/{departmentId}/batch-add-doctors
 * @param {number} departmentId - 科室ID
 * @param {Array<string>} doctorIdentifiers - 医生工号列表
 */
export function batchAddDoctorsToDepartment(departmentId, doctorIdentifiers) {
    return request({
        url: `/api/departments/${departmentId}/batch-add-doctors`,
        method: 'post',
        data: doctorIdentifiers,
    });
}

/**
 * 删除科室（支持非空科室删除）
 * DELETE /api/departments/{departmentId}
 * @param {number} departmentId - 科室ID
 */
export function deleteDepartment(departmentId) {
    return request({
        url: `/api/departments/${departmentId}`,
        method: 'delete',
    });
}