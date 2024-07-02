<script setup>
import { computed, ref } from 'vue'
import MockRequestApi, { getDefaultData, saveMockParams } from '@/api/mock/MockRequestApi'
import { calcParamTarget, previewRequest, processResponse } from '@/api/mock/MockDataApi'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestForm from '@/views/components/mock/MockRequestForm.vue'

const showWindow = ref(false)
const groupItem = ref()
const requestItem = ref()
const previewData = ref()
const paramTarget = ref()
const responseTarget = ref()

const toPreviewRequest = async (mockGroup, mockRequest, viewData) => {
  groupItem.value = mockGroup
  requestItem.value = mockRequest
  const requestDataPromise = MockRequestApi.getById(mockRequest.id)
  if (!viewData) {
    const defaultDataPromise = getDefaultData(mockRequest.id, { showErrorMessage: false })
    const defaultData = await defaultDataPromise
    viewData = defaultData.resultData
  }
  previewData.value = viewData
  const requestData = await requestDataPromise
  requestItem.value = requestData.resultData
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, requestItem.value, previewData.value)
  doDataPreview()
}

const requestPath = computed(() => {
  if (groupItem.value && requestItem.value) {
    return `/mock/${groupItem.value.groupPath}${requestItem.value.requestPath}`
  }
  return ''
})

const doDataPreview = () => {
  console.log('========================paramTarget1', paramTarget.value)
  let requestUrl = requestPath.value
  paramTarget.value?.pathParams?.forEach(pathParam => {
    requestUrl = requestUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathParam.value)
  })
  const params = paramTarget.value?.requestParams?.reduce((results, item) => {
    results[item.name] = item.value
    return results
  }, {})
  const data = paramTarget.value?.showRequestBody ? paramTarget.value.requestBody : null
  const headers = Object.assign(paramTarget.value?.showRequestBody ? { 'content-type': paramTarget.value?.contentType } : {},
    paramTarget.value?.headerParams?.reduce((results, item) => {
      results[item.name] = item.value
      return results
    }, {}))
  const config = {
    loading: false,
    params,
    data,
    headers
  }
  // const dataItemId = this.currentDataItem && this.previewDataItemFlag ? this.currentDataItem.id : null
  const dataItemId = previewData.value?.id
  doSaveMockParams()
  previewRequest(requestUrl, requestItem.value, dataItemId, config)
    .then(calcResponse, calcResponse)
}

const calcResponse = (response) => {
  responseTarget.value = processResponse(response)
  console.log('========================response', responseTarget.value)
}

const doSaveMockParams = () => {
  if (paramTarget.value) {
    const requestId = requestItem.value?.id
    const id = previewData.value?.id
    const mockParams = JSON.stringify(paramTarget.value)
    saveMockParams({
      requestId,
      id,
      mockParams
    }, { loading: false })
  }
}

defineExpose({
  toPreviewRequest
})

</script>

<template>
  <common-window
    v-model="showWindow"
    show-fullscreen
  >
    <template #header>
      <span class="el-dialog__title">
        数据预览【{{ requestPath }}】
        <mock-url-copy-link :url-path="requestPath" />
      </span>
    </template>
    <el-container class="flex-column">
      <mock-request-form
        v-if="requestItem"
        v-model="paramTarget"
        :response-target="responseTarget"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
