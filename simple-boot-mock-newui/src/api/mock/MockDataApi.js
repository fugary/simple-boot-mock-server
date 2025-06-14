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

export const ALL_STATUS_CODES = [
  { code: 200, labelCn: '成功', labelEn: 'OK' },
  { code: 201, labelCn: '已创建', labelEn: 'Created' },
  { code: 202, labelCn: '已接受', labelEn: 'Accepted' },
  { code: 203, labelCn: '非权威信息', labelEn: 'Non-Authoritative Information' },
  { code: 204, labelCn: '无内容', labelEn: 'No Content' },
  { code: 205, labelCn: '重置内容', labelEn: 'Reset Content' },
  { code: 206, labelCn: '部分内容', labelEn: 'Partial Content' },
  { code: 301, labelCn: '永久重定向', labelEn: 'Moved Permanently' },
  { code: 302, labelCn: '临时重定向', labelEn: 'Temporary Redirect' },
  { code: 304, labelCn: '未修改', labelEn: 'Not Modified' },
  { code: 307, labelCn: '临时重定向', labelEn: 'Temporary Redirect' },
  { code: 308, labelCn: '永久重定向', labelEn: 'Permanent Redirect' },
  { code: 400, labelCn: '错误请求', labelEn: 'Bad Request' },
  { code: 401, labelCn: '未授权', labelEn: 'Unauthorized' },
  { code: 403, labelCn: '禁止访问', labelEn: 'Forbidden' },
  { code: 404, labelCn: '未找到', labelEn: 'Not Found' },
  { code: 405, labelCn: '方法不允许', labelEn: 'Method Not Allowed' },
  { code: 406, labelCn: '不可接受', labelEn: 'Not Acceptable' },
  { code: 409, labelCn: '冲突', labelEn: 'Conflict' },
  { code: 410, labelCn: '已删除', labelEn: 'Gone' },
  { code: 411, labelCn: '需要有效长度', labelEn: 'Length Required' },
  { code: 412, labelCn: '前置条件失败', labelEn: 'Precondition Failed' },
  { code: 413, labelCn: '请求实体过大', labelEn: 'Payload Too Large' },
  { code: 414, labelCn: '请求URI过长', labelEn: 'URI Too Long' },
  { code: 415, labelCn: '不支持的媒体类型', labelEn: 'Unsupported Media Type' },
  { code: 416, labelCn: '请求范围不满足', labelEn: 'Range Not Satisfiable' },
  { code: 417, labelCn: '期望失败', labelEn: 'Expectation Failed' },
  { code: 418, labelCn: '我是茶壶', labelEn: "I'm a teapot" },
  { code: 422, labelCn: '无法处理的实体', labelEn: 'Unprocessable Entity' },
  { code: 429, labelCn: '请求过多', labelEn: 'Too Many Requests' },
  { code: 431, labelCn: '请求头字段过大', labelEn: 'Request Header Fields Too Large' },
  { code: 451, labelCn: '因法律原因不可用', labelEn: 'Unavailable For Legal Reasons' },
  { code: 500, labelCn: '服务器内部错误', labelEn: 'Internal Server Error' },
  { code: 501, labelCn: '未实现', labelEn: 'Not Implemented' },
  { code: 502, labelCn: '错误网关', labelEn: 'Bad Gateway' },
  { code: 503, labelCn: '服务不可用', labelEn: 'Service Unavailable' },
  { code: 504, labelCn: '网关超时', labelEn: 'Gateway Timeout' }
]

export const ALL_CONTENT_TYPES_LIST = [
  { contentType: 'application/json' },
  { contentType: 'application/xml' },
  { contentType: 'text/html' },
  { contentType: 'text/plain' },
  { contentType: 'text/css' },
  { contentType: 'application/javascript' },
  { contentType: 'application/x-www-form-urlencoded', response: false },
  { contentType: 'multipart/form-data', response: false },
  { contentType: 'application/octet-stream', stream: true },
  { contentType: 'image/jpeg', stream: true },
  { contentType: 'image/png', stream: true },
  { contentType: 'image/gif', stream: true },
  { contentType: 'application/pdf', stream: true },
  { contentType: 'audio/mpeg', stream: true },
  { contentType: 'audio/ogg', stream: true },
  { contentType: 'video/mp4', stream: true },
  { contentType: 'video/ogg', stream: true }
]

export const DEFAULT_CONTENT_TYPE = 'application/json'

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

export const searchHistories = (id, data, config) => {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/histories/${id}`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export const loadHistoryDiff = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_DATA_URL}/loadHistoryDiff`,
    method: 'POST',
    data
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
  let error = null
  if (response.response) {
    error = response
    response = response.response
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
    error,
    data,
    requestInfo,
    requestHeaders,
    responseHeaders
  }
}

export const calcParamTarget = (groupItem, requestItem, previewData, schemasConf) => {
  const value = previewData?.mockParams || requestItem?.mockParams
  const requestPath = `/mock/${groupItem.groupPath}${requestItem?.requestPath}`
  let pathParams = calcSchemaParameters(schemasConf, item => item.in === 'path')
  if (!pathParams?.length) {
    pathParams = calcParamTargetByUrl(requestPath)
  }
  const target = {
    pathParams,
    requestParams: calcSchemaParameters(schemasConf),
    headerParams: calcSchemaParameters(schemasConf, item => item.in === 'header'),
    [FORM_DATA]: [],
    [FORM_URL_ENCODED]: [],
    method: requestItem?.method || 'GET',
    responseStatusCode: previewData?.statusCode,
    responseBody: previewData?.responseBody,
    responseFormat: previewData?.responseFormat,
    contentType: previewData?.contentType || DEFAULT_CONTENT_TYPE,
    defaultCharset: previewData?.defaultCharset
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

/**
 * 请求参数Schema计算
 * @param schemasConf
 * @param filter
 * @returns {*[]}
 */
export const calcSchemaParameters = (schemasConf, filter = item => item.in === 'query') => {
  const schemaContent = schemasConf?.schemas?.[0]?.parametersSchema
  if (schemaContent) {
    const paramSchemas = JSON.parse(schemaContent)
    if (isArray(paramSchemas)) {
      return paramSchemas.filter(filter).map(param => {
        return {
          name: param.name,
          value: param.schema?.default || '',
          enabled: true,
          valueRequired: param.required,
          valueSuggestions: param.schema?.enum,
          dynamicOption: () => ({ required: param.required })
        }
      })
    }
  }
  return []
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
