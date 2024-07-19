import * as monaco from 'monaco-editor'
import MockHintDataCode from '@/vendors/mockjs/MockHintData?raw'
import { MockRandom } from '@/vendors/mockjs/MockHintData'

export const getCompletionItemProvider = (matchReg, getSuggestions) => {
  return function (model, position) {
    const word = model.getWordUntilPosition(position)
    const textUntilPosition = model.getValueInRange({
      startLineNumber: 1,
      startColumn: 1,
      endLineNumber: position.lineNumber,
      endColumn: position.column
    })
    const match = textUntilPosition.match(matchReg)
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

const getMockJsPlaceholders = () => {
  const keyArr = Object.keys(MockRandom)
  return getCompletionItemProvider(/@/, range => keyArr.map(key => {
    console.log('========================key', key, MockRandom[key].toString())
    return {
      label: `"@${key}"`,
      kind: monaco.languages.CompletionItemKind.Text,
      insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
      insertText: `"@${key}"\${0}`,
      range
    }
  }))
}

export const initMockJsHints = () => {
  if (!monaco.languages.__initedMockJsHints__) {
    monaco.languages.__initedMockJsHints__ = true
    console.log('==============================initMockJsHints', MockHintDataCode, MockRandom)
    const pureMockHintDataCode = MockHintDataCode.replace(/export const .+/, '') // 去掉export语句
    monaco.languages.typescript.javascriptDefaults.addExtraLib(pureMockHintDataCode, 'MockHintData.js')
    monaco.languages.registerCompletionItemProvider('json', {
      provideCompletionItems: getMockJsPlaceholders()
    })
  }
}
