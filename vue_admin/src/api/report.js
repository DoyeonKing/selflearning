import request from '@/utils/request'

/**
 * 根据挂号记录统计医生“最低在岗工时”+“门诊人次”
 * GET /api/reports/registration-hours
 * params: { departmentId?, startDate, endDate, doctorId? }
 */
export function getRegistrationHours(params) {
  return request({
    url: '/api/reports/registration-hours',
    method: 'get',
    params
  })
}

/**
 * 导出“挂号工时 + 门诊人次”Excel
 */
export function exportRegistrationHoursExcel(params) {
  return request({
    url: '/api/reports/registration-hours/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

