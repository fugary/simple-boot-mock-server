<script setup>
import { ref } from 'vue'
import MockDataApi, {
  calcParamTarget,
  calcRequestBody,
  preProcessParams,
  previewRequest,
  processResponse
} from '@/api/mock/MockDataApi'
import MockRequestApi, { loadSchemas } from '@/api/mock/MockRequestApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'
import { $i18nKey } from '@/messages'
import { MOCK_DATA_MATCH_PATTERN_HEADER, MOCK_DATA_PATH_PARAMS_HEADER } from '@/consts/MockConstants'
import { processEvnParams } from '@/services/mock/MockCommonService'

const showWindow = ref(false)
const groupItem = ref()
const currentItem = ref()
const paramTarget = ref()
const responseTarget = ref()
const schemas = ref([])
const componentsSpec = ref()

let saveResolve
const toTestMatchPattern = (mockGroup, mockRequest, viewData) => {
  groupItem.value = mockGroup
  currentItem.value = viewData || mockRequest // 当前预览的是request还是data
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, mockRequest, viewData)
  paramTarget.value.requestPath = '/mock/checkMatchPattern'
  const matchPattern = viewData?.matchPattern || mockRequest?.matchPattern
  if (matchPattern) {
    paramTarget.value.matchPattern = matchPattern
  }
  loadSchemas({
    requestId: mockRequest.id,
    dataId: viewData?.id
  }).then(schemasData => {
    schemas.value = schemasData.schemas
    componentsSpec.value = schemasData.componentSpec
  })
  return new Promise(resolve => (saveResolve = resolve))
}

const doDataPreview = () => {
  const params = preProcessParams(paramTarget.value?.requestParams).reduce((results, item) => {
    results[item.name] = processEvnParams(paramTarget.value.groupConfig, item.value)
    return results
  }, {})
  const { data, hasBody } = calcRequestBody(paramTarget)
  const headers = Object.assign(hasBody ? { 'content-type': paramTarget.value?.contentType } : {},
    preProcessParams(paramTarget.value?.headerParams).reduce((results, item) => {
      results[item.name] = processEvnParams(paramTarget.value.groupConfig, item.value)
      return results
    }, {}))
  const config = {
    loading: true,
    params,
    data,
    headers
  }
  if (paramTarget.value.pathParams?.length) {
    const pathParams = paramTarget.value.pathParams.map(param => {
      return {
        ...param,
        value: processEvnParams(paramTarget.value.groupConfig, param.value)
      }
    })
    params[MOCK_DATA_PATH_PARAMS_HEADER] = encodeURIComponent(JSON.stringify(pathParams))
  }
  if (paramTarget.value.matchPattern) {
    params[MOCK_DATA_MATCH_PATTERN_HEADER] = encodeURIComponent(paramTarget.value.matchPattern)
  }
  previewRequest({
    url: paramTarget.value.requestPath,
    method: paramTarget.value.method
  }, config).then(calcResponse, calcResponse)
}

const calcResponse = (response) => {
  responseTarget.value = processResponse(response)
}

const saveMatchPattern = () => {
  currentItem.value.matchPattern = paramTarget.value?.matchPattern
  if (currentItem.value.requestId) {
    MockDataApi.saveOrUpdate(currentItem.value).then(() => {
      showWindow.value = false
      saveResolve?.(currentItem.value)
    })
  } else {
    MockRequestApi.saveOrUpdate(currentItem.value).then(() => {
      showWindow.value = false
      saveResolve?.(currentItem.value)
    })
  }
}

defineExpose({
  toTestMatchPattern
})

</script>

<template>
  <common-window
    v-model="showWindow"
    width="1100px"
    :ok-label="$i18nKey('common.label.commonSave', 'mock.label.matchPattern')"
    show-fullscreen
    :ok-click="saveMatchPattern"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <template #header>
      <span class="el-dialog__title">
        {{ $t('mock.msg.matchPatternTest') }}
      </span>
    </template>
    <el-container class="flex-column">
      <mock-request-form
        v-model="paramTarget"
        :request-path="paramTarget.requestPath"
        :response-target="responseTarget"
        match-pattern-mode
        :schemas="schemas"
        :schema-spec="componentsSpec"
        @send-request="doDataPreview"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
