import request from '@/utils/request'

// 查询模板管理列表
export function listTemplate(query) {
  return request({
    url: '/rdbms/template/list',
    method: 'get',
    params: query
  })
}

// 查询模板管理详细
export function getTemplate(tpId) {
  return request({
    url: '/rdbms/template/' + tpId,
    method: 'get'
  })
}

// 新增模板管理
export function addTemplate(data) {
  return request({
    url: '/rdbms/template',
    method: 'post',
    data: data
  })
}

// 修改模板管理
export function updateTemplate(data) {
  return request({
    url: '/rdbms/template',
    method: 'put',
    data: data
  })
}

// 删除模板管理
export function delTemplate(tpId) {
  return request({
    url: '/rdbms/template/' + tpId,
    method: 'delete'
  })
}

// 模板类型
export function getTemplateTypes() {
  return request({
    url: '/rdbms/template/types',
    method: 'get'
  })
}

