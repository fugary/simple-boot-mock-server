import { useResourceApi } from '@/hooks/ApiHooks'
import { BASE_URL } from '@/config'
import { $http } from '@/vendors/axios'

export const ALL_METHODS = [
  { method: 'GET', type: 'primary' },
  { method: 'POST', type: 'success' },
  { method: 'DELETE', type: 'danger' },
  { method: 'PUT', type: 'warning' },
  { method: 'PATCH', type: 'info' }]

/**
 * mock地址信息
 * @param path
 * @return {`${string}${string}`}
 */
export const getMockUrl = (path) => {
  if (/^https?:\/\//.test(path)) {
    return path
  }
  let baseUrl = location.origin
  if (/^https?:\/\//.test(BASE_URL)) {
    baseUrl = BASE_URL
  }
  return `${baseUrl}${path}`
}

const MOCK_REQUEST_URL = '/admin/requests'

export const getDefaultData = (id, config) => {
  return $http(Object.assign({
    url: `${MOCK_REQUEST_URL}/getDefaultData/${id}`,
    method: 'get'
  }, config)).then(response => response.data)
}

export const copyMockRequest = (id, config) => {
  return $http(Object.assign({
    url: `${MOCK_REQUEST_URL}/copyMockRequest/${id}`,
    method: 'POST'
  }, config)).then(response => response.data)
}

export const saveMockParams = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_REQUEST_URL}/saveMockParams`,
    method: 'post',
    data
  }, config))
}

export const checkParamsFilled = (params) => {
  return !params?.length || params.filter(param => param.enabled).every(param => param.name && param.value)
}

export default useResourceApi(MOCK_REQUEST_URL)
