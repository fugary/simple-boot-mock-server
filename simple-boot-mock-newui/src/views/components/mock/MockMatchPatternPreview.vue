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
import { $i18nBundle, $i18nKey } from '@/messages'
import {
  MOCK_DATA_MATCH_PATTERN_HEADER,
  MOCK_DATA_PATH_PARAMS_HEADER,
  MOCK_DATA_USER_HEADER
} from '@/consts/MockConstants'
import { addRequestParamsToResult, processEvnParams } from '@/services/mock/MockCommonService'
import { toGetParams } from '@/utils'
import { ElMessage } from 'element-plus'
import { isString } from 'lodash-es'

const showWindow = ref(false)
const groupItem = ref()
const currentItem = ref()
const paramTarget = ref()
const responseTarget = ref()
const schemasConf = ref({})

let saveResolve
const toTestMatchPattern = (mockGroup, mockRequest, viewData) => {
  groupItem.value = mockGroup
  currentItem.value = viewData || mockRequest // 当前预览的是request还是data
  showWindow.value = true
  paramTarget.value = calcParamTarget(groupItem.value, mockRequest, viewData, schemasConf.value)
  paramTarget.value.requestPath = '/mock/checkMatchPattern'
  const matchPattern = currentItem.value?.matchPattern
  if (matchPattern) {
    paramTarget.value.matchPattern = matchPattern
  }
  loadSchemas({
    requestId: mockRequest.id,
    dataId: viewData?.id
  }).then(schemasData => {
    schemasConf.value = schemasData
  })
  return new Promise(resolve => (saveResolve = resolve))
}

const doDataPreview = () => {
  const params = preProcessParams(paramTarget.value?.requestParams).reduce((results, item) => {
    addRequestParamsToResult(results, item.name, processEvnParams(paramTarget.value.groupConfig, item.value))
    return results
  }, {})
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
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
    })
  } else {
    MockRequestApi.saveOrUpdate(currentItem.value).then(() => {
      showWindow.value = false
      saveResolve?.(currentItem.value)
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
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
        :schemas="schemasConf.schemas"
        :schema-spec="schemasConf.componentSpec"
        @send-request="doDataPreview"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
