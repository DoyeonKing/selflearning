import request from '@/utils/request';
import { useDoctorStore } from '@/stores/doctorStore';

/**
 * 获取所有排班列表（用于医生端过滤）
 * (假设后端接口 GET /api/schedules 会根据token自动过滤医生)
 * @param {Object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期 (YYYY-MM-DD)
 * @param {string} [params.endDate] - 结束日期 (YYYY-MM-DD)
 */
export function getAllSchedules(params = {}) {
    // 注意：我们假设 /api/schedules 是一个公共接口
    // 医生端调用时，我们获取所有数据（设置一个较大的size）
    // 然后在前端进行过滤
    return request({
        url: '/api/schedules',
        method: 'get',
        params: {
            ...params,
            page: 0, // 从第一页开始
            size: 2000 // 获取一个足够大的数量
        }
    });
}

/**
 * 根据医生ID获取排班列表
 * @param {number} doctorId - 医生ID
 * @param {Object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期 (YYYY-MM-DD)
 * @param {string} [params.endDate] - 结束日期 (YYYY-MM-DD)
 * @param {number} [params.page] - 页码，默认0
 * @param {number} [params.size] - 每页数量，默认100
 */
export function getSchedulesByDoctorId(doctorId, params = {}) {
    return request({
        url: `/api/schedules/doctor/${doctorId}`,
        method: 'get',
        params: {
            page: 0,
            size: 100,
            ...params
        }
    });
}