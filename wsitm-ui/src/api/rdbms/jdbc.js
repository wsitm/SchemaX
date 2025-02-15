import request from '@/utils/request'

// 查询驱动管理列表
export function listJdbc(query) {
  return request({
    url: '/rdbms/jdbc/list',
    method: 'get',
    params: query
  })
}

// 查询驱动管理详细
export function getJdbc(jdbcId) {
  return request({
    url: '/rdbms/jdbc/' + jdbcId,
    method: 'get'
  })
}

// 新增驱动管理
export function addJdbc(data) {
  return request({
    url: '/rdbms/jdbc',
    method: 'post',
    data: data
  })
}

// 修改驱动管理
export function updateJdbc(data) {
  return request({
    url: '/rdbms/jdbc',
    method: 'put',
    data: data
  })
}

// 删除驱动管理
export function delJdbc(jdbcId) {
  return request({
    url: '/rdbms/jdbc/' + jdbcId,
    method: 'delete'
  })
}

// 装载驱动
export function loadJdbc(jdbcId, action) {
  return request({
    url: `/rdbms/jdbc/load/${jdbcId}/${action}`,
    method: 'post'
  })
}
