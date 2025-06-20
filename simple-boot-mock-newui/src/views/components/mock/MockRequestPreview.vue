<script setup>
import { computed, nextTick, ref } from 'vue'
import MockRequestApi, { loadSchemas, saveMockParams } from '@/api/mock/MockRequestApi'
import MockDataApi, {
  calcParamTarget,
  calcRequestBody,
  preProcessParams,
  previewRequest,
  processResponse
} from '@/api/mock/MockDataApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'
import { AUTH_OPTION_CONFIG } from '@/services/mock/MockAuthorizationService'
import { MOCK_DATA_ID_HEADER, MOCK_REQUEST_ID_HEADER } from '@/consts/MockConstants'
import { cloneDeep, isArray } from 'lodash-es'
import { addRequestParamsToResult, calcPreviewHeaders, processEvnParams } from '@/services/mock/MockCommonService'
import { toGetParams } from '@/utils'

const groupItem = ref()
const requestItem = ref()
const previewData = ref()
const paramTarget = ref()
const responseTarget = ref()
const schemasConf = ref({})

let saveCallback
const toPreviewRequest = async (mockGroup, mockRequest, viewData, callback) => {
  groupItem.value = mockGroup
  requestItem.value = mockRequest
  previewData.value = viewData
  const requestDataPromise = MockRequestApi.getById(mockRequest.id)
  schemasConf.value = await loadSchemas({
    requestId: mockRequest.id,
    dataId: viewData?.id
  })
  if (viewData?.id) {
    const viewDataPromise = MockDataApi.getById(viewData.id)
    const requestViewData = await viewDataPromise
    previewData.value = requestViewData.resultData
  }
  const requestData = await requestDataPromise
  requestItem.value = requestData.resultData
  saveCallback = callback
  clearParamsAndResponse()
  return nextTick(() => {
    paramTarget.value = calcParamTarget(groupItem.value, requestItem.value, previewData.value, schemasConf.value)
  })
}

const requestPath = computed(() => {
  if (groupItem.value && requestItem.value) {
    return `/mock/${groupItem.value.groupPath}${requestItem.value.requestPath}`
  }
  return ''
})

const doDataPreview = async () => {
  console.log('========================paramTarget1', paramTarget.value)
  let requestUrl = requestPath.value
  paramTarget.value?.pathParams?.forEach(pathParam => {
    const pathValue = processEvnParams(paramTarget.value.groupConfig, pathParam.value)
    if (pathValue) {
      requestUrl = requestUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathValue)
        .replace(new RegExp(`\\{${pathParam.name}\\}`, 'g'), pathValue)
    }
  })
  const params = preProcessParams(paramTarget.value?.requestParams).reduce((results, item) => {
    addRequestParamsToResult(results, item.name, processEvnParams(paramTarget.value.groupConfig, item.value))
    return results
  }, {})
  const { data, hasBody } = calcRequestBody(paramTarget)
  const headers = Object.assign(hasBody ? { 'content-type': paramTarget.value?.requestContentType } : {},
    preProcessParams(paramTarget.value?.headerParams).reduce((results, item) => {
      results[item.name] = processEvnParams(paramTarget.value.groupConfig, item.value)
      return results
    }, {}))
  const config = {
    loading: true,
    params,
    paramsSerializer: toGetParams,
    data,
    headers
  }
  calcPreviewHeaders(paramTarget.value, requestUrl, config)
  requestItem.value?.id && (headers[MOCK_REQUEST_ID_HEADER] = requestItem.value?.id)
  previewData.value?.id && (headers[MOCK_DATA_ID_HEADER] = previewData.value?.id)
  if (previewData.value?.id) {
    await doSaveMockResponseBody()// data保存
  } else {
    await doSaveMockParams() // request mockParams保存
  }
  const authContent = paramTarget.value.authContent
  if (authContent) {
    await AUTH_OPTION_CONFIG[authContent.authType]?.parseAuthInfo(authContent, headers, params, paramTarget)
  }
  previewRequest({
    url: requestUrl,
    method: requestItem.value.method
  }, config)
    .then(calcResponse, calcResponse)
}

const calcResponse = (response) => {
  responseTarget.value = processResponse(response)
  console.log('===============================responseTarget', responseTarget.value)
}

const calcMockParams = () => {
  const paramTargetVal = cloneDeep(paramTarget.value)
  delete paramTargetVal.responseBody
  delete paramTargetVal.responseFormat
  delete paramTargetVal.method
  delete paramTargetVal.groupConfig
  delete paramTargetVal.contentType
  paramTargetVal.formData?.forEach(nv => {
    if (isArray(nv.value)) {
      nv.value = []
    }
  })
  return paramTargetVal
}

const doSaveMockParams = () => {
  if (paramTarget.value) {
    const requestId = requestItem.value?.id
    const id = previewData.value?.id
    const paramTargetVal = calcMockParams()
    const mockParams = JSON.stringify(paramTargetVal)
    return saveMockParams({
      requestId,
      id,
      mockParams
    }, { loading: false })
  }
}

const doSaveMockResponseBody = () => {
  if (previewData.value && checkDataChange()) {
    return MockDataApi.saveOrUpdate(previewData.value)
      .then((data) => {
        if (data.success && data.resultData) {
          ElMessage.success($i18nBundle('common.msg.saveSuccess'))
          saveCallback?.(data.resultData)
          previewData.value = data.resultData
        }
      })
  }
}

const checkDataChange = () => {
  let changed = false;
  ['responseBody', 'responseFormat', 'contentType', 'defaultCharset'].forEach(key => {
    if (paramTarget.value[key] !== previewData.value[key]) {
      previewData.value[key] = paramTarget.value[key]
      changed = true
    }
  })
  const paramTargetVal = calcMockParams()
  if (JSON.stringify(paramTargetVal) !== previewData.value.mockParams) {
    previewData.value.mockParams = JSON.stringify(paramTargetVal)
    changed = true
  }
  return changed
}

const clearParamsAndResponse = () => {
  responseTarget.value = undefined
  paramTarget.value = undefined
}

defineExpose({
  toPreviewRequest,
  clearParamsAndResponse
})

</script>

<template>
  <el-container class="flex-column">
    <mock-request-form
      v-if="requestItem && paramTarget"
      v-model="paramTarget"
      :request-path="requestPath"
      :response-target="responseTarget"
      :mock-response-editable="!!previewData"
      :schemas="schemasConf.schemas"
      :schema-spec="schemasConf.componentSpec"
      v-bind="$attrs"
      @send-request="doDataPreview"
      @save-mock-response-body="doSaveMockResponseBody"
    />
  </el-container>
</template>

<style scoped>

</style>
