<script setup>
import { $checkLang, useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed, ref } from 'vue'
import { cloneDeep } from 'lodash-es'

const { contentRef, languageRef, editorRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })
const showWindow = ref(false)
const currentMockData = ref()

const editModel = ref({
  language: languageRef
})

const toEditDataResponse = (mockData) => {
  currentMockData.value = cloneDeep(mockData)
  contentRef.value = mockData.responseBody
  showWindow.value = true
  languageRef.value = $checkLang(mockData.responseBody)
}

const emit = defineEmits(['saveDataResponse'])

const saveDataResponse = () => {
  currentMockData.value.responseBody = contentRef.value
  showWindow.value = false
  emit('saveDataResponse', currentMockData.value)
}

const languageSelectOption = {
  label: '文本格式',
  type: 'select',
  prop: 'language',
  children: [
    { label: 'JSON', value: 'json' },
    { label: 'MockJS/JavaScript', value: 'javascript' },
    { label: 'XML/HTML', value: 'html' },
    { label: 'PlainText', value: 'text' }
  ],
  attrs: {
    clearable: false
  }
}

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 195px)' : '400px')

defineExpose({ toEditDataResponse })

</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    width="1000px"
    destroy-on-close
    title="编辑数据响应体"
    show-fullscreen
    append-to-body
    :ok-click="saveDataResponse"
  >
    <el-container class="flex-column">
      <common-form-control
        :model="editModel"
        :option="languageSelectOption"
        @change="languageRef=$event"
      />
      <vue-monaco-editor
        v-model:value="contentRef"
        :language="languageRef"
        :height="codeHeight"
        :options="monacoEditorOptions"
        @mount="editorRef=$event"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
