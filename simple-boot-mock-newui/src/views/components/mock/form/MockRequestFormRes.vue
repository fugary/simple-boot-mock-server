<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'
import { computed, watch, ref, reactive, nextTick, useTemplateRef, onBeforeUnmount } from 'vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { showDiagnoseWindow, showJsonDataWindow } from '@/utils/DynamicUtils'
import { $i18nKey, $i18nBundle } from '@/messages'
import {
  isHtmlContentType,
  isJson,
  isXml,
  isMediaContentType,
  statusCodeTagType
} from '@/services/mock/MockCommonService'
import { isString } from 'lodash-es'
import { $coreConfirm, $coreError } from '@/utils'
import { calcContentLanguage, getMockConfirmConfig, isStreamContentType } from '@/consts/MockConstants'
import { downloadByLink } from '@/api/mock/MockGroupApi'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: undefined
  },
  editable: {
    type: Boolean,
    default: false
  }
})

const paramTarget = defineModel('modelValue', {
  type: Object,
  default: true
})
const currentTabName = ref('responseData')

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
  currentTabName.value = 'responseData'
  const oriContentType = responseTarget?.responseHeaders?.find(header => header.name?.toLowerCase() === 'content-type')?.value
  const contentType = isMediaContentType(oriContentType) || isStreamContentType(oriContentType) || isHtmlContentType(oriContentType) ? oriContentType : undefined
  clearMediaItems()
  if (responseTarget?.data && contentType) {
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
    setTimeout(() => {
      languageRef.value = calcContentLanguage(oriContentType) || languageRef.value
      formatDocument()
    })
  }
}, { immediate: true })

const requestInfo = computed(() => {
  return props.responseTarget?.requestInfo
})

const mockHitInfo = computed(() => props.responseTarget?.mockHitInfo)
const mockHitTypeLabel = computed(() => {
  const labelKeyMap = {
    mock: 'mock.label.mockReturn',
    proxy: 'mock.label.proxyReturn',
    none: 'mock.label.noReturn'
  }
  return $i18nBundle(labelKeyMap[mockHitInfo.value?.returnType] || labelKeyMap.none)
})
const mockHitTagType = computed(() => {
  const tagTypeMap = {
    mock: 'success',
    proxy: 'warning',
    none: 'danger'
  }
  return tagTypeMap[mockHitInfo.value?.returnType] || tagTypeMap.none
})
const proxyResponseSavable = computed(() => {
  return props.editable && mockHitInfo.value?.returnType === 'proxy' &&
    props.responseTarget?.data !== undefined && !props.responseTarget?.savedAsMockData
})
const diagnoseInfo = computed(() => props.responseTarget?.diagnoseInfo)
const openDiagnoseDialog = () => showDiagnoseWindow(diagnoseInfo.value)
const requestMismatchInfo = computed(() => props.responseTarget?.requestMismatchInfo)

const codeHeight = '300px'
const emit = defineEmits(['saveMockParams', 'saveProxyResponseData'])

const jsonResponseData = computed(() => isJson(props.responseTarget?.data) || isXml(props.responseTarget?.data))
const toShowJsonDataWindow = () => {
  paramTarget.value.tableConfig = paramTarget.value.tableConfig || {}
  return showJsonDataWindow(props.responseTarget?.data, {
    editable: props.editable,
    tableConfig: paramTarget.value.tableConfig,
    'onUpdate:tableConfig': (value) => {
      paramTarget.value.tableConfig = value
      emit('saveMockParams', paramTarget.value)
    }
  })
}
</script>

<template>
  <el-container
    ref="currentElRef"
    class="flex-column padding-top2"
  >
    <el-alert
      v-if="requestMismatchInfo"
      type="warning"
      show-icon
      :closable="false"
      class="mock-request-mismatch-alert"
    >
      <template #title>
        <span class="mock-request-mismatch-alert__content">
          <span>{{ $t('mock.msg.realDebugRequestMismatch') }}</span>
          <el-tag
            type="primary"
            effect="light"
            class="mock-request-mismatch-alert__item"
          >
            {{ $t('mock.label.currentRequest') }}: {{ requestMismatchInfo.current }}
          </el-tag>
          <el-tag
            type="success"
            effect="light"
            class="mock-request-mismatch-alert__item"
          >
            {{ $t('mock.label.matchedRequest') }}: {{ requestMismatchInfo.matched }}
          </el-tag>
          <el-link
            v-if="diagnoseInfo"
            type="primary"
            @click="openDiagnoseDialog"
          >
            {{ $i18nKey('common.label.commonView', 'mock.label.diagnose') }}
          </el-link>
        </span>
      </template>
    </el-alert>
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
          v-if="responseTarget"
          style="display: flex; margin-top: -7px;"
        >
          <el-link
            v-if="mockHitInfo && diagnoseInfo"
            v-common-tooltip="$i18nKey('common.label.commonView', 'mock.label.diagnose')"
            :type="mockHitTagType"
            class="mock-hit-diagnose-link margin-right3"
            @click="openDiagnoseDialog"
          >
            <span>{{ mockHitTypeLabel }}</span>
          </el-link>
          <el-tag
            v-else-if="mockHitInfo"
            :type="mockHitTagType"
            class="margin-right3"
          >
            {{ mockHitTypeLabel }}
          </el-tag>
          <el-link
            v-if="proxyResponseSavable"
            v-common-tooltip="$i18nKey('common.label.commonSave', 'mock.label.mockData')"
            type="success"
            underline="never"
            class="margin-right3"
            @click="emit('saveProxyResponseData', responseTarget)"
          >
            <common-icon
              :size="18"
              icon="SaveOutlined"
            />
          </el-link>
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
            :type="statusCodeTagType(requestInfo.status)"
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
                <new-window-edit-link
                  v-model="contentRef"
                  class="margin-left3"
                  icon="View"
                  :tooltip="$t('common.label.view')"
                  full-editor
                  read-only
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
              :theme="useGlobalConfigStore().monacoTheme"
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
    </el-tabs>
  </el-container>
</template>

<style scoped>
.mock-request-mismatch-alert__content {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  line-height: 20px;
}

.mock-request-mismatch-alert__item {
  height: auto;
  min-height: 22px;
  padding: 3px 8px;
  white-space: normal;
}

.mock-hit-diagnose-link {
  align-items: center;
  gap: 4px;
  font-weight: 600;
  line-height: 24px;
}
</style>
