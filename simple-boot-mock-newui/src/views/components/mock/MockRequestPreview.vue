<script setup>
import { computed, ref } from 'vue'
import MockRequestApi, { getDefaultData } from '@/api/mock/MockRequestApi'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestForm from '@/views/components/mock/MockRequestForm.vue'

const showWindow = ref(false)

const groupItem = ref(null)
const requestItem = ref(null)
const previewData = ref(null)

const previewRequest = async (mockGroup, mockRequest, viewData) => {
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
}

const requestPath = computed(() => {
  if (groupItem.value && requestItem.value) {
    return `/mock/${groupItem.value.groupPath}${requestItem.value.requestPath}`
  }
  return ''
})

defineExpose({
  previewRequest
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
        :request="requestItem"
        :data="previewData"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
