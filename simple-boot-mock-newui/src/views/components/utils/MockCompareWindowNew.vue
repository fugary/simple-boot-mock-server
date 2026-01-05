<script setup lang="jsx">
import { computed, onBeforeUnmount, ref, shallowRef } from 'vue'
import { $i18nBundle } from '@/messages'
import { isFunction } from 'lodash-es'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

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
  },
  height: {
    type: [Number, String],
    default: '500px'
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

const fullscreen = ref(false)
const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 150px)' : props.height)
const theme = computed(() => useGlobalConfigStore().isDarkTheme ? 'vs-dark' : 'vs')

</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    :title="title||$t('mock.label.compare')"
    width="1000px"
    show-fullscreen
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
  >
    <el-container class="flex-column">
      <vue-monaco-diff-editor
        v-if="original && modified"
        :theme="theme"
        :original="fieldConfig.originalContent"
        :modified="fieldConfig.modifiedContent"
        language="markdown"
        :options="diffOptions"
        :height="codeHeight"
        @mount="handleMount"
      >
        <div v-loading="true" />
      </vue-monaco-diff-editor>
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
