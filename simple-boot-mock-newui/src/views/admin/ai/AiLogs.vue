<script setup>
import { computed, h, onActivated, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { AiLogApi, loadAiConfigOptions } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'
import { $coreConfirm } from '@/utils'
import { useDefaultPage } from '@/config'
import AiLogDetailWindow from './AiLogDetailWindow.vue'
import { ElSpace, ElTag } from 'element-plus'
import { AI_PROVIDERS } from '@/consts/AiConstants'

const route = useRoute()
const router = useRouter()
const configs = ref([])
let querySequence = 0
const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: useDefaultPage() },
  searchMethod: async params => {
    const sequence = ++querySequence
    const { createDates, ...query } = params
    const response = await AiLogApi.search({
      ...query,
      configId: query.configId || undefined,
      status: query.status === '' ? undefined : query.status,
      startDate: createDates?.[0],
      endDate: createDates?.[1]
    })
    return sequence === querySequence ? response : { success: false }
  }
})
const routeConfigId = () => {
  const value = Number(route.query.configId)
  return Number.isSafeInteger(value) && value > 0 ? value : undefined
}
const syncRouteConfig = () => {
  const value = routeConfigId()
  if (value === searchParam.value.configId) return false
  searchParam.value.configId = value
  return true
}
const { initLoadOnce } = useInitLoadOnce(async () => {
  syncRouteConfig()
  const response = await loadAiConfigOptions()
  if (response.success) configs.value = response.resultData || []
  await searchMethod(1)
})
onMounted(initLoadOnce)
onActivated(initLoadOnce)
watch(() => route.query.configId, () => {
  if (route.name === 'AiLogs' && syncRouteConfig()) searchMethod(1)
})
const changeConfig = async () => {
  searchParam.value.configId = searchParam.value.configId || undefined
  await router.replace({ query: { ...route.query, configId: searchParam.value.configId } })
  await searchMethod(1)
}
const configOptions = computed(() => {
  const records = [...configs.value]
  const selectedId = searchParam.value.configId
  if (selectedId && !records.some(record => record.id === selectedId)) {
    records.push({ id: selectedId, configName: '#' + selectedId, deleted: true })
  }
  return records.map(record => {
    const provider = AI_PROVIDERS.find(provider => provider.value === record.provider)
    const markers = [
      record.isDefault === 1 ? $i18nBundle('mock.label.default') : '',
      record.deleted ? $i18nBundle('mock.label.aiDeletedConfig') : record.status === 0 ? $i18nBundle('common.label.statusDisabled') : ''
    ].filter(Boolean)
    return {
      value: record.id,
      label: [provider ? $i18nBundle(provider.labelKey) : '', record.configName, record.baseUrl, ...markers].filter(Boolean).join(' · '),
      slots: {
        default: () => h(ElSpace, { size: 6 }, () => [
          provider ? h(ElTag, { type: provider.tagType, size: 'small' }, () => $i18nBundle(provider.labelKey)) : null,
          h('span', record.configName),
          record.baseUrl ? h('small', { class: 'margin-left2' }, record.baseUrl) : null,
          ...markers.map(marker => h(ElTag, { type: 'info', size: 'small' }, () => marker))
        ])
      }
    }
  })
})
const detailVisible = ref(false)
const detailRecord = ref(null)
let detailSequence = 0
const view = async row => {
  const sequence = ++detailSequence
  const res = await AiLogApi.getById(row.id)
  if (res.success && sequence === detailSequence) {
    detailRecord.value = res.resultData
    detailVisible.value = true
  }
}
const remove = async row => {
  await $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
  const res = await AiLogApi.deleteById(row.id)
  if (res.success) {
    await searchMethod(tableData.value.length === 1 ? Math.max(1, searchParam.value.page.pageNumber - 1) : undefined)
  }
}
const options = computed(() => [
  { labelKey: 'common.label.keywords', prop: 'keyword' },
  {
    labelKey: 'mock.label.aiConfigName',
    prop: 'configId',
    type: 'select',
    children: configOptions.value,
    attrs: { filterable: true, clearable: true, style: { width: '320px' } },
    change: changeConfig
  },
  { labelKey: 'mock.label.aiModel', prop: 'model' },
  {
    labelKey: 'common.label.createDate',
    prop: 'createDates',
    type: 'date-picker',
    attrs: { type: 'datetimerange', valueFormat: 'YYYY-MM-DD HH:mm:ss', unlinkPanels: true, clearable: true }
  },
  { labelKey: 'common.label.status', prop: 'status', type: 'select', children: [{ labelKey: 'mock.label.aiSuccess', value: 1 }, { labelKey: 'mock.label.aiFailed', value: 0 }], attrs: { clearable: true }, change: () => searchMethod(1) }
])
const columns = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'configName', minWidth: '160px' },
  { labelKey: 'mock.label.aiModel', prop: 'model', minWidth: '180px', attrs: { showOverflowTooltip: true } },
  { labelKey: 'common.label.status', width: '100px', attrs: { align: 'center' }, formatter: row => h(ElTag, { type: row.status === 1 ? 'success' : 'danger' }, () => $i18nBundle(row.status === 1 ? 'mock.label.aiSuccess' : 'mock.label.aiFailed')) },
  { labelKey: 'mock.label.aiDuration', prop: 'durationMs', width: '120px', attrs: { align: 'center' } },
  { labelKey: 'mock.label.aiTotalTokens', prop: 'totalTokens', width: '110px', attrs: { align: 'center' } },
  { labelKey: 'mock.label.aiError', prop: 'errorMessage', minWidth: '200px', attrs: { showOverflowTooltip: true } },
  { labelKey: 'common.label.createDate', prop: 'createDate', dateFormat: 'YYYY-MM-DD HH:mm:ss', minWidth: '170px' }
])
const buttons = [{ labelKey: 'common.label.view', type: 'primary', click: view }, { labelKey: 'common.label.delete', type: 'danger', click: remove }]
</script>
<template>
  <el-container class="flex-column">
    <common-form
      inline
      :model="searchParam"
      :options="options"
      :submit-label="$t('common.label.search')"
      @submit-form="searchMethod(1)"
    />
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :loading="loading"
      :buttons-column-attrs="{ minWidth: '150px', fixed: 'right' }"
      @row-dblclick="view($event)"
      @page-size-change="searchMethod()"
      @current-page-change="searchMethod()"
    />
    <ai-log-detail-window
      v-if="detailRecord"
      v-model="detailVisible"
      :record="detailRecord"
    />
  </el-container>
</template>
