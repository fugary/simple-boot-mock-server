<script setup>
import { computed } from 'vue'
import { $i18nBundle } from '@/messages'
import { isExternalLink } from '@/components/utils'
import { statusCodeTagType } from '@/services/mock/MockCommonService'
import MockDiagnoseStepDetail from '@/views/components/mock/MockDiagnoseStepDetail.vue'
import {
  getDiagnoseCodeLabel,
  getDiagnoseDataSelectionLabel,
  getDiagnoseDetailLabel,
  getDiagnoseStageLabel,
  shouldShowDiagnoseKey
} from '@/services/mock/MockDiagnoseService'

const props = defineProps({
  steps: {
    type: Array,
    default: () => []
  }
})
const emit = defineEmits(['showRawData'])

const statusTagTypes = {
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  error: 'danger',
  info: 'primary'
}
const stepStatusMap = {
  success: 'success',
  warning: 'process',
  danger: 'error',
  error: 'error',
  info: 'process'
}
const detailPriorityKeys = [
  'actualDelayMs',
  'configuredDelayMs',
  'delaySource',
  'group',
  'scenario',
  'request',
  'data',
  'proxySource',
  'proxyUrl',
  'url',
  'method',
  'path',
  'requestPath',
  'groupPath',
  'statusCode',
  'contentType',
  'durationMs',
  'costTime',
  'forceRequest',
  'count',
  'total',
  'enabledCount',
  'requestId',
  'dataId',
  'matchPattern',
  'message'
]
const patternMatchedCodes = new Set(['request_pattern_matched', 'data_pattern_matched'])
const consoleMessageTagTypes = {
  error: 'danger',
  warn: 'warning',
  debug: 'info'
}
const diagnoseStageGroups = [
  {
    name: 'ingress',
    labelKey: 'mock.label.diagnoseGroupIngress',
    descriptionKey: 'mock.label.diagnoseGroupIngressDesc'
  },
  {
    name: 'group',
    labelKey: 'mock.label.diagnoseGroupGroup',
    descriptionKey: 'mock.label.diagnoseGroupGroupDesc'
  },
  {
    name: 'scenario',
    labelKey: 'mock.label.diagnoseGroupScenario',
    descriptionKey: 'mock.label.diagnoseGroupScenarioDesc'
  },
  {
    name: 'request',
    labelKey: 'mock.label.diagnoseGroupRequest',
    descriptionKey: 'mock.label.diagnoseGroupRequestDesc'
  },
  {
    name: 'data',
    labelKey: 'mock.label.diagnoseGroupData',
    descriptionKey: 'mock.label.diagnoseGroupDataDesc'
  },
  {
    name: 'post_processor',
    labelKey: 'mock.label.diagnoseGroupPostProcessor',
    descriptionKey: 'mock.label.diagnoseGroupPostProcessorDesc'
  },
  {
    name: 'delay',
    labelKey: 'mock.label.diagnoseGroupDelay',
    descriptionKey: 'mock.label.diagnoseGroupDelayDesc'
  },
  {
    name: 'result',
    labelKey: 'mock.label.diagnoseGroupResult',
    descriptionKey: 'mock.label.diagnoseGroupResultDesc'
  }
]
const otherStageGroup = {
  name: 'other',
  labelKey: 'mock.label.diagnoseGroupOther',
  descriptionKey: 'mock.label.diagnoseGroupOtherDesc'
}
const diagnoseStageGroupOrder = diagnoseStageGroups.concat(otherStageGroup)
const diagnoseStageGroupMap = diagnoseStageGroups.reduce((result, group) => {
  result[group.name] = group
  return result
}, {})
const statusPriority = {
  danger: 4,
  error: 4,
  warning: 3,
  success: 2,
  info: 1
}

const stepTagType = status => statusTagTypes[status] || statusTagTypes.info
const stepStatus = status => stepStatusMap[status] || stepStatusMap.info
const formatDuration = value => value === undefined || value === null || value === '' ? '' : `${value} ms`
const getInfoId = value => value?.id ?? value?.groupId ?? value?.requestId ?? value?.dataId ?? value?.scenarioId
const getInfoName = value => value?.name || value?.groupName || value?.requestName || value?.dataName || value?.scenarioName
const getInfoKey = value => value?.key || value?.groupPath || value?.requestPath || value?.scenarioCode
const formatInfoObject = (key, value) => {
  const id = getInfoId(value)
  const name = getInfoName(value)
  const itemKey = getInfoKey(value)
  const defaultLabel = key === 'scenario' && value.defaultScenario ? getDiagnoseDetailLabel('defaultScenario') : ''
  const idText = id == null ? '' : `#${id}`
  const mainText = name || itemKey || idText || defaultLabel
  const metaTexts = [
    idText && mainText !== idText ? idText : '',
    defaultLabel && mainText !== defaultLabel ? defaultLabel : ''
  ].filter(Boolean)
  return mainText && metaTexts.length ? `${mainText} (${metaTexts.join(', ')})` : mainText
}
const formatDetailValue = (key, value) => {
  if (value === undefined || value === null || value === '') return ''
  if (key === 'durationMs' || key === 'actualDelayMs' || key === 'configuredDelayMs' || key === 'costTime') return formatDuration(value)
  if (key === 'delaySource' || key === 'proxySource') return getDiagnoseDetailLabel(value)
  if (key === 'dataSelection') return getDiagnoseDataSelectionLabel(value)
  if (Array.isArray(value)) return `${value.length}`
  if (typeof value === 'boolean') return $i18nBundle(value ? 'common.label.yes' : 'common.label.no')
  if (typeof value === 'object') return formatInfoObject(key, value) || JSON.stringify(value)
  return String(value)
}
const shouldShowDetail = (details, key, value) => {
  if (key === 'forceRequest' && value !== true) return false
  if (key === 'requestId' && details.request && getInfoId(details.request) === value) return false
  if (key === 'dataId' && details.data && getInfoId(details.data) === value) return false
  if (key === 'groupPath' && details.group && !getInfoName(details.group) && getInfoKey(details.group) === value) {
    return false
  }
  return true
}
const toDetailChip = (key, value, step) => {
  const text = formatDetailValue(key, value)
  const label = getDiagnoseDetailLabel(key)
  const type = key === 'statusCode'
    ? statusCodeTagType(value)
    : key === 'message' ? consoleMessageTagTypes[step?.details?.level] : undefined
  return {
    key,
    label,
    value: text,
    type,
    copyText: text,
    externalLink: isExternalLink(text) ? text : ''
  }
}
const toDetailItemChips = (key, value, step) => {
  const chips = [toDetailChip(key, value, step)]
  if (key === 'data' && value?.dataSelection) {
    chips.push(toDetailChip('dataSelection', value.dataSelection, step))
  }
  return chips
}
const getMatchedPattern = step => {
  if (!patternMatchedCodes.has(step?.code) || step?.details?.matchPattern) return ''
  return step?.details?.request?.matchPattern || step?.details?.data?.matchPattern || ''
}
const normalizeStepDetails = step => {
  const details = step?.details
  if (!details) return null
  const matchPattern = getMatchedPattern(step)
  return matchPattern ? { ...details, matchPattern } : details
}
const toDetailChips = step => {
  const details = normalizeStepDetails(step)
  if (!details) return []
  const entries = Object.entries(details)
  return detailPriorityKeys
    .map(key => entries.find(([entryKey]) => entryKey === key))
    .filter(Boolean)
    .filter(([key, value]) => shouldShowDetail(details, key, value))
    .flatMap(([key, value]) => toDetailItemChips(key, value, step))
    .filter(item => item.value)
}
const toFlowStep = (step, index) => {
  const stageLabel = getDiagnoseStageLabel(step.stage)
  const codeLabel = getDiagnoseCodeLabel(step.code)
  return {
    raw: step,
    index: index + 1,
    stageGroup: step.stageGroup,
    stage: step.stage,
    stageLabel,
    showStageKey: shouldShowDiagnoseKey(stageLabel, step.stage),
    status: step.status,
    code: step.code,
    codeLabel,
    showCodeKey: shouldShowDiagnoseKey(codeLabel, step.code),
    statusType: stepTagType(step.status),
    stepStatus: stepStatus(step.status),
    chips: toDetailChips(step)
  }
}
const flowSteps = computed(() => (props.steps || []).map(toFlowStep))
const useGroupedFlow = computed(() => flowSteps.value.every(step => step.stageGroup))
const getStageGroup = step => diagnoseStageGroupMap[step.stageGroup] || otherStageGroup
const getGroupStatus = steps => {
  return steps.reduce((target, step) => {
    const status = step.status || 'info'
    return (statusPriority[status] || 0) > (statusPriority[target] || 0) ? status : target
  }, 'info')
}
const flowGroups = computed(() => {
  const groupMap = new Map()
  flowSteps.value.forEach(step => {
    const groupConfig = getStageGroup(step)
    if (!groupMap.has(groupConfig.name)) {
      groupMap.set(groupConfig.name, {
        name: groupConfig.name,
        label: $i18nBundle(groupConfig.labelKey),
        description: $i18nBundle(groupConfig.descriptionKey),
        steps: []
      })
    }
    groupMap.get(groupConfig.name).steps.push(step)
  })
  return diagnoseStageGroupOrder
    .map(groupConfig => groupMap.get(groupConfig.name))
    .filter(Boolean)
    .map((group, index) => {
      const steps = group.steps.map((step, stepIndex) => ({
        ...step,
        subIndex: stepIndex + 1
      }))
      const status = getGroupStatus(group.steps)
      return {
        ...group,
        index: index + 1,
        steps,
        singleStep: steps.length === 1 ? steps[0] : null,
        status,
        statusType: stepTagType(status),
        stepStatus: stepStatus(status)
      }
    })
})
const showStep = step => emit('showRawData', step.raw)
</script>

<template>
  <el-empty
    v-if="!flowSteps.length"
    :description="$t('common.msg.noData')"
  />
  <el-steps
    v-else-if="!useGroupedFlow"
    direction="vertical"
    class="mock-diagnose-flow"
  >
    <el-step
      v-for="step in flowSteps"
      :key="`${step.index}-${step.stage}-${step.code}`"
      :status="step.stepStatus"
      :class="['mock-diagnose-flow__step', `is-${step.status || 'info'}`]"
      :style="{ '--mock-step-index': `'${step.index}'` }"
    >
      <template #title>
        <div
          class="mock-diagnose-flow__title"
          @dblclick="showStep(step)"
        >
          <el-tag
            effect="light"
            :type="step.statusType"
            class="mock-diagnose-flow__stage"
          >
            <span class="mock-diagnose-flow__stage-label">{{ step.stageLabel }}</span>
            <span
              v-if="step.showStageKey"
              class="mock-diagnose-flow__stage-key"
            >
              {{ step.stage }}
            </span>
          </el-tag>
        </div>
      </template>
      <template #description>
        <mock-diagnose-step-detail
          :step="step"
          @show-raw-data="showStep"
        />
      </template>
    </el-step>
  </el-steps>
  <el-steps
    v-else
    direction="vertical"
    class="mock-diagnose-stage-flow"
  >
    <el-step
      v-for="group in flowGroups"
      :key="group.name"
      :status="group.stepStatus"
      :class="['mock-diagnose-stage-flow__step', `is-${group.status || 'info'}`]"
      :style="{ '--mock-stage-index': `'${group.index}'` }"
    >
      <template #title>
        <div class="mock-diagnose-stage-flow__title">
          <el-tag
            effect="light"
            :type="group.statusType"
            class="mock-diagnose-stage-flow__stage"
          >
            {{ group.label }}
          </el-tag>
          <div class="mock-diagnose-stage-flow__desc">
            {{ group.description }}
          </div>
        </div>
      </template>
      <template #description>
        <div class="mock-diagnose-stage-flow__content">
          <mock-diagnose-step-detail
            v-if="group.singleStep"
            :step="group.singleStep"
            @show-raw-data="showStep"
          />
          <el-steps
            v-else
            direction="vertical"
            class="mock-diagnose-flow"
          >
            <el-step
              v-for="step in group.steps"
              :key="`${step.index}-${step.stage}-${step.code}`"
              :status="step.stepStatus"
              :class="['mock-diagnose-flow__step', `is-${step.status || 'info'}`]"
              :style="{ '--mock-step-index': `'${step.subIndex}'` }"
            >
              <template #title>
                <div
                  class="mock-diagnose-flow__title"
                  @dblclick="showStep(step)"
                >
                  <el-tag
                    effect="light"
                    :type="step.statusType"
                    class="mock-diagnose-flow__stage"
                  >
                    <span class="mock-diagnose-flow__stage-label">{{ step.stageLabel }}</span>
                    <span
                      v-if="step.showStageKey"
                      class="mock-diagnose-flow__stage-key"
                    >
                      {{ step.stage }}
                    </span>
                  </el-tag>
                </div>
              </template>
              <template #description>
                <mock-diagnose-step-detail
                  :step="step"
                  @show-raw-data="showStep"
                />
              </template>
            </el-step>
          </el-steps>
        </div>
      </template>
    </el-step>
  </el-steps>
</template>

<style scoped>
.mock-diagnose-stage-flow {
  padding: 2px 0 0 2px;
}

:deep(.mock-diagnose-stage-flow__step > .el-step__main) {
  padding-bottom: 10px;
}

:deep(.mock-diagnose-stage-flow__step.is-warning > .el-step__head .el-step__icon.is-text),
:deep(.mock-diagnose-flow__step.is-warning .el-step__icon.is-text) {
  color: var(--el-color-warning);
  border-color: var(--el-color-warning);
}

:deep(.mock-diagnose-stage-flow__step.is-info > .el-step__head .el-step__icon.is-text),
:deep(.mock-diagnose-flow__step.is-info .el-step__icon.is-text) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

:deep(.mock-diagnose-stage-flow__step > .el-step__head .el-step__icon-inner.is-status),
:deep(.mock-diagnose-flow__step .el-step__icon-inner.is-status) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
  transform: none;
}

:deep(.mock-diagnose-stage-flow__step > .el-step__head .el-step__icon-inner.is-status svg),
:deep(.mock-diagnose-flow__step .el-step__icon-inner.is-status svg) {
  display: none;
}

:deep(.mock-diagnose-stage-flow__step > .el-step__head .el-step__icon-inner.is-status::before) {
  content: var(--mock-stage-index);
}

.mock-diagnose-stage-flow__title {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 100%;
}

.mock-diagnose-stage-flow__stage {
  height: auto;
  min-height: 26px;
  padding: 4px 10px;
  font-weight: 700;
  white-space: normal;
}

.mock-diagnose-stage-flow__content {
  padding: 4px 0 0;
}

.mock-diagnose-stage-flow__desc {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.mock-diagnose-flow {
  padding: 2px 0 0 2px;
}

:deep(.mock-diagnose-flow__step .el-step__main) {
  padding-bottom: 10px;
}

:deep(.mock-diagnose-flow__step .el-step__icon-inner.is-status::before) {
  content: var(--mock-step-index);
}

.mock-diagnose-flow__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  cursor: pointer;
}

.mock-diagnose-flow__stage {
  max-width: 100%;
  height: auto;
  min-height: 24px;
  padding: 4px 8px;
  white-space: normal;
}

.mock-diagnose-flow__stage-label {
  font-weight: 600;
}

.mock-diagnose-flow__stage-key {
  font-family: var(--el-font-family);
  font-size: 12px;
  font-weight: 400;
  opacity: 0.72;
}

.mock-diagnose-flow__stage-key::before {
  content: "(";
  margin-left: 4px;
}

.mock-diagnose-flow__stage-key::after {
  content: ")";
}

@media (max-width: 768px) {
  :deep(.mock-diagnose-stage-flow__step > .el-step__main) {
    padding-left: 2px;
  }

  .mock-diagnose-stage-flow__title {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
