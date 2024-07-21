<script setup>
import { $i18nBundle } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const paramTarget = defineModel('modelValue', {
  type: Object,
  required: true
})

const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

const matchPatternOption = {
  labelKey: 'mock.label.matchPattern',
  type: 'vue-monaco-editor',
  prop: 'matchPattern',
  tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
  required: true,
  attrs: {
    value: paramTarget.value.matchPattern,
    'onUpdate:value': (value) => {
      paramTarget.value.matchPattern = value
      contentRef.value = value
      languageRef.value = 'javascript'
    },
    language: languageRef.value,
    height: '100px',
    options: monacoEditorOptions
  }
}
</script>

<template>
  <common-form-control
    :model="paramTarget"
    :option="matchPatternOption"
  />
</template>

<style scoped>

</style>
