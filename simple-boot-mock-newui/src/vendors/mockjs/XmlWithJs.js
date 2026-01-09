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
    label: 'fetch(url, options)',
    detail: 'fetch支持，async/await异步函数支持'
  }, {
    label: 'require(url, options)',
    detail: '提供一个异步版的 CommonJS 风格 require 方法，用于动态加载第三方库。支持 module.exports 和 exports 形式的模块导出，不支持 ESM (export / import) 模块。返回 Promise，可用于异步获取远程或本地模块内容。'
  }, {
    label: 'clearRequireCache()',
    detail: '清空 require 函数的缓存。默认情况下，require 函数会缓存已加载过的 URL 模块数据，避免重复加载。'
  }, {
    label: 'xml2Json()',
    detail: '将XML字符串转换成JSON格式'
  }, {
    label: 'json2Xml()',
    detail: '将JSON字符串转换成XML格式'
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
  }, {
    label: 'encryptAES()',
    detail: '数据AES加密，并输出Base64数据格式'
  }, {
    label: 'encryptDES()',
    detail: '数据DES加密，并输出Base64数据格式'
  }, {
    label: 'encrypt3DES()',
    detail: '数据3DES加密，并输出Base64数据格式'
  }, {
    label: 'encryptSM4()',
    detail: '数据SM4加密，并输出Base64数据格式'
  }, {
    label: 'encryptRSA()',
    detail: '数据RSA加密，并输出Base64数据格式'
  }, {
    label: 'decryptAES()',
    detail: '数据AES解密'
  }, {
    label: 'decryptDES()',
    detail: '数据DES解密'
  }, {
    label: 'decrypt3DES()',
    detail: '数据3DES解密'
  }, {
    label: 'decryptSM4()',
    detail: '数据SM4解密'
  }, {
    label: 'decryptRSA()',
    detail: '数据RSA解密'
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
        [/<\/(([\w-]+:)?[\w-]+)>/, 'tag.html'],
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

  async function formatMixedContent (fullText) {
    const jsBlocks = []
    let placeholderIndex = 0
    // 1. 抽取 {{}} 内 JS 代码，替换成占位符
    const processedText = fullText.replace(/(\{\{[\s\S]*?\}\})/g, (match) => {
      const placeholder = `__JS_PLACEHOLDER_${placeholderIndex++}__`
      const content = match.slice(2, -2)
      const hadLineBreak = content.includes('\n')
      jsBlocks.push({ placeholder, content: content.trim(), hadLineBreak })
      return placeholder
    })
    // 2. 先格式化 XML（带占位符）
    const formattedXml = formatXmlWithIndent(processedText)
    // 3. 格式化所有 JS 代码块
    const formattedJsList = await Promise.all(
      jsBlocks.map(block => formatJsCodeAndDecideBraces(block.content, block.hadLineBreak))
    )
    // 4. 替换回格式化后的 JS 代码块
    let finalText = formattedXml
    jsBlocks.forEach((block, i) => {
      // 这里不加换行，保持JS格式器返回的格式
      const formattedBlock = `{{${formattedJsList[i]}}}`
      finalText = finalText.replace(new RegExp(block.placeholder.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), formattedBlock)
    })
    return finalText
  }

  // 简单的 XML 缩进实现，忽略占位符，不改动占位符文本
  function formatXmlWithIndent (xml, indentSize = 2) {
    let depth = 0
    const lines = xml.split('\n').map(line => line.trim())
    const formattedLines = []
    for (const line of lines) {
      if (!line) {
        formattedLines.push('')
        continue
      }
      if (line.match(/^__JS_PLACEHOLDER_\d+__$/)) {
        // 占位符整行，按当前depth缩进
        formattedLines.push(' '.repeat(depth * indentSize) + line)
        continue
      }
      if (line.match(/^<\/[^>]+>/)) {
        depth = Math.max(depth - 1, 0)
      }
      formattedLines.push(' '.repeat(depth * indentSize) + line)
      if (line.match(/^<[^!?/][^>]*[^/]?>$/)) {
        depth++
      }
    }
    return formattedLines.join('\n')
  }

  async function formatJsCodeAndDecideBraces (code, hadLineBreak) {
    return new Promise((resolve) => {
      const jsModel = monaco.editor.createModel(code, 'javascript')
      const tempEditor = monaco.editor.create(document.createElement('div'), {
        model: jsModel,
        tabSize: 2,
        insertSpaces: true,
        automaticLayout: false,
        lineNumbers: 'off',
        glyphMargin: false,
        folding: false,
        minimap: { enabled: false },
        scrollbar: { vertical: 'hidden', horizontal: 'hidden' },
        overviewRulerLanes: 0,
        renderLineHighlight: 'none'
      })
      tempEditor.getAction('editor.action.formatDocument').run().then(() => {
        let formatted = jsModel.getValue()
        // 清理首尾空行
        formatted = formatted.replace(/^\s*\n/, '').replace(/\n\s*$/, '')
        const isMultiLine = formatted.includes('\n')
        if (hadLineBreak) {
          if (isMultiLine) {
            formatted = `\n${formatted}\n`
          }
        } else {
          if (isMultiLine) {
            formatted = formatted.replace(/\n\s*/g, ' ')
          }
        }
        jsModel.dispose()
        tempEditor.dispose()
        resolve(formatted)
      })
    })
  }

  monaco.languages.registerDocumentFormattingEditProvider(XML_WITH_JS_ID, {
    async provideDocumentFormattingEdits (model) {
      return [{
        range: model.getFullModelRange(),
        text: await formatMixedContent(model.getValue())
      }]
    }
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
