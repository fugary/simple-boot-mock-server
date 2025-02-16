import { useResourceApi } from '@/hooks/ApiHooks'
import { $http, hasLoading } from '@/vendors/axios'
import axios from 'axios'
import { $coreHideLoading, $coreShowLoading } from '@/utils'
import { isArray, isString } from 'lodash-es'
import {
  FORM_URL_ENCODED,
  FORM_DATA,
  MOCK_DATA_PREVIEW_HEADER,
  MOCK_META_DATA_REQ,
  NONE,
  LANG_TO_CONTENT_TYPES
} from '@/consts/MockConstants'
import { processEvnParams } from '@/services/mock/MockCommonService'

const MOCK_DATA_URL = '/admin/data'

export const ALL_STATUS_CODES = [200, 201, 202, 301, 302, 307, 400, 401, 404, 405, 415, 500, 502, 503]

export const ALL_CONTENT_TYPES = ['application/json', 'application/xml', 'text/html', 'text/plain', 'text/css', 'application/javascript', 'application/x-www-form-urlencoded']

export const generateJWT = function (data, config) {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/generateJwt`,
    method: 'post',
    data
  }, config)).then(response => response.data)
}

export const markDefault = function (data, config) {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/markDefault`,
    method: 'post',
    data
  }, config))
}

export const copyMockData = (id, config) => {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/copyMockData/${id}`,
    method: 'POST'
  }, config)).then(response => response.data)
}

export const previewRequest = function (reqData, config) {
  const req = axios.create({
    baseURL: import.meta.env.VITE_APP_API_BASE_URL, // url = base url + request url
    timeout: 60000 // request timeout,
  })
  const headers = Object.assign({
    [MOCK_DATA_PREVIEW_HEADER]: true
  }, config.headers || {})// 预览的时候强制指定一个ID
  config.__startTime = new Date().getTime()
  if (hasLoading(config)) {
    $coreShowLoading(isString(config.loading) ? config.loading : undefined)
  }
  return req(Object.assign({
    url: reqData.url,
    method: reqData.method,
    transformResponse: res => res// 信息不要转换掉，这边需要预览原始信息
  }, config, { headers }))
}

export const processResponse = function (response) {
  console.log('=========================response', response)
  const { config } = response
  if (hasLoading(config)) {
    $coreHideLoading()
  }
  if (!response.status) {
    response = response.response || {}
  }
  const { headers = {}, request = {}, status } = response
  const requestInfo = {
    url: request.responseURL || config.url,
    method: config.method?.toUpperCase(),
    status,
    duration: config.__startTime ? new Date().getTime() - config.__startTime : 0
  }
  const requestHeaders = JSON.parse(headers[MOCK_META_DATA_REQ] || '[]').sort((a, b) => a.name.localeCompare(b.name))
  const responseHeaders = []
  for (const name in headers) {
    if (name !== MOCK_META_DATA_REQ) {
      responseHeaders.push({
        name,
        value: headers[name]
      })
    }
  }
  const data = response.data
  return {
    data,
    requestInfo,
    requestHeaders,
    responseHeaders
  }
}

export const calcParamTarget = (groupItem, requestItem, previewData) => {
  const value = previewData?.mockParams || requestItem?.mockParams
  const requestPath = `/mock/${groupItem.groupPath}${requestItem?.requestPath}`
  const target = {
    pathParams: calcParamTargetByUrl(requestPath),
    requestParams: [],
    headerParams: [],
    [FORM_DATA]: [],
    [FORM_URL_ENCODED]: [],
    method: requestItem?.method || 'GET',
    responseBody: previewData?.responseBody,
    responseFormat: previewData?.responseFormat,
    contentType: previewData?.contentType || 'application/json'
  }
  if (value) {
    const pathParams = target.pathParams
    const savedTarget = JSON.parse(value)
    delete savedTarget.method
    delete savedTarget.responseBody
    if (target.method === 'GET') {
      delete savedTarget.requestBody
    }
    Object.assign(target, savedTarget || {})
    if (savedTarget.pathParams && savedTarget.pathParams.length) {
      const savePathParams = savedTarget.pathParams.reduce((result, item) => {
        result[item.name] = item.value
        return result
      }, {})
      pathParams.forEach(item => {
        item.value = savePathParams[item.name] || ''
      })
    }
    target.pathParams = pathParams
  }
  if (groupItem.groupConfig) {
    target.groupConfig = JSON.parse(groupItem.groupConfig)
  }
  return target
}

export const preProcessParams = (params = []) => {
  return params.filter(param => param.enabled && !!param.name)
}

/**
 * 各种类型的body解析
 *
 * @param paramTarget
 * @return {{data: (string|*), hasBody: boolean}}
 */
export const calcRequestBody = (paramTarget) => {
  const contentType = paramTarget.value.requestContentType
  let data = paramTarget.value.requestBody
  let hasBody = true
  if (contentType === NONE) {
    data = undefined
    hasBody = false
  } else if (contentType === LANG_TO_CONTENT_TYPES[FORM_DATA]) {
    data = new FormData()
    preProcessParams(paramTarget.value[FORM_DATA]).forEach(item => {
      if (isArray(item.value)) {
        item.value.filter(file => !!file?.raw).forEach(file => data.append(item.name, file.raw))
      } else {
        data.append(item.name, item.value)
      }
    })
  } else if (contentType === LANG_TO_CONTENT_TYPES[FORM_URL_ENCODED]) {
    const params = preProcessParams(paramTarget.value[FORM_URL_ENCODED])
    data = Object.fromEntries(params.map(item => [item.name, processEvnParams(paramTarget.value.groupConfig, item.value)]))
  }
  if (isString(data)) {
    data = processEvnParams(paramTarget.value.groupConfig, data)
  }
  return {
    hasBody,
    data
  }
}

export const calcParamTargetByUrl = (calcRequestUrl) => {
  return calcRequestUrl.split('/').filter(seg => seg.match(/^[:{]/))
    .map(seg => seg.replace(/[:{}]/g, ''))
    .reduce((newArr, arrItem) => {
      if (newArr.indexOf(arrItem) < 0) {
        newArr.push(arrItem)
      }
      return newArr
    }, []).map(name => {
      return {
        name,
        value: ''
      }
    })
}

export default useResourceApi(MOCK_DATA_URL)
