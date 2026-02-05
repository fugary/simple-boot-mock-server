import { $coreConfirm, getSingleSelectOptions, includesAnyIgnoreCase } from '@/utils'
import { $i18nKey, $i18nBundle } from '@/messages'
import { sample } from 'openapi-sampler'
import { XMLBuilder } from 'fast-xml-parser'
import { cloneDeep, isArray, isFunction, isObject, isPlainObject, isString } from 'lodash-es'
import {
  ALL_CONTENT_TYPES_LIST,
  CHARSET_LIST,
  LANGUAGE_LIST1,
  isStreamContentType
} from '@/consts/MockConstants'

/**
 * 添加数据
 * @param results
 * @param name
 * @param value
 */
export const addRequestParamsToResult = (results, name, value) => {
  if (results[name]) {
    if (!isArray(results[name])) {
      results[name] = [results[name]]
    }
    results[name].push(value)
  } else {
    results[name] = value
  }
  return results
}

export const generateSchemaSample = (schemaBody, type, spec) => {
  return $coreConfirm($i18nKey('common.msg.commonConfirm', 'common.label.generateData'))
    .then(() => {
      let schema = isString(schemaBody) ? JSON.parse(schemaBody) : cloneDeep(schemaBody)
      schema = removeSchemaDeprecated(schema)
      const json = sample(schema, undefined, spec)
      let resStr
      if (type?.includes('xml')) {
        const builder = new XMLBuilder({
          ignoreAttributes: false,
          format: true,
          indentBy: '\t'
        })
        const rootName = schema.xml?.name || 'root'
        const xml = {
          [rootName]: json
        }
        resStr = builder.build(xml)
      } else {
        resStr = JSON.stringify(json)
      }
      return resStr
    })
}

/**
 * 删除deprecated数据
 *
 * @param schema
 * @returns {*}
 */
export const removeSchemaDeprecated = schema => {
  const properties = schema?.properties
  if (properties) {
    Object.keys(properties).forEach(key => {
      if (properties[key]?.deprecated) {
        delete properties[key]
      } else {
        properties[key] = removeSchemaDeprecated(properties[key])
      }
    })
  }
  return schema
}

export const calcSuggestionsFunc = (keySuggestions) => {
  if (isFunction(keySuggestions)) {
    return keySuggestions
  } else if (isArray(keySuggestions)) {
    return (queryString, cb) => {
      const dataList = keySuggestions.map(value => isObject(value) ? value : ({ value }))
        .filter(item => {
          let valueStr = item?.value ?? ''
          valueStr = isString(valueStr) ? valueStr : valueStr.toString()
          return valueStr.toLowerCase?.().includes(queryString?.toLowerCase())
        })
      cb(dataList)
    }
  }
}
/**
 * 组合多个Suggestions配置
 * @param args
 * @returns {(function(*, *): void)|*}
 */
export const concatValueSuggestions = (...args) => {
  const suggestionsArr = args?.filter(suggestions => !!suggestions)
  if (suggestionsArr?.length) {
    return (queryString, cb) => {
      const dataList = []
      suggestionsArr.forEach(suggestions => {
        const callback = items => isArray(items) && dataList.push(...items)
        const suggestionsFunc = calcSuggestionsFunc(suggestions)
        if (suggestionsFunc) {
          suggestionsFunc(queryString, callback)
        }
      })
      cb(dataList)
    }
  }
}

export const calcEnvSuggestions = (groupConfig) => {
  if (groupConfig) {
    groupConfig = isString(groupConfig) ? JSON.parse(groupConfig) : groupConfig
    return (groupConfig?.envParams || [])
      .filter(param => param.enabled && param.name)
      .map(param => `{{${param.name}}}`)
  }
}

const HEADER_SUGGESTIONS = [{
  keys: ['accept-encoding', 'content-encoding'],
  values: ['gzip', 'deflate', 'br']
}, {
  keys: ['accept', 'content-type'],
  values: ALL_CONTENT_TYPES_LIST.map(i => i.contentType)
}, {
  keys: ['cache-control'],
  values: ['no-cache', 'no-store', 'max-age=3600']
}, {
  keys: ['authorization'],
  values: ['Bearer ', 'Basic ']
}, {
  keyWords: ['charset', 'encoding'],
  values: CHARSET_LIST
}, {
  keyWords: ['language', 'locale', 'lang'],
  values: LANGUAGE_LIST1
}]

export const calcHeaderSuggestions = name => {
  if (!name) return []
  const header = HEADER_SUGGESTIONS.find(h =>
    includesAnyIgnoreCase(name, h.keys) || includesAnyIgnoreCase(name, h.keyWords)
  )
  return header ? header.values : []
}

export const processEvnParams = (groupConfig, dataValue, encode) => {
  if (groupConfig && isString(dataValue) && dataValue.includes('{{') && dataValue.includes('}}')) {
    groupConfig = groupConfig && isString(groupConfig) ? JSON.parse(groupConfig) : groupConfig
    if (groupConfig?.envParams?.length) {
      groupConfig?.envParams.filter(param => param.enabled && param.name && isString(param.value)).forEach(item => {
        dataValue = dataValue.replace(`{{${item.name}}}`, (item.value || '').trim())
      })
    }
  }
  if (isString(dataValue) && encode) {
    dataValue = encodeURIComponent(dataValue)
  }
  return dataValue
}

export const useContentTypeOption = (config = {}) => {
  const { prop = 'contentType', charset = true, ...rest } = isString(config) ? { prop: config } : config
  const contentTypesOptions = getSingleSelectOptions(...ALL_CONTENT_TYPES_LIST
    .filter(type => type.response !== false)
    .map(type => type.contentType))
  const charsetOption = charset
    ? {
        label: 'Charset',
        prop: 'defaultCharset',
        type: 'autocomplete',
        tooltip: $i18nBundle('mock.msg.responseCharsetTooltip'),
        attrs: {
          fetchSuggestions: CHARSET_LIST.map(value => ({ value }))
        }
      }
    : undefined
  return {
    label: 'Content Type',
    prop,
    type: 'select',
    children: contentTypesOptions,
    attrs: {
      clearable: false,
      filterable: true,
      allowCreate: true,
      ...rest
    },
    charsetOption
  }
}

export const checkImageAccept = headers => Object.entries(headers || {}).find(([key]) => key.toLowerCase() === 'accept')?.[1]

export const isMediaUrl = (url) => /\.(png|jpg|jpeg|gif|webp|bmp|svg|mp3|wav|mp4|webm|ogg|pdf|zip|doc|docx|xls|xlsx|ppt|pptx)(\?.*)?$/i.test(url)

export const calcPreviewHeaders = (paramTarget, url, config) => {
  const imageExt = isStreamContentType(paramTarget.contentType) || isMediaUrl(url)
  const accept = checkImageAccept(config?.headers)
  if (imageExt || (accept && (isStreamContentType(accept) || isMediaContentType(accept)))) {
    config.responseType = 'blob'
  }
}

export const isMediaContentType = contentType => ['image', 'audio', 'video'].find(type => contentType?.includes(type))

export const isHtmlContentType = contentType => ['text/html'].find(type => contentType?.includes(type))

const defaultCheckFunc = schema => !schema?.__contentType || schema?.__contentType?.includes('json') || schema?.__contentType?.includes('*/*')

export const generateSampleCheck = (schemaBody, schemaSpec, schemaType, checkFun = defaultCheckFunc) => {
  let parsedSchemaBody = isString(schemaBody) ? JSON.parse(schemaBody) : schemaBody
  if (parsedSchemaBody) {
    parsedSchemaBody.__contentType = schemaType
    if (!isArray(parsedSchemaBody)) {
      parsedSchemaBody = [parsedSchemaBody]
    }
    return parsedSchemaBody.find(schema => checkFun(schema))
  }
}

export const generateSampleCheckResults = (schemaBody, schemaSpec, schemaType) => {
  const results = []
  if (schemaBody) {
    const jsonSchema = generateSampleCheck(schemaBody, schemaSpec, schemaType)
    jsonSchema && results.push({
      type: 'json',
      schema: jsonSchema
    })
    const xmlSchema = generateSampleCheck(schemaBody, schemaSpec, schemaType, schema => {
      return !!schema?.xml || !!schema?.__contentType?.includes('xml')
    })
    xmlSchema && results.push({
      type: 'xml',
      schema: xmlSchema
    })
  }
  return results
}

export const isJson = str => {
  if (isString(str)) {
    str = str.trim()
    return str.startsWith('{') || str.startsWith('[')
  }
  return false
}

export const isXml = str => {
  if (isString(str)) {
    str = str.trim()
    return str.startsWith('<') && str.endsWith('>') && !str.toLowerCase().includes('<!doctype html>')
  }
  return false
}

/**
 * 查找：
 * 1. 第一个数组的值
 * 2. key 为 x.x.x 形式（作为 lodash.get 路径）
 */
export function findArrayAndPath (obj) {
  let arrayData
  const arrayPath = []

  function traverse (current, path = []) {
    if (!current || typeof current !== 'object') return
    if (Array.isArray(current)) return
    for (const key of Object.keys(current)) {
      const value = current[key]
      const currentPath = [...path, key]
      if (Array.isArray(value) && isPlainObject(value[0])) {
        arrayPath.push(currentPath)
        if (arrayData === undefined) {
          arrayData = value
        }
      } else {
        traverse(value, currentPath)
      }
    }
  }

  traverse(obj)

  return {
    data: obj,
    arrayData,
    arrayPath
  }
}

export function checkArrayAndPath (jsonStr) {
  if (isJson(jsonStr)) {
    try {
      return findArrayAndPath(JSON.parse(jsonStr))
    } catch (e) {
      console.error('解析json错误', e)
    }
  }
  return {}
}

export const toProxyUrlParams = (proxyUrl) => {
  let proxyUrlParams = []
  if (isJson(proxyUrl)) {
    proxyUrlParams = JSON.parse(proxyUrl)
  } else if (isString(proxyUrl)) {
    proxyUrlParams = [{
      enabled: true,
      name: 'default',
      value: proxyUrl
    }]
  }
  return proxyUrlParams
}

export const calcProxyUrl = (proxyUrl) => toProxyUrlParams(proxyUrl).find(url => url.enabled && !!url.value)?.value

export const getProxyUrlOptions = () => {
  return {
    nameDynamicOption: () => {
      return {
        placeholder: $i18nKey('common.msg.commonInput', 'common.label.name'),
        labelWidth: '30px',
        colSpan: 6,
        showLabel: false
      }
    },
    valueDynamicOption: () => {
      return {
        placeholder: $i18nKey('common.msg.commonInput', 'mock.label.proxyUrl'),
        labelWidth: '10px',
        colSpan: 10,
        showLabel: false,
        rules: [{
          message: $i18nBundle('mock.msg.proxyUrlMsg'),
          validator: (_, proxyUrl) => {
            return !proxyUrl || /^https?:\/\/.+/.test(proxyUrl)
          }
        }]
      }
    }
  }
}
