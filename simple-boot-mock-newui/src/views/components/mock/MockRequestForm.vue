<script setup>
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed, watch } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: () => undefined
  },
  requestPath: {
    type: String,
    required: true
  }
})
const paramTarget = defineModel('modelValue', {
  type: Object,
  default: () => ({})
})

const checkParamsFilled = (params) => {
  return !params?.length || params.every(param => param.name && param.value)
}

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, languageSelectOption, formatDocument } = useMonacoEditorOptions({ readOnly: false })
const codeHeight = '300px'
contentRef.value = paramTarget.value?.requestBody
languageRef.value = paramTarget.value?.requestFormat || languageRef.value
const {
  contentRef: resContentRef, languageRef: resLanguageRef,
  editorRef: resEditorRef, monacoEditorOptions: resMonacoEditorOptions,
  languageModel: resLanguageModel, languageSelectOption: resLanguageSelectOption,
  formatDocument: resFormatDocument
} = useMonacoEditorOptions()
watch(() => props.responseTarget?.data, (data) => {
  resContentRef.value = data
  setTimeout(() => resFormatDocument())
})

const requestHeaderLength = computed(() => {
  return (paramTarget.value?.headerParams?.length || 0) + (props.responseTarget?.requestHeaders?.length || 0)
})

const requestUrl = computed(() => {
  let reqUrl = props.requestPath
  paramTarget.value?.pathParams?.forEach(pathParam => {
    reqUrl = reqUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathParam.value)
  })
  return reqUrl
})

const requestInfo = computed(() => {
  return props.responseTarget?.requestInfo
})

const emit = defineEmits(['sendRequest'])

const sendRequest = (form) => {
  form.validate(valid => {
    if (valid) {
      console.log('===============================发送请求', valid, paramTarget.value)
      paramTarget.value.requestFormat = languageRef.value
      emit('sendRequest', paramTarget.value)
    }
  })
}

const showRequestBody = computed(() => {
  return paramTarget.value?.method !== 'GET'
})

</script>

<template>
  <el-container class="flex-column">
    <common-form
      :show-buttons="false"
      :model="paramTarget"
    >
      <template #default="{form}">
        <el-row>
          <el-col :span="21">
            <el-descriptions
              :column="1"
              class="form-edit-width-100 margin-bottom3"
              border
            >
              <el-descriptions-item
                :label="paramTarget.method"
                min-width="40px"
              >
                <el-text
                  class="padding-right1"
                  truncated
                >
                  {{ requestUrl }}
                </el-text>
                <mock-url-copy-link
                  :url-path="requestUrl"
                />
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col
            :span="3"
            class="padding-top1 padding-left2"
          >
            <el-button
              type="primary"
              @click="sendRequest(form)"
            >
              发送请求
            </el-button>
          </el-col>
        </el-row>
        <el-tabs
          type="border-card"
          class="form-edit-width-100"
        >
          <el-tab-pane
            v-if="paramTarget.pathParams?.length"
          >
            <template #label>
              <el-badge
                :type="checkParamsFilled(paramTarget.pathParams) ? 'primary' : 'danger'"
                :value="paramTarget.pathParams?.length"
                :show-zero="false"
              >
                路径参数
              </el-badge>
            </template>
            <common-params-edit
              v-model="paramTarget.pathParams"
              :show-remove-button="false"
              name-read-only
              form-prop="pathParams"
              :show-add-button="false"
            />
          </el-tab-pane>
          <el-tab-pane>
            <template #label>
              <el-badge
                :type="checkParamsFilled(paramTarget.requestParams) ? 'primary' : 'danger'"
                :value="paramTarget.requestParams?.length"
                :show-zero="false"
              >
                请求参数
              </el-badge>
            </template>
            <common-params-edit
              v-model="paramTarget.requestParams"
              form-prop="requestParams"
            />
          </el-tab-pane>
          <el-tab-pane>
            <template #label>
              <el-badge
                :type="checkParamsFilled(paramTarget.headerParams) ? 'primary' : 'danger'"
                :value="requestHeaderLength"
                :show-zero="false"
              >
                请求头
              </el-badge>
            </template>
            <common-params-edit
              v-model="paramTarget.headerParams"
              form-prop="headerParams"
            />
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
          <el-tab-pane v-if="showRequestBody">
            <template #label>
              <el-badge
                type="primary"
                :hidden="!paramTarget.requestBody?.length"
                is-dot
              >
                请求体
              </el-badge>
            </template>
            <el-container class="flex-column">
              <common-form-control
                :model="languageModel"
                :option="languageSelectOption"
              >
                <template #childAfter>
                  <mock-url-copy-link
                    :content="contentRef"
                    tooltip="复制请求体内容"
                  />
                  <el-link
                    v-common-tooltip="'格式化请求体'"
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
                @change="paramTarget.requestBody=$event"
              />
            </el-container>
          </el-tab-pane>
        </el-tabs>
      </template>
    </common-form>
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
              :model="resLanguageModel"
              :option="resLanguageSelectOption"
              @change="resLanguageRef=$event"
            >
              <template #childAfter>
                <mock-url-copy-link
                  :content="resContentRef"
                  tooltip="复制响应体内容"
                />
                <el-link
                  v-common-tooltip="'格式化响应体'"
                  type="primary"
                  :underline="false"
                  class="margin-left3"
                  @click="resFormatDocument"
                >
                  <common-icon
                    :size="18"
                    icon="FormatIndentIncreaseFilled"
                  />
                </el-link>
              </template>
            </common-form-control>
            <vue-monaco-editor
              v-model:value="resContentRef"
              :language="resLanguageRef"
              :height="codeHeight"
              :options="resMonacoEditorOptions"
              @mount="resEditorRef=$event"
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
      </el-tabs>
    </el-container>
  </el-container>
</template>

<style scoped>

</style>
