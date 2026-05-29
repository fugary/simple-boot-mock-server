<script setup lang="jsx">
import { computed, ref } from 'vue'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockDiagnoseFlow from '@/views/components/mock/MockDiagnoseFlow.vue'
import { ElTag } from 'element-plus'

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
  info: 'info'
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
const formatText = text => text === undefined || text === null || text === '' ? '' : <span>{text}</span>
const statusCodeTagType = statusCode => {
  const code = Number(statusCode)
  if (!Number.isFinite(code)) return 'info'
  if (code >= 500) return 'danger'
  if (code >= 400) return 'warning'
  if (code >= 300) return 'info'
  return 'success'
}
const formatDuration = duration => duration === undefined || duration === null || duration === '' ? '' : `${duration} ms`
const formatItem = (item, defaultLabelKey) => {
  if (!item) return ''
  const id = item.id == null ? '' : `#${item.id}`
  const mainText = item.name || item.key || id || (item.defaultScenario && defaultLabelKey ? $i18nBundle(defaultLabelKey) : '')
  const metaTexts = [
    id && mainText !== id ? id : '',
    item.name && item.key ? item.key : ''
  ].filter(Boolean)
  return formatText(metaTexts.length ? `${mainText} (${metaTexts.join(', ')})` : mainText)
}
const formatProxyUrl = proxyUrl => {
  if (!proxyUrl) return ''
  return <span class="mock-diagnose-proxy-url">
    <span class="mock-diagnose-proxy-url__text">{proxyUrl}</span>
    <MockUrlCopyLink content={proxyUrl} class="margin-left1" />
  </span>
}
const formatHttpInfo = diagnoseInfo => {
  const duration = formatDuration(diagnoseInfo?.durationMs)
  const items = [
    diagnoseInfo?.statusCode !== undefined && diagnoseInfo?.statusCode !== null
      ? <span key="statusCode" class="mock-diagnose-http-info__item">
        <span class="mock-diagnose-http-info__label">{$i18nBundle('mock.label.statusCode')}</span>
        <ElTag size="small" type={statusCodeTagType(diagnoseInfo.statusCode)}>
          {diagnoseInfo.statusCode}
        </ElTag>
      </span>
      : '',
    diagnoseInfo?.contentType
      ? <span key="contentType" class="mock-diagnose-http-info__item">
        <span class="mock-diagnose-http-info__label">Content Type</span>
        <span class="mock-diagnose-http-info__value">{diagnoseInfo.contentType}</span>
      </span>
      : '',
    duration
      ? <span key="durationMs" class="mock-diagnose-http-info__item">
        <span class="mock-diagnose-http-info__label">{$i18nBundle('mock.label.logTime')}</span>
        <span class="mock-diagnose-http-info__value">{duration}</span>
      </span>
      : ''
  ].filter(Boolean)
  return items.length ? <span class="mock-diagnose-http-info">{items}</span> : ''
}
const diagnoseSummaryItems = computed(() => [
  { labelKey: 'mock.label.mockGroup', value: formatItem(props.diagnoseInfo?.group) },
  { labelKey: 'mock.label.scenario', value: formatItem(props.diagnoseInfo?.scenario, 'mock.label.defaultScenario') },
  { labelKey: 'mock.label.mockRequest', value: formatItem(props.diagnoseInfo?.request) },
  { labelKey: 'mock.label.mockData', value: formatItem(props.diagnoseInfo?.data) },
  { labelKey: 'mock.label.proxyUrl', value: formatProxyUrl(props.diagnoseInfo?.proxyUrl) },
  { label: 'HTTP', value: formatHttpInfo(props.diagnoseInfo) }
].filter(item => item.value))
const toJson = data => typeof data === 'string' ? data : JSON.stringify(data, null, 2)
const toShowRawData = data => {
  showCodeWindow(toJson(data), {
    language: 'json',
    title: $i18nBundle('mock.label.diagnose')
  })
}
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
    click: toShowRawData
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
        <el-tag :type="stepTagType(row.status)">
          {{ row.stage }}
        </el-tag>
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

:deep(.mock-diagnose-proxy-url) {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
}

:deep(.mock-diagnose-proxy-url__text) {
  overflow-wrap: anywhere;
}

:deep(.mock-diagnose-http-info) {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  max-width: 100%;
}

:deep(.mock-diagnose-http-info__item) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

:deep(.mock-diagnose-http-info__label) {
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

:deep(.mock-diagnose-http-info__value) {
  overflow-wrap: anywhere;
}
</style>
