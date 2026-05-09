import { getMockUrl } from '@/api/mock/MockRequestApi'
import { calcRequestBody, preProcessParams } from '@/api/mock/MockDataApi'
import { AUTH_OPTION_CONFIG } from '@/services/mock/MockAuthorizationService'
import { addRequestParamsToResult, processEvnParams } from '@/services/mock/MockCommonService'
import { AUTH_TYPE, FORM_DATA, FORM_URL_ENCODED, LANG_TO_CONTENT_TYPES } from '@/consts/MockConstants'
import { isArray, isObject, isString } from 'lodash-es'

function stripQuotes (str) {
  return str.replace(/^'(.*)'$/, '$1').replace(/^"(.*)"$/, '$1')
}

/**
 * 将 curl 命令字符串转换为 JSON 对象
 * @param {string} curlCmd - curl 命令字符串
 * @returns {Object} 转换后的请求描述对象
 */
export function curl2Json (curlCmd) {
  const result = {
    method: 'GET',
    url: '',
    origin: '',
    path: '',
    query: [], // 数组结构
    headers: [], // 数组结构
    body: null,
    bodyStr: null
  }

  const clean = curlCmd
    .replace(/\\\r?\n/g, '')
    .replace(/\s+/g, ' ')
    .replace(/\^/g, '')
    .trim()

  const tokens = clean.match(/('[^']*'|"[^"]*"|\S+)/g) || []

  let urlToken = tokens.find(t => /^https?:\/\//.test(stripQuotes(t)))
  // 降级扫描：避免 tokens 失效
  if (!urlToken) {
    const urlMatch = clean.match(/https?:\/\/[^\s'"]+/)
    if (urlMatch) urlToken = urlMatch[0]
  }
  // 最终仍未找到
  if (!urlToken) {
    console.warn('Curl URL not found, returning null url')
    result.url = null
    return result
  }

  const rawUrl = stripQuotes(urlToken)
  const urlObj = new URL(rawUrl)

  result.url = `${urlObj.origin}${urlObj.pathname}`
  result.origin = urlObj.origin
  result.path = urlObj.pathname

  // --------- query 变成数组 ----------
  urlObj.searchParams.forEach((v, k) => {
    result.query.push({ name: k, value: v, enabled: true })
  })

  // --------- method ----------
  const methodIndex = tokens.findIndex(t => t === '-X')
  if (methodIndex >= 0 && tokens[methodIndex + 1]) {
    result.method = tokens[methodIndex + 1].toUpperCase()
  }

  // --------- headers 变成数组 ----------
  tokens.forEach((token, i) => {
    if (token === '-H' && tokens[i + 1]) {
      const header = stripQuotes(tokens[i + 1])
      const s = header.split(/:\s*/)
      const key = s.shift()
      const val = s.join(':')
      if (key) {
        result.headers.push({
          name: key,
          value: val,
          enabled: true
        })
      }
    }
  })

  // --------- body ----------
  const dataIndex = tokens.findIndex(t => t === '-d' || t === '--data' || t === '--data-raw')
  if (dataIndex >= 0 && tokens[dataIndex + 1]) {
    const dataStr = stripQuotes(tokens[dataIndex + 1])
    result.bodyStr = dataStr
    try {
      result.body = JSON.parse(dataStr)
    } catch {
      result.body = null
    }
  }

  return result
}

export const calcUrl = rawUrl => {
  if (/^https?:\/\//.test(rawUrl)) {
    try {
      return new URL(rawUrl)
    } catch (e) {
      console.error(e)
    }
  }
  if (rawUrl) {
    return {
      pathname: rawUrl
    }
  }
}

export const CURL_SHELL = {
  BASH: 'bash',
  CMD: 'cmd'
}

const quoteCurlValue = (value, shell = CURL_SHELL.BASH) => {
  value = `${value ?? ''}`
  if (shell === CURL_SHELL.CMD) {
    return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/%/g, '^%')}"`
  }
  return `'${value.replace(/'/g, "'\\''")}'`
}

const hasHeader = (headers, key) => {
  const lowerKey = key.toLowerCase()
  return Object.keys(headers).some(name => name?.toLowerCase() === lowerKey)
}

const formatCurlBody = data => {
  if (data === undefined || data === null) {
    return ''
  }
  if (isString(data)) {
    return data
  }
  if (isObject(data)) {
    return JSON.stringify(data)
  }
  return `${data}`
}

const calcEnabledParams = (paramTargetVal, params = []) => {
  return preProcessParams(params).reduce((results, item) => {
    addRequestParamsToResult(results, item.name, processEvnParams(paramTargetVal.groupConfig, item.value, true))
    return results
  }, {})
}

const appendQueryParams = (url, params) => {
  const queryParts = []
  Object.keys(params).forEach(name => {
    const values = isArray(params[name]) ? params[name] : [params[name]]
    values.forEach(value => {
      queryParts.push(`${encodeURIComponent(name)}=${encodeURIComponent(value ?? '')}`)
    })
  })
  return queryParts.length ? `${url}${url.includes('?') ? '&' : '?'}${queryParts.join('&')}` : url
}

const appendCurlOption = (lines, option, value, shell) => {
  lines.push(value === undefined ? option : `${option} ${quoteCurlValue(value, shell)}`)
}

const appendUrlencodedBody = (lines, paramTargetVal, shell) => {
  preProcessParams(paramTargetVal[FORM_URL_ENCODED]).forEach(item => {
    appendCurlOption(lines, '--data-urlencode', `${item.name}=${processEvnParams(paramTargetVal.groupConfig, item.value) ?? ''}`, shell)
  })
}

const appendFormDataBody = (lines, paramTargetVal, shell) => {
  preProcessParams(paramTargetVal[FORM_DATA]).forEach(item => {
    if (isArray(item.value)) {
      item.value.filter(file => !!file?.raw).forEach(file => {
        appendCurlOption(lines, '-F', `${item.name}=@${file.name}`, shell)
      })
    } else {
      appendCurlOption(lines, '-F', `${item.name}=${processEvnParams(paramTargetVal.groupConfig, item.value) ?? ''}`, shell)
    }
  })
}

const joinCurlCommand = (lines, shell) => {
  return shell === CURL_SHELL.CMD
    ? lines.join(' ^\r\n  ')
    : lines.join(' \\\n  ')
}

export const buildCurlCommand = async (paramTargetVal, requestPath, shell = CURL_SHELL.BASH) => {
  if (!paramTargetVal || !requestPath) {
    return ''
  }
  const method = (paramTargetVal.method || 'GET').toUpperCase()
  const queryParams = calcEnabledParams(paramTargetVal, paramTargetVal.requestParams)
  const headers = preProcessParams(paramTargetVal.headerParams).reduce((results, item) => {
    results[item.name] = processEvnParams(paramTargetVal.groupConfig, item.value)
    return results
  }, {})
  const authParams = { ...queryParams }
  const authContent = paramTargetVal.authContent
  if (authContent && authContent.authType !== AUTH_TYPE.NONE) {
    await AUTH_OPTION_CONFIG[authContent.authType]?.parseAuthInfo(authContent, headers, authParams, { value: paramTargetVal })
  }

  const url = appendQueryParams(getMockUrl(requestPath), authParams)
  const lines = [`curl ${quoteCurlValue(url, shell)}`]
  const bodyLines = []
  let hasRequestBody = false
  if (!isGetMethod(method)) {
    const contentType = paramTargetVal.requestContentType
    const { data, hasBody } = calcRequestBody({ value: paramTargetVal })
    hasRequestBody = hasBody
    if (hasBody) {
      if (contentType && contentType !== LANG_TO_CONTENT_TYPES[FORM_DATA] && !hasHeader(headers, 'Content-Type')) {
        headers['Content-Type'] = contentType
      }
      if (contentType === LANG_TO_CONTENT_TYPES[FORM_DATA]) {
        appendFormDataBody(bodyLines, paramTargetVal, shell)
      } else if (contentType === LANG_TO_CONTENT_TYPES[FORM_URL_ENCODED]) {
        appendUrlencodedBody(bodyLines, paramTargetVal, shell)
      } else {
        const body = formatCurlBody(data)
        if (body) {
          appendCurlOption(bodyLines, '--data-raw', body, shell)
        }
      }
    }
  }

  if (!['GET', 'POST'].includes(method) || (method === 'POST' && !hasRequestBody)) {
    appendCurlOption(lines, '-X', method, shell)
  }
  Object.keys(headers).forEach(name => {
    appendCurlOption(lines, '-H', `${name}: ${headers[name] ?? ''}`, shell)
  })
  return joinCurlCommand([...lines, ...bodyLines], shell)
}

export const isGetMethod = method => {
  method = (method || 'GET').toUpperCase()
  return method === 'GET'
}

export const isCURLStr = curlStr => curlStr?.trim()?.match(/^curl\s+/ig)

export const extendCurlParams = (paramTarget, curlStr) => {
  if (!isCURLStr(curlStr)) { // curl格式
    return
  }
  const curlObj = curl2Json(curlStr)
  console.log('===============================curl', curlObj, curlStr)
  if (paramTarget.value) {
    paramTarget.value.requestBody = !isGetMethod(paramTarget.value.method || curlObj.method) ? curlObj.bodyStr : undefined
    paramTarget.value.requestParams = curlObj.query || []
    paramTarget.value.headerParams = curlObj.headers || []
  }
  return curlObj
}

export const pasteCurl2Request = (request, curlStr) => {
  if (isCURLStr(curlStr)) { // curl格式
    const mockParams = request.mockParams
      ? JSON.parse(request.mockParams)
      : {}
    const curlObj = extendCurlParams({
      value: mockParams
    }, curlStr)
    request.method = curlObj.method
    request.requestPath = curlObj.path || request.requestPath
    request.proxyUrl = curlObj.origin || request.proxyUrl
    request.mockParams = request.mockParams || JSON.stringify(mockParams)
    return true
  }
}
