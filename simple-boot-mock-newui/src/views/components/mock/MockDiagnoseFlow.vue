<script setup>
import { computed } from 'vue'
import { $i18nBundle } from '@/messages'

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
  info: 'info'
}
const stepStatusMap = {
  success: 'success',
  warning: 'process',
  danger: 'error',
  error: 'error',
  info: 'wait'
}
const detailLabelMap = {
  group: 'mock.label.mockGroup',
  request: 'mock.label.mockRequest',
  data: 'mock.label.mockData',
  scenario: 'mock.label.scenario',
  proxyUrl: 'mock.label.proxyUrl',
  statusCode: 'mock.label.statusCode',
  contentType: 'Content Type',
  durationMs: 'mock.label.logTime',
  method: 'mock.label.method',
  path: 'mock.label.requestPath',
  requestPath: 'mock.label.requestPath',
  matchPattern: 'mock.label.matchPattern'
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
  'count',
  'total',
  'enabledCount',
  'requestId',
  'dataId',
  'scenarioCode',
  'matchPattern',
  'message'
]

const stepTagType = status => statusTagTypes[status] || statusTagTypes.info
const stepStatus = status => stepStatusMap[status] || stepStatusMap.info
const detailLabel = key => {
  const label = detailLabelMap[key]
  return label?.includes('.') ? $i18nBundle(label) : label || key
}
const formatDuration = value => value === undefined || value === null || value === '' ? '' : `${value} ms`
const formatInfoObject = (key, value) => {
  const id = value.id ?? value.groupId ?? value.requestId ?? value.dataId ?? value.scenarioId
  const name = value.name || value.groupName || value.requestName || value.dataName || value.scenarioName
  const itemKey = value.key || value.groupPath || value.requestPath || value.scenarioCode
  const defaultLabel = key === 'scenario' && value.defaultScenario ? $i18nBundle('mock.label.defaultScenario') : ''
  const mainText = name || itemKey || (id == null ? '' : `#${id}`) || defaultLabel
  const metaTexts = [
    id != null && mainText !== `#${id}` ? `#${id}` : '',
    name && itemKey ? itemKey : ''
  ].filter(Boolean)
  return mainText && metaTexts.length ? `${mainText} (${metaTexts.join(', ')})` : mainText
}
const formatDetailValue = (key, value) => {
  if (value === undefined || value === null || value === '') return ''
  if (key === 'durationMs') return formatDuration(value)
  if (Array.isArray(value)) return `${value.length}`
  if (typeof value === 'object') return formatInfoObject(key, value) || JSON.stringify(value)
  return String(value)
}
const toDetailChips = details => {
  if (!details) return []
  const entries = Object.entries(details)
  return detailPriorityKeys
    .map(key => entries.find(([entryKey]) => entryKey === key))
    .filter(Boolean)
    .map(([key, value]) => ({ key, label: detailLabel(key), value: formatDetailValue(key, value) }))
    .filter(item => item.value)
}
const flowSteps = computed(() => (props.steps || []).map((step, index) => ({
  raw: step,
  index: index + 1,
  stage: step.stage,
  status: step.status,
  code: step.code,
  statusType: stepTagType(step.status),
  stepStatus: stepStatus(step.status),
  chips: toDetailChips(step.details)
})))
const showStep = step => emit('showRawData', step.raw)
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
      :class="['mock-diagnose-flow__step', `is-${step.status || 'info'}`]"
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
            {{ step.stage }}
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
            {{ step.code }}
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
              class="mock-diagnose-flow__chip"
            >
              <span class="mock-diagnose-flow__chip-label">{{ chip.label }}</span>
              <span class="mock-diagnose-flow__chip-value">{{ chip.value }}</span>
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

@media (max-width: 768px) {
  .mock-diagnose-flow__chip-value {
    max-width: 220px;
  }
}
</style>
