<script setup lang="jsx">
import { computed, ref } from 'vue'
import { ElMessage, ElText } from 'element-plus'
import { defineFormOptions } from '@/components/utils'
import { copyMockRequest } from '@/api/mock/MockRequestApi'
import { $i18nBundle, $i18nConcat } from '@/messages'

const props = defineProps({
  scenarioList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['copySuccess'])

const DEFAULT_SCENARIO_VALUE = '__DEFAULT_SCENARIO__'

const showWindow = ref(false)
const currentRequest = ref({})
const copyModel = ref({
  requestId: null,
  scenarioCode: DEFAULT_SCENARIO_VALUE
})

const windowTitle = computed(() => {
  return $i18nConcat($i18nBundle('common.label.copyTo'), $i18nBundle('mock.label.scenario'))
})

const scenarioOptions = computed(() => {
  return [{
    label: $i18nBundle('mock.label.defaultScenario'),
    value: DEFAULT_SCENARIO_VALUE
  }, ...props.scenarioList.map(item => ({
    label: item.scenarioName,
    value: item.scenarioCode
  }))]
})

const toScenarioSelectValue = (scenarioCode) => {
  return scenarioCode || DEFAULT_SCENARIO_VALUE
}

const normalizeScenarioCode = (scenarioCode) => {
  return scenarioCode === DEFAULT_SCENARIO_VALUE ? '' : scenarioCode
}

const getScenarioLabel = (scenarioCode) => {
  if (!scenarioCode) {
    return $i18nBundle('mock.label.defaultScenario')
  }
  return props.scenarioList.find(item => item.scenarioCode === scenarioCode)?.scenarioName || scenarioCode
}

const formOptions = computed(() => defineFormOptions([{
  labelKey: 'mock.label.mockRequest',
  type: 'common-form-label',
  formatter () {
    return <>
      <ElText tag="b">{currentRequest.value?.requestPath} #{currentRequest.value?.method}</ElText>
      {currentRequest.value?.requestName
        ? <><br/><ElText type="info">{currentRequest.value.requestName}</ElText></>
        : ''}
    </>
  }
}, {
  labelKey: 'mock.label.copyFromScenario',
  type: 'common-form-label',
  formatter () {
    return <ElText>{getScenarioLabel(currentRequest.value?.scenarioCode)}</ElText>
  }
}, {
  labelKey: 'mock.label.copyToScenario',
  prop: 'scenarioCode',
  type: 'select',
  required: true,
  children: scenarioOptions.value,
  attrs: {
    clearable: false
  }
}]))

const toCopyRequest = (request) => {
  currentRequest.value = request || {}
  copyModel.value = {
    requestId: request?.id || null,
    scenarioCode: toScenarioSelectValue(request?.scenarioCode)
  }
  showWindow.value = true
}

const saveCopyRequest = ({ form }) => {
  form.validate(valid => {
    if (!valid) {
      return
    }
    copyMockRequest(copyModel.value.requestId, {
      scenarioCode: normalizeScenarioCode(copyModel.value.scenarioCode)
    }, { loading: true }).then(data => {
      if (data?.success) {
        ElMessage.success($i18nBundle('common.msg.copySuccess'))
        showWindow.value = false
        emit('copySuccess', data.resultData)
      }
    }).catch(() => false)
  })
  return false
}

defineExpose({
  toCopyRequest
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    :ok-click="saveCopyRequest"
    :title="windowTitle"
  >
    <el-container class="flex-column">
      <common-form
        class="form-edit-width-90"
        :options="formOptions"
        :model="copyModel"
        label-width="140px"
        :show-buttons="false"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
