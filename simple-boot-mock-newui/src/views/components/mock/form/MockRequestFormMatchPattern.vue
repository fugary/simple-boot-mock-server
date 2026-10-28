<script setup lang="jsx">
import { $i18nBundle, $i18nKey } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed } from 'vue'
import { showCodeWindow, showMockTips } from '@/utils/DynamicUtils'
import { defineFormOptions } from '@/components/utils'
import { ElTag, ElText } from 'element-plus'
import { isBoolean } from 'lodash-es'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

const props = defineProps({
  expressionOptions: {
    type: Object,
    default: () => ({})
  },
  responseTarget: {
    type: Object,
    default: null
  }
})

const paramTarget = defineModel('modelValue', {
  type: Object,
  required: true
})

const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

languageRef.value = 'javascript'

const matchPatternOptions = computed(() => {
  return defineFormOptions([{
    labelKey: props.expressionOptions.labelKey || 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltips: [{
      tooltip: $i18nBundle('common.label.newWindowEdit'),
      tooltipIcon: 'EditPen',
      tooltipFunc: () => showCodeWindow(paramTarget.value?.matchPattern, {
        language: 'javascript',
        title: $i18nKey('common.label.commonEdit', props.expressionOptions.labelKey || 'mock.label.matchPattern'),
        readOnly: false,
        change: (value, lang) => {
          paramTarget.value.matchPattern = value
          languageRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      enabled: props.expressionOptions.booleanResult !== false,
      tooltipFunc: () => showMockTips('matchPattern')
    }],
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: paramTarget.value.matchPattern,
      value: paramTarget.value.matchPattern,
      'onUpdate:value': (value) => {
        paramTarget.value.matchPattern = value
        contentRef.value = value
        languageRef.value = 'javascript'
      },
      language: languageRef.value,
      height: '100px',
      theme: useGlobalConfigStore().monacoTheme,
      options: monacoEditorOptions
    }
  }, {
    labelKey: props.expressionOptions.booleanResult === false ? 'mock.label.expressionResult' : 'mock.label.matchResult',
    type: 'common-form-label',
    formatter () {
      if (props.responseTarget && props.responseTarget.data) {
        const data = JSON.parse(props.responseTarget.data)
        if (data.success) {
          const resultData = data.resultData
          if (props.expressionOptions.booleanResult === false) {
            const resultType = resultData === null ? 'null' : Array.isArray(resultData) ? 'array' : typeof resultData
            return <>
              <ElTag type="info">{resultType}</ElTag>
              <ElText class="margin-left1" style="white-space: pre-wrap; overflow-wrap: anywhere;">{JSON.stringify(resultData, null, 2) ?? String(resultData)}</ElText>
            </>
          }
          const isBool = isBoolean(resultData)
          return <>
            <ElTag type={resultData ? 'success' : 'danger'}>{String(!!resultData)}</ElTag>
            {!isBool ? <ElText class="margin-left1" type="info">[{resultData}]</ElText> : ''}
          </>
        }
        return <ElText type="danger">{data.message}：{String(data.resultData)}</ElText>
      }
      return '——'
    }
  }])
})
</script>

<template>
  <el-container class="flex-column">
    <template
      v-for="(option, index) in matchPatternOptions"
      :key="index"
    >
      <common-form-control
        :model="paramTarget"
        label-width="180px"
        :option="option"
      />
    </template>
  </el-container>
</template>

<style scoped>

</style>
