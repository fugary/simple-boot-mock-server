import * as monaco from 'monaco-editor'
import BetterMockJsCode from '@/vendors/mockjs/BetterMockJs.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import { MockRandom } from '@/vendors/mockjs/MockJsonHintData'

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
    console.log('==============================initMockJsHints', BetterMockJsCode, MockRandom)
    monaco.languages.typescript.javascriptDefaults.addExtraLib(BetterMockJsCode, 'MockJsonHintData.js')
    monaco.languages.typescript.javascriptDefaults.addExtraLib(RequestHintDataCode, 'RequestHintData.js')
    monaco.languages.registerCompletionItemProvider('json', {
      provideCompletionItems: getMockJsPlaceholders()
    })
  }
}
