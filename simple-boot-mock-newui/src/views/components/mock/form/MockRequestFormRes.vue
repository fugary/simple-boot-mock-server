<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { computed, watch, ref } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: undefined
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
const currentTabName = ref(props.responseTarget ? 'responseData' : 'mockResponseBody')

const {
  contentRef, languageRef, editorRef, monacoEditorOptions,
  languageModel, normalLanguageSelectOption, formatDocument
} = useMonacoEditorOptions()

watch(() => props.responseTarget, (responseTarget) => {
  currentTabName.value = responseTarget ? 'responseData' : 'mockResponseBody'
  contentRef.value = responseTarget?.data
  setTimeout(() => formatDocument())
}, { immediate: true })

const requestInfo = computed(() => {
  return props.responseTarget?.requestInfo
})

const {
  contentRef: contentRef2, languageRef: languageRef2, editorRef: editorRef2, monacoEditorOptions: monacoEditorOptions2,
  languageModel: languageModel2, languageSelectOption: languageSelectOption2, formatDocument: formatDocument2
} = useMonacoEditorOptions({ readOnly: false, language: paramTarget.value.responseFormat })

watch(languageRef2, language => {
  paramTarget.value.responseFormat = language
})

contentRef2.value = paramTarget.value?.responseBody

const codeHeight = '300px'

const emit = defineEmits(['saveMockResponseBody'])

</script>

<template>
  <el-container
    class="flex-column padding-top2"
  >
    <el-tabs
      v-model="currentTabName"
      type="border-card"
      class="form-edit-width-100 margin-top3 common-tabs"
      addable
    >
      <template
        #add-icon
      >
        <template v-if="responseTarget">
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
      </template>
      <el-tab-pane
        v-if="responseTarget"
        name="responseData"
      >
        <template #label>
          <el-badge
            type="primary"
            :hidden="!responseTarget?.data?.length"
            is-dot
          >
            {{ $t('mock.label.responseBody') }}
          </el-badge>
        </template>
        <el-container class="flex-column">
          <common-form-control
            :model="languageModel"
            :option="normalLanguageSelectOption"
            @change="languageRef=$event"
          >
            <template #childAfter>
              <mock-url-copy-link
                :content="contentRef"
                :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.responseBody')"
              />
              <el-link
                v-common-tooltip="$i18nKey('common.label.commonFormat', 'mock.label.responseBody')"
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
                v-common-tooltip="$t('mock.msg.showRawData')"
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
      <el-tab-pane
        v-if="responseTarget"
        name="responseHeaders"
      >
        <template #label>
          <el-badge
            type="primary"
            :value="responseTarget.responseHeaders?.length"
            :show-zero="false"
          >
            {{ $t('mock.label.responseHeaders') }}
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
      <el-tab-pane
        v-if="mockResponseEditable"
        name="mockResponseBody"
      >
        <template #label>
          <el-badge
            type="primary"
            :hidden="!paramTarget.responseBody?.length"
            is-dot
          >
            {{ $t('mock.label.mockResponseBody') }}
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
                :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.mockResponseBody')"
              />
              <el-link
                v-common-tooltip="$i18nKey('common.label.commonFormat', 'mock.label.mockResponseBody')"
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
                v-common-tooltip="$t('mock.msg.saveMockResponse')"
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
