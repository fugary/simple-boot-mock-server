<script setup>
import { $checkLang, useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed, ref } from 'vue'
import { cloneDeep } from 'lodash-es'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

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

const responseBodyTooltip = `响应内容支持根据请求参数替换，使用{{request.parameters.xxx}}格式替换数据: <br>
        request.body——body内容对象（仅json）<br>
        request.bodyStr——body内容字符串<br>
        request.headers——头信息对象<br>
        request.parameters——请求参数对象<br>
        request.pathParameters——路径参数对象`

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
      编辑数据响应内容
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
            tooltip="复制数据响应内容"
          />
          <el-link
            v-common-tooltip="'格式化数据响应内容'"
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
