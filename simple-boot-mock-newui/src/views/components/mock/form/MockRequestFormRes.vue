<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { computed, watch } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const props = defineProps({
  responseTarget: {
    type: Object,
    required: true
  },
  mockResponseEditable: {
    type: Boolean,
    default: false
  }
})

const paramTarget = defineModel('modelValue', {
  type: Object,
  default: true
})

const {
  contentRef, languageRef, editorRef, monacoEditorOptions,
  languageModel, languageSelectOption, formatDocument
} = useMonacoEditorOptions()

watch(() => props.responseTarget?.data, (data) => {
  contentRef.value = data
  setTimeout(() => formatDocument())
}, { immediate: true })

const requestInfo = computed(() => {
  return props.responseTarget?.requestInfo
})

const {
  contentRef: contentRef2, languageRef: languageRef2, editorRef: editorRef2, monacoEditorOptions: monacoEditorOptions2,
  languageModel: languageModel2, languageSelectOption: languageSelectOption2, formatDocument: formatDocument2
} = useMonacoEditorOptions({ readOnly: false })

contentRef2.value = paramTarget.value?.responseBody

const codeHeight = '300px'

const emit = defineEmits(['saveMockResponseBody'])

</script>

<template>
  <el-container
    v-if="responseTarget"
    class="flex-column padding-top2"
  >
    <el-tabs
      type="border-card"
      class="form-edit-width-100 margin-top3 common-tabs"
      addable
    >
      <template #add-icon>
        <el-text
          :type="requestInfo.status===200?'success':'danger'"
          class="padding-right3"
        >
          Status: {{ requestInfo.status }}
        </el-text>
        <el-text
          type="success"
          class="padding-right3"
        >
          Method: {{ requestInfo.method }}
        </el-text>
        <el-text
          type="success"
          class="padding-right3"
        >
          Duration: {{ requestInfo.duration }}ms
        </el-text>
      </template>
      <el-tab-pane>
        <template #label>
          响应体
        </template>
        <el-container class="flex-column">
          <common-form-control
            :model="languageModel"
            :option="languageSelectOption"
            @change="languageRef=$event"
          >
            <template #childAfter>
              <mock-url-copy-link
                :content="contentRef"
                tooltip="复制响应体内容"
              />
              <el-link
                v-common-tooltip="'格式化响应体'"
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
              <el-link
                v-common-tooltip="'显示未格式化原始数据'"
                type="primary"
                :underline="false"
                class="margin-left3"
                @click="contentRef=responseTarget?.data"
              >
                <common-icon
                  :size="40"
                  icon="RawOnFilled"
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
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <el-badge
            type="primary"
            :value="responseTarget.responseHeaders?.length"
            :show-zero="false"
          >
            响应头
          </el-badge>
        </template>
        <el-descriptions
          :column="1"
          class="form-edit-width-100"
          border
        >
          <el-descriptions-item
            v-for="info in responseTarget.responseHeaders"
            :key="info.name"
            :label="info.name"
            min-width="150px"
          >
            {{ info.value }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane v-if="mockResponseEditable">
        <template #label>
          <el-badge
            type="primary"
            :hidden="!paramTarget.responseBody?.length"
            is-dot
          >
            Mock响应体
          </el-badge>
        </template>
        <el-container class="flex-column">
          <common-form-control
            :model="languageModel2"
            :option="languageSelectOption2"
            @change="languageRef2=$event"
          >
            <template #childAfter>
              <mock-url-copy-link
                :content="contentRef2"
                tooltip="复制响应体内容"
              />
              <el-link
                v-common-tooltip="'格式化响应体'"
                type="primary"
                :underline="false"
                class="margin-left3"
                @click="formatDocument2"
              >
                <common-icon
                  :size="18"
                  icon="FormatIndentIncreaseFilled"
                />
              </el-link>
              <el-link
                v-common-tooltip="'保存响应数据，保存后才能测试生效'"
                type="primary"
                :underline="false"
                class="margin-left3"
                @click="emit('saveMockResponseBody', paramTarget.responseBody)"
              >
                <common-icon
                  :size="18"
                  icon="SaveFilled"
                />
              </el-link>
            </template>
          </common-form-control>
          <vue-monaco-editor
            v-model:value="contentRef2"
            :language="languageRef2"
            :height="codeHeight"
            :options="monacoEditorOptions2"
            @mount="editorRef2=$event"
            @change="paramTarget.responseBody=$event"
          />
        </el-container>
      </el-tab-pane>
    </el-tabs>
  </el-container>
</template>

<style scoped>

</style>
