import request from '@/utils/request'

// 查询连接配置列表
export function listConnect(query) {
  return request({
    url: '/rdbms/connect/list',
    method: 'get',
    params: query
  })
}

// 查询连接配置详细
export function getConnect(connectId) {
  return request({
    url: '/rdbms/connect/' + connectId,
    method: 'get'
  })
}

// 新增连接配置
export function addConnect(data) {
  return request({
    url: '/rdbms/connect',
    method: 'post',
    data: data
  })
}


// 修改连接配置
export function updateConnect(data) {
  return request({
    url: '/rdbms/connect',
    method: 'put',
    data: data
  })
}

// 删除连接配置
export function delConnect(connectId) {
  return request({
    url: '/rdbms/connect/' + connectId,
    method: 'delete'
  })
}

// 查询连接配置详细
export function checkConnect(data) {
  return request({
    url: '/rdbms/connect/check',
    method: 'post',
    data: data
  })
}

// 刷新缓存
export function flushCache(connectId) {
  return request({
    url: '/rdbms/connect/flush/' + connectId,
    method: 'post'
  })
}


// 查询表格信息
export function getTableInfo(connectId) {
  return request({
    url: '/rdbms/connect/tables/' + connectId,
    method: 'get'
  })
}

// 查询连接关联模板
export function listConnectTemplate(connectId) {
  return request({
    url: '/rdbms/connect/' + connectId + '/templates',
    method: 'get'
  })
}

// 保存连接关联模板
export function saveConnectTemplate(connectId, data) {
  return request({
    url: '/rdbms/connect/' + connectId + '/templates',
    method: 'put',
    data: data
  })
}


// 获取所有的方言列表
export function getDialects() {
  return request({
    url: '/rdbms/connect/dialects',
    method: 'get'
  })
}

// 查询表格信息
export function getTableDDL(connectId, database) {
  return request({
    url: '/rdbms/connect/ddl/' + connectId,
    method: 'get',
    params: {
      database
    }
  })
}
