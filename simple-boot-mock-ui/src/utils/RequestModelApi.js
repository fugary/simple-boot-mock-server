import axios from 'axios'
import { Message } from 'element-ui'

export const $http = axios.create({
  baseURL: process.env.VUE_APP_BASE_URL,
  timeout: 10000
})

$http.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      Message.error(res.message || 'Unknown Error')
      return Promise.reject(new Error(res.message || 'Unknown Error'))
    } else {
      return res
    }
  },
  error => {
    Message.error(error.message)
    return Promise.reject(error)
  })

export default function (url, methods = {}) {
  function search (params, config) {
    return $http(Object.assign({
      url,
      method: 'get',
      params
    }, config))
  }

  function getById (id, config) {
    return $http(Object.assign({
      url: `${url}/${id}`,
      method: 'get'
    }, config))
  }

  function deleteById (id, config) {
    return $http(Object.assign({
      url: `${url}/${id}`,
      method: 'delete'
    }, config))
  }

  function saveOrUpdate (data, config) {
    return $http(Object.assign({
      url,
      method: 'post',
      data
    }, config))
  }

  return {
    search,
    getById,
    deleteById,
    saveOrUpdate,
    ...methods
  }
}
