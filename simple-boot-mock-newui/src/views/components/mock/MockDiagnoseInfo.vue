<script setup>
import { computed } from 'vue'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'

const props = defineProps({
  diagnoseInfo: {
    type: Object,
    required: true
  }
})

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

const diagnoseResultTypeLabel = computed(() => {
  const labelKeyMap = {
    mock: 'mock.label.mockReturn',
    proxy: 'mock.label.proxyReturn',
    none: 'mock.label.noReturn',
    error: 'mock.label.diagnoseError'
  }
  return $i18nBundle(labelKeyMap[props.diagnoseInfo?.resultType] || labelKeyMap.none)
})
const diagnoseResultTagType = computed(() => diagnoseTagTypes[props.diagnoseInfo?.resultType] || diagnoseTagTypes.none)
const stepTagType = status => diagnoseTagTypes[status] || diagnoseTagTypes.info
const formatItem = item => {
  if (!item) return ''
  return [
    item.id == null ? '' : `#${item.id}`,
    item.name,
    item.key
  ].filter(Boolean).join(' / ')
}
const diagnoseSummaryItems = computed(() => [
  { labelKey: 'mock.label.groupName', value: formatItem(props.diagnoseInfo?.group) },
  { labelKey: 'mock.label.scenarioName', value: formatItem(props.diagnoseInfo?.scenario) },
  { labelKey: 'mock.label.requestName', value: formatItem(props.diagnoseInfo?.request) },
  { labelKey: 'mock.label.dataId', value: formatItem(props.diagnoseInfo?.data) },
  { labelKey: 'mock.label.proxyUrl', value: props.diagnoseInfo?.proxyUrl }
].filter(item => item.value))
const toJson = data => typeof data === 'string' ? data : JSON.stringify(data, null, 2)
const toShowRawData = data => {
  showCodeWindow(toJson(data), {
    language: 'json',
    title: $i18nBundle('mock.label.diagnose')
  })
}
</script>

<template>
  <el-container class="flex-column">
    <div class="margin-bottom2">
      <el-tag :type="diagnoseResultTagType">
        {{ diagnoseResultTypeLabel }}
      </el-tag>
      <el-link
        v-common-tooltip="$t('mock.msg.showRawData')"
        type="primary"
        underline="never"
        class="margin-left3"
        @click="toShowRawData(diagnoseInfo)"
      >
        <common-icon
          :size="40"
          icon="RawOnFilled"
        />
      </el-link>
    </div>
    <el-descriptions
      v-if="diagnoseSummaryItems.length"
      :column="1"
      border
      size="small"
      class="margin-bottom3"
    >
      <el-descriptions-item
        v-for="item in diagnoseSummaryItems"
        :key="item.labelKey"
        :label="$t(item.labelKey)"
      >
        {{ item.value }}
      </el-descriptions-item>
    </el-descriptions>
    <el-table
      :data="diagnoseInfo.steps || []"
      border
      size="small"
      class="mock-diagnose-table"
      @row-dblclick="toShowRawData"
    >
      <el-table-column
        :label="$t('mock.label.diagnoseStage')"
        prop="stage"
        min-width="130px"
      />
      <el-table-column
        :label="$t('common.label.status')"
        prop="status"
        min-width="100px"
      >
        <template #default="{ row }">
          <el-tag :type="stepTagType(row.status)">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('mock.label.diagnoseCode')"
        prop="code"
        min-width="180px"
      />
      <el-table-column
        :label="$t('mock.label.diagnoseDetails')"
        min-width="260px"
      >
        <template #default="{ row }">
          <el-text truncated>
            {{ JSON.stringify(row.details || {}) }}
          </el-text>
        </template>
      </el-table-column>
    </el-table>
  </el-container>
</template>

<style scoped>
:deep(.mock-diagnose-table .el-table__row) {
  cursor: pointer;
}
</style>
