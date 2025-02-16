<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { computed, watch, ref } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { $i18nKey, $i18nBundle } from '@/messages'
import { generateSchemaSample, useContentTypeOption } from '@/services/mock/MockCommonService'
import MockGenerateSample from '@/views/components/mock/form/MockGenerateSample.vue'
import { isString } from 'lodash-es'
import { $coreError } from '@/utils'
import MockDataExample from '@/views/components/mock/form/MockDataExample.vue'
import { calcContentType } from '@/consts/MockConstants'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: undefined
  },
  mockResponseEditable: {
    type: Boolean,
    default: false
  },
  schemaType: {
    type: String,
    default: 'json'
  },
  schemaBody: {
    type: String,
    default: ''
  },
  examples: {
    type: Array,
    default: () => []
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

const responseImg = ref()

watch(() => props.responseTarget, (responseTarget) => {
  currentTabName.value = responseTarget ? 'responseData' : 'mockResponseBody'
  responseImg.value = null
  const contentType = responseTarget?.responseHeaders?.find(header => header.name?.toLowerCase() === 'content-type' && header.value?.includes('image'))
  if (responseTarget?.data && contentType) {
    if (isString(responseTarget.data)) {
      $coreError($i18nBundle('mock.msg.checkImageAccept'))
    } else {
      const base64Data = btoa(new Uint8Array(responseTarget.data).reduce((data, byte) => data + String.fromCharCode(byte), '')) // 将 ArrayBuffer 转换为 Base64
      responseImg.value = `data:${contentType};base64,${base64Data}`
    }
  } else {
    contentRef.value = responseTarget?.data
    const isRedirect = !!responseTarget?.responseHeaders?.find(header => header.name === 'mock-data-redirect')
    setTimeout(() => {
      isRedirect && (languageRef.value = 'text')
      formatDocument()
    })
  }
}, { immediate: true })

const requestInfo = computed(() => {
  return props.responseTarget?.requestInfo
})

const {
  contentRef: contentRef2, languageRef: languageRef2, editorRef: editorRef2, monacoEditorOptions: monacoEditorOptions2,
  languageModel: languageModel2, languageSelectOption: languageSelectOption2, formatDocument: formatDocument2, checkEditorLang
} = useMonacoEditorOptions({ readOnly: false, language: paramTarget.value.responseFormat })

watch(languageRef2, language => {
  paramTarget.value.responseFormat = language
})

contentRef2.value = paramTarget.value?.responseBody

const codeHeight = '300px'

const emit = defineEmits(['saveMockResponseBody'])

const generateSample = async (type) => {
  contentRef2.value = await generateSchemaSample(props.schemaBody, type)
  setTimeout(() => checkEditorLang())
}
const selectExample = (example) => {
  contentRef2.value = isString(example.value) ? example.value : JSON.stringify(example.value)
  setTimeout(() => checkEditorLang())
}

const contentTypeOption = useContentTypeOption('contentType')
const langOption = {
  ...languageSelectOption2.value,
  change (language) {
    if (paramTarget.value) {
      paramTarget.value.responseFormat = language
      paramTarget.value.contentType = calcContentType(language, paramTarget.value?.responseBody)
    }
  }
}
watch(contentRef2, val => {
  paramTarget.value.responseBody = val
})
const supportXml = computed(() => {
  const schemaBodyObj = isString(props.schemaBody) ? JSON.parse(props.schemaBody) : props.schemaBody
  return !!schemaBodyObj?.xml
})
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
          <template v-if="responseImg">
            <img
              :src="responseImg"
              alt="response image"
            >
          </template>
          <template v-else>
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
              class="common-resize-vertical"
              @mount="editorRef=$event"
            />
          </template>
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
            :model="paramTarget"
            :option="contentTypeOption"
          />
          <common-form-control
            :model="languageModel2"
            :option="langOption"
            @change="languageRef2=$event"
          >
            <template #childAfter>
              <mock-url-copy-link
                :content="contentRef2"
                :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.mockResponseBody')"
              />
              <new-window-edit-link
                v-model="contentRef2"
                class="margin-left3"
                full-editor
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
                @click="emit('saveMockResponseBody', paramTarget)"
              >
                <common-icon
                  :size="18"
                  icon="SaveOutlined"
                />
              </el-link>
              <el-link
                v-if="schemaBody"
                v-common-tooltip="$i18nKey('common.label.commonView', 'common.label.schema')"
                type="primary"
                :underline="false"
                class="margin-left3"
                @click="showCodeWindow(schemaBody)"
              >
                <common-icon
                  :size="18"
                  icon="ContentPasteSearchFilled"
                />
              </el-link>
              <mock-generate-sample
                v-if="schemaBody&&supportXml"
                @generate-sample="generateSample"
              />
              <el-link
                v-if="schemaBody&&!supportXml"
                v-common-tooltip="$t('common.label.generateJsonData')"
                type="primary"
                :underline="false"
                class="margin-left3"
                @click="generateSample('json')"
              >
                <common-icon
                  :size="18"
                  icon="DataObjectFilled"
                />
              </el-link>
              <mock-data-example
                v-if="examples.length"
                :examples="examples"
                @select-example="selectExample"
              />
            </template>
          </common-form-control>
          <vue-monaco-editor
            v-model:value="contentRef2"
            class="common-resize-vertical"
            :language="languageRef2"
            :height="codeHeight"
            :options="monacoEditorOptions2"
            @mount="editorRef2=$event"
          />
        </el-container>
      </el-tab-pane>
    </el-tabs>
  </el-container>
</template>

<style scoped>

</style>
