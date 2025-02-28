import VueMonacoEditor, { loader } from '@guolao/vue-monaco-editor'
import { ref, watch, toRaw, h, withDirectives, resolveDirective, computed } from 'vue'
import * as monaco from 'monaco-editor'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import JsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import { isFunction } from 'lodash-es'
import { initMockJsHints } from '@/vendors/mockjs/MockJsHints'

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
  readOnly: true,
  language: 'javascript',
  fixedOverflowWidgets: true
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

/**
 * vs路径问题：https://www.npmjs.com/package/@guolao/vue-monaco-editor
 */
self.MonacoEnvironment = {
  getWorker: function (workerId, label) {
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

const langCheckConfig = [{
  type: 'xmlWithJs',
  checkReg: (val) => {
    return /\{\{[\s\S]*}}/m.test(val)
  }
}, {
  type: 'html',
  checkReg: (val) => {
    return /^(<[\s\S]*>)/.test(val) && !/(^\{[\s\S]*})|(^\[[\s\S]*])/.test(val)
  }
}, {
  type: 'javascript',
  checkReg: /function|var\s+|let\s+|const\s+|return\s+|=>|Mock\.mock/
}, {
  type: 'json',
  checkReg: /(^\{[\s\S]*})|(^\[[\s\S]*])/
}, {
  type: 'sql',
  checkReg: /(SELECT\s.*?\bFROM\b)|(INSERT\s.*?\bINTO\b)|(UPDATE\s.*?\bSET\b)|(DELETE\s.*?\bFROM\b)/i
}]

export const $checkLang = value => {
  const val = value?.trim() || ''
  if (val) {
    for (const langConfig of langCheckConfig) {
      const checkReg = langConfig.checkReg
      if ((checkReg instanceof RegExp && checkReg.test(val)) || (isFunction(checkReg) && checkReg(val))) {
        return langConfig.type
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
    editor.getAction('editor.action.formatDocument')?.run().then(function () {
      if (readOnly) {
        editor.updateOptions({
          readOnly: true
        })
      }
    })
  }, delay)
}

export const processPasteCode = data => {
  data = data?.replace(/(\\r|\\n|\\t)+/ig, '').replace(/(?!(\\\\\\))[\\]+/ig, '').replace(/^\s+/, '').replace(/\s+$/, '')
  if (data?.match(/^&lt;.*/)) {
    data = data.replace(/&lt;/ig, '<').replace(/&gt;/ig, '>')
  }
  return data
}
/**
 * @param config {IStandaloneEditorConstructionOptions} 配置信息
 */
export const useMonacoEditorOptions = (config) => {
  const contentRef = ref('')
  const languageRef = ref('')
  const editorRef = ref()
  const monacoEditorOptions = defineMonacoOptions(config)
  const languageModel = ref({
    language: languageRef
  })
  const languageSelectOption = ref({
    labelKey: 'mock.label.dataFormat',
    type: 'radio-group',
    prop: 'language',
    model: languageModel,
    children: [
      { label: 'JSON', value: 'json' },
      { label: 'JavaScript', value: 'javascript' },
      { label: 'XML/HTML', value: 'html' },
      { label: 'XML+JS', value: 'xmlWithJs' },
      { label: 'PlainText', value: 'text' }
    ],
    attrs: {
      clearable: false
    }
  })
  const normalLanguageSelectOption = computed(() => {
    return {
      ...languageSelectOption.value,
      children: languageSelectOption.value.children
        .filter(item => ['json', 'html', 'text'].includes(item.value))
    }
  })
  const formatDocument = () => {
    if (editorRef.value) {
      $formatDocument(editorRef.value, monacoEditorOptions.readOnly)
    }
  }
  const checkEditorLang = (lang) => {
    languageRef.value = lang || $checkLang(contentRef.value) || monacoEditorOptions.language
    if (contentRef.value && editorRef.value) {
      formatDocument()
    }
  }
  watch([contentRef, editorRef], ([content], [oldContent]) => {
    if (!oldContent && content) {
      checkEditorLang()
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
    languageModel,
    languageSelectOption,
    normalLanguageSelectOption,
    formatDocument,
    checkEditorLang
  }
}

export const getLoadingDiv = (attrs = {}) => {
  const loadingDirective = [[resolveDirective('loading'), true]]
  return withDirectives(h('div', { style: 'height:100%', ...attrs }), loadingDirective)
}

const fixEditorSetValue = (props, context) => {
  const onMountKey = 'onMount'
  const onMountFunc = context.attrs?.[onMountKey] || function () {}
  const newOnMount = instance => { // editor实例
    if (!instance.__newSetValue__) {
      const setValue = instance.setValue
      instance.setValue = function (value) {
        setValue.call(this, value || '')
      }
      instance.__newSetValue__ = true
    }
    onMountFunc(instance)
  }
  return { ...props, [onMountKey]: newOnMount }
}

export default {
  install (app) {
    app.component(VueMonacoEditor.name, {
      setup (props, context) {
        monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
          diagnosticCodesToIgnore: [1003, 1005, 1128]
        })
        initMockJsHints()
        loader.config({ monaco })
        return () => h(VueMonacoEditor, fixEditorSetValue(props, context), () => [getLoadingDiv()])
      }
    })
  }
}
