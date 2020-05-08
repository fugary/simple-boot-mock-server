import request from '@/utils/request'

export default function(url) {
  function search(params, config) {
    return request(Object.assign({
      url,
      method: 'get',
      params
    }, config))
  }

  function getById(id, config) {
    return request(Object.assign({
      url: `${url}/${id}`,
      method: 'get'
    }, config))
  }

  function removeById(id, config) {
    return request(Object.assign({
      url: `${url}/${id}`,
      method: 'delete'
    }, config))
  }

  function saveOrUpdate(data, config) {
    return request(Object.assign({
      url,
      method: 'post',
      data
    }, config))
  }

  return {
    search,
    getById,
    removeById,
    saveOrUpdate
  }
}
