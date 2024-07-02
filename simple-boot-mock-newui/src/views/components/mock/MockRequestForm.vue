<script setup>
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { watch } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: () => undefined
  }
})
const paramTarget = defineModel('modelValue', {
  type: Object,
  default: () => ({})
})

const checkParamsFilled = (params) => {
  return !params?.length || params.every(param => param.name && param.value)
}

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, languageSelectOption } = useMonacoEditorOptions({ readOnly: false })
const codeHeight = '350px'
const {
  contentRef: resContentRef, languageRef: resLanguageRef,
  editorRef: resEditorRef, monacoEditorOptions: resMonacoEditorOptions,
  languageModel: resLanguageModel, languageSelectOption: resLanguageSelectOption,
  formatDocument
} = useMonacoEditorOptions()
watch(() => props.responseTarget?.data, (data) => {
  resContentRef.value = data
  setTimeout(() => formatDocument())
})

</script>

<template>
  <el-container class="flex-column">
    <common-form
      :show-buttons="false"
      :model="paramTarget"
    >
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
              :value="paramTarget.headerParams?.length"
              :show-zero="false"
            >
              请求头
            </el-badge>
          </template>
          <common-params-edit
            v-model="paramTarget.headerParams"
            form-prop="headerParams"
          />
        </el-tab-pane>
        <el-tab-pane>
          <template #label>
            <el-badge
              :type="paramTarget.requestBody ? 'primary' : 'danger'"
              :value="paramTarget.requestBody?.length"
              :show-zero="false"
            >
              请求体
            </el-badge>
          </template>
          <el-container class="flex-column">
            <common-form-control
              :model="languageModel"
              :option="languageSelectOption"
              @change="languageRef=$event"
            />
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
        <el-tab-pane
          v-if="responseTarget"
          label="实际请求头"
        >
          <template #label>
            <el-badge
              type="info"
              :value="responseTarget.requestHeaders?.length"
              :show-zero="false"
            >
              实际请求头
            </el-badge>
          </template>
          <el-descriptions
            :column="1"
            class="form-edit-width-100"
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
          v-if="responseTarget"
        >
          <template #label>
            <el-badge
              type="info"
              :value="responseTarget.responseHeaders?.length"
              :show-zero="false"
            >
              实际响应头
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
    </common-form>
    <el-container
      v-if="responseTarget"
      class="flex-column margin-top2 padding-top2"
    >
      <el-descriptions
        :column="1"
        class="form-edit-width-100"
        border
      >
        <el-descriptions-item
          v-for="info in responseTarget.requestInfo"
          :key="info.name"
          :label="info.name"
          min-width="100px"
        >
          <el-tag
            v-if="info.name==='Code'"
            effect="dark"
            :type="info.value===200?'success':'danger'"
          >
            {{ info.value }}
          </el-tag>
          <span v-else>
            {{ info.value }}
          </span>&nbsp;
          <mock-url-copy-link
            v-if="info.name==='URL'"
            :url-path="info.value"
          />
        </el-descriptions-item>
      </el-descriptions>
      <el-container class="padding-top3 flex-column">
        <common-form-control
          :model="resLanguageModel"
          :option="resLanguageSelectOption"
          @change="resLanguageRef=$event"
        />
        <vue-monaco-editor
          v-model:value="resContentRef"
          :language="resLanguageRef"
          :height="codeHeight"
          :options="resMonacoEditorOptions"
          @mount="resEditorRef=$event"
        />
      </el-container>
    </el-container>
  </el-container>
</template>

<style scoped>

</style>
