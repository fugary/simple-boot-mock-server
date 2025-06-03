<script setup>
import { $i18nBundle } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed } from 'vue'
import { showMockTips } from '@/utils/DynamicUtils'

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
    tooltip: $i18nBundle('mock.label.clickToShowDetails'),
    tooltipFunc: () => showMockTips(),
    required: true,
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: paramTarget.value.matchPattern,
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
