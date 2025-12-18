<script setup>
import { reactive, ref, computed, isRef, toRaw, watch } from 'vue'
import { processPasteCode, useMonacoEditorOptions, useMonacoDiffEditorOptions } from '@/vendors/monaco-editor'
import { $i18nBundle, $i18nKey } from '@/messages'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { showCodeWindow as dynamicShowCodeWindow } from '@/utils/DynamicUtils'
import { isObject } from 'lodash-es'
import { $copyText } from '@/utils'

const showWindow = ref(false)
const { contentRef: codeText, languageRef, languageModel, languageSelectOption, normalLanguageSelectOption, formatDocument, editorRef, monacoEditorOptions } = useMonacoEditorOptions()

const { gotoDiffPosition, diffOptions, diffChanged, handleMount, originalContent, modifiedContent } = useMonacoDiffEditorOptions()

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
  diffEditor: false,
  showCopy: true,
  copyAndClose: false,
  buttons: [],
  change: () => {}
})

let codeRef = null
/**
 * @param code {String} 代码数据
 * @param config {CodeWindowConfig} 配置信息
 */
const showCodeWindow = (code, config = {}) => {
  if (isObject(code) && !isRef(code)) {
    config = code
    code = config.content
  }
  codeText.value = code
  if (isRef(code)) {
    codeRef = code
    codeText.value = code.value
  }
  Object.assign(codeConfig, config)
  if (codeConfig.diffEditor) {
    originalContent.value = config.originalContent
    modifiedContent.value = config.modifiedContent
  }
  showWindow.value = true
  monacoEditorOptions.readOnly = config.readOnly ?? monacoEditorOptions.readOnly
  diffOptions.value.readOnly = config.readOnly ?? diffOptions.value.readOnly
  diffOptions.value.originalEditable = !diffOptions.value.readOnly
  fullscreen.value = codeConfig.fullscreen
  if (config.language) {
    languageRef.value = config.language
  }
  setTimeout(formatDocument)
}

defineExpose({
  showCodeWindow
})

const fullscreen = ref(false)

const codeHeight = computed(() => {
  const offset = codeConfig.diffEditor ? 150 : 190
  return fullscreen.value ? `calc(100dvh - ${offset}px)` : codeConfig.height
})

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
  return [{
    labelKey: 'common.label.copy',
    type: 'success',
    enabled: codeConfig.showCopy && !codeConfig.diffEditor,
    click () {
      $copyText(codeText.value)
      if (codeConfig.copyAndClose) {
        showWindow.value = false
      }
    }
  }, {
    label: $i18nKey('common.label.commonCopy', 'common.label.originalContent'),
    type: 'success',
    enabled: codeConfig.showCopy && codeConfig.diffEditor,
    click () {
      $copyText(originalContent.value)
      if (codeConfig.copyAndClose) {
        showWindow.value = false
      }
    }
  }, {
    label: $i18nKey('common.label.commonCopy', 'common.label.modifiedContent'),
    type: 'info',
    enabled: codeConfig.showCopy && codeConfig.diffEditor,
    click () {
      $copyText(modifiedContent.value)
      if (codeConfig.copyAndClose) {
        showWindow.value = false
      }
    }
  }, ...buttons]
})

watch(codeText, text => {
  if (!codeConfig.diffEditor) {
    codeConfig.change(text, languageRef.value)
  }
})
watch([originalContent, modifiedContent], ([original, modified]) => {
  if (codeConfig.diffEditor) {
    codeConfig.change(original, modified)
  }
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
    append-to-body
    show-fullscreen
    :buttons="calcButtons"
    :close-on-click-modal="codeConfig.closeOnClickModal"
  >
    <template #header>
      {{ codeConfig.title }}
      <template v-if="codeConfig.diffEditor">
        <el-link
          v-common-tooltip="'Previous'"
          underline="never"
          class="margin-left3"
          @click.stop="gotoDiffPosition(true)"
          @dblclick.stop
        >
          <common-icon
            :size="20"
            icon="ArrowUpwardFilled"
          />
        </el-link>
        <el-link
          v-common-tooltip="'Next'"
          underline="never"
          class="margin-left1"
          @click.stop="gotoDiffPosition();"
          @dblclick.stop
        >
          <common-icon
            :size="20"
            icon="ArrowDownwardFilled"
          />
        </el-link>
      </template>
    </template>
    <el-container class="flex-column">
      <common-form-control
        v-if="!codeConfig.diffEditor"
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
      <vue-monaco-diff-editor
        v-if="codeConfig.diffEditor"
        theme="vs-dark"
        :language="codeConfig.language"
        :original="originalContent"
        :modified="modifiedContent"
        :options="diffOptions"
        :height="codeHeight"
        @mount="handleMount"
        @change="diffChanged"
      >
        <div v-loading="true" />
      </vue-monaco-diff-editor>
      <vue-monaco-editor
        v-else
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
