<script setup>

import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const paramTarget = defineModel('modelValue', {
  type: Object,
  required: true
})

const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

const matchPatternOption = {
  label: '匹配规则',
  type: 'vue-monaco-editor',
  prop: 'matchPattern',
  tooltip: `匹配规则支持javascript表达式，可以使用request请求数据: <br>
        request.body——body内容对象（仅json）<br>
        request.bodyStr——body内容字符串<br>
        request.headers——头信息对象<br>
        request.parameters——请求参数对象<br>
        request.pathParameters——路径参数对象
    `,
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
