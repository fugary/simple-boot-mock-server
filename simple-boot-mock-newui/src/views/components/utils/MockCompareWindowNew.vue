<script setup lang="jsx">
import { computed, onBeforeUnmount, shallowRef } from 'vue'
import { $i18nBundle } from '@/messages'
import { isFunction } from 'lodash-es'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  original: {
    type: Object,
    default: undefined
  },
  modified: {
    type: Object,
    default: undefined
  },
  historyOptionsMethod: {
    type: Function,
    default: () => []
  }
})

const showWindow = defineModel({
  type: Boolean,
  default: false
})

const diffOptions = {
  automaticLayout: true,
  formatOnType: true,
  formatOnPaste: true,
  readOnly: true
}
const diffEditorRef = shallowRef()
const handleMount = diffEditor => (diffEditorRef.value = diffEditor)

const calcHistoryContent = (doc, history) => {
  const options = props.historyOptionsMethod(doc, history).filter(item => item.enabled !== false)
  return options.map(option => {
    const propValue = isFunction(option.prop) ? option.prop(doc, history) : doc[option.prop]
    return `[${option.labelKey ? $i18nBundle(option.labelKey) : option.label}]
${propValue ?? ''}`
  }).join('\n\n')
}

const fieldConfig = computed(() => {
  return {
    originalContent: calcHistoryContent(props.original, true),
    modifiedContent: calcHistoryContent(props.modified)
  }
})

const showCompareWindowNew = () => {
  showWindow.value = true
}
onBeforeUnmount(() => {
  diffEditorRef.value?.dispose()
})
defineExpose({
  showCompareWindowNew
})

</script>

<template>
  <common-window
    v-model="showWindow"
    :title="title||$t('mock.label.compare')"
    width="1000px"
    show-fullscreen
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
  >
    <el-container class="flex-column">
      <vue-monaco-diff-editor
        v-if="original && modified"
        theme="vs-dark"
        :original="fieldConfig.originalContent"
        :modified="fieldConfig.modifiedContent"
        language="markdown"
        :options="diffOptions"
        style="height:500px;"
        @mount="handleMount"
      >
        <div v-loading="true" />
      </vue-monaco-diff-editor>
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
