import request from "@/utils/request";


export function convertDDL(convert) {
  return request({
    url: '/rdbms/convert/toDDL',
    method: 'post',
    data: convert
  })
}

// DDL预检与问题诊断
export function precheckDDL(data) {
  return request({
    url: '/rdbms/convert/precheck',
    method: 'post',
    data
  })
}
