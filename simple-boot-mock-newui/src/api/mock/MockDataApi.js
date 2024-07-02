import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'
import axios from 'axios'

const MOCK_DATA_URL = '/admin/data'

export const ALL_STATUS_CODES = [200, 302, 400, 404, 500]

export const ALL_CONTENT_TYPES = ['application/json', 'application/xml', 'text/html', 'text/css', 'application/javascript']

export const preview = function (data, config) {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/preview`,
    method: 'post',
    data
  }, config))
}

export const markDefault = function (data, config) {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/markDefault`,
    method: 'post',
    data
  }, config))
}

export const previewRequest = function (requestUrl, requestItem, dataId, config) {
  const req = axios.create({
    baseURL: import.meta.env.VITE_APP_API_BASE_URL, // url = base url + request url
    timeout: 60000 // request timeout,
  })
  const headers = Object.assign({
    'mock-data-preview': true
  }, dataId ? { 'mock-data-id': dataId } : {}, config.headers || {})// 预览的时候强制指定一个ID
  return req(Object.assign({
    url: requestUrl,
    method: requestItem.method,
    transformResponse: res => res // 信息不要转换掉，这边需要预览原始信息
  }, config, { headers }))
}

export const processResponse = function (response) {
  const { config } = response
  if (!response.status) {
    response = response.response || {}
  }
  const { headers = {}, request = {}, status } = response
  const requestInfo = [{
    name: 'URL',
    value: request.responseURL || config.url
  }, {
    name: 'Code',
    value: status
  }, {
    name: 'Method',
    value: config.method
  }]
  const requestHeaders = JSON.parse(headers['mock-meta-req'] || '[]')
  const responseHeaders = []
  for (const name in headers) {
    if (name !== 'mock-meta-req') {
      responseHeaders.push({
        name,
        value: headers[name]
      })
    }
  }
  const data = response.data
  // if (data) {
  //   const result = hljs.highlightAuto(data)
  //   if (result.language === 'json') {
  //     data = JSON.stringify(JSON.parse(data), null, '  ')
  //   }
  // }
  return {
    data,
    requestInfo,
    requestHeaders,
    responseHeaders
  }
}

export default useResourceApi(MOCK_DATA_URL)
