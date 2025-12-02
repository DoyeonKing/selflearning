import request from '@/utils/request';
import { useDoctorStore } from '@/stores/doctorStore';

/**
 * 获取医生当日的患者列表（包含预约、病史等信息）
 *
 * @param {number} doctorId - 医生ID
 * @param {string} date - 查询日期 (YYYY-MM-DD)
 * @param {Object} [extraParams] - 额外查询参数
 * @param {string} [extraParams.query] - 搜索关键词 (姓名/手机号/身份证号)
 * @param {string} [extraParams.sort] - 排序字段, e.g., 'appointmentNumber,asc'
 * @param {number} [extraParams.page] - 页码 (从0开始)
 * @param {number} [extraParams.size] - 每页大小
 */
export function getTodaysPatients(doctorId, date, extraParams = {}) {
    return request({
        url: '/api/doctors/todays-appointments',
        method: 'get',
        params: {
            doctorId,
            date,
            ...extraParams
        }
    });
}

/**
 * (备用) 根据ID获取患者的详细档案
 * @param {number} patientId
 */
export function getPatientProfile(patientId) {
    return request({
        url: `/api/patient-profiles/${patientId}`, // 路径基于您的表结构推断
        method: 'get'
    });
}

/**
 * 搜索患者（根据姓名）
 * @param {string} name - 患者姓名
 * @param {Object} params - 额外参数
 */
export function searchPatientsByName(name, params = {}) {
    return request({
        url: '/api/patients/search',
        method: 'get',
        params: {
            name,
            page: 0,
            size: 20,
            ...params
        }
    });
}