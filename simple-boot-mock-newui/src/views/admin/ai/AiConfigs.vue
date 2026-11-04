<script setup lang="jsx">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { AiConfigApi } from '@/api/mock/AiApi'
import { $i18nBundle, $i18nKey } from '@/messages'
import { $coreConfirm, formatDate, isAdminUser } from '@/utils'
import { useDefaultPage } from '@/config'
import AiConfigTestWindow from './AiConfigTestWindow.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { ElTag } from 'element-plus'
import { useAiModels } from '@/hooks/AiHooks'
import { useSearchStatus } from '@/consts/GlobalConstants'
import { AI_PROVIDERS } from '@/consts/AiConstants'
import { omit } from 'lodash-es'
import { useAiHelpStore } from '@/stores/AiHelpStore'

const router = useRouter()
const modelCache = useAiHelpStore().modelCache
const isAdmin = computed(() => isAdminUser())
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
const testDraft = ref(false)
let editSequence = 0
const edit = async (row, copy = false) => {
  const sequence = ++editSequence
  if (row?.id) {
    const res = await AiConfigApi.getById(row.id)
    if (!res.success || sequence !== editSequence) return
    item.value = { ...res.resultData }
    modelCache.remember(item.value)
    if (copy) {
      item.value = {
        ...omit(item.value, ['id', 'version', 'userName', 'creator', 'createDate', 'modifier', 'modifyDate', 'modifyFrom']),
        configName: `${item.value.configName || ''}-copy`,
        isDefault: 0
      }
    }
  } else {
    item.value = { provider: 'openai', apiKey: '', status: 1, isDefault: 0 }
  }
  editVisible.value = true
}
const save = async data => {
  const cachedModels = modelCache.get(data)
  const res = await AiConfigApi.saveOrUpdate(data)
  if (!res.success) {
    throw new Error(res.message)
  }
  modelCache.invalidate(res.resultData.id)
  modelCache.remember({ ...res.resultData, apiKey: data.apiKey })
  if (cachedModels.length) modelCache.set(res.resultData, true, cachedModels)
  await searchMethod()
}
const toggling = ref(false)
const toggleConfig = async (row, prop, value) => {
  if (toggling.value) return
  toggling.value = true
  try {
    await save({ ...row, [prop]: Number(value) })
  } catch {
    // Save errors are reported by the shared HTTP handler; keep the displayed value unchanged.
  } finally {
    toggling.value = false
  }
}
const remove = async row => {
  await $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
  const res = await AiConfigApi.deleteById(row.id)
  if (res.success) {
    modelCache.invalidate(row.id)
    await searchMethod(tableData.value.length === 1 ? Math.max(1, searchParam.value.page.pageNumber - 1) : undefined)
  }
}
const showTest = (row, draft = false) => {
  testDraft.value = draft
  testItem.value = { ...row }
  testVisible.value = true
}
const editWindowAttrs = computed(() => isAdmin.value
  ? {
      buttons: [{
        labelKey: 'common.label.test',
        type: 'success',
        click: ({ form }) => {
          form.validate(valid => {
            if (valid) showTest(item.value, true)
          })
          return false
        }
      }]
    }
  : { title: $i18nKey('common.label.commonView', $i18nBundle('mock.label.aiConfig')) })

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
    children: AI_PROVIDERS.map(({ labelKey, optionLabelKey, value }) => ({ labelKey: optionLabelKey || labelKey, value })),
    change: () => searchMethod(1)
  },
  { labelKey: 'mock.label.aiModelName', prop: 'defaultModel' },
  useSearchStatus({ change: () => searchMethod(1) })
])
const options = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'configName', required: true },
  { labelKey: 'mock.label.aiProvider', prop: 'provider', type: 'select', required: true, children: AI_PROVIDERS.map(({ labelKey, optionLabelKey, value }) => ({ labelKey: optionLabelKey || labelKey, value })) },
  { labelKey: 'mock.label.aiBaseUrl', prop: 'baseUrl', type: 'autocomplete', required: true, placeholder: $i18nBundle('mock.label.aiBaseUrlHint'), attrs: { fetchSuggestions: baseUrlSuggestions } },
  { labelKey: 'mock.label.aiApiKey', prop: 'apiKey', required: !item.value.id, placeholder: $i18nBundle('mock.label.aiKeyHint'), attrs: { type: 'password', showPassword: true, autocomplete: 'new-password' } },
  modelOption.value,
  { labelKey: 'common.label.statusEnable', prop: 'status', type: 'switch', attrs: { activeValue: 1, inactiveValue: 0 } },
  { labelKey: 'mock.label.aiDefault', prop: 'isDefault', type: 'switch', attrs: { activeValue: 1, inactiveValue: 0 } }
])
const viewOptions = computed(() => isAdmin.value
  ? options.value
  : options.value
    .filter(option => option.prop !== 'apiKey')
    .map(option => ({ ...option, disabled: true, required: false, tooltip: undefined, tooltipFunc: undefined })))
const columns = computed(() => [
  { labelKey: 'mock.label.aiConfigName', prop: 'configName', minWidth: '160px' },
  {
    labelKey: 'mock.label.aiProvider',
    prop: 'provider',
    minWidth: '190px',
    attrs: { align: 'center' },
    formatter: row => {
      const provider = AI_PROVIDERS.find(item => item.value === row.provider)
      return <ElTag type={provider?.tagType || 'info'}>{provider ? $i18nBundle(provider.labelKey) : row.provider}</ElTag>
    }
  },
  { labelKey: 'mock.label.aiModel', prop: 'defaultModel', minWidth: '180px', attrs: { showOverflowTooltip: true } },
  { labelKey: 'mock.label.aiBaseUrl', prop: 'baseUrl', minWidth: '220px', attrs: { showOverflowTooltip: true } },
  {
    labelKey: 'common.label.status',
    prop: 'status',
    width: '100px',
    attrs: { align: 'center' },
    formatter: row => <DelFlagTag v-model={row.status}
                                clickToToggle={isAdmin.value && !toggling.value}
                                onToggleValue={value => toggleConfig(row, 'status', value)} />
  },
  {
    labelKey: 'mock.label.setDefault',
    prop: 'isDefault',
    width: '100px',
    attrs: { align: 'center' },
    formatter: row => <DelFlagTag v-model={row.isDefault}
                                clickToToggle={isAdmin.value && !toggling.value}
                                typeConfig={{ 1: 'primary', 0: 'info' }}
                                valueConfig={{
                                  1: $i18nBundle('mock.label.setDefault'),
                                  0: $i18nBundle('mock.label.cancelDefault')
                                }}
                                onToggleValue={value => toggleConfig(row, 'isDefault', value)} />
  },
  { labelKey: 'common.label.createDate', prop: 'createDate', width: '170px', formatter: row => formatDate(row.createDate) }
])
const buttons = computed(() => [
  { labelKey: isAdmin.value ? 'common.label.edit' : 'common.label.view', type: 'primary', click: row => edit(row) },
  { labelKey: 'common.label.test', type: 'success', buttonIf: row => row.status === 1, click: row => showTest(row) },
  { labelKey: 'common.label.delete', type: 'danger', buttonIf: () => isAdmin.value, click: remove }
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
          type="success"
          @click="showTest()"
        >
          {{ $t('common.label.test') }}
        </el-button>
        <el-button
          v-if="isAdmin"
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
      buttons-slot="moreActions"
      @page-size-change="searchMethod()"
      @current-page-change="searchMethod()"
    >
      <template #moreActions="{ row }">
        <el-dropdown class="more-actions">
          <el-button size="small">
            {{ $t('common.label.more') }}
            <common-icon
              icon="ArrowDown"
              class="margin-left1"
            />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-if="isAdmin"
                @click="edit(row, true)"
              >
                {{ $t('common.label.copy') }}
              </el-dropdown-item>
              <el-dropdown-item @click="router.push({ name: 'AiLogs', query: { configId: row.id } })">
                {{ $t('mock.label.aiLogs') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </common-table>
    <simple-edit-window
      v-model="item"
      v-model:show-edit-window="editVisible"
      :name="$i18nBundle('mock.label.aiConfig')"
      :form-options="viewOptions"
      :editable="isAdmin"
      :window-attrs="editWindowAttrs"
      :save-current-item="save"
      label-width="140px"
    />
    <ai-config-test-window
      v-if="testVisible"
      v-model="testVisible"
      :config="testItem"
      :draft="testDraft"
    />
  </el-container>
</template>

<style scoped>
.more-actions {
  margin-left: 12px;
  vertical-align: middle;
}

.more-actions :deep(.el-button) {
  outline: none;
}
</style>
