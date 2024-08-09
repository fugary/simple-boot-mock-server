<script setup>
import { $i18nBundle } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { watch, computed } from 'vue'

const paramTarget = defineModel('modelValue', {
  type: Object,
  required: true
})

const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

languageRef.value = 'javascript'

const matchPatternOption = computed(() => {
  return {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
    required: true,
    attrs: {
      class: 'common-resize-vertical',
      value: paramTarget.value.matchPattern,
      'onUpdate:value': (value) => {
        contentRef.value = value
        languageRef.value = 'javascript'
      },
      language: languageRef.value,
      height: '100px',
      options: monacoEditorOptions
    }
  }
})

watch(contentRef, val => {
  paramTarget.value.matchPattern = val
})

</script>

<template>
  <common-form-control
    :model="paramTarget"
    label-width="180px"
    :option="matchPatternOption"
  />
</template>

<style scoped>

</style>
