<script setup>
import { $i18nBundle } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'
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
  >
    <template #afterLabel>
      <new-window-edit-link
        v-model="contentRef"
        style="padding:5px"
        language="javascript"
      />
    </template>
  </common-form-control>
</template>

<style scoped>

</style>
