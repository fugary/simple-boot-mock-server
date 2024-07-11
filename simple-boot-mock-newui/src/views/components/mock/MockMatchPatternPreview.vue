<script setup>
import { ref } from 'vue'
import MockDataApi, { calcParamTarget, previewRequest, processResponse } from '@/api/mock/MockDataApi'
import MockRequestApi from '@/api/mock/MockRequestApi'
import MockRequestForm from '@/views/components/mock/form/MockRequestForm.vue'

const showWindow = ref(false)
const groupItem = ref()
const currentItem = ref()
const paramTarget = ref()
const responseTarget = ref()

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
  return new Promise(resolve => (saveResolve = resolve))
}

const doDataPreview = () => {
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
  if (paramTarget.value.pathParams?.length) {
    headers['mock-data-path-params'] = JSON.stringify(paramTarget.value.pathParams)
  }
  if (paramTarget.value.matchPattern) {
    headers['mock-data-match-pattern'] = paramTarget.value.matchPattern
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
    width="1000px"
    ok-label="保存匹配规则"
    show-fullscreen
    :ok-click="saveMatchPattern"
    destroy-on-close
  >
    <template #header>
      <span class="el-dialog__title">
        匹配规则测试
      </span>
    </template>
    <el-container class="flex-column">
      <mock-request-form
        v-model="paramTarget"
        :request-path="paramTarget.requestPath"
        :response-target="responseTarget"
        match-pattern-mode
        @send-request="doDataPreview"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
