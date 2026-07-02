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
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
import { $copyText, getSingleSelectOptions } from '@/utils'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { isString } from 'lodash-es'
import {
  calcEnvSuggestions,
  calcHeaderSuggestions,
  generateSampleCheckResults,
  generateSchemaSample,
  calcProxyUrl,
  showSchemaCodeWindow
} from '@/services/mock/MockCommonService'
import MockGenerateSample from '@/views/components/mock/form/MockGenerateSample.vue'
import MockDataExample from '@/views/components/mock/form/MockDataExample.vue'
import NewWindowEditLink from '@/views/components/utils/NewWindowEditLink.vue'
import { buildCurlCommand, CURL_SHELL, extendCurlParams, isGetMethod } from '@/services/mock/CurlProcessService'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

const props = defineProps({
  requestPath: {
    type: String,
    default: ''
  },
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

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, normalLanguageSelectOption, formatDocument, checkEditorLang } = useMonacoEditorOptions({
  readOnly: false,
  language: paramTarget.value.requestFormat,
  excludeCheckLangs: ['xmlWithJs']
})
const codeHeight = '300px'

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
  return (paramTarget.value?.headerParams?.length || 0)
})

const showRequestBody = computed(() => {
  return !isGetMethod(paramTarget.value?.method)
})

watch(languageRef, lang => {
  paramTarget.value.requestFormat = lang
  paramTarget.value.requestContentType = calcContentType(lang, paramTarget.value.requestBody) || NONE
}, { immediate: true })

watch(contentRef, val => {
  paramTarget.value.requestBody = val
})

const toSuggestionArray = suggestions => Array.isArray(suggestions) ? suggestions : []
const suggestionValue = suggestion => suggestion && typeof suggestion === 'object'
  ? suggestion.value ?? suggestion.description
  : suggestion
const syncAutoValueSuggestions = params => {
  toSuggestionArray(params).forEach(param => {
    const newAutoSuggestions = calcHeaderSuggestions(param.name)
    if (newAutoSuggestions.length) {
      const oldSuggestions = toSuggestionArray(param.valueSuggestions)
      const oldValues = new Set(oldSuggestions.map(suggestionValue))
      const newSuggestions = [
        ...oldSuggestions,
        ...newAutoSuggestions.filter(suggestion => !oldValues.has(suggestionValue(suggestion)))
      ]
      if (newSuggestions.length !== oldSuggestions.length) {
        param.valueSuggestions = newSuggestions
      }
    }
  })
}

watch(() => [paramTarget.value.headerParams, paramTarget.value.requestParams], (allParams = []) => {
  allParams.forEach(syncAutoValueSuggestions)
}, { deep: true })

const currentTabName = ref('requestParamsTab')
const authContentModel = ref({})
const paramList = ['requestBody', 'pathParams', 'requestParams', 'headerParams']
const initParamTarget = () => {
  contentRef.value = paramTarget.value?.requestBody
  languageRef.value = paramTarget.value?.requestFormat || languageRef.value
  authContentModel.value = {
    authType: AUTH_TYPE.NONE
  }
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
initParamTarget()
const authValid = ref(true)

const generateSample = async (schema) => {
  contentRef.value = await generateSchemaSample(schema.schema, schema.type, props.schemaSpec)
  setTimeout(() => checkEditorLang())
}
const selectExample = (example) => {
  contentRef.value = isString(example.value) ? example.value : JSON.stringify(example.value)
  setTimeout(() => checkEditorLang())
}

const envSuggestions = computed(() => calcEnvSuggestions(paramTarget.value?.groupConfig))

const supportedGenerates = computed(() => generateSampleCheckResults(props.schemaBody, props.schemaSpec, props.schemaType))

const emit = defineEmits(['resetRequestForm'])
const resetRequestForm = () => {
  emit('resetRequestForm')
  setTimeout(initParamTarget)
}
const curlContent = ref('')
const CURL_COMMAND = {
  PASTE: 'paste',
  COPY_BASH: 'copyBash',
  COPY_CMD: 'copyCmd',
  COPY_PROXY_BASH: 'copyProxyBash',
  COPY_PROXY_CMD: 'copyProxyCmd'
}

const currentProxyUrl = computed(() => {
  return calcProxyUrl(paramTarget.value?.proxyUrl)
})

const processCurlWindow = () => {
  showCodeWindow(curlContent, {
    title: $i18nConcat($i18nBundle('common.label.paste'), 'CURL'),
    showCancel: true,
    readOnly: false,
    language: 'shell',
    okLabel: $i18nBundle('common.label.confirm'),
    ok (str) {
      extendCurlParams(paramTarget, str)
      initParamTarget()
    }
  })
}

const copyAsCurl = async (shell = CURL_SHELL.BASH, useProxyUrl = false) => {
  const proxyUrl = useProxyUrl ? currentProxyUrl.value : null
  const curlCommand = await buildCurlCommand(paramTarget.value, props.requestPath, shell, proxyUrl)
  $copyText(curlCommand)
}

const handleCurlCommand = (command) => {
  const commandMap = {
    [CURL_COMMAND.PASTE]: () => processCurlWindow(),
    [CURL_COMMAND.COPY_BASH]: () => copyAsCurl(CURL_SHELL.BASH, false),
    [CURL_COMMAND.COPY_CMD]: () => copyAsCurl(CURL_SHELL.CMD, false),
    [CURL_COMMAND.COPY_PROXY_BASH]: () => copyAsCurl(CURL_SHELL.BASH, true),
    [CURL_COMMAND.COPY_PROXY_CMD]: () => copyAsCurl(CURL_SHELL.CMD, true)
  }
  commandMap[command]?.()
}

</script>

<template>
  <el-tabs
    v-model="currentTabName"
    type="border-card"
    class="form-edit-width-100 common-tabs"
    addable
  >
    <template
      #add-icon
    >
      <el-dropdown
        class="margin-right2"
        @command="handleCurlCommand"
      >
        <el-link
          type="primary"
          style="margin-top: -11px"
        >
          CURL
        </el-link>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :command="CURL_COMMAND.PASTE">
              {{ $i18nConcat($i18nBundle('common.label.paste'), 'CURL') }}
            </el-dropdown-item>
            <el-dropdown-item :command="CURL_COMMAND.COPY_BASH">
              {{ $i18nConcat($i18nBundle('common.label.copy'), 'cURL (bash)') }}
            </el-dropdown-item>
            <el-dropdown-item :command="CURL_COMMAND.COPY_CMD">
              {{ $i18nConcat($i18nBundle('common.label.copy'), 'cURL (cmd)') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="currentProxyUrl"
              :command="CURL_COMMAND.COPY_PROXY_BASH"
            >
              {{ $i18nConcat($i18nBundle('common.label.copy'), 'cURL Proxy (bash)') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="currentProxyUrl"
              :command="CURL_COMMAND.COPY_PROXY_CMD"
            >
              {{ $i18nConcat($i18nBundle('common.label.copy'), 'cURL Proxy (cmd)') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-link
        type="primary"
        style="margin-top: -11px"
        @click="resetRequestForm"
      >
        {{ $t('common.label.reset') }}
      </el-link>
    </template>
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
              v-if="schemaBody"
              v-common-tooltip="$i18nKey('common.label.commonView', 'common.label.schema')"
              type="primary"
              underline="never"
              class="margin-left3"
              @click="showSchemaCodeWindow(schemaBody, schemaSpec)"
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
          :theme="useGlobalConfigStore().monacoTheme"
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
