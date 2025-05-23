import { $coreConfirm, getSingleSelectOptions } from '@/utils'
import { $i18nKey } from '@/messages'
import { sample } from 'openapi-sampler'
import { XMLBuilder } from 'fast-xml-parser'
import { isArray, isFunction, isString, cloneDeep } from 'lodash-es'
import { ALL_CONTENT_TYPES } from '@/api/mock/MockDataApi'

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
      const dataList = keySuggestions.filter(item => item.toLowerCase().includes(queryString?.toLowerCase()))
        .map(value => ({ value }))
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

export const processEvnParams = (groupConfig, dataValue) => {
  if (groupConfig && isString(dataValue) && dataValue.includes('{{') && dataValue.includes('}}')) {
    groupConfig = groupConfig && isString(groupConfig) ? JSON.parse(groupConfig) : groupConfig
    if (groupConfig?.envParams?.length) {
      groupConfig?.envParams.filter(param => param.enabled && param.name && isString(param.value)).forEach(item => {
        dataValue = dataValue.replace(`{{${item.name}}}`, (item.value || '').trim())
      })
    }
  }
  return dataValue
}

export const useContentTypeOption = (prop = 'contentType', charset = true) => {
  const charsetOption = charset
    ? {
        label: 'Charset',
        prop: 'defaultCharset',
        type: 'autocomplete',
        tooltip: 'Default charset is UTF-8, none for responding without charset',
        attrs: {
          fetchSuggestions: ['UTF-8', 'ISO-8859-1', 'GBK', 'GB2312', 'GB18030', 'UTF-16'].map(value => ({ value }))
        }
      }
    : undefined
  return {
    label: 'Content Type',
    prop,
    type: 'select',
    children: getSingleSelectOptions(...ALL_CONTENT_TYPES),
    attrs: {
      clearable: false
    },
    charsetOption
  }
}

export const checkImageAccept = headers => Object.keys(headers || {}).find(key => key.toLowerCase() === 'accept')

export const isImageUrl = (url) => /\.(png|jpg|jpeg|gif|webp|bmp)(\?.*)?$/i.test(url)

export const calcPreviewHeaders = (url, config) => {
  const imageExt = isImageUrl(url)
  const accept = checkImageAccept(config?.headers)
  if (imageExt || (accept && config.headers[accept]?.includes('image'))) {
    config.responseType = 'blob'
  }
}

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
