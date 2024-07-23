import * as monaco from 'monaco-editor'
import BetterMockJsCode from '@/vendors/mockjs/BetterMockJs.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import { getMockJsPlaceholders } from '@/vendors/mockjs/MockJsonHintData'
import { initXmlWithJs } from '@/vendors/mockjs/XmlWithJs'

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
    initXmlWithJs(monaco)
  }
}
