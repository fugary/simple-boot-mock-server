<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { computed, watch, ref, reactive, nextTick, useTemplateRef, onBeforeUnmount } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { showCodeWindow, showJsonDataWindow, showMockTips } from '@/utils/DynamicUtils'
import { $i18nKey, $i18nBundle } from '@/messages'
import {
  generateSampleCheckResults,
  generateSchemaSample,
  isHtmlContentType,
  isJson,
  isMediaContentType,
  useContentTypeOption
} from '@/services/mock/MockCommonService'
import MockGenerateSample from '@/views/components/mock/form/MockGenerateSample.vue'
import { isString } from 'lodash-es'
import { $coreConfirm, $coreError } from '@/utils'
import MockDataExample from '@/views/components/mock/form/MockDataExample.vue'
import { calcContentLanguage, calcContentType, getMockConfirmConfig, isStreamContentType } from '@/consts/MockConstants'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'
import { downloadByLink } from '@/api/mock/MockGroupApi'

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
  schemaSpec: {
    type: Object,
    default: undefined
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

const currentElRef = useTemplateRef('currentElRef')
const mediaConfig = reactive({
  responseImg: null,
  responseAudio: null,
  responseVideo: null,
  responseHtml: null,
  responseStream: null
})
const previewHtml = ref(false)
const clearMediaItems = (remove) => {
  for (const key in mediaConfig) {
    remove && currentElRef.value?.$el?.querySelector(`.${key}El`)?.remove()
    mediaConfig[key] = null
  }
}

onBeforeUnmount(() => clearMediaItems(true))
const mediaUrl = computed(() => {
  for (const key in mediaConfig) {
    if (mediaConfig[key] && key !== 'responseHtml') {
      return mediaConfig[key]
    }
  }
  return null
})
const downloadResponse = (url) => {
  const urlSegments = props.responseTarget?.requestInfo?.url?.split('/') || []
  downloadByLink(url, urlSegments[urlSegments.length - 1])
}
watch(() => props.responseTarget, async (responseTarget) => {
  currentTabName.value = responseTarget ? 'responseData' : 'mockResponseBody'
  const oriContentType = responseTarget?.responseHeaders?.find(header => header.name?.toLowerCase() === 'content-type')?.value
  const contentType = isMediaContentType(oriContentType) || isStreamContentType(oriContentType) || isHtmlContentType(oriContentType) ? oriContentType : undefined
  if (responseTarget?.data && contentType) {
    clearMediaItems()
    if (isString(responseTarget.data)) {
      if (isHtmlContentType(contentType)) {
        previewHtml.value = true
        mediaConfig.responseHtml = `data:text/html;charset=utf-8,${encodeURIComponent(responseTarget.data)}`
        contentRef.value = responseTarget.data
      } else {
        $coreError($i18nBundle('mock.msg.checkImageAccept'))
      }
    } else {
      if (isMediaContentType(contentType)) {
        nextTick(() => {
          if (contentType?.includes('image')) {
            mediaConfig.responseImg = URL.createObjectURL(responseTarget.data)
          } else if (contentType?.includes('audio')) {
            mediaConfig.responseAudio = URL.createObjectURL(responseTarget.data)
          } else if (contentType?.includes('video')) {
            mediaConfig.responseVideo = URL.createObjectURL(responseTarget.data)
          }
        })
      } else {
        const streamUrl = mediaConfig.responseStream = URL.createObjectURL(responseTarget.data)
        $coreConfirm($i18nBundle('mock.msg.previewStreamConfirm'), getMockConfirmConfig()).then(() => {
          downloadResponse(streamUrl)
        }, async () => {
          contentRef.value = await responseTarget?.data?.text?.()
        })
      }
    }
  } else {
    let content = responseTarget?.data
    if (responseTarget?.data instanceof Blob) {
      content = await responseTarget?.data.text()
    }
    contentRef.value = content
    const isRedirect = !!responseTarget?.responseHeaders?.find(header => header.name === 'mock-data-redirect')
    setTimeout(() => {
      if (isRedirect) {
        languageRef.value = 'text'
      } else {
        languageRef.value = calcContentLanguage(oriContentType) || languageRef.value
      }
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

const emit = defineEmits(['saveMockResponseBody', 'sendMockRequest'])

const generateSample = async (schema) => {
  contentRef2.value = await generateSchemaSample(schema.schema, schema.type)
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
      paramTarget.value.contentType = calcContentType(language, paramTarget.value?.responseBody, paramTarget.value.contentType)
    }
  }
}
watch(contentRef2, val => {
  paramTarget.value.responseBody = val
})
const supportedGenerates = computed(() => generateSampleCheckResults(props.schemaBody, props.schemaSpec, props.schemaType))
const redirectMockResponse = computed(() => {
  const status = paramTarget.value?.responseStatusCode || 200
  return status >= 300 && status < 400
})
const jsonResponseData = computed(() => isJson(props.responseTarget?.data))
const toShowJsonDataWindow = () => {
  paramTarget.value.tableConfig = paramTarget.value.tableConfig || {}
  return showJsonDataWindow(props.responseTarget?.data, {
    tableConfig: paramTarget.value.tableConfig,
    'onUpdate:tableConfig': (value) => (paramTarget.value.tableConfig = value)
  })
}
</script>

<template>
  <el-container
    ref="currentElRef"
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
        <div
          v-if="currentTabName==='mockResponseBody'"
          style="display: flex; margin-top: -10px;"
        >
          <el-button
            type="primary"
            size="small"
            class="margin-left2"
            @click="$emit('sendMockRequest', paramTarget)"
          >
            {{ $t('mock.label.sendRequest') }}
          </el-button>
        </div>
        <div
          v-else-if="responseTarget"
          style="display: flex; margin-top: -7px;"
        >
          <el-link
            v-if="mediaConfig.responseHtml"
            v-common-tooltip="previewHtml?$i18nKey('common.label.commonView', 'common.label.code'):$t('common.label.preview')"
            type="primary"
            underline="never"
            class="margin-right3"
            @click="previewHtml=!previewHtml"
          >
            <common-icon
              :size="previewHtml?20:18"
              :icon="previewHtml?'CodeOutlined':'View'"
            />
          </el-link>
          <el-link
            v-if="mediaUrl"
            v-common-tooltip="$t('mock.label.downloadAsFile')"
            type="primary"
            underline="never"
            class="margin-right3"
            @click="downloadResponse(mediaUrl)"
          >
            <common-icon
              :size="18"
              icon="DownloadFilled"
            />
          </el-link>
          <el-text
            v-common-tooltip="responseTarget?.error?.message"
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
        </div>
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
          <img
            v-if="mediaConfig.responseImg"
            :src="mediaConfig.responseImg"
            alt="response image"
          >
          <audio
            v-else-if="mediaConfig.responseAudio"
            class="responseAudioEl"
            :src="mediaConfig.responseAudio"
            controls
          >
            Your browser does not support the audio element.
          </audio>
          <video
            v-else-if="mediaConfig.responseVideo"
            class="responseVideoEl"
            controls
            :src="mediaConfig.responseVideo"
          >
            Your browser does not support the video tag.
          </video>
          <iframe
            v-else-if="mediaConfig.responseHtml&&previewHtml"
            class="iframe-preview"
            :src="mediaConfig.responseHtml"
            height="500px"
          />
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
                  underline="never"
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
                  underline="never"
                  class="margin-left3"
                  @click="contentRef=responseTarget?.data"
                >
                  <common-icon
                    :size="40"
                    icon="RawOnFilled"
                  />
                </el-link>
                <el-link
                  v-if="jsonResponseData"
                  v-common-tooltip="$t('mock.label.viewAsTable')"
                  type="primary"
                  underline="never"
                  class="margin-left3"
                  @click="toShowJsonDataWindow()"
                >
                  <common-icon
                    :size="18"
                    icon="TableRowsFilled"
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
        v-if="responseTarget?.requestHeaders?.length"
        name="requestHeaders"
      >
        <template #label>
          <el-badge
            type="primary"
            :value="responseTarget.requestHeaders?.length"
            :show-zero="false"
          >
            {{ $t('mock.label.requestHeaders') }}
          </el-badge>
        </template>
        <el-descriptions
          v-if="responseTarget"
          :column="1"
          class="form-edit-width-100 margin-top3"
          border
        >
          <el-descriptions-item
            v-for="info in responseTarget.requestHeaders"
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
            {{ $t(redirectMockResponse?'mock.label.redirectUrl':'mock.label.mockResponseBody') }}
          </el-badge>
          <el-link
            v-common-tooltip="$t('mock.label.clickToShowDetails')"
            underline="never"
            type="primary"
            class="margin-left1"
            @click="showMockTips"
          >
            <common-icon icon="QuestionFilled" />
          </el-link>
        </template>
        <el-container class="flex-column">
          <el-row v-if="!redirectMockResponse">
            <el-col :span="12">
              <common-form-control
                :model="paramTarget"
                :option="contentTypeOption"
              />
            </el-col>
            <el-col
              :span="12"
              class="padding-left2"
            >
              <common-form-control
                :model="paramTarget"
                :option="contentTypeOption.charsetOption"
              />
            </el-col>
          </el-row>
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
                underline="never"
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
                underline="never"
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
                underline="never"
                class="margin-left3"
                @click="showCodeWindow(schemaBody)"
              >
                <common-icon
                  :size="18"
                  icon="ContentPasteSearchFilled"
                />
              </el-link>
              <template v-if="supportedGenerates?.length">
                <mock-generate-sample
                  v-if="supportedGenerates.length>1"
                  :schemas="supportedGenerates"
                  :title="$t('common.label.generateData')"
                  @generate-sample="generateSample($event)"
                />
                <el-link
                  v-if="supportedGenerates.length===1"
                  v-common-tooltip="$t('common.label.generateData')"
                  type="primary"
                  underline="never"
                  class="margin-left3"
                  @click="generateSample(supportedGenerates[0])"
                >
                  <common-icon
                    :size="18"
                    :icon="`custom-icon-${supportedGenerates[0]?.type}`"
                  />
                </el-link>
              </template>
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
