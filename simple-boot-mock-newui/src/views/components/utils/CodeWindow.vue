<script setup>
import { reactive, ref, computed, isRef } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { $i18nBundle, $i18nKey } from '@/messages'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

const showWindow = ref(false)
const { contentRef: codeText, languageRef, languageModel, languageSelectOption, formatDocument, editorRef, monacoEditorOptions } = useMonacoEditorOptions()

/**
 * @typedef {{copyAndClose?: boolean, showCopy?: boolean, width?: string, title?: string, height?: string, closeOnClickModal?: boolean, readOnly?: boolean}} CodeWindowConfig
 */
const codeConfig = reactive({
  title: $i18nBundle('common.label.info'),
  width: '800px',
  height: '350px',
  closeOnClickModal: true,
  readOnly: true
})

let codeRef = null
/**
 * @param code {String} 代码数据
 * @param config {CodeWindowConfig} 配置信息
 */
const showCodeWindow = (code, config = {}) => {
  codeText.value = code
  codeText.value = code
  if (isRef(code)) {
    codeRef = code
    codeText.value = code.value
  }
  Object.assign(codeConfig, config)
  showWindow.value = true
  monacoEditorOptions.readOnly = config.readOnly ?? monacoEditorOptions.readOnly
  setTimeout(() => {
    formatDocument()
  })
}

defineExpose({
  showCodeWindow
})

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 180px)' : codeConfig.height)

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
    append-to-body
    show-fullscreen
    :close-on-click-modal="codeConfig.closeOnClickModal"
  >
    <el-container class="flex-column">
      <common-form-control
        :model="languageModel"
        :option="languageSelectOption"
      >
        <template #childAfter>
          <mock-url-copy-link
            :content="codeText"
            :tooltip="$i18nKey('common.label.commonCopy', 'common.label.code')"
          />
          <el-link
            v-common-tooltip="$i18nKey('common.label.commonFormat', 'common.label.code')"
            type="primary"
            :underline="false"
            class="margin-left3"
            @click="formatDocument"
          >
            <common-icon
              :size="18"
              icon="FormatIndentIncreaseFilled"
            />
          </el-link>
        </template>
      </common-form-control>
      <vue-monaco-editor
        v-model:value="codeText"
        :language="languageRef"
        :height="codeHeight"
        :options="monacoEditorOptions"
        @mount="editorRef=$event"
        @change="codeRef=$event"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
