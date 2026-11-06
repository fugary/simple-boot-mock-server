import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { bindPostProcessorDiagnostics } from '@/vendors/mockjs/MockJsHints'
import { $i18nBundle, $i18nKey } from '@/messages'
import { showCodeWindow, showMockTips } from '@/utils/DynamicUtils'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { computed, watch } from 'vue'
import { calcContentType } from '@/consts/MockConstants'

export const useMockResponseEditor = mockData => {
  const isRedirect = computed(() => mockData.value?.statusCode >= 300 && mockData.value?.statusCode < 400)
  const editor = useMonacoEditorOptions(
    { readOnly: false, excludeCheckLangs: ['shell', 'sql'] },
    () => mockData.value?.responseFormat || (isRedirect.value ? 'text' : undefined)
  )
  watch([mockData, isRedirect], () => {
    editor.contentRef.value = mockData.value?.responseBody
    editor.checkEditorLang()
  })
  const responseFormatOption = {
    ...editor.languageSelectOption.value,
    change (format) {
      const data = mockData.value
      data.responseFormat = format
      data.contentType = calcContentType(format, editor.contentRef.value, data.contentType)
    }
  }
  return { ...editor, isRedirect, responseFormatOption }
}

export const useMockMonacoFieldOption = (modelRef, config) => {
  const { labelKey, prop, showTips, tipKey, onTest } = Object.assign({
    labelKey: 'mock.label.matchPattern',
    prop: 'matchPattern',
    language: 'javascript',
    showTips: true
  }, config)
  const onEditorMount = prop === 'postProcessor' ? bindPostProcessorDiagnostics : undefined
  const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({
    readOnly: false,
    minimap: { enabled: false }
  })
  return {
    labelKey,
    type: 'vue-monaco-editor',
    prop,
    tooltips: [{
      tooltip: $i18nBundle('common.label.newWindowEdit'),
      tooltipIcon: 'EditPen',
      tooltipFunc: () => showCodeWindow(modelRef.value?.[prop], {
        language: 'javascript',
        onEditorMount,
        title: $i18nKey('common.label.commonEdit', labelKey),
        readOnly: false,
        change: (value, lang) => {
          modelRef.value[prop] = value
          languageRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.msg.expressionTest'),
      tooltipIcon: 'VideoPlay',
      tooltipFunc: onTest,
      enabled: !!onTest
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      tooltipFunc: () => showMockTips(tipKey),
      enabled: showTips
    }],
    attrs: {
      class: 'common-resize-vertical',
      onMount: onEditorMount,
      defaultValue: modelRef.value?.[prop],
      value: modelRef.value?.[prop],
      'onUpdate:value': (value) => {
        modelRef.value[prop] = value
        contentRef.value = value
        languageRef.value = 'javascript'
      },
      language: languageRef.value || 'javascript',
      theme: useGlobalConfigStore().monacoTheme,
      height: '100px',
      options: monacoEditorOptions
    }
  }
}
