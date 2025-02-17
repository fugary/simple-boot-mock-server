/* eslint-disable no-useless-escape */
import {
  configToSuggestion,
  getCompletionItemProvider,
  getMockJsPlaceholders,
  hoverMockRandom,
  baseHoverHints, configToHover
} from '@/vendors/mockjs/MockJsonHintData'

export const XML_WITH_JS_ID = 'xmlWithJs'
export const initXmlWithJs = (monaco) => {
  // 自定义语言
  monaco.languages.register({ id: XML_WITH_JS_ID })
  const LANG_KEYWORDS = [
    'break', 'case', 'catch', 'class', 'continue', 'const', 'constructor', 'debugger', 'default', 'delete', 'do', 'else', 'enum', 'export', 'extends', 'false', 'finally', 'for', 'function', 'if', 'import', 'in', 'instanceof', 'new', 'null', 'return', 'super', 'switch', 'this', 'throw', 'true', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield', 'let', 'static', 'implements', 'interface', 'package', 'private', 'protected', 'public', 'await', 'abstract', 'boolean', 'byte', 'char', 'double', 'final', 'float', 'goto', 'int', 'long', 'native', 'short', 'synchronized', 'throws', 'transient', 'volatile'
  ]

  const REQUEST_HINTS = [{
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
  }]

  const GLOBAL_HINTS = [{
    label: 'request',
    detail: 'request请求对象',
    desc: '包含params，body，bodyStr，headers，parameters，pathParameters'
  }, {
    label: 'Mock',
    detail: 'MockJS对象',
    desc: '生成假数据工具'
  }, {
    label: 'decodeHex()',
    detail: '将十六进制字符串解码为普通字符串'
  }, {
    label: 'encodeHex()',
    detail: '将普通字符串编码为十六进制格式'
  }, {
    label: 'md5Hex()',
    detail: '数据MD5加密，并输出十六进制数据格式'
  }, {
    label: 'md5Base64()',
    detail: '数据MD5加密，并输出Base64数据格式'
  }, {
    label: 'sha1Hex()',
    detail: '数据SHA1加密，并输出十六进制数据格式'
  }, {
    label: 'sha1Base64()',
    detail: '数据SHA1加密，并输出Base64数据格式'
  }, {
    label: 'sha256Hex()',
    detail: '数据SHA256加密，并输出十六进制数据格式'
  }, {
    label: 'sha256Base64()',
    detail: '数据SHA256加密，并输出Base64数据格式'
  }]

  const MOCK_HINTS = [{
    label: 'mock()',
    detail: 'Mock.mock',
    desc: 'MockJS生成数据方法'
  }, {
    label: 'Random',
    detail: 'Mock.Random',
    desc: 'Mock.Random生成随机数据'
  }]

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
        [/<!DOCTYPE/i, 'metatag.html', '@doctype'], // 大小写不敏感匹配
        [/<!--/, 'comment.html', '@comment'],
        [/<\?/, 'metatag.html', '@processing'],
        [/<(([\w-]+:)?[\w-]+)/, { token: 'tag.html', next: '@tag.$1' }],
        [/<\/(([\w-]+:)?[\w-]+)/, 'tag.html'],
        [/[^<{]+/, 'text.html']
      ],
      // ========== XML 专用状态 ==========
      doctype: [
        [/[^>]+/, 'metatag.content.html'],
        [/>/, 'metatag.html', '@pop']
      ],
      comment: [
        [/-->/, 'comment.html', '@pop'],
        [/[^-]+/, 'comment.content.html'],
        [/./, 'comment.content.html']
      ],
      processing: [
        [/\?>/, 'metatag.html', '@pop'],
        [/[^\?]+/, 'metatag.content.html'],
        [/./, 'metatag.content.html']
      ],
      tag: [
        [/\//, 'tag.html'],
        [/\w+/, 'attribute.name.html'], // 使用HTML专用token类型
        [/=/, 'delimiter.html'], // 修改这里的关键点
        [/"([^"]*)"/, 'attribute.value.html'],
        [/'([^']*)'/, 'attribute.value.html'],
        [/>/, { token: 'tag.html', next: '@pop' }],
        [/\s+/, '']
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
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: '`', close: '`' }
    ],
    surroundingPairs: [
      { open: '<', close: '>' },
      { open: '{{', close: '}}' },
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: '`', close: '`' }
    ]
  })
  const baseXmlWithJsMatcher = (text) => {
    const left = text.match(/\{\{/g)
    const right = text.match(/}}/g)
    return !!left && left.length > (right?.length || 0)
  }

  const baseHoverHintsAll = (word) => {
    let item = baseHoverHints(word, GLOBAL_HINTS)
    if (!item) {
      item = baseHoverHints(word, REQUEST_HINTS)
    }
    if (!item) {
      item = baseHoverHints(word, MOCK_HINTS)
    }
    return item
  }

  // 添加悬停提示
  monaco.languages.registerHoverProvider(XML_WITH_JS_ID, {
    provideHover: function (model, position) {
      // 获取整个文档的内容
      const fullContent = model.getValue()
      const cursorOffset = model.getOffsetAt(position)
      // 向前查找 '{{' 且不是 '}}'
      const beforeCursor = fullContent.slice(0, cursorOffset)
      const hasOpenBrace = beforeCursor.lastIndexOf('{{')
      const hasCloseBraceBeforeOpen = beforeCursor.lastIndexOf('}}')

      if (hasOpenBrace === -1 || (hasCloseBraceBeforeOpen !== -1 && hasCloseBraceBeforeOpen > hasOpenBrace)) {
        return null
      }
      // 向后查找 '}}' 且不是 '{{'
      const afterCursor = fullContent.slice(cursorOffset)
      const hasCloseBrace = afterCursor.indexOf('}}')
      const hasOpenBraceAfterClose = afterCursor.indexOf('{{')
      if (hasCloseBrace === -1 || (hasOpenBraceAfterClose !== -1 && hasOpenBraceAfterClose < hasCloseBrace)) {
        return null
      }
      // 光标在 '{{ }}' 内，显示悬停提示
      const word = model.getWordAtPosition(position)
      if (word) {
        let item = baseHoverHintsAll(word)
        if (!item) {
          item = hoverMockRandom(model, position, word)
        }
        if (item) {
          return configToHover(item, position, word)
        }
      }
      return null
    }
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['', '@'],
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
      return GLOBAL_HINTS.map(config => configToSuggestion(config, range))
    }, baseXmlWithJsMatcher)
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['.'],
    provideCompletionItems: getCompletionItemProvider(range => {
      return REQUEST_HINTS.map(config => configToSuggestion(config, range))
    }, (text) => baseXmlWithJsMatcher(text) && /request\./.test(text))
  })
  monaco.languages.registerCompletionItemProvider(XML_WITH_JS_ID, {
    triggerCharacters: ['.'],
    provideCompletionItems: getCompletionItemProvider(range => {
      return MOCK_HINTS.map(config => configToSuggestion(config, range))
    }, (text) => baseXmlWithJsMatcher(text) && /Mock\./.test(text))
  })
}
