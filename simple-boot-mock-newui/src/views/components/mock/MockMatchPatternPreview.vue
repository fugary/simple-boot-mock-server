<script setup>
import { ref } from 'vue'
import MockDataApi, {
  calcParamTarget,
  calcRequestBody,
  loadPreviewMeta,
  preProcessParams,
  previewRequest,
  processResponse
} from '@/api/mock/MockDataApi'
import MockRequestApi, { loadSchemas } from '@/api/mock/MockRequestApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'
import { $i18nBundle } from '@/messages'
import {
  MOCK_DATA_MATCH_PATTERN_HEADER,
  MOCK_DATA_PATH_PARAMS_HEADER,
  MOCK_DATA_USER_HEADER
} from '@/consts/MockConstants'
import { processEvnParams, extractQueryParams } from '@/services/mock/MockCommonService'
import { toGetParams } from '@/utils'
import { ElMessage } from 'element-plus'
import { isString } from 'lodash-es'

const showWindow = ref(false)
const groupItem = ref()
const currentItem = ref()
const paramTarget = ref()
const responseTarget = ref()
const schemasConf = ref({})
const editable = ref(true)
const expressionOptions = ref({})

let saveResolve
const toTestMatchPattern = (mockGroup, mockRequest, viewData, isEditable = true, options = {}) => {
  expressionOptions.value = options
  responseTarget.value = undefined
  groupItem.value = mockGroup
  currentItem.value = viewData || mockRequest // 当前预览的是request还是data
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, mockRequest, viewData, schemasConf.value)
  paramTarget.value.requestPath = '/mock/checkMatchPattern'
  editable.value = isEditable
  paramTarget.value.matchPattern = options.expression ?? currentItem.value?.matchPattern ?? ''
  if (mockRequest.id) {
    loadSchemas({
      requestId: mockRequest.id,
      dataId: viewData?.id
    }).then(schemasData => {
      schemasConf.value = schemasData
    })
  }
  return new Promise(resolve => (saveResolve = resolve))
}

const doDataPreview = () => {
  if (!paramTarget.value.matchPattern?.trim()) {
    responseTarget.value = undefined
    ElMessage.warning($i18nBundle('mock.msg.expressionRequired'))
    return
  }
  const params = extractQueryParams(paramTarget.value?.requestParams, paramTarget.value.groupConfig)
  const { data, hasBody } = calcRequestBody(paramTarget)
  const headers = Object.assign(hasBody ? { 'content-type': paramTarget.value?.contentType } : {},
    preProcessParams(paramTarget.value?.headerParams).reduce((results, item) => {
      results[item.name] = processEvnParams(paramTarget.value.groupConfig, item.value)
      return results
    }, {}))
  if (groupItem.value?.userName) {
    headers[MOCK_DATA_USER_HEADER] = groupItem.value.userName
  }
  const config = {
    loading: true,
    params,
    paramsSerializer: toGetParams,
    data,
    headers
  }
  if (hasBody && isString(data)) { // 字符串不让axios处理，防止调试请求和postman有差异
    config.transformRequest = req => req
  }
  if (paramTarget.value.pathParams?.length) {
    const pathParams = paramTarget.value.pathParams.map(param => {
      return {
        ...param,
        value: processEvnParams(paramTarget.value.groupConfig, param.value, true)
      }
    })
    params[MOCK_DATA_PATH_PARAMS_HEADER] = encodeURIComponent(JSON.stringify(pathParams))
  }
  if (paramTarget.value.matchPattern) {
    const expression = paramTarget.value.matchPattern
    params[MOCK_DATA_MATCH_PATTERN_HEADER] = encodeURIComponent(expressionOptions.value.booleanResult === false ? `(${expression}\n)` : expression)
  }
  previewRequest({
    url: paramTarget.value.requestPath,
    method: paramTarget.value.method
  }, config).then(calcResponse, calcResponse)
}

const calcResponse = async (response) => {
  responseTarget.value = processResponse(response)
  if (!response?.chunk) {
    const target = responseTarget.value
    await loadPreviewMeta(target)
    if (responseTarget.value?.diagnoseId === target.diagnoseId) {
      responseTarget.value = { ...target }
    }
  }
}

const saveMatchPattern = async () => {
  if (expressionOptions.value.onSave) {
    await expressionOptions.value.onSave(paramTarget.value?.matchPattern)
    showWindow.value = false
    saveResolve?.(paramTarget.value?.matchPattern)
    return
  }
  currentItem.value.matchPattern = paramTarget.value?.matchPattern
  if (currentItem.value.requestId) {
    MockDataApi.saveOrUpdate(currentItem.value, { loading: true, timeout: 60000 }).then(() => {
      showWindow.value = false
      saveResolve?.(currentItem.value)
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
    })
  } else {
    MockRequestApi.saveOrUpdate(currentItem.value).then(() => {
      showWindow.value = false
      saveResolve?.(currentItem.value)
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
    })
  }
  showWindow.value = false
}

defineExpose({
  toTestMatchPattern
})

</script>

<template>
  <common-window
    v-model="showWindow"
    width="1100px"
    :ok-label="expressionOptions.saveLabel || $t('mock.label.savePattern')"
    :cancel-label="$t('common.label.close')"
    show-fullscreen
    :ok-click="saveMatchPattern"
    :show-ok="editable"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <template #header>
      <span class="el-dialog__title">
        {{ $t('mock.msg.expressionTest') }}
      </span>
    </template>
    <el-container class="flex-column">
      <mock-request-form
        v-model="paramTarget"
        :request-path="paramTarget.requestPath"
        :response-target="responseTarget"
        match-pattern-mode
        :expression-options="expressionOptions"
        :schemas="schemasConf.schemas"
        :schema-spec="schemasConf.componentSpec"
        :editable="editable"
        @send-request="doDataPreview"
        @save-match-pattern="saveMatchPattern"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
