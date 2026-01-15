import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { $i18nBundle, $i18nKey } from '@/messages'
import { showCodeWindow, showMockTips } from '@/utils/DynamicUtils'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

export const useMockMonacoFieldOption = (modelRef, config) => {
  const { labelKey, prop, showTips, tipKey } = Object.assign({
    labelKey: 'mock.label.matchPattern',
    prop: 'matchPattern',
    language: 'javascript',
    showTips: true
  }, config)
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
        title: $i18nKey('common.label.commonEdit', labelKey),
        readOnly: false,
        change: (value, lang) => {
          modelRef.value[prop] = value
          languageRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      tooltipFunc: () => showMockTips(tipKey),
      enabled: showTips
    }],
    attrs: {
      class: 'common-resize-vertical',
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
