import request from '@/utils/request'

export function listSnapshot(query) {
  return request({
    url: '/rdbms/snapshot/list',
    method: 'get',
    params: query
  })
}

export function getSnapshot(snapshotId) {
  return request({
    url: '/rdbms/snapshot/' + snapshotId,
    method: 'get'
  })
}

export function getSnapshotTables(snapshotId) {
  return request({
    url: '/rdbms/snapshot/' + snapshotId + '/tables',
    method: 'get'
  })
}

export function createSnapshot(connectId, data) {
  return request({
    url: '/rdbms/snapshot/connect/' + connectId,
    method: 'post',
    data
  })
}

export function delSnapshot(snapshotId) {
  return request({
    url: '/rdbms/snapshot/' + snapshotId,
    method: 'delete'
  })
}
