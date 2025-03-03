<script setup>
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { computed, ref, watch } from 'vue'
import { checkParamsFilled } from '@/api/mock/MockRequestApi'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import {
  AUTH_TYPE,
  calcContentType,
  NONE,
  FORM_DATA,
  FORM_URL_ENCODED,
  SPECIAL_LANGS,
  DEFAULT_HEADERS
} from '@/consts/MockConstants'
import MockRequestFormAuthorization from '@/views/components/mock/form/MockRequestFormAuthorization.vue'
import { $i18nKey } from '@/messages'
import { getSingleSelectOptions } from '@/utils'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { isString } from 'lodash-es'
import { calcEnvSuggestions, generateSampleCheckResults, generateSchemaSample } from '@/services/mock/MockCommonService'
import MockGenerateSample from '@/views/components/mock/form/MockGenerateSample.vue'
import MockDataExample from '@/views/components/mock/form/MockDataExample.vue'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'

const props = defineProps({
  showAuthorization: {
    type: Boolean,
    default: false
  },
  responseTarget: {
    type: Object,
    default: undefined
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
  default: () => ({})
})

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, normalLanguageSelectOption, formatDocument, checkEditorLang } = useMonacoEditorOptions({ readOnly: false })
const codeHeight = '300px'
contentRef.value = paramTarget.value?.requestBody
languageRef.value = paramTarget.value?.requestFormat || languageRef.value

const customLanguageSelectOption = computed(() => {
  return {
    ...normalLanguageSelectOption.value,
    children: [...getSingleSelectOptions(NONE), ...normalLanguageSelectOption.value.children, {
      value: FORM_DATA,
      label: 'form-data'
    }, {
      value: FORM_URL_ENCODED,
      label: 'form-urlencoded'
    }]
  }
})

const isSpecialLang = computed(() => SPECIAL_LANGS.includes(languageRef.value))
const isNone = computed(() => NONE === languageRef.value)
const isFormData = computed(() => FORM_DATA === languageRef.value)
const isFormUrlEncoded = computed(() => FORM_URL_ENCODED === languageRef.value)

const requestHeaderLength = computed(() => {
  return (paramTarget.value?.headerParams?.length || 0) + (props.responseTarget?.requestHeaders?.length || 0)
})

const showRequestBody = computed(() => {
  return paramTarget.value?.method !== 'GET'
})

watch(languageRef, lang => {
  paramTarget.value.requestFormat = lang
  paramTarget.value.requestContentType = calcContentType(lang, paramTarget.value.requestBody) || NONE
}, { immediate: true })

watch(contentRef, val => {
  paramTarget.value.requestBody = val
})

const currentTabName = ref('requestParamsTab')
const authContentModel = ref({
  authType: AUTH_TYPE.NONE
})
const paramList = ['requestBody', 'pathParams', 'requestParams', 'headerParams']
if (paramTarget.value) {
  currentTabName.value = paramTarget.value.method !== 'GET' ? 'requestBodyTab' : 'requestParamsTab'
  for (const key of paramList) {
    if (paramTarget.value[key]?.length) {
      currentTabName.value = `${key}Tab`
      break
    }
  }
  if (paramTarget.value.authContent) {
    authContentModel.value = paramTarget.value.authContent
  } else {
    paramTarget.value.authContent = authContentModel.value
  }
}
const authValid = ref(true)

const generateSample = async (schema) => {
  contentRef.value = await generateSchemaSample(schema.schema, schema.type)
  setTimeout(() => checkEditorLang())
}
const selectExample = (example) => {
  contentRef.value = isString(example.value) ? example.value : JSON.stringify(example.value)
  setTimeout(() => checkEditorLang())
}

const envSuggestions = computed(() => calcEnvSuggestions(paramTarget.value?.groupConfig))

const supportedGenerates = computed(() => generateSampleCheckResults(props.schemaBody, props.schemaSpec, props.schemaType))
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
          {{ $t('mock.label.pathParams') }}
        </el-badge>
      </template>
      <common-params-edit
        v-model="paramTarget.pathParams"
        :show-remove-button="false"
        name-read-only
        form-prop="pathParams"
        :show-add-button="false"
        :show-paste-button="false"
        :value-suggestions="envSuggestions"
      />
    </el-tab-pane>
    <el-tab-pane name="requestParamsTab">
      <template #label>
        <el-badge
          :type="checkParamsFilled(paramTarget.requestParams) ? 'primary' : 'danger'"
          :value="paramTarget.requestParams?.length"
          :show-zero="false"
        >
          {{ $t('mock.label.queryParams') }}
        </el-badge>
      </template>
      <common-params-edit
        v-model="paramTarget.requestParams"
        form-prop="requestParams"
        :value-suggestions="envSuggestions"
      />
    </el-tab-pane>
    <el-tab-pane name="headerParamsTab">
      <template #label>
        <el-badge
          :type="checkParamsFilled(paramTarget.headerParams) ? 'primary' : 'danger'"
          :value="requestHeaderLength"
          :show-zero="false"
        >
          {{ $t('mock.label.requestHeaders') }}
        </el-badge>
      </template>
      <common-params-edit
        v-model="paramTarget.headerParams"
        form-prop="headerParams"
        :name-suggestions="DEFAULT_HEADERS"
        :value-suggestions="envSuggestions"
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
          :hidden="isNone"
          is-dot
        >
          {{ $t('mock.label.requestBody') }}
        </el-badge>
      </template>
      <el-container class="flex-column">
        <common-form-control
          :model="languageModel"
          :option="customLanguageSelectOption"
        >
          <template #childAfter>
            <mock-url-copy-link
              :content="contentRef"
              :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.requestBody')"
            />
            <new-window-edit-link
              v-if="!isSpecialLang"
              v-model="contentRef"
              class="margin-left3"
            />
            <el-link
              v-common-tooltip="$i18nKey('common.label.commonFormat', 'mock.label.requestBody')"
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
                :underline="false"
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
        <template v-if="isFormData || isFormUrlEncoded">
          <common-params-edit
            v-model="paramTarget[languageRef]"
            :form-prop="`${languageRef}`"
            :file-flag="isFormData"
            :value-suggestions="!isFormData?envSuggestions:null"
          />
        </template>
        <vue-monaco-editor
          v-if="!isSpecialLang"
          v-model:value="contentRef"
          :language="languageRef"
          :height="codeHeight"
          :options="monacoEditorOptions"
          class="common-resize-vertical"
          @mount="editorRef=$event"
        />
      </el-container>
    </el-tab-pane>
    <el-tab-pane
      v-if="showAuthorization"
      name="authorizationTab"
    >
      <template #label>
        <el-badge
          :type="authValid ? 'primary' : 'danger'"
          :hidden="authContentModel.authType === AUTH_TYPE.NONE"
          is-dot
        >
          {{ $t('mock.label.authorization') }}
        </el-badge>
      </template>
      <mock-request-form-authorization
        v-model="authContentModel"
        v-model:auth-valid="authValid"
        :group-config="paramTarget.groupConfig"
      />
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>

</style>
