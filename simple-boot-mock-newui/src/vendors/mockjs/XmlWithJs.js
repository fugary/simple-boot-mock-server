/* eslint-disable no-useless-escape */
import { configToSuggestion, getCompletionItemProvider, getMockJsPlaceholders } from '@/vendors/mockjs/MockJsonHintData'

export const XML_WITH_JS_ID = 'xmlWithJs'
export const initXmlWithJs = (monaco) => {
  // 自定义语言
  monaco.languages.register({ id: XML_WITH_JS_ID })
  const LANG_KEYWORDS = [
    'break', 'case', 'catch', 'class', 'continue', 'const', 'constructor', 'debugger', 'default', 'delete', 'do', 'else', 'enum', 'export', 'extends', 'false', 'finally', 'for', 'function', 'if', 'import', 'in', 'instanceof', 'new', 'null', 'return', 'super', 'switch', 'this', 'throw', 'true', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield', 'let', 'static', 'implements', 'interface', 'package', 'private', 'protected', 'public', 'await', 'abstract', 'boolean', 'byte', 'char', 'double', 'final', 'float', 'goto', 'int', 'long', 'native', 'short', 'synchronized', 'throws', 'transient', 'volatile'
  ]
  // 定义嵌套规则
  monaco.languages.setMonarchTokensProvider(XML_WITH_JS_ID, {
    defaultToken: '',
    tokenPostfix: '',
    brackets: [
      { open: '<', close: '>', token: 'delimiter.angle' },
      { open: '{{', close: '}}', token: 'delimiter.curly' },
      { open: '{', close: '}', token: 'delimiter.curly' },
      { open: '[', close: ']', token: 'delimiter.square' },
      { open: '(', close: ')', token: 'delimiter.parenthesis' }
    ],
    tokenizer: {
      root: [
        [/{{/, { token: 'delimiter.curly', next: '@jsInBraces' }],
        [/<\/?[\w\s="/.':;#-\/\?]+>/, 'tag'],
        [/[^{<]+/, 'text']
      ],
      jsInBraces: [
        [/}}/, { token: 'delimiter.curly', next: '@pop' }],
        [/[{}]/, 'delimiter.curly'],
        { include: 'javascript' }
      ],
      javascript: [
        [/\/\/.*$/, 'comment'],
        [/\/\*.*\*\//, 'comment'],
        [/[{}\[\]()]/, 'bracket'],
        [/[<>](?!@symbols)/, 'delimiter.angle'],
        [/[@$a-zA-Z_]\w*/, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],
        [/[;,.]/, 'delimiter'],
        [/"([^"\\]|\\.)*$/, 'string.invalid'],
        [/"/, 'string', '@string_double'],
        [/'([^'\\]|\\.)*$/, 'string.invalid'],
        [/'/, 'string', '@string_single'],
        [/\`/, 'string', '@string_backtick'],
        [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
        [/\d+/, 'number']
      ],
      string_double: [
        [/[^\\"]+/, 'string'],
        [/\\./, 'string.escape.invalid'],
        [/"/, 'string', '@pop']
      ],
      string_single: [
        [/[^\\']+/, 'string'],
        [/\\./, 'string.escape.invalid'],
        [/'/, 'string', '@pop']
      ],
      string_backtick: [
        [/\${/, { token: 'delimiter.curly', next: '@braced_expression' }],
        [/[^\\`$]+/, 'string'],
        [/\\./, 'string.escape.invalid'],
        [/`/, 'string', '@pop']
      ],
      braced_expression: [
        [/}/, { token: 'delimiter.curly', next: '@pop' }],
        { include: 'javascript' }
      ]
    },
    keywords: LANG_KEYWORDS,
    symbols: /[=><!~?:&|+\-*\/\^%]+/
  })

  // 注册语言配置，添加自动补全括号和中括号功能
  monaco.languages.setLanguageConfiguration(XML_WITH_JS_ID, {
    brackets: [
      ['<', '>'],
      ['{{', '}}'],
      ['{', '}'],
      ['[', ']'],
      ['(', ')']
    ],
    autoClosingPairs: [
      { open: '<', close: '>' },
      { open: '{{', close: '}}' },
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' }
    ],
    surroundingPairs: [
      { open: '<', close: '>' },
      { open: '{{', close: '}}' },
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" }
    ]
  })
  const baseXmlWithJsMatcher = (text) => {
    const left = text.match(/\{\{/g)
    const right = text.match(/}}/g)
    return !!left && left.length > (right?.length || 0)
  }
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: [''],
    provideCompletionItems: getMockJsPlaceholders({ prefix: '', matcher: baseXmlWithJsMatcher })
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['.'],
    provideCompletionItems: getMockJsPlaceholders({
      prefix: '',
      matcher: text => baseXmlWithJsMatcher(text) && /Mock.Random\./.test(text),
      labelFun: key => `${key}()`
    })
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    provideCompletionItems: getCompletionItemProvider(range => {
      return LANG_KEYWORDS.map(keyword => configToSuggestion({ label: keyword }, range))
    }, baseXmlWithJsMatcher)
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    provideCompletionItems: getCompletionItemProvider(range => {
      return [{
        label: 'request',
        detail: 'request请求对象',
        desc: '包含params，body，bodyStr，headers，parameters，pathParameters'
      }, {
        label: 'Mock',
        detail: 'MockJS对象',
        desc: '生成假数据工具'
      }].map(config => configToSuggestion(config, range))
    }, baseXmlWithJsMatcher)
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['.'],
    provideCompletionItems: getCompletionItemProvider(range => {
      return [{
        label: 'body',
        detail: 'request.body',
        desc: 'body内容对象'
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
    }, (text) => baseXmlWithJsMatcher(text) && /request\./.test(text))
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['.'],
    provideCompletionItems: getCompletionItemProvider(range => {
      return [{
        label: 'mock()',
        detail: 'Mock.mock',
        desc: 'MockJS生成数据方法'
      }, {
        label: 'Random',
        detail: 'Mock.Random',
        desc: 'Mock.Random生成随机数据'
      }].map(config => configToSuggestion(config, range))
    }, (text) => baseXmlWithJsMatcher(text) && /Mock\./.test(text))
  })
}
