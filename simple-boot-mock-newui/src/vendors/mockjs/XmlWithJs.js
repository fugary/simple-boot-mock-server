/* eslint-disable no-useless-escape */
export const XML_WITH_JS_ID = 'xmlWithJs'
export const initXmlWithJs = (monaco) => {
  // 自定义语言
  monaco.languages.register({ id: XML_WITH_JS_ID })
  // 定义嵌套规则
  monaco.languages.setMonarchTokensProvider(XML_WITH_JS_ID, {
    defaultToken: '',
    tokenPostfix: '',
    brackets: [
      { open: '<', close: '>', token: 'delimiter.angle' },
      { open: '{{', close: '}}', token: 'delimiter.curly' }
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
    keywords: [
      'break', 'case', 'catch', 'class', 'continue', 'const', 'constructor', 'debugger', 'default', 'delete', 'do', 'else', 'enum', 'export', 'extends', 'false', 'finally', 'for', 'function', 'if', 'import', 'in', 'instanceof', 'new', 'null', 'return', 'super', 'switch', 'this', 'throw', 'true', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield', 'let', 'static', 'implements', 'interface', 'package', 'private', 'protected', 'public', 'await', 'abstract', 'boolean', 'byte', 'char', 'double', 'final', 'float', 'goto', 'int', 'long', 'native', 'short', 'synchronized', 'throws', 'transient', 'volatile'
    ],
    symbols: /[=><!~?:&|+\-*\/\^%]+/
  })
}
