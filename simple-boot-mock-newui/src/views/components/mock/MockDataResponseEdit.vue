<script setup>
import { $checkLang, useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed, ref } from 'vue'
import { cloneDeep } from 'lodash-es'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { $i18nKey, $i18nBundle } from '@/messages'

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, languageSelectOption, formatDocument } = useMonacoEditorOptions({ readOnly: false })
const showWindow = ref(false)
const currentMockData = ref()

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

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 195px)' : '400px')

defineExpose({ toEditDataResponse })

const responseBodyTooltip = $i18nBundle('mock.msg.responseBodyTooltip', ['{{request.params.xxx}}'])

</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    width="1000px"
    destroy-on-close
    show-fullscreen
    append-to-body
    :ok-click="saveDataResponse"
  >
    <template #header>
      {{ $i18nKey('common.label.commonEdit', 'mock.label.responseBody1') }}
      <el-link
        v-common-tooltip="responseBodyTooltip"
        :underline="false"
      >
        <common-icon icon="QuestionFilled" />
      </el-link>
    </template>
    <el-container class="flex-column">
      <common-form-control
        :model="languageModel"
        :option="languageSelectOption"
      >
        <template #childAfter>
          <mock-url-copy-link
            :content="contentRef"
            :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.responseBody1')"
          />
          <el-link
            v-common-tooltip="$i18nKey('common.label.commonFormat', 'mock.label.responseBody1')"
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
