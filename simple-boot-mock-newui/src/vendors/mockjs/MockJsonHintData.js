/* eslint-disable @typescript-eslint/no-unused-vars */
// 与Mock.Random属性对应的数据
import * as monaco from 'monaco-editor'

export const MockRandom = {
  boolean: {
    func: (min, max, current) => {},
    desc: '返回一个随机的布尔值'
  },
  natural: {
    func: (min, max) => {},
    desc: '返回一个随机的自然数'
  },
  integer: {
    func: (min, max) => {},
    desc: '返回一个随机的整数'
  },
  float: {
    func: (min, max, dmin, dmax) => {},
    desc: '返回一个随机的浮点数'
  },
  character: {
    func: (pool) => {},
    desc: '返回一个随机字符'
  },
  string: {
    func: (pool, min, max) => {},
    desc: '返回一个随机字符串'
  },
  range: {
    func: (start, stop, step) => {},
    desc: '返回一个整型数组'
  },
  date: {
    func: (format) => {},
    desc: '返回一个随机的日期字符串'
  },
  time: {
    func: (format) => {},
    desc: '返回一个随机的时间字符串'
  },
  datetime: {
    func: (format) => {},
    desc: '返回一个随机的日期和时间字符串'
  },
  now: {
    func: (util, format) => {},
    desc: '返回当前的日期和时间字符串'
  },
  timestamp: {
    func: () => {},
    desc: '随机生成一个时间戳'
  },
  image: {
    func: (size, background, foreground, format, text) => {},
    desc: '随机生成一个随机的图片地址'
  },
  dataImage: {
    func: (size, text) => {},
    desc: '随机生成一段随机的 Base64 图片编码'
  },
  color: {
    func: () => {},
    desc: '随机生成一个有吸引力的颜色'
  },
  hex: {
    func: () => {},
    desc: '随机生成一个有吸引力的颜色'
  },
  rgb: {
    func: () => {},
    desc: '随机生成一个有吸引力的颜色'
  },
  rgba: {
    func: () => {},
    desc: '随机生成一个有吸引力的颜色'
  },
  hsl: {
    func: () => {},
    desc: '随机生成一个有吸引力的颜色'
  },
  paragraph: {
    func: (min, max) => {},
    desc: '随机生成一段文本'
  },
  cparagraph: {
    func: (min, max) => {},
    desc: '随机生成一段中文文本'
  },
  sentence: {
    func: (min, max) => {},
    desc: '随机生成一个句子'
  },
  csentence: {
    func: (min, max) => {},
    desc: '随机生成一段中文句子'
  },
  word: {
    func: (min, max) => {},
    desc: '随机生成一个单词'
  },
  cword: {
    func: (pool, min, max) => {},
    desc: '随机生成一个汉字'
  },
  title: {
    func: (min, max) => {},
    desc: '随机生成一句标题'
  },
  ctitle: {
    func: (min, max) => {},
    desc: '随机生成一句中文标题'
  },
  first: {
    func: () => {},
    desc: '随机生成一个常见的英文名'
  },
  last: {
    func: () => {},
    desc: '随机生成一个常见的英文姓'
  },
  name: {
    func: (middle) => {},
    desc: '随机生成一个常见的英文姓名'
  },
  cfirst: {
    func: () => {},
    desc: '随机生成一个常见的中文名'
  },
  clast: {
    func: () => {},
    desc: '随机生成一个常见的中文姓'
  },
  cname: {
    func: () => {},
    desc: '随机生成一个常见的中文姓名'
  },
  url: {
    func: (protocol, host) => {},
    desc: '随机生成一个 URL'
  },
  protocol: {
    func: () => {},
    desc: '随机生成一个 URL 协议'
  },
  domain: {
    func: () => {},
    desc: '随机生成一个域名'
  },
  dtl: {
    func: () => {},
    desc: '随机生成一个顶级域名'
  },
  email: {
    func: (domain) => {},
    desc: '随机生成一个邮件地址'
  },
  ip: {
    func: () => {},
    desc: '随机生成一个 IP 地址'
  },
  region: {
    func: () => {},
    desc: '随机生成一个（中国）大区'
  },
  province: {
    func: () => {},
    desc: '随机生成一个（中国）省'
  },
  city: {
    func: (prefix) => {},
    desc: '随机生成一个（中国）市'
  },
  county: {
    func: (prefix) => {},
    desc: '随机生成一个（中国）县'
  },
  zip: {
    func: () => {},
    desc: '随机生成一个邮政编码'
  },
  capitalize: {
    func: (word) => {},
    desc: '把字符串的第一个字母转换为大写'
  },
  upper: {
    func: (str) => {},
    desc: '把字符串转换为大写'
  },
  lower: {
    func: (str) => {},
    desc: '把字符串转换为小写'
  },
  pick: {
    func: (arr, min, max) => {},
    desc: '从数组中随机选取一个元素'
  },
  shuffle: {
    func: (arr, min, max) => {},
    desc: '打乱数组中元素的顺序'
  },
  guid: {
    func: () => {},
    desc: '随机生成一个 GUID'
  },
  id: {
    func: () => {},
    desc: '随机生成一个 18 位身份证'
  },
  increment: {
    func: (step) => {},
    desc: '生成一个全局的自增整数'
  },
  version: {
    func: (depth) => {},
    desc: '随机生成一个版本号'
  },
  phone: {
    func: () => {},
    desc: '生成一个中国的手机号'
  }
}

export const getMockRandomItems = ({ quote, prefix = '@', labelFun } = {}) => {
  const keyArr = Object.keys(MockRandom)
  labelFun = labelFun || (key => quote ? `"${prefix}${key}"` : `${prefix}${key}`)
  return keyArr.map(key => {
    const config = MockRandom[key]
    const detail = `${key}${config.func.toString()}`.replace(/\s+/g, '').replace('=>{}', '')
    return {
      label: labelFun(key),
      detail,
      desc: config.desc
    }
  })
}

export const getMockJsPlaceholders = ({ quote, prefix = '@', matcher, labelFun } = {}) => {
  return getCompletionItemProvider(range => getMockRandomItems({ quote, prefix, labelFun }).map(config => configToSuggestion(config, range)), matcher)
}

export const configToSuggestion = (config, range) => {
  let insertText = config.label
  if (/[)"]$/.test(insertText)) {
    insertText = `${insertText.substring(0, insertText.length - 1)}\${0}${insertText.charAt(insertText.length - 1)}`
  }
  return {
    label: {
      label: config.label,
      detail: config.detail ? ('——>' + config.detail) : undefined,
      description: config.desc
    },
    kind: monaco.languages.CompletionItemKind.Text,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    insertText,
    range
  }
}

export const configToHover = (config, position, word) => {
  let desc = config.desc
  const hasDesc = !!desc
  if (!hasDesc) {
    desc = config.detail
  }
  return {
    range: new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn),
    contents: [
      { value: config.label + (hasDesc && config.detail ? ('—>' + config.detail) : '') },
      { value: desc }
    ]
  }
}

export const getCompletionItemProvider = (getSuggestions, checkMatch, textRangeFun) => {
  return function (model, position) {
    const word = model.getWordUntilPosition(position)
    textRangeFun = textRangeFun || ((model, position) => {
      return model.getValueInRange({
        startLineNumber: 1,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column
      })
    })
    const textUntilPosition = textRangeFun(model, position)
    const match = checkMatch?.(textUntilPosition) ?? true
    if (!match) {
      return { suggestions: [] }
    }
    const range = {
      startLineNumber: position.lineNumber,
      endLineNumber: position.lineNumber,
      startColumn: word.startColumn,
      endColumn: word.endColumn
    }
    return {
      incomplete: true,
      suggestions: getSuggestions(range)
    }
  }
}

export const baseHoverHints = (word, arr) => {
  return arr.find((item) => item.label.includes(word.word))
}

export const hoverMockRandom = (model, position, word) => {
  if (word) {
    const wordAt = model.getValueInRange(new monaco.Range(position.lineNumber, word.startColumn - 1, position.lineNumber, word.endColumn))
    if (wordAt.startsWith('@')) {
      return baseHoverHints({ word: wordAt }, getMockRandomItems().map(config => ({ ...config, detail: `@${config.detail}` })))
    }
  }
}
