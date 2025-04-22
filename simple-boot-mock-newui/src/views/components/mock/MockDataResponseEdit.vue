<script setup>
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { computed, ref } from 'vue'
import { cloneDeep, isString } from 'lodash-es'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { $i18nKey, $i18nBundle } from '@/messages'
import { showCodeWindow } from '@/utils/DynamicUtils'
import MockGenerateSample from '@/views/components/mock/form/MockGenerateSample.vue'
import MockDataExample from '@/views/components/mock/form/MockDataExample.vue'
import { generateSampleCheckResults, generateSchemaSample, useContentTypeOption } from '@/services/mock/MockCommonService'
import { loadSchemas } from '@/api/mock/MockRequestApi'
import { calcContentType } from '@/consts/MockConstants'

const { contentRef, languageRef, editorRef, monacoEditorOptions, languageModel, languageSelectOption, formatDocument, checkEditorLang } = useMonacoEditorOptions({ readOnly: false })
const showWindow = ref(false)
const currentMockData = ref()
const schemas = ref([])
const componentsSpec = ref()
const isRedirect = ref(false)

const toEditDataResponse = (mockData) => {
  currentMockData.value = cloneDeep(mockData)
  contentRef.value = mockData.responseBody
  showWindow.value = true
  loadSchemas({
    requestId: mockData.requestId,
    dataId: mockData?.id
  }).then(schemasData => {
    schemas.value = schemasData.schemas
    componentsSpec.value = schemasData.componentSpec
  })
  const status = currentMockData.value?.statusCode || 200
  isRedirect.value = status >= 300 && status < 400 // redirect
  languageRef.value = mockData.responseFormat || (isRedirect.value ? 'text' : undefined)
  if (!languageRef.value) {
    checkEditorLang()
  }
}

const emit = defineEmits(['saveDataResponse'])

const saveDataResponse = () => {
  currentMockData.value.responseBody = contentRef.value
  showWindow.value = false
  emit('saveDataResponse', currentMockData.value)
}

const fullscreen = ref(false)

const codeHeight = computed(() => fullscreen.value ? 'calc(100dvh - 235px)' : '400px')

defineExpose({ toEditDataResponse })

const schema = computed(() => schemas.value?.[0])
const schemaBody = computed(() => schema.value?.responseBodySchema)
const responseExamples = computed(() => {
  const examples = schema.value?.responseExamples
  return examples ? JSON.parse(examples) : []
})
const generateSample = async (schema) => {
  contentRef.value = await generateSchemaSample(schema.schema, schema.type)
  setTimeout(() => checkEditorLang())
}
const selectExample = (example) => {
  contentRef.value = isString(example.value) ? example.value : JSON.stringify(example.value)
  setTimeout(() => checkEditorLang())
}

const contentTypeOption = useContentTypeOption()
const langOption = {
  ...languageSelectOption.value,
  change (val) {
    if (currentMockData.value) {
      currentMockData.value.responseFormat = val
      currentMockData.value.contentType = calcContentType(val, currentMockData.value?.responseBody)
    }
  }
}
const supportedGenerates = computed(() => generateSampleCheckResults(schemaBody.value, componentsSpec.value, schema.value?.responseMediaType))
</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    width="1000px"
    destroy-on-close
    show-fullscreen
    append-to-body
    :ok-click="saveDataResponse"
  >
    <template #header>
      {{ $i18nKey('common.label.commonEdit', isRedirect?'mock.label.redirectUrl':'mock.label.responseBody1') }}
      <el-link
        v-common-tooltip="$i18nBundle('mock.msg.responseBodyTooltip', ['{{ request.params.name }}'])"
        :underline="false"
      >
        <common-icon icon="QuestionFilled" />
      </el-link>
    </template>
    <el-container class="flex-column">
      <el-row v-if="!isRedirect">
        <el-col :span="12">
          <common-form-control
            :model="currentMockData"
            :option="contentTypeOption"
          />
        </el-col>
        <el-col :span="12">
          <common-form-control
            :model="currentMockData"
            :option="contentTypeOption.charsetOption"
          />
        </el-col>
      </el-row>
      <common-form-control
        :model="languageModel"
        :option="langOption"
      >
        <template #childAfter>
          <mock-url-copy-link
            :content="contentRef"
            :tooltip="$i18nKey('common.label.commonCopy', 'mock.label.responseBody1')"
          />
          <el-link
            v-common-tooltip="$i18nKey('common.label.commonFormat', 'mock.label.responseBody1')"
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
            v-if="responseExamples.length"
            :examples="responseExamples"
            @select-example="selectExample"
          />
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
  </common-window>
</template>

<style scoped>

</style>
