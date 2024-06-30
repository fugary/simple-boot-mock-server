import VueMonacoEditor, { loader } from '@guolao/vue-monaco-editor'
import { ref, watch, toRaw, h, withDirectives, resolveDirective } from 'vue'
const MonacoLoader = () => import('monaco-editor')

const WorkerImporters = {
  JsonWorker: () => import('monaco-editor/esm/vs/language/json/json.worker?worker'),
  CssWorker: () => import('monaco-editor/esm/vs/language/css/css.worker?worker'),
  HtmlWorker: () => import('monaco-editor/esm/vs/language/html/html.worker?worker'),
  JsWorker: () => import('monaco-editor/esm/vs/language/typescript/ts.worker?worker'),
  EditorWorker: () => import('monaco-editor/esm/vs/editor/editor.worker?worker')
}
/**
 * 默认配置
 * @type {IStandaloneEditorConstructionOptions}
 */
const defaultConfig = {
  automaticLayout: true,
  autoIndent: 'full',
  scrollBeyondLastLine: false,
  theme: 'vs-dark',
  wordWrap: 'on',
  readOnly: true
}

/**
 * @param [config] {IStandaloneEditorConstructionOptions} 配置信息
 * @return {IStandaloneEditorConstructionOptions}
 */
export const defineMonacoOptions = (config) => {
  return {
    ...defaultConfig,
    ...config
  }
}

const getMonacoWorker = async (key) => {
  const workerModule = await WorkerImporters[key]?.()
  return workerModule?.default
}

/**
 * vs路径问题：https://www.npmjs.com/package/@guolao/vue-monaco-editor
 */
self.MonacoEnvironment = {
  getWorker: async function (workerId, label) {
    const JsonWorker = await getMonacoWorker('JsonWorker')
    const CssWorker = await getMonacoWorker('CssWorker')
    const HtmlWorker = await getMonacoWorker('HtmlWorker')
    const JsWorker = await getMonacoWorker('JsWorker')
    const EditorWorker = await getMonacoWorker('EditorWorker')
    switch (label) {
      case 'json':
        return new JsonWorker()
      case 'css':
      case 'scss':
      case 'less':
        return new CssWorker()
      case 'html':
      case 'handlebars':
      case 'razor':
        return new HtmlWorker()
      case 'typescript':
      case 'javascript':
        return new JsWorker()
      default:
        return new EditorWorker()
    }
  }
}

const langCheckConfig = {
  json: /(\{[\s\S]*})|(\[[\s\S]*])/,
  html: /(<[\s\S]*>)/,
  sql: /(SELECT\s.*?\bFROM\b)|(INSERT\s.*?\bINTO\b)|(UPDATE\s.*?\bSET\b)|(DELETE\s.*?\bFROM\b)/i
}

export const $checkLang = value => {
  const val = value?.trim() || ''
  if (val) {
    for (const langKey in langCheckConfig) {
      if (langCheckConfig[langKey].test(val)) {
        return langKey
      }
    }
  }
}

export const $formatDocument = (editor, readOnly, delay = 200) => {
  setTimeout(function () { // 延迟执行格式化
    if (readOnly) {
      editor.updateOptions({ // 只读格式化操作无效，需要先去掉只读状态
        readOnly: false
      })
    }
    editor.getAction('editor.action.formatDocument').run().then(function () {
      if (readOnly) {
        editor.updateOptions({
          readOnly: true
        })
      }
    })
  }, delay)
}

const processPasteCode = data => {
  data = data?.replace(/(\\r|\\n|\\t)+/ig, '').replace(/(?!(\\\\\\))[\\]+/ig, '').replace(/^\s+/, '').replace(/\s+$/, '')
  if (data?.match(/^&lt;.*/)) {
    data = data.replace(/&lt;/ig, '<').replace(/&gt;/ig, '>')
  }
  return data
}

export const useMonacoEditorOptions = (config) => {
  const contentRef = ref('')
  const languageRef = ref('')
  const editorRef = ref()
  const monacoEditorOptions = defineMonacoOptions(config)
  const formatDocument = () => {
    if (editorRef.value) {
      self.MonacoEnvironment.getWorker(languageRef.value).then(() => {
        $formatDocument(editorRef.value, monacoEditorOptions.readOnly)
      })
    }
  }
  watch([contentRef, editorRef], () => {
    languageRef.value = $checkLang(contentRef.value)
    if (contentRef.value && editorRef.value && monacoEditorOptions.readOnly) {
      formatDocument()
    }
    if (editorRef.value && !editorRef.value.__internalPasteFunc__) {
      const editor = toRaw(editorRef.value)
      editor.__internalPasteFunc__ = () => {
        const value = editor.getValue()
        contentRef.value = processPasteCode(value)
        editor.setValue(contentRef.value)
        formatDocument()
      }
      editorRef.value.onDidPaste(editorRef.value.__internalPasteFunc__)
    }
  })
  return {
    contentRef,
    languageRef,
    editorRef,
    monacoEditorOptions,
    formatDocument
  }
}

export const getLoadingDiv = (attrs = {}) => {
  const loadingDirective = [[resolveDirective('loading'), true]]
  return withDirectives(h('div', { style: 'height:100%', ...attrs }), loadingDirective)
}

export default {
  install (app) {
    app.component(VueMonacoEditor.name, {
      setup (props) {
        if (loader.__getMonacoInstance() === null) {
          MonacoLoader().then(monaco => loader.config({ monaco }))
        }
        return () => h(VueMonacoEditor, props, () => [getLoadingDiv()])
      }
    })
  }
}
