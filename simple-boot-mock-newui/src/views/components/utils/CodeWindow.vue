<script setup>
import { defineTableButtons } from '@/components/utils'
import { $copyText } from '@/utils'
import { reactive, ref, computed } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { $i18nBundle } from '@/messages'

const showWindow = ref(false)
const { contentRef: codeText, languageRef, editorRef, monacoEditorOptions } = useMonacoEditorOptions()

/**
 * @typedef {{copyAndClose?: boolean, showCopy?: boolean, width?: string, title?: string, height?: string}} CodeWindowConfig
 */
const codeConfig = reactive({
  title: $i18nBundle('common.label.info'),
  width: '600px',
  height: '300px',
  showCopy: true,
  copyAndClose: false
})

const buttons = computed(() => defineTableButtons([{
  labelKey: 'common.label.copy',
  type: 'success',
  enabled: codeConfig.showCopy,
  click () {
    $copyText(codeText.value)
    if (codeConfig.copyAndClose) {
      showWindow.value = false
    }
  }
}]))

/**
 * @param code {String} 代码数据
 * @param config {CodeWindowConfig} 配置信息
 */
const showCodeWindow = (code, config = {}) => {
  codeText.value = code
  Object.assign(codeConfig, config)
  showWindow.value = true
}

defineExpose({
  showCodeWindow
})

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 96px)' : codeConfig.height)

</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    :width="codeConfig.width"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="codeConfig.title"
    class="flex-column"
    append-to-body
    :buttons="buttons"
    show-fullscreen
  >
    <vue-monaco-editor
      v-if="codeText"
      v-model:value="codeText"
      :language="languageRef"
      :height="codeHeight"
      :options="monacoEditorOptions"
      @mount="editorRef=$event"
    />
  </common-window>
</template>

<style scoped>

</style>
