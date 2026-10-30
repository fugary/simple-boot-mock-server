<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { AiConfigApi } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'
import { $coreConfirm, formatDate } from '@/utils'
import { useDefaultPage } from '@/config'
import AiConfigTestWindow from './AiConfigTestWindow.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { ElTag } from 'element-plus'
import { useAiModels } from '@/hooks/AiHooks'
import { useSearchStatus } from '@/consts/GlobalConstants'
import { AI_PROVIDERS } from '@/consts/AiConstants'

const router = useRouter()
let querySequence = 0
const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: useDefaultPage() },
  searchMethod: async params => {
    const sequence = ++querySequence
    const response = await AiConfigApi.search(params)
    return sequence === querySequence ? response : { success: false }
  }
})
onMounted(() => searchMethod())
const editVisible = ref(false)
const item = ref({})
const { modelOption } = useAiModels(item, item, 'defaultModel')
const testVisible = ref(false)
const testItem = ref({})
let editSequence = 0
const edit = async row => {
  const sequence = ++editSequence
  if (row?.id) {
    const res = await AiConfigApi.getById(row.id)
    if (!res.success || sequence !== editSequence) return
    item.value = { ...res.resultData }
  } else {
    item.value = { provider: 'openai', apiKey: '', status: 1, isDefault: 0 }
  }
  editVisible.value = true
}
const save = async data => {
  const res = await AiConfigApi.saveOrUpdate(data)
  if (!res.success) {
    throw new Error(res.message)
  }
  await searchMethod()
}
const remove = async row => {
  await $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
  const res = await AiConfigApi.deleteById(row.id)
  if (res.success) {
    await searchMethod(tableData.value.length === 1 ? Math.max(1, searchParam.value.page.pageNumber - 1) : undefined)
  }
}
const showTest = row => {
  testItem.value = row
  testVisible.value = true
}

const baseUrlSuggestions = (query, callback) => {
  const defaultUrl = AI_PROVIDERS.find(provider => provider.value === item.value.provider)?.baseUrl
  callback([...new Set([query, defaultUrl].filter(Boolean))].map(value => ({ value })))
}
const searchOptions = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'keyword' },
  {
    labelKey: 'mock.label.aiProvider',
    prop: 'provider',
    type: 'select',
    children: AI_PROVIDERS.map(({ labelKey, value }) => ({ labelKey, value })),
    change: () => searchMethod(1)
  },
  { labelKey: 'mock.label.aiModelName', prop: 'defaultModel' },
  useSearchStatus({ change: () => searchMethod(1) })
])
const options = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'configName', required: true },
  { labelKey: 'mock.label.aiProvider', prop: 'provider', type: 'select', required: true, children: AI_PROVIDERS.map(({ labelKey, value }) => ({ labelKey, value })) },
  { labelKey: 'mock.label.aiBaseUrl', prop: 'baseUrl', type: 'autocomplete', required: true, placeholder: $i18nBundle('mock.label.aiBaseUrlHint'), attrs: { fetchSuggestions: baseUrlSuggestions } },
  { labelKey: 'mock.label.aiApiKey', prop: 'apiKey', required: !item.value.id, placeholder: $i18nBundle('mock.label.aiKeyHint'), attrs: { type: 'password', showPassword: true, autocomplete: 'new-password' } },
  modelOption.value,
  { labelKey: 'common.label.statusEnable', prop: 'status', type: 'switch', attrs: { activeValue: 1, inactiveValue: 0 } },
  { labelKey: 'mock.label.aiDefault', prop: 'isDefault', type: 'switch', attrs: { activeValue: 1, inactiveValue: 0 } }
])
const columns = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'configName', minWidth: '160px' },
  {
    labelKey: 'mock.label.aiProvider',
    prop: 'provider',
    minWidth: '190px',
    attrs: { align: 'center' },
    formatter: row => {
      const provider = AI_PROVIDERS.find(item => item.value === row.provider)
      return h(ElTag, { type: provider?.tagType || 'info' }, () => provider ? $i18nBundle(provider.labelKey) : row.provider)
    }
  },
  { labelKey: 'mock.label.aiModel', prop: 'defaultModel', minWidth: '180px', attrs: { showOverflowTooltip: true } },
  { labelKey: 'mock.label.aiBaseUrl', prop: 'baseUrl', minWidth: '220px', attrs: { showOverflowTooltip: true } },
  { labelKey: 'common.label.statusEnable', width: '100px', attrs: { align: 'center' }, formatter: row => h(ElTag, { type: row.status === 1 ? 'success' : 'info' }, () => $i18nBundle(row.status === 1 ? 'common.label.statusEnable' : 'common.label.statusDisable')) },
  { labelKey: 'mock.label.default', width: '100px', attrs: { align: 'center' }, formatter: row => h(ElTag, { type: row.isDefault === 1 ? 'primary' : 'info' }, () => $i18nBundle(row.isDefault === 1 ? 'common.label.yes' : 'common.label.no')) },
  { labelKey: 'common.label.createDate', prop: 'createDate', width: '170px', formatter: row => formatDate(row.createDate) }
])
const buttons = computed(() => [
  { labelKey: 'common.label.edit', type: 'primary', click: edit },
  { labelKey: 'common.label.test', type: 'success', click: showTest },
  { labelKey: 'mock.label.aiLogs', type: 'info', click: row => router.push({ name: 'AiLogs', query: { configId: row.id } }) },
  { labelKey: 'common.label.delete', type: 'danger', click: remove }
])
</script>
<template>
  <el-container class="flex-column">
    <common-form
      inline
      :model="searchParam"
      :options="searchOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="searchMethod(1)"
    >
      <template #buttons>
        <el-button
          type="info"
          @click="edit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
      </template>
    </common-form>
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :loading="loading"
      :buttons-column-attrs="{ minWidth: '280px', fixed: 'right' }"
      @page-size-change="searchMethod()"
      @current-page-change="searchMethod()"
    />
    <simple-edit-window
      v-model="item"
      v-model:show-edit-window="editVisible"
      :name="$i18nBundle('mock.label.aiConfig')"
      :form-options="options"
      :save-current-item="save"
      label-width="140px"
    />
    <ai-config-test-window
      v-if="testVisible"
      v-model="testVisible"
      :config="testItem"
    />
  </el-container>
</template>
