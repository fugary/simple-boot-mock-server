import { isString, isNumber, isBoolean, isObject, isArray } from 'lodash-es'
import { isXml } from '@/services/mock/MockCommonService'
import { XMLBuilder, XMLParser } from 'fast-xml-parser'

const xmlOptions = {
  ignoreAttributes: false,
  format: true
}

export const generateMockTemplateFromData = (dataStr) => {
  if (!dataStr || !dataStr.trim()) {
    return dataStr
  }

  // Try Parse as JSON
  try {
    const jsonObj = JSON.parse(dataStr)
    const mockJson = transformObjToMock(jsonObj)
    return JSON.stringify(mockJson, null, 2)
  } catch {
    // try XML
  }

  // Try Parse as XML
  if (isXml(dataStr)) {
    try {
      const parser = new XMLParser(xmlOptions)
      const xmlObj = parser.parse(dataStr)
      const mockXmlObj = transformObjToMock(xmlObj)
      const builder = new XMLBuilder(xmlOptions)
      return builder.build(mockXmlObj)
    } catch (e) {
      console.error('XML parse error in transformer:', e)
    }
  }

  return dataStr // Unrecognized, return original
}

const isEmail = (str) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(str)
const isUrl = (str) => /^https?:\/\//i.test(str)
const isGuid = (str) => /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(str)
const isPhone = (str) => /^1[3-9]\d{9}$/.test(str)
const isDate = (str) => /^\d{4}-\d{2}-\d{2}$/.test(str)
const isDateTime = (str) => /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(str)
const isChinese = (str) => /[\u4e00-\u9fa5]/.test(str)

const transformObjToMock = (obj) => {
  if (isArray(obj)) {
    // If it's an array with items, we can transform the first item and mock its generation
    if (obj.length > 0) {
      // Just transform elements
      return obj.map(item => transformObjToMock(item))
      // Notice: For Mock.js arrays "list|1-10": [], but that requires mutating keys.
      // Transforming values to mock values is safer and valid everywhere.
    }
    return obj
  } else if (isObject(obj)) {
    const newObj = {}
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        newObj[key] = transformObjToMock(obj[key])
      }
    }
    return newObj
  } else if (isString(obj)) {
    return inferStringMock(obj)
  } else if (isNumber(obj)) {
    if (Number.isInteger(obj)) {
      if (obj.toString().length === 10 || obj.toString().length === 13) {
        // likely timestamp
        return '@timestamp'
      }
      return '@integer(1, 1000)'
    } else {
      return '@float(0, 100, 1, 2)'
    }
  } else if (isBoolean(obj)) {
    return '@boolean'
  }
  return obj
}

const inferStringMock = (str) => {
  if (!str) return '@string'
  if (isGuid(str)) return '@guid'
  if (isEmail(str)) return '@email'
  if (isUrl(str)) return '@url'
  if (isPhone(str)) return '@phone'
  if (isDateTime(str)) return '@datetime'
  if (isDate(str)) return '@date'

  if (str.length === 18 && /^\d{17}[\dXx]$/.test(str)) return '@id' // Chinese ID card

  if (isChinese(str)) {
    if (str.length >= 2 && str.length <= 4) return '@cname'
    if (str.length > 4 && str.length < 15) return '@csentence'
    if (str.length >= 15) return '@cparagraph'
    return '@cword'
  } else {
    if (str.length >= 2 && str.length <= 8 && /^[A-Z][a-z]+$/.test(str)) return '@first'
    if (str.length > 8 && str.length < 30) return '@sentence'
    if (str.length >= 30) return '@paragraph'
    return '@word'
  }
}
