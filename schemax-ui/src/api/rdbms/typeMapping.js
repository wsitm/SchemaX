import request from '@/utils/request'

export function listTypeMapping(query) {
  return request({
    url: '/rdbms/type-mapping/list',
    method: 'get',
    params: query
  })
}

export function getTypeMapping(ruleId) {
  return request({
    url: '/rdbms/type-mapping/' + ruleId,
    method: 'get'
  })
}

export function addTypeMapping(data) {
  return request({
    url: '/rdbms/type-mapping',
    method: 'post',
    data
  })
}

export function updateTypeMapping(data) {
  return request({
    url: '/rdbms/type-mapping',
    method: 'put',
    data
  })
}

export function delTypeMapping(ruleId) {
  return request({
    url: '/rdbms/type-mapping/' + ruleId,
    method: 'delete'
  })
}

export function testTypeMapping(data) {
  return request({
    url: '/rdbms/type-mapping/test',
    method: 'post',
    data
  })
}

export function getTypeMappingDatabases() {
  return request({
    url: '/rdbms/type-mapping/databases',
    method: 'get'
  })
}
