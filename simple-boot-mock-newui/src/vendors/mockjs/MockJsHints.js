import * as monaco from 'monaco-editor'
import BetterMockJsCode from '@/vendors/mockjs/BetterMockJs.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import { MockRandom } from '@/vendors/mockjs/MockJsonHintData'

export const getCompletionItemProvider = (checkMatch, getSuggestions) => {
  return function (model, position) {
    const word = model.getWordUntilPosition(position)
    const textUntilPosition = model.getValueInRange({
      startLineNumber: 1,
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

const getMockJsPlaceholders = (quote) => {
  const keyArr = Object.keys(MockRandom)
  return getCompletionItemProvider(() => true, range => keyArr.map(key => {
    const config = MockRandom[key]
    const detail = `——>${key}${config.func.toString()}`.replace(/\s+/g, '').replace('=>{}', '')
    return {
      label: {
        label: quote ? `"@${key}"` : `@${key}`,
        detail,
        description: config.desc
      },
      kind: monaco.languages.CompletionItemKind.Text,
      insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
      insertText: quote ? `"@${key}\${0}"` : `@${key}`,
      range
    }
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
      provideCompletionItems: getMockJsPlaceholders(true)
    })
    monaco.languages.registerCompletionItemProvider('javascript', {
      triggerCharacters: ['"', "'", '`'],
      provideCompletionItems: getMockJsPlaceholders()
    })
  }
}
