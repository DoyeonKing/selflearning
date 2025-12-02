import request from '@/utils/request'

/**
 * 获取就医规范列表（分页）
 */
export function getGuidelines(params) {
  const requestParams = {
    page: params.page || 1,
    pageSize: params.pageSize || 10
  };
  
  // 只有当参数有值且不为空字符串时才添加到请求中
  if (params.keyword && params.keyword.trim() !== '') {
    requestParams.keyword = params.keyword;
  }
  if (params.category && params.category.trim() !== '') {
    requestParams.category = params.category;
  }
  if (params.status && params.status.trim() !== '') {
    requestParams.status = params.status;
  }
  
  return request({
    url: '/api/medical-guidelines',
    method: 'get',
    params: requestParams
  })
}

/**
 * 获取规范详情
 */
export function getGuidelineById(id) {
  return request({
    url: `/api/medical-guidelines/${id}`,
    method: 'get'
  })
}

/**
 * 创建规范
 */
export function createGuideline(data) {
  return request({
    url: '/api/medical-guidelines',
    method: 'post',
    data: {
      title: data.title,
      content: data.content,
      category: data.category,
      status: data.status,
      createdBy: data.createdBy
    }
  })
}

/**
 * 更新规范
 */
export function updateGuideline(id, data) {
  return request({
    url: `/api/medical-guidelines/${id}`,
    method: 'put',
    data: {
      title: data.title,
      content: data.content,
      category: data.category,
      status: data.status
    }
  })
}

/**
 * 删除规范
 */
export function deleteGuideline(id) {
  return request({
    url: `/api/medical-guidelines/${id}`,
    method: 'delete'
  })
}

