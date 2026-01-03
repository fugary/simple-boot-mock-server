<script setup>
import { computed, nextTick, ref } from 'vue'
import MockRequestApi, { loadSchemas, saveMockParams } from '@/api/mock/MockRequestApi'
import MockDataApi, {
  calcParamTarget,
  calcRequestBody,
  preProcessParams,
  previewRequest,
  previewSseRequest,
  processResponse
} from '@/api/mock/MockDataApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'
import { AUTH_OPTION_CONFIG } from '@/services/mock/MockAuthorizationService'
import { MOCK_DATA_ID_HEADER, MOCK_REQUEST_ID_HEADER } from '@/consts/MockConstants'
import { cloneDeep, isArray, pickBy, isString } from 'lodash-es'
import { addRequestParamsToResult, calcPreviewHeaders, processEvnParams } from '@/services/mock/MockCommonService'
import { toGetParams } from '@/utils'

const groupItem = ref()
const requestItem = ref()
const previewData = ref()
const paramTarget = ref()
const responseTarget = ref()
const schemasConf = ref({})
const editable = ref(true)

let saveCallback
const toPreviewRequest = async (mockGroup, mockRequest, viewData, callback, isEditable = true) => {
  editable.value = isEditable
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

const resetParamTarget = () => {
  clearParamsAndResponse()
  nextTick(() => {
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
    const pathValue = processEvnParams(paramTarget.value.groupConfig, pathParam.value, true)
    if (pathValue) {
      requestUrl = requestUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathValue)
        .replace(new RegExp(`\\{${pathParam.name}\\}`, 'g'), pathValue)
    }
  })
  const params = preProcessParams(paramTarget.value?.requestParams).reduce((results, item) => {
    addRequestParamsToResult(results, item.name, processEvnParams(paramTarget.value.groupConfig, item.value, true))
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
  if (hasBody && isString(data)) { // 字符串不让axios处理，防止调试请求和postman有差异
    config.transformRequest = req => req
  }
  calcPreviewHeaders(paramTarget.value, requestUrl, config)
  requestItem.value?.id && (headers[MOCK_REQUEST_ID_HEADER] = requestItem.value?.id)
  previewData.value?.id && (headers[MOCK_DATA_ID_HEADER] = previewData.value?.id)
  await doSaveMockResponseBody()// mock请求数据保存
  const authContent = paramTarget.value.authContent
  if (authContent) {
    await AUTH_OPTION_CONFIG[authContent.authType]?.parseAuthInfo(authContent, headers, params, paramTarget)
  }

  if (headers.Accept?.includes('text/event-stream') || paramTarget.value.contentType?.includes('text/event-stream')) {
    const sseData = ref('')

    previewSseRequest({
      url: requestUrl,
      method: requestItem.value.method
    }, config, (data, isInitial) => {
      if (isInitial) {
        responseTarget.value = processResponse({
          config: data.config,
          headers: data.headers,
          status: data.status,
          data: ''
        })
      } else {
        // Accumulate chunks
        sseData.value += data
        // Trigger reactivity by creating new object
        responseTarget.value = {
          ...responseTarget.value,
          data: sseData.value
        }
      }
    }).catch((error) => {
      responseTarget.value = processResponse({
        config: { url: requestUrl, method: requestItem.value.method, __startTime: config.__startTime },
        response: { data: error.message, status: error.response?.status || 500 }
      })
    })
  } else {
    previewRequest({
      url: requestUrl,
      method: requestItem.value.method
    }, config)
      .then(calcResponse, calcResponse)
  }
}

const calcResponse = (response) => {
  responseTarget.value = processResponse(response)
  console.log('===============================responseTarget', responseTarget.value)
}

const calcMockParams = () => {
  const paramTargetVal = cloneDeep(paramTarget.value)
  paramTargetVal.formData?.forEach(nv => {
    if (isArray(nv.value)) {
      nv.value = []
    }
  })
  const keepKeys = ['formData', 'formUrlencoded', 'authContent', 'tableConfig']
  return pickBy(paramTargetVal, (_, key) => {
    return key.startsWith('request') || key.endsWith('Params') || keepKeys.includes(key)
  })
}

const doSaveMockResponseBody = () => {
  if (editable.value) {
    if (previewData.value?.id) {
      if (previewData.value && checkDataChange()) {
        return MockDataApi.saveOrUpdate(previewData.value)
          .then((data) => {
            if (data.success && data.resultData) {
              ElMessage.success($i18nBundle('common.msg.saveSuccess'))
              previewData.value = data.resultData
              return saveCallback?.(data.resultData)
            }
          }, error => {
            console.log('saveError', error)
            return error
          })
      }
    } else if (paramTarget.value && checkMockParamsChange(requestItem)) {
      const requestId = requestItem.value?.id
      const id = previewData.value?.id
      return saveMockParams({
        requestId,
        id,
        mockParams: requestItem.value.mockParams
      }, { loading: false })
        .then((data) => {
          console.log('=========================data', data)
          ElMessage.success($i18nBundle('common.msg.saveSuccess'))
          return saveCallback?.(data.resultData)
        })
    }
  }
}

const checkDataChange = () => {
  let changed = false;
  ['responseBody', 'responseFormat', 'contentType', 'defaultCharset'].forEach(key => {
    if ((paramTarget.value[key] || '') !== (previewData.value[key] || '')) {
      previewData.value[key] = paramTarget.value[key]
      changed = true
    }
  })
  if (checkMockParamsChange(previewData)) {
    changed = true
  }
  return changed
}

const checkMockParamsChange = (item) => {
  const paramTargetStr = JSON.stringify(calcMockParams())
  if (paramTargetStr !== item.value.mockParams) {
    item.value.mockParams = paramTargetStr
    return true
  }
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
      :editable="editable"
      :schemas="schemasConf.schemas"
      :schema-spec="schemasConf.componentSpec"
      v-bind="$attrs"
      @send-request="doDataPreview"
      @save-mock-response-body="doSaveMockResponseBody"
      @reset-request-form="resetParamTarget"
    />
  </el-container>
</template>

<style scoped>

</style>
