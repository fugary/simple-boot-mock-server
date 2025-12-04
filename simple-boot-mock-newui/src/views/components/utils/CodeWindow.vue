<script setup>
import { reactive, ref, computed, isRef, toRaw, watch } from 'vue'
import { processPasteCode, useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { $i18nBundle, $i18nKey } from '@/messages'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { showCodeWindow as dynamicShowCodeWindow } from '@/utils/DynamicUtils'

const showWindow = ref(false)
const { contentRef: codeText, languageRef, languageModel, languageSelectOption, normalLanguageSelectOption, formatDocument, editorRef, monacoEditorOptions } = useMonacoEditorOptions()

/**
 * @typedef {{copyAndClose?: boolean, showCopy?: boolean, width?: string, title?: string, height?: string, closeOnClickModal?: boolean, readOnly?: boolean}} CodeWindowConfig
 */
const codeConfig = reactive({
  title: $i18nBundle('common.label.info'),
  width: '1000px',
  height: '350px',
  fullEditor: false,
  closeOnClickModal: true,
  readOnly: true,
  showSelectButton: false,
  buttons: [],
  change: () => {}
})

let codeRef = null
/**
 * @param code {String} 代码数据
 * @param config {CodeWindowConfig} 配置信息
 */
const showCodeWindow = (code, config = {}) => {
  codeText.value = code
  if (isRef(code)) {
    codeRef = code
    codeText.value = code.value
  }
  Object.assign(codeConfig, config)
  showWindow.value = true
  monacoEditorOptions.readOnly = config.readOnly ?? monacoEditorOptions.readOnly
  if (config.language) {
    languageRef.value = config.language
  }
  setTimeout(() => {
    formatDocument()
  })
}

defineExpose({
  showCodeWindow
})

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 190px)' : codeConfig.height)

const langOption = computed(() => {
  if (codeConfig.language) {
    return {
      ...languageSelectOption.value,
      children: languageSelectOption.value.children
        .filter(item => item.value === codeConfig.language)
    }
  }
  return codeConfig.fullEditor ? languageSelectOption.value : normalLanguageSelectOption.value
})

const calcButtons = computed(() => {
  let buttons = codeConfig.buttons || []
  if (codeConfig.showSelectButton) {
    buttons = [{
      enabled: !!editorRef.value,
      label: $i18nKey('common.label.commonView', 'common.label.selection'),
      type: 'success',
      click () {
        const editor = toRaw(editorRef.value)
        const selectedStr = editor.getModel()?.getValueInRange(editor.getSelection())
        if (selectedStr) {
          dynamicShowCodeWindow(processPasteCode(selectedStr))
        }
      }
    }, ...buttons]
  }
  return buttons
})

watch(codeText, text => {
  codeConfig.change(text, languageRef.value)
})

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
    :buttons="calcButtons"
    :close-on-click-modal="codeConfig.closeOnClickModal"
  >
    <el-container class="flex-column">
      <common-form-control
        :model="languageModel"
        :option="langOption"
      >
        <template #childAfter>
          <mock-url-copy-link
            :content="codeText"
            :tooltip="$i18nKey('common.label.commonCopy', 'common.label.code')"
          />
          <el-link
            v-common-tooltip="$i18nKey('common.label.commonFormat', 'common.label.code')"
            type="primary"
            underline="never"
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
