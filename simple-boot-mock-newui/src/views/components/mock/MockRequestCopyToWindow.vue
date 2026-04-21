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
  },
  allowMove: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['copySuccess', 'transferSuccess'])

const DEFAULT_SCENARIO_VALUE = '__DEFAULT_SCENARIO__'

const showWindow = ref(false)
const currentRequest = ref({})
const copyModel = ref({
  requestId: null,
  action: 'copy',
  scenarioCode: DEFAULT_SCENARIO_VALUE
})

const getActionLabel = (action) => {
  return action === 'move'
    ? $i18nBundle('common.label.move')
    : $i18nBundle('common.label.copy')
}

const actionOptions = computed(() => {
  const options = [{
    label: getActionLabel('copy'),
    value: 'copy'
  }]
  if (props.allowMove) {
    options.push({
      label: getActionLabel('move'),
      value: 'move'
    })
  }
  return options
})

const currentActionTargetLabel = computed(() => {
  return copyModel.value.action === 'move'
    ? $i18nBundle('common.label.moveTo')
    : $i18nBundle('common.label.copyTo')
})

const windowTitle = computed(() => {
  return $i18nConcat(currentActionTargetLabel.value, $i18nBundle('mock.label.scenario'))
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

const isSameScenarioCode = (sourceScenarioCode, targetScenarioCode) => {
  return normalizeScenarioCode(toScenarioSelectValue(sourceScenarioCode)) === normalizeScenarioCode(targetScenarioCode)
}

const getScenarioLabel = (scenarioCode) => {
  if (!scenarioCode) {
    return $i18nBundle('mock.label.defaultScenario')
  }
  return props.scenarioList.find(item => item.scenarioCode === scenarioCode)?.scenarioName || scenarioCode
}

const formOptions = computed(() => defineFormOptions([{
  labelKey: 'common.label.operation',
  prop: 'action',
  type: 'segmented',
  required: true,
  enabled: props.allowMove,
  attrs: {
    clearable: false,
    options: actionOptions.value
  }
}, {
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
  label: $i18nConcat(currentActionTargetLabel.value, $i18nBundle('mock.label.scenario')),
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
    action: 'copy',
    scenarioCode: toScenarioSelectValue(request?.scenarioCode)
  }
  showWindow.value = true
}

const saveCopyRequest = ({ form }) => {
  form.validate(valid => {
    if (!valid) {
      return
    }
    if (copyModel.value.action === 'move' && isSameScenarioCode(currentRequest.value?.scenarioCode, copyModel.value.scenarioCode)) {
      ElMessage.error($i18nBundle('common.msg.moveSameTarget'))
      return
    }
    copyMockRequest(copyModel.value.requestId, {
      action: copyModel.value.action,
      scenarioCode: normalizeScenarioCode(copyModel.value.scenarioCode)
    }, { loading: true }).then(data => {
      if (data?.success) {
        const successKey = copyModel.value.action === 'move'
          ? 'common.msg.moveSuccess'
          : 'common.msg.copySuccess'
        ElMessage.success($i18nBundle(successKey))
        showWindow.value = false
        emit('transferSuccess', data.resultData)
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
