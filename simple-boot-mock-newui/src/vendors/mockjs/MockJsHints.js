import * as monaco from 'monaco-editor'
import BetterMockJsCode from 'better-mock/typings/index.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import DayjsCode from 'dayjs/index.d.ts?raw'
import { configToHover, getMockJsPlaceholders, hoverMockRandom } from '@/vendors/mockjs/MockJsonHintData'
import { initXmlWithJs, XML_WITH_JS_ID } from '@/vendors/mockjs/XmlWithJs'

const REQUEST_DIAGNOSTICS_OWNER = 'simple-mock-request-diagnostics'
const OBJECT_MEMBER_REGEXP = /\b(request|response)\s*\.\s*([A-Za-z_$][\w$]*)\b/g
const GLOBAL_OBJECT_FIELDS = {
  request: 'url method contentType headers parameters pathParameters cookies body headersStr parametersStr bodyStr path host protocol ip userAgent params query header type queryString'.split(' '),
  response: 'statusCode body bodyStr headers'.split(' ')
}
const GLOBAL_OBJECT_FIELD_SETS = Object.fromEntries(
  Object.entries(GLOBAL_OBJECT_FIELDS).map(([objectName, fields]) => [objectName, new Set(fields)])
)

const isInsideTemplateExpression = (content, offset) => {
  const before = content.slice(0, offset)
  const openIndex = before.lastIndexOf('{{')
  const closeBeforeOpenIndex = before.lastIndexOf('}}')
  if (openIndex === -1 || closeBeforeOpenIndex > openIndex) {
    return false
  }
  const after = content.slice(offset)
  const closeIndex = after.indexOf('}}')
  const nextOpenIndex = after.indexOf('{{')
  return closeIndex !== -1 && (nextOpenIndex === -1 || closeIndex < nextOpenIndex)
}

const refreshRequestDiagnostics = (model) => {
  const languageId = model.getLanguageId()
  if (!['javascript', XML_WITH_JS_ID].includes(languageId)) {
    monaco.editor.setModelMarkers(model, REQUEST_DIAGNOSTICS_OWNER, [])
    return
  }
  const content = model.getValue()
  const markers = []
  let match
  OBJECT_MEMBER_REGEXP.lastIndex = 0
  while ((match = OBJECT_MEMBER_REGEXP.exec(content)) !== null) {
    if (languageId === XML_WITH_JS_ID && !isInsideTemplateExpression(content, match.index)) {
      continue
    }
    const objectName = match[1]
    const field = match[2]
    if (GLOBAL_OBJECT_FIELD_SETS[objectName].has(field)) {
      continue
    }
    const start = model.getPositionAt(match.index)
    const end = model.getPositionAt(match.index + match[0].length)
    markers.push({
      severity: monaco.MarkerSeverity.Warning,
      message: `${objectName}.${field} is undefined. Available ${objectName} fields: ${GLOBAL_OBJECT_FIELDS[objectName].join(', ')}.`,
      startLineNumber: start.lineNumber,
      startColumn: start.column,
      endLineNumber: end.lineNumber,
      endColumn: end.column
    })
  }
  monaco.editor.setModelMarkers(model, REQUEST_DIAGNOSTICS_OWNER, markers)
}

const initRequestDiagnostics = () => {
  const bindModel = (model) => {
    if (model.__simpleMockRequestDiagnostics__) {
      refreshRequestDiagnostics(model)
      return
    }
    model.__simpleMockRequestDiagnostics__ = true
    model.onDidChangeContent(() => refreshRequestDiagnostics(model))
    model.onDidChangeLanguage(() => refreshRequestDiagnostics(model))
    model.onWillDispose(() => monaco.editor.setModelMarkers(model, REQUEST_DIAGNOSTICS_OWNER, []))
    refreshRequestDiagnostics(model)
  }
  monaco.editor.getModels().forEach(bindModel)
  monaco.editor.onDidCreateModel(bindModel)
}

export const initMockJsHints = () => {
  if (!monaco.languages.__initedMockJsHints__) {
    monaco.languages.__initedMockJsHints__ = true
    const MockJsCode = BetterMockJsCode.replace(/export\s*=\s*Mock;/, 'const Mock: Mock.BetterMock;')
      .replace('country(', 'county(') // 单词似乎拼错了
    monaco.languages.typescript.javascriptDefaults.addExtraLib(MockJsCode, 'BetterMockJs.js')
    monaco.languages.typescript.javascriptDefaults.addExtraLib(RequestHintDataCode, 'RequestHintData.js')
    monaco.languages.typescript.javascriptDefaults.addExtraLib(DayjsCode.replace(/export\s*=\s*dayjs;/, ''), 'DayjsCode.ts')
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
    monaco.languages.registerHoverProvider(['json', 'javascript'], {
      provideHover: function (model, position) {
        const word = model.getWordAtPosition(position)
        if (word) {
          const item = hoverMockRandom(model, position, word)
          if (item) {
            return configToHover(item, position, word)
          }
        }
        return null
      }
    })
    initXmlWithJs(monaco)
    initRequestDiagnostics()
  }
}
