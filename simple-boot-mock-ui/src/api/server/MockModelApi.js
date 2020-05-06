import request from '@/utils/request'

export default function(url) {
  function search(params) {
    return request({
      url,
      method: 'get',
      params
    })
  }

  function getById(id) {
    return request({
      url: `${url}/${id}`,
      method: 'get'
    })
  }

  function removeById(id) {
    return request({
      url: `${url}/${id}`,
      method: 'delete'
    })
  }

  function saveOrUpdate(data) {
    return request({
      url,
      method: 'post',
      data
    })
  }

  return {
    search,
    getById,
    removeById,
    saveOrUpdate
  }
}
