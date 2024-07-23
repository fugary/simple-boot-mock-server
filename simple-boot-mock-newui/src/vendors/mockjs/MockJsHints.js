import * as monaco from 'monaco-editor'
import BetterMockJsCode from '@/vendors/mockjs/BetterMockJs.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import { MockRandom } from '@/vendors/mockjs/MockJsonHintData'

export const configToSuggestion = (config, range) => {
  let insertText = config.label
  if (/[)"]$/.test(insertText)) {
    insertText = `${insertText.substring(0, insertText.length - 1)}\${0}${insertText.charAt(insertText.length - 1)}`
  }
  return {
    label: {
      label: config.label,
      detail: '——>' + config.detail,
      description: config.desc
    },
    kind: monaco.languages.CompletionItemKind.Text,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    insertText,
    range
  }
}

export const getCompletionItemProvider = (checkMatch, getSuggestions) => {
  return function (model, position) {
    const word = model.getWordUntilPosition(position)
    const textUntilPosition = model.getValueInRange({
      startLineNumber: position.lineNumber,
      startColumn: 1,
      endLineNumber: position.lineNumber,
      endColumn: position.column
    })
    const match = checkMatch(textUntilPosition)
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

const getMockJsPlaceholders = ({ quote, prefix = '@', matcher = () => true, labelFun } = {}) => {
  const keyArr = Object.keys(MockRandom)
  labelFun = labelFun || (key => quote ? `"${prefix}${key}"` : `${prefix}${key}`)
  return getCompletionItemProvider(matcher, range => keyArr.map(key => {
    const config = MockRandom[key]
    const detail = `——>${key}${config.func.toString()}`.replace(/\s+/g, '').replace('=>{}', '')
    return configToSuggestion({
      label: labelFun(key),
      detail,
      desc: config.desc
    }, range)
  }))
}

export const initMockJsHints = () => {
  if (!monaco.languages.__initedMockJsHints__) {
    monaco.languages.__initedMockJsHints__ = true
    monaco.languages.typescript.javascriptDefaults.addExtraLib(BetterMockJsCode, 'BetterMockJs.js')
    monaco.languages.typescript.javascriptDefaults.addExtraLib(RequestHintDataCode, 'RequestHintData.js')
    monaco.languages.registerCompletionItemProvider('json', {
      triggerCharacters: ['"'],
      provideCompletionItems: getMockJsPlaceholders()
    })
    monaco.languages.registerCompletionItemProvider('json', {
      triggerCharacters: ['@'],
      provideCompletionItems: getMockJsPlaceholders({ quote: true })
    })
    monaco.languages.registerCompletionItemProvider('javascript', {
      triggerCharacters: ['"', "'", '`'],
      provideCompletionItems: getMockJsPlaceholders()
    })
    monaco.languages.registerCompletionItemProvider('html', {
      triggerCharacters: ['@'],
      provideCompletionItems: getMockJsPlaceholders({ prefix: '' })
    })
    monaco.languages.registerCompletionItemProvider('html', {
      triggerCharacters: ['.'],
      provideCompletionItems: getMockJsPlaceholders({
        prefix: '',
        matcher: text => /Mock.Random\./.test(text),
        labelFun: key => `${key}()`
      })
    })
    monaco.languages.registerCompletionItemProvider('html', {
      provideCompletionItems: getCompletionItemProvider((text) => /\{\{/.test(text), range => {
        return [{
          label: 'request',
          detail: 'request请求对象',
          desc: '包含params，body，bodyStr，headers，parameters，pathParameters'
        }, {
          label: 'Mock',
          detail: 'MockJS对象',
          desc: '生成假数据工具'
        }].map(config => configToSuggestion(config, range))
      })
    })
    monaco.languages.registerCompletionItemProvider('html', {
      triggerCharacters: ['.'],
      provideCompletionItems: getCompletionItemProvider((text) => /request\./.test(text), range => {
        return [{
          label: 'body',
          detail: 'request.body',
          desc: 'body内容对象（仅json）'
        }, {
          label: 'bodyStr',
          detail: 'request.bodyStr',
          desc: 'body内容字符串'
        }, {
          label: 'headers',
          detail: 'request.headers',
          desc: '头信息对象'
        }, {
          label: 'parameters',
          detail: 'request.parameters',
          desc: '请求参数对象'
        }, {
          label: 'pathParameters',
          detail: 'request.pathParameters',
          desc: '路径参数对象'
        }, {
          label: 'params',
          detail: 'request.params',
          desc: '请求参数和路径参数合并'
        }].map(config => configToSuggestion(config, range))
      })
    })
    monaco.languages.registerCompletionItemProvider('html', {
      triggerCharacters: ['.'],
      provideCompletionItems: getCompletionItemProvider((text) => /Mock\./.test(text), range => {
        return [{
          label: 'mock()',
          detail: 'Mock.mock',
          desc: 'MockJS生成数据方法'
        }, {
          label: 'Random',
          detail: 'Mock.Random',
          desc: 'Mock.Random生成随机数据'
        }].map(config => configToSuggestion(config, range))
      })
    })
  }
}
