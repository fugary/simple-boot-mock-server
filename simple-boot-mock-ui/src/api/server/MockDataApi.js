import MockModelApi from '@/api/server/MockModelApi'
import request from '@/utils/request'
import axios from 'axios'

const MOCK_DATA_URL = '/admin/data'
const MockDataApi = new MockModelApi(MOCK_DATA_URL)

MockDataApi.preview = function(data, config) {
  return request(Object.assign({
    url: `${MOCK_DATA_URL}/preview`,
    method: 'post',
    data
  }, config))
}

MockDataApi.markDefault = function(data, config) {
  return request(Object.assign({
    url: `${MOCK_DATA_URL}/markDefault`,
    method: 'post',
    data
  }, config))
}

MockDataApi.previewRequest = function(requestUrl, requestItem, dataId, config) {
  const req = axios.create({
    baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
    // withCredentials: true, // send cookies when cross-domain requests
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

MockDataApi.processResponse = function(response) {
  if (!response.status) {
    response = response.response
  }
  const { data, headers, request, status, config } = response
  const requestInfo = [{
    name: 'Request URL',
    value: request.responseURL
  }, {
    name: 'Status Code',
    value: status
  }, {
    name: 'Request Method',
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
  return {
    data,
    requestInfo,
    requestHeaders,
    responseHeaders
  }
}

export default MockDataApi
