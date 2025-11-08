import request from "@/utils/request";


export function convertDDL(convert) {
  return request({
    url: '/rdbms/convert/toDDL',
    method: 'post',
    data: convert
  })
}
