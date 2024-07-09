<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { computed, ref, watch } from 'vue'
import { checkParamsFilled } from '@/api/mock/MockRequestApi'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: undefined
  }
})

const paramTarget = defineModel('modelValue', {
  type: Object,
  default: true
})

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, languageSelectOption, formatDocument } = useMonacoEditorOptions({ readOnly: false })
const codeHeight = '300px'
contentRef.value = paramTarget.value?.requestBody
languageRef.value = paramTarget.value?.requestFormat || languageRef.value

const requestHeaderLength = computed(() => {
  return (paramTarget.value?.headerParams?.length || 0) + (props.responseTarget?.requestHeaders?.length || 0)
})

const showRequestBody = computed(() => {
  return paramTarget.value?.method !== 'GET'
})

watch(languageRef, lang => {
  paramTarget.value.requestFormat = lang
})

const currentTabName = ref('requestParamsTab')
const paramList = ['pathParams', 'requestParams', 'headerParams', 'requestBody']
if (paramTarget.value) {
  currentTabName.value = paramTarget.value.method !== 'GET' ? 'requestBodyTab' : 'requestParamsTab'
  for (const key of paramList) {
    if (paramTarget.value[key]?.length) {
      currentTabName.value = `${key}Tab`
      break
    }
  }
}
</script>

<template>
  <el-tabs
    v-model="currentTabName"
    type="border-card"
    class="form-edit-width-100"
  >
    <el-tab-pane
      v-if="paramTarget.pathParams?.length"
      name="pathParamsTab"
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
        :show-paste-button="false"
      />
    </el-tab-pane>
    <el-tab-pane name="requestParamsTab">
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
    <el-tab-pane name="headerParamsTab">
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
        header-flag
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
    <el-tab-pane
      v-if="showRequestBody"
      name="requestBodyTab"
    >
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

<style scoped>

</style>
