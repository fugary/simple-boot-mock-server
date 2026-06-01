<script setup>
import { computed } from 'vue'
import { $i18nBundle } from '@/messages'
import { $copyText, $openNewWin } from '@/utils'
import { isExternalLink } from '@/components/utils'
import { statusCodeTagType } from '@/services/mock/MockCommonService'
import {
  getDiagnoseCodeLabel,
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
  'group',
  'scenario',
  'request',
  'data',
  'proxyUrl',
  'url',
  'method',
  'path',
  'requestPath',
  'groupPath',
  'statusCode',
  'contentType',
  'durationMs',
  'forceRequest',
  'count',
  'total',
  'enabledCount',
  'requestId',
  'dataId',
  'matchPattern',
  'message'
]

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
  if (key === 'durationMs') return formatDuration(value)
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
const toDetailChip = (key, value) => {
  const text = formatDetailValue(key, value)
  const label = getDiagnoseDetailLabel(key)
  return {
    key,
    label,
    value: text,
    type: key === 'statusCode' ? statusCodeTagType(value) : '',
    copyText: text,
    externalLink: isExternalLink(text) ? text : ''
  }
}
const toDetailChips = details => {
  if (!details) return []
  const entries = Object.entries(details)
  return detailPriorityKeys
    .map(key => entries.find(([entryKey]) => entryKey === key))
    .filter(Boolean)
    .filter(([key, value]) => shouldShowDetail(details, key, value))
    .map(([key, value]) => toDetailChip(key, value))
    .filter(item => item.value)
}
const flowSteps = computed(() => (props.steps || []).map((step, index) => ({
  raw: step,
  index: index + 1,
  stage: step.stage,
  stageLabel: getDiagnoseStageLabel(step.stage),
  showStageKey: shouldShowDiagnoseKey(getDiagnoseStageLabel(step.stage), step.stage),
  isResult: step.stage === 'result',
  status: step.status,
  code: step.code,
  codeLabel: getDiagnoseCodeLabel(step.code),
  showCodeKey: shouldShowDiagnoseKey(getDiagnoseCodeLabel(step.code), step.code),
  statusType: stepTagType(step.status),
  stepStatus: stepStatus(step.status),
  chips: toDetailChips(step.details)
})))
const showStep = step => emit('showRawData', step.raw)
const copyChip = chip => {
  if (chip.copyText) {
    $copyText(chip.copyText)
  }
}
const openChipLink = chip => {
  if (chip.externalLink) {
    $openNewWin(chip.externalLink)
  }
}
</script>

<template>
  <el-empty
    v-if="!flowSteps.length"
    :description="$t('common.msg.noData')"
  />
  <el-steps
    v-else
    direction="vertical"
    class="mock-diagnose-flow"
  >
    <el-step
      v-for="step in flowSteps"
      :key="`${step.index}-${step.stage}-${step.code}`"
      :status="step.stepStatus"
      :class="['mock-diagnose-flow__step', `is-${step.status || 'info'}`, { 'is-result': step.isResult }]"
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
        <div
          class="mock-diagnose-flow__content"
          @dblclick="showStep(step)"
        >
          <el-link
            type="primary"
            underline="never"
            class="mock-diagnose-flow__code"
            @click="showStep(step)"
          >
            <span class="mock-diagnose-flow__code-label">{{ step.codeLabel }}</span>
            <span
              v-if="step.showCodeKey"
              class="mock-diagnose-flow__code-key"
            >
              {{ step.code }}
            </span>
          </el-link>
          <div
            v-if="step.chips.length"
            class="mock-diagnose-flow__chips"
          >
            <el-tag
              v-for="chip in step.chips"
              :key="chip.key"
              size="small"
              effect="plain"
              :type="chip.type"
              class="mock-diagnose-flow__chip"
              role="button"
              tabindex="0"
              @click.stop="copyChip(chip)"
              @dblclick.stop
              @keydown.enter.prevent.stop="copyChip(chip)"
              @keydown.space.prevent.stop="copyChip(chip)"
            >
              <span class="mock-diagnose-flow__chip-label">
                <span>{{ chip.label }}</span>
              </span>
              <span class="mock-diagnose-flow__chip-value">{{ chip.value }}</span>
              <el-link
                v-if="chip.externalLink"
                v-common-tooltip="$t('mock.label.linkAddress')"
                type="primary"
                underline="never"
                class="mock-diagnose-flow__chip-link"
                @click.stop="openChipLink(chip)"
              >
                <common-icon
                  :size="13"
                  icon="Link"
                />
              </el-link>
            </el-tag>
          </div>
        </div>
      </template>
    </el-step>
  </el-steps>
</template>

<style scoped>
.mock-diagnose-flow {
  padding: 4px 0 0 2px;
}

:deep(.mock-diagnose-flow__step .el-step__main) {
  min-height: 72px;
  padding-bottom: 8px;
}

:deep(.mock-diagnose-flow__step.is-warning .el-step__icon.is-text) {
  color: var(--el-color-warning);
  border-color: var(--el-color-warning);
}

:deep(.mock-diagnose-flow__step.is-info .el-step__icon.is-text) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

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

:deep(.mock-diagnose-flow__step .el-step__icon-inner.is-status svg) {
  display: none;
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

.mock-diagnose-flow__stage-label,
.mock-diagnose-flow__code-label {
  font-weight: 600;
}

.mock-diagnose-flow__stage-key,
.mock-diagnose-flow__code-key {
  font-family: var(--el-font-family);
  font-size: 12px;
  font-weight: 400;
  opacity: 0.72;
}

.mock-diagnose-flow__stage-key::before,
.mock-diagnose-flow__code-key::before {
  content: "(";
  margin-left: 4px;
}

.mock-diagnose-flow__stage-key::after,
.mock-diagnose-flow__code-key::after {
  content: ")";
}

.mock-diagnose-flow__content {
  padding-top: 2px;
  cursor: pointer;
}

.mock-diagnose-flow__code {
  max-width: 100%;
  overflow-wrap: anywhere;
  vertical-align: top;
}

.mock-diagnose-flow__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.mock-diagnose-flow__chip {
  max-width: 100%;
  cursor: pointer;
  transition: border-color var(--el-transition-duration), background-color var(--el-transition-duration);
}

.mock-diagnose-flow__chip:hover {
  background-color: var(--el-fill-color-light);
  border-color: var(--el-tag-hover-color);
}

.mock-diagnose-flow__chip-label {
  color: var(--el-text-color-secondary);
}

.mock-diagnose-flow__chip-label::after {
  content: ":";
  margin-right: 4px;
}

.mock-diagnose-flow__chip-value {
  display: inline-block;
  max-width: min(420px, 58vw);
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.mock-diagnose-flow__chip-link {
  margin-left: 6px;
  vertical-align: text-bottom;
}

@media (max-width: 768px) {
  .mock-diagnose-flow__chip-value {
    max-width: 220px;
  }
}
</style>
