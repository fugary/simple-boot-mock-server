import * as monaco from 'monaco-editor'
import BetterMockJsCode from 'better-mock/typings/index.d.ts?raw'
import RequestHintDataCode from '@/vendors/mockjs/RequestHintData.d.ts?raw'
import DayjsCode from 'dayjs/index.d.ts?raw'
import { getMockJsPlaceholders } from '@/vendors/mockjs/MockJsonHintData'
import { initXmlWithJs } from '@/vendors/mockjs/XmlWithJs'

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
    initXmlWithJs(monaco)
  }
}
