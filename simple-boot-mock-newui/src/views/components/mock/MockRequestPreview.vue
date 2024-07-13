<script setup>
import { computed, ref } from 'vue'
import MockRequestApi, { saveMockParams } from '@/api/mock/MockRequestApi'
import MockDataApi, { calcParamTarget, previewRequest, processResponse } from '@/api/mock/MockDataApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'

const showWindow = ref(false)
const groupItem = ref()
const requestItem = ref()
const previewData = ref()
const paramTarget = ref()
const responseTarget = ref()

let saveCallback
const toPreviewRequest = async (mockGroup, mockRequest, viewData, callback) => {
  groupItem.value = mockGroup
  requestItem.value = mockRequest
  previewData.value = viewData
  const requestDataPromise = MockRequestApi.getById(mockRequest.id)
  if (viewData?.id) {
    const viewDataPromise = MockDataApi.getById(viewData.id)
    const requestViewData = await viewDataPromise
    previewData.value = requestViewData.resultData
  }
  const requestData = await requestDataPromise
  requestItem.value = requestData.resultData
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, requestItem.value, previewData.value)
  saveCallback = callback
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
    if (pathParam.value) {
      requestUrl = requestUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathParam.value)
        .replace(new RegExp(`\\{${pathParam.name}\\}`, 'g'), pathParam.value)
    }
  })
  const params = paramTarget.value?.requestParams?.reduce((results, item) => {
    results[item.name] = item.value
    return results
  }, {})
  const data = paramTarget.value.requestBody
  const headers = Object.assign(data ? { 'content-type': paramTarget.value?.contentType } : {},
    paramTarget.value?.headerParams?.reduce((results, item) => {
      results[item.name] = item.value
      return results
    }, {}))
  const config = {
    loading: true,
    params,
    data,
    headers
  }
  const dataItemId = previewData.value?.id
  if (dataItemId) {
    headers['mock-data-id'] = dataItemId
  }
  doSaveMockParams()
  if (paramTarget.value?.responseBody !== previewData.value?.responseBody) {
    await doSaveMockResponseBody()
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

const doSaveMockParams = () => {
  if (paramTarget.value) {
    const requestId = requestItem.value?.id
    const id = previewData.value?.id
    const paramTargetVal = { ...paramTarget.value }
    delete paramTargetVal.responseBody
    delete paramTargetVal.method
    const mockParams = JSON.stringify(paramTargetVal)
    saveMockParams({
      requestId,
      id,
      mockParams
    }, { loading: false })
  }
}

const doSaveMockResponseBody = () => {
  if (previewData.value) {
    previewData.value.responseBody = paramTarget.value.responseBody
    return MockDataApi.saveOrUpdate(previewData.value)
      .then(() => {
        ElMessage.success($i18nBundle('common.msg.saveSuccess'))
        saveCallback?.(previewData.value)
      })
  }
}

defineExpose({
  toPreviewRequest
})

</script>

<template>
  <common-window
    v-model="showWindow"
    width="1000px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    show-fullscreen
    destroy-on-close
  >
    <template #header>
      <span class="el-dialog__title">
        请求测试
      </span>
    </template>
    <el-container class="flex-column">
      <mock-request-form
        v-if="requestItem"
        v-model="paramTarget"
        :request-path="requestPath"
        :response-target="responseTarget"
        :mock-response-editable="!!previewData"
        @send-request="doDataPreview"
        @save-mock-response-body="doSaveMockResponseBody"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
