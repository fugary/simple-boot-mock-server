<script setup>
import { computed, ref } from 'vue'
import MockRequestApi, { saveMockParams } from '@/api/mock/MockRequestApi'
import { calcParamTarget, previewRequest, processResponse } from '@/api/mock/MockDataApi'
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
  previewData.value = viewData
  const requestDataPromise = MockRequestApi.getById(mockRequest.id)
  const requestData = await requestDataPromise
  requestItem.value = requestData.resultData
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, requestItem.value, previewData.value)
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
  doSaveMockParams()
  previewRequest(requestUrl, requestItem.value, dataItemId, config)
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
        @send-request="doDataPreview"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
