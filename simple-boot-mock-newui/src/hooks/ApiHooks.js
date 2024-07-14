import { $http } from '@/vendors/axios'
import { isObject } from 'lodash-es'
import { toFlatKeyValue } from '@/utils'

/**
 * 通用crud restful api减少开发
 * @param url
 * @param methods
 * @return {*&{search: (function(*, *): Promise<axios.AxiosResponse<any>>), getById: (function(*, *): Promise<axios.AxiosResponse<any>>), deleteById: (function(*, *): Promise<axios.AxiosResponse<any>>), saveOrUpdate: (function(*, *): Promise<axios.AxiosResponse<any>>)}}
 */
export const useResourceApi = (url, methods) => {
  function search (params, config) {
    if (isObject(params)) {
      params = toFlatKeyValue(params)
    }
    return $http(Object.assign({
      url,
      method: 'get',
      params
    }, config)).then(response => response.data)
  }

  function getById (id, config) {
    return $http(Object.assign({
      url: `${url}/${id}`,
      method: 'get'
    }, config)).then(response => response.data)
  }

  function deleteById (id, config) {
    return $http(Object.assign({
      url: `${url}/${id}`,
      method: 'delete'
    }, config)).then(response => response.data)
  }

  function saveOrUpdate (data, config) {
    return $http(Object.assign({
      url,
      method: 'post',
      data
    }, config)).then(response => response.data)
  }

  function removeByIds (ids, config) {
    return $http(Object.assign({
      url: `${url}/removeByIds/${ids}`,
      method: 'delete'
    }, config)).then(response => response.data)
  }

  return {
    search,
    getById,
    deleteById,
    removeByIds,
    saveOrUpdate,
    ...methods
  }
}
