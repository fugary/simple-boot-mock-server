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

export const isGetMethod = method => {
  method = (method || 'GET').toUpperCase()
  return method === 'GET'
}

export const extendCurlParams = (paramTarget, curlStr) => {
  const curlObj = curl2Json(curlStr)
  console.log('===============================curl', curlObj, curlStr)
  if (paramTarget.value) {
    paramTarget.value.requestBody = !isGetMethod(paramTarget.value.method) ? curlObj.bodyStr : undefined
    paramTarget.value.requestParams = curlObj.query || []
    paramTarget.value.headerParams = curlObj.headers || []
  }
  return curlObj
}
