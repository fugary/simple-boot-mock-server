<script setup lang="jsx">
import { computed, ref } from 'vue'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'
import MockDiagnoseFlow from '@/views/components/mock/MockDiagnoseFlow.vue'
import { $copyText, $openNewWin } from '@/utils'
import { isExternalLink } from '@/components/utils'
import { ElLink, ElTag } from 'element-plus'
import CommonIcon from '@/components/common-icon/index.vue'
import { statusCodeTagType } from '@/services/mock/MockCommonService'
import {
  getDiagnoseCodeLabel,
  getDiagnoseDataSelectionLabel,
  getDiagnoseDetailLabel,
  getDiagnoseStageLabel,
  shouldShowDiagnoseKey
} from '@/services/mock/MockDiagnoseService'

const props = defineProps({
  diagnoseInfo: {
    type: Object,
    required: true
  }
})

const diagnoseViewMode = ref('flow')
const diagnoseTagTypes = {
  mock: 'success',
  proxy: 'warning',
  none: 'danger',
  error: 'danger',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  info: 'primary'
}
const diagnoseResultLabelKeys = {
  mock: 'mock.label.mockReturn',
  proxy: 'mock.label.proxyReturn',
  none: 'mock.label.noReturn',
  error: 'mock.label.diagnoseError'
}

const diagnoseResultTypeLabel = computed(() => {
  return $i18nBundle(diagnoseResultLabelKeys[props.diagnoseInfo?.resultType] || diagnoseResultLabelKeys.none)
})
const diagnoseResultTagType = computed(() => diagnoseTagTypes[props.diagnoseInfo?.resultType] || diagnoseTagTypes.none)
const stepTagType = status => diagnoseTagTypes[status] || diagnoseTagTypes.info
const hasValue = value => value !== undefined && value !== null && value !== ''
const formatDuration = duration => hasValue(duration) ? `${duration} ms` : ''
const copySummaryText = (event, text) => {
  event?.stopPropagation()
  if (text) {
    $copyText(text)
  }
}
const onSummaryChipKeydown = (event, text) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    copySummaryText(event, text)
  }
}
const toSummaryChip = ({ label, text, type, externalLink = '' }) => {
  if (!hasValue(text)) return ''
  const copyText = String(text)
  return <ElTag
    size="small"
    effect="plain"
    type={type || undefined}
    class="mock-diagnose-summary-chip"
    role="button"
    tabindex={0}
    title={$i18nBundle('common.msg.clickToCopy')}
    onClick={event => copySummaryText(event, copyText)}
    onKeydown={event => onSummaryChipKeydown(event, copyText)}
  >
    {label ? <span class="mock-diagnose-summary-chip__label">{label}</span> : ''}
    <span class="mock-diagnose-summary-chip__value">{copyText}</span>
    {externalLink
      ? <ElLink
          type="primary"
          underline="never"
          class="mock-diagnose-summary-chip__link"
          title={$i18nBundle('mock.label.linkAddress')}
          onClick={event => {
            event.stopPropagation()
            $openNewWin(externalLink)
          }}
        >
          <CommonIcon size={13} icon="Link"/>
        </ElLink>
      : ''}
  </ElTag>
}
const toSummaryChips = chipConfigs => {
  const visibleChips = chipConfigs.filter(Boolean).map(toSummaryChip).filter(Boolean)
  return visibleChips.length ? <span class="mock-diagnose-summary-chips">{visibleChips}</span> : ''
}
const formatItemText = (item, defaultLabelKey) => {
  if (!item) return ''
  const id = item.id == null ? '' : `#${item.id}`
  const defaultLabel = item.defaultScenario && defaultLabelKey ? $i18nBundle(defaultLabelKey) : ''
  const mainText = item.name || item.key || id || defaultLabel
  const metaTexts = [
    id && mainText !== id ? id : '',
    item.name && item.key ? item.key : '',
    defaultLabel && mainText !== defaultLabel ? defaultLabel : ''
  ].filter(Boolean)
  return metaTexts.length ? `${mainText} (${metaTexts.join(', ')})` : mainText
}
const formatItem = (item, defaultLabelKey) => {
  return toSummaryChips([{ text: formatItemText(item, defaultLabelKey) }])
}
const formatDataItem = item => {
  return toSummaryChips([
    { text: formatItemText(item) },
    item?.dataSelection
      ? {
          label: getDiagnoseDetailLabel('dataSelection'),
          text: getDiagnoseDataSelectionLabel(item.dataSelection)
        }
      : ''
  ])
}
const formatProxyInfo = (proxyUrl, proxySource) => {
  return toSummaryChips([
    proxySource
      ? {
          label: getDiagnoseDetailLabel('proxySource'),
          text: getDiagnoseDetailLabel(proxySource)
        }
      : '',
    {
      text: proxyUrl,
      externalLink: isExternalLink(proxyUrl) ? proxyUrl : ''
    }
  ])
}
const formatHttpInfo = diagnoseInfo => {
  const duration = formatDuration(diagnoseInfo?.durationMs)
  return toSummaryChips([
    hasValue(diagnoseInfo?.statusCode)
      ? {
          label: $i18nBundle('mock.label.statusCode'),
          text: diagnoseInfo.statusCode,
          type: statusCodeTagType(diagnoseInfo.statusCode)
        }
      : '',
    diagnoseInfo?.contentType
      ? {
          label: 'Content Type',
          text: diagnoseInfo.contentType
        }
      : '',
    duration
      ? {
          label: $i18nBundle('mock.label.logTime'),
          text: duration
        }
      : ''
  ])
}
const diagnoseSummaryItems = computed(() => [
  { labelKey: 'mock.label.mockGroup', value: formatItem(props.diagnoseInfo?.group) },
  { labelKey: 'mock.label.scenario', value: formatItem(props.diagnoseInfo?.scenario, 'mock.label.defaultScenario') },
  { labelKey: 'mock.label.mockRequest', value: formatItem(props.diagnoseInfo?.request) },
  { labelKey: 'mock.label.mockData', value: formatDataItem(props.diagnoseInfo?.data) },
  {
    labelKey: 'mock.label.proxyUrl',
    value: formatProxyInfo(props.diagnoseInfo?.proxyUrl, props.diagnoseInfo?.proxySource)
  },
  { label: 'HTTP', value: formatHttpInfo(props.diagnoseInfo) }
].filter(item => item.value))
const toJson = data => typeof data === 'string' ? data : JSON.stringify(data, null, 2)
const toShowRawData = data => {
  showCodeWindow(toJson(data), {
    language: 'json',
    title: $i18nBundle('mock.label.diagnose')
  })
}
const getStageLabel = row => getDiagnoseStageLabel(row?.stage)
const getCodeLabel = row => getDiagnoseCodeLabel(row?.code)
const formatDetails = row => JSON.stringify(row.details || {})
const diagnoseStepColumns = [
  {
    label: '#',
    width: '60px',
    attrs: {
      type: 'index',
      align: 'center'
    }
  },
  {
    labelKey: 'mock.label.diagnoseStage',
    prop: 'stage',
    minWidth: '140px',
    slot: 'stage'
  },
  {
    labelKey: 'mock.label.diagnoseCode',
    prop: 'code',
    minWidth: '180px',
    slot: 'code'
  },
  {
    labelKey: 'mock.label.diagnoseDetails',
    prop: 'details',
    minWidth: '260px',
    formatter: formatDetails,
    click: toShowRawData,
    linkAttrs: {
      underline: 'never',
      class: 'mock-diagnose-detail-link'
    }
  }
]
</script>

<template>
  <el-container class="flex-column">
    <div class="mock-diagnose-header margin-bottom2">
      <div class="mock-diagnose-header__main">
        <el-tag :type="diagnoseResultTagType">
          {{ diagnoseResultTypeLabel }}
        </el-tag>
        <el-radio-group
          v-model="diagnoseViewMode"
          size="small"
        >
          <el-radio-button value="flow">
            {{ $t('mock.label.diagnoseFlow') }}
          </el-radio-button>
          <el-radio-button value="table">
            {{ $t('mock.label.diagnoseDetails') }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-link
        v-common-tooltip="$t('mock.msg.showRawData')"
        type="primary"
        underline="never"
        @click="toShowRawData(diagnoseInfo)"
      >
        <common-icon
          :size="40"
          icon="RawOnFilled"
        />
      </el-link>
    </div>
    <common-descriptions
      v-if="diagnoseSummaryItems.length"
      :items="diagnoseSummaryItems"
      :column="1"
      border
      size="small"
      class="margin-bottom3"
    />
    <mock-diagnose-flow
      v-if="diagnoseViewMode === 'flow'"
      :steps="diagnoseInfo.steps || []"
      @show-raw-data="toShowRawData"
    />
    <common-table
      v-else
      :data="diagnoseInfo.steps || []"
      :columns="diagnoseStepColumns"
      class="mock-diagnose-table"
      size="small"
      @row-dblclick="toShowRawData"
    >
      <template #stage="{ row }">
        <el-tag
          :type="stepTagType(row.status)"
          class="mock-diagnose-key-tag"
        >
          <span class="mock-diagnose-key-tag__label">{{ getStageLabel(row) }}</span>
          <span
            v-if="shouldShowDiagnoseKey(getStageLabel(row), row.stage)"
            class="mock-diagnose-key-tag__key"
          >
            {{ row.stage }}
          </span>
        </el-tag>
      </template>
      <template #code="{ row }">
        <el-link
          type="primary"
          underline="never"
          class="mock-diagnose-table-code"
          @click="toShowRawData(row)"
        >
          <span class="mock-diagnose-table-code__label">{{ getCodeLabel(row) }}</span>
          <span
            v-if="shouldShowDiagnoseKey(getCodeLabel(row), row.code)"
            class="mock-diagnose-table-code__key"
          >
            {{ row.code }}
          </span>
        </el-link>
      </template>
    </common-table>
  </el-container>
</template>

<style scoped>
.mock-diagnose-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mock-diagnose-header__main {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

:deep(.mock-diagnose-table .el-table__row) {
  cursor: pointer;
}

.mock-diagnose-detail-link {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mock-diagnose-key-tag {
  max-width: 100%;
  height: auto;
  min-height: 24px;
  padding: 4px 8px;
  white-space: normal;
}

.mock-diagnose-key-tag__label,
.mock-diagnose-table-code__label {
  font-weight: 600;
}

.mock-diagnose-key-tag__key,
.mock-diagnose-table-code__key {
  font-size: 12px;
  font-weight: 400;
  opacity: 0.72;
}

.mock-diagnose-key-tag__key::before,
.mock-diagnose-table-code__key::before {
  content: "(";
  margin-left: 4px;
}

.mock-diagnose-key-tag__key::after,
.mock-diagnose-table-code__key::after {
  content: ")";
}

.mock-diagnose-table-code {
  max-width: 100%;
  overflow-wrap: anywhere;
  vertical-align: top;
}

:deep(.mock-diagnose-summary-chips) {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 100%;
}

:deep(.mock-diagnose-summary-chip) {
  max-width: 100%;
  cursor: pointer;
  transition: border-color var(--el-transition-duration), background-color var(--el-transition-duration);
}

:deep(.mock-diagnose-summary-chip:hover) {
  background-color: var(--el-fill-color-light);
  border-color: var(--el-tag-hover-color);
}

:deep(.mock-diagnose-summary-chip__label) {
  color: var(--el-text-color-secondary);
}

:deep(.mock-diagnose-summary-chip__label::after) {
  content: ":";
  margin-right: 4px;
}

:deep(.mock-diagnose-summary-chip__value) {
  display: inline-block;
  max-width: min(680px, 62vw);
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

:deep(.mock-diagnose-summary-chip__link) {
  margin-left: 6px;
  vertical-align: text-bottom;
}

@media (max-width: 768px) {
  :deep(.mock-diagnose-summary-chip__value) {
    max-width: 220px;
  }
}
</style>
