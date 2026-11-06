import * as monaco from 'monaco-editor'
import BetterMockJsCode from 'better-mock/typings/index.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import { $i18nBundle } from '@/messages'
import DayjsCode from 'dayjs/index.d.ts?raw'
import { configToHover, getMockJsPlaceholders, hoverMockRandom } from '@/vendors/mockjs/MockJsonHintData'
import { initXmlWithJs, XML_WITH_JS_ID } from '@/vendors/mockjs/XmlWithJs'

const REQUEST_DIAGNOSTICS_OWNER = 'simple-mock-request-diagnostics'
const OBJECT_MEMBER_REGEXP = /\b(request|response)\s*\.\s*([A-Za-z_$][\w$]*)/g
const GLOBAL_OBJECT_FIELDS = {
  request: 'url method contentType headers parameters pathParameters cookies body headersStr parametersStr bodyStr path host protocol ip userAgent params query header type queryString'.split(' '),
  response: 'statusCode body bodyStr headers'.split(' ')
}
const GLOBAL_OBJECT_FIELD_SETS = Object.fromEntries(
  Object.entries(GLOBAL_OBJECT_FIELDS).map(([objectName, fields]) => [objectName, new Set(fields)])
)

const postProcessorEditors = new Set()

export const bindPostProcessorDiagnostics = editor => {
  if (postProcessorEditors.has(editor)) return
  postProcessorEditors.add(editor)
  let model = editor.getModel()
  refreshRequestDiagnostics(model)
  editor.onDidChangeModel(() => {
    refreshRequestDiagnostics(model)
    model = editor.getModel()
    refreshRequestDiagnostics(model)
  })
  editor.onDidDispose(() => {
    postProcessorEditors.delete(editor)
    refreshRequestDiagnostics(model)
  })
}

const refreshRequestDiagnostics = (model) => {
  if (!model || model.isDisposed()) return
  const languageId = model.getLanguageId()
  if (!['javascript', XML_WITH_JS_ID].includes(languageId)) {
    monaco.editor.setModelMarkers(model, REQUEST_DIAGNOSTICS_OWNER, [])
    return
  }
  const content = model.getValue()
  const hasResponse = [...postProcessorEditors].some(editor => editor.getModel() === model)
  // Match the backend template boundaries, including opening braces inside JS strings.
  const scripts = languageId === XML_WITH_JS_ID
    ? [...content.matchAll(/\{\{([\s\S]+?)}}/g)].map(match => ({ code: match[1], offset: match.index + 2 }))
    : [{ code: content, offset: 0 }]
  const markers = []
  for (const { code, offset } of scripts) {
    // A bare identifier may be a declaration or parameter. Skip uncertain scopes.
    const uncertainObjects = new Set(Object.keys(GLOBAL_OBJECT_FIELDS).filter(name =>
      new RegExp(`\\b${name}\\b(?!\\s*\\.)`).test(code)))
    for (const match of code.matchAll(OBJECT_MEMBER_REGEXP)) {
      const [, objectName, field] = match
      if ((objectName === 'response' && !hasResponse) || uncertainObjects.has(objectName) ||
        GLOBAL_OBJECT_FIELD_SETS[objectName].has(field)) {
        continue
      }
      const start = model.getPositionAt(offset + match.index)
      const end = model.getPositionAt(offset + match.index + match[0].length)
      markers.push({
        severity: monaco.MarkerSeverity.Warning,
        message: $i18nBundle('mock.msg.unknownScriptField', { name: objectName, field, fields: GLOBAL_OBJECT_FIELDS[objectName].join(', ') }),
        startLineNumber: start.lineNumber,
        startColumn: start.column,
        endLineNumber: end.lineNumber,
        endColumn: end.column
      })
    }
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
