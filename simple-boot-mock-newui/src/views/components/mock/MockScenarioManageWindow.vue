<script setup lang="jsx">
import { computed, ref, watch } from 'vue'
import { ElTag } from 'element-plus'
import { defineFormOptions, defineTableButtons, defineTableColumns } from '@/components/utils'
import { $i18nBundle } from '@/messages'
import { $coreConfirm, getStyleGrow } from '@/utils'
import { useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import MockScenarioApi, { activateScenario, toggleScenarioStatus } from '@/api/mock/MockScenarioApi'

const props = defineProps({
  groupItem: {
    type: Object,
    default: () => ({})
  },
  searchParam: {
    type: Object,
    default: () => ({})
  },
  writable: {
    type: Boolean,
    default: true
  },
  deletable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['updated'])

const show = defineModel('show', {
  type: Boolean,
  default: false
})

const { tableData: scenarios, loading, searchParam: scenarioQuery, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: useDefaultPage() },
  saveParam: false,
  searchMethod: MockScenarioApi.search
})
const copyScenarios = ref([])
const showEditWindow = ref(false)
const currentScenario = ref({})

// 默认场景是虚拟记录，固定在首行，不计入后端分页总数。
const listData = computed(() => [{
  id: 0,
  scenarioCode: null,
  scenarioName: $i18nBundle('mock.label.defaultScenario'),
  status: 1,
  description: '',
  _default: true
}, ...scenarios.value])

const loadScenarios = async (pageNumber) => {
  const data = await searchMethod(pageNumber, { groupId: props.groupItem?.id })
  if (data?.success && data.page) {
    const lastPage = Math.max(data.page.pageCount, 1)
    if (data.page.pageNumber > lastPage) return loadScenarios(lastPage)
  }
  return data
}

const resetSearch = () => {
  Object.assign(scenarioQuery.value, { keyword: '', status: null })
  return loadScenarios(1)
}

const searchOptions = computed(() => defineFormOptions([{
  labelKey: 'common.label.keywords',
  prop: 'keyword',
  placeholder: $i18nBundle('mock.label.scenarioSearchPlaceholder'),
  attrs: { style: 'width: 260px', onClear: () => loadScenarios(1) }
}, useSearchStatus({
  change: () => loadScenarios(1),
  attrs: { style: 'width: 130px' }
})]))

watch([show, () => props.groupItem?.id], ([visible]) => {
  if (visible) {
    scenarios.value = []
    resetSearch()
  }
})

const isActive = (item) => (props.groupItem?.activeScenarioCode || null) === (item?.scenarioCode || null)

const onAddScenario = async () => {
  const groupId = props.groupItem?.id
  if (!groupId) return
  const data = await MockScenarioApi.search({ groupId }, { loading: true })
  if (!show.value || groupId !== props.groupItem?.id) return
  copyScenarios.value = data.resultData || []
  currentScenario.value = {
    groupId,
    scenarioName: '',
    description: '',
    status: 1,
    copyFromScenarioCode: props.searchParam?.scenarioCode === undefined ? (props.groupItem?.activeScenarioCode || '') : props.searchParam?.scenarioCode
  }
  showEditWindow.value = true
}

const onEditScenario = (item) => {
  currentScenario.value = { ...item }
  showEditWindow.value = true
}

const saveCurrentScenario = (item) => {
  const scenarioName = item?.scenarioName?.trim()
  if (!scenarioName) {
    return Promise.reject(new Error($i18nBundle('common.msg.commonInput', $i18nBundle('mock.label.scenarioName'))))
  }
  return MockScenarioApi.saveOrUpdate({
    ...item,
    groupId: props.groupItem?.id,
    scenarioName
  }, { loading: true }).then(async data => {
    if (!item.id) {
      await resetSearch()
      if (scenarioQuery.value.page.pageCount > 1) {
        await loadScenarios(scenarioQuery.value.page.pageCount)
      }
    } else {
      await loadScenarios()
    }
    emit('updated', data.resultData || item)
    return data
  })
}

const onDeleteScenario = (item) => {
  return $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockScenarioApi.deleteById(item.id, { loading: true }))
    .then(() => loadScenarios())
    .then(() => emit('updated'))
}

const onActivateScenario = (item) => {
  return activateScenario({
    groupId: props.groupItem?.id,
    scenarioCode: item?.scenarioCode || null
  }, { loading: true }).then(() => {
    emit('updated', item)
  })
}

const onToggleScenarioStatus = (item) => {
  return toggleScenarioStatus(item.id, { loading: true }).then(() => {
    return loadScenarios().then(() => {
      emit('updated')
      return true
    })
  })
}

const columns = computed(() => defineTableColumns([{
  labelKey: 'mock.label.scenarioName',
  minWidth: '220px',
  formatter (item) {
    return <>
      {item.scenarioName}
      {isActive(item) ? <ElTag type="success" size="small" class="margin-left1">{$i18nBundle('mock.label.activeScenario')}</ElTag> : ''}
    </>
  }
}, {
  labelKey: 'common.label.status',
  width: '150px',
  formatter (item) {
    return <DelFlagTag
      v-model={item.status}
      clickToToggle={!item._default && props.writable}
      onToggleValue={() => onToggleScenarioStatus(item)}
    />
  }
}, {
  labelKey: 'common.label.description',
  property: 'description',
  minWidth: '240px'
}]))

const buttons = computed(() => defineTableButtons([{
  tooltip: $i18nBundle('common.label.edit'),
  icon: 'Edit',
  round: true,
  type: 'primary',
  click: item => onEditScenario(item),
  buttonIf: item => !item._default && props.writable
}, {
  tooltip: $i18nBundle('mock.label.activateScenario'),
  icon: 'Flag',
  round: true,
  type: 'primary',
  click: item => onActivateScenario(item),
  buttonIf: item => !isActive(item) && !!item.status && props.writable
}, {
  tooltip: $i18nBundle('common.label.delete'),
  icon: 'DeleteFilled',
  round: true,
  type: 'danger',
  click: item => onDeleteScenario(item),
  buttonIf: item => !item._default && props.deletable
}]))

const formOptions = computed(() => {
  const options = [{
    labelKey: 'mock.label.scenarioName',
    prop: 'scenarioName',
    required: true,
    style: getStyleGrow(10)
  }]

  if (!currentScenario.value?.id) {
    options.push({
      labelKey: 'mock.label.copyFromScenario',
      prop: 'copyFromScenarioCode',
      type: 'select',
      children: [{ label: $i18nBundle('mock.label.defaultScenario'), value: '' }, ...copyScenarios.value.map(item => ({
        label: item.scenarioName || item.scenarioCode,
        value: item.scenarioCode
      }))],
      attrs: { filterable: true },
      style: getStyleGrow(10)
    })
  }

  options.push({
    ...useFormStatus(),
    style: getStyleGrow(10)
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    },
    style: getStyleGrow(10)
  })
  return defineFormOptions(options)
})

</script>

<template>
  <common-window
    v-model="show"
    :title="$t('mock.label.scenarioManage')"
    width="1000px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    append-to-body
  >
    <el-container class="flex-column">
      <common-form
        inline
        :model="scenarioQuery"
        :options="searchOptions"
        :submit-label="$t('common.label.search')"
        @submit-form="loadScenarios(1)"
      >
        <template #buttons>
          <el-button
            type="default"
            @click="resetSearch"
          >
            {{ $t('common.label.reset') }}
          </el-button>
          <el-button
            v-if="writable"
            v-common-tooltip="$i18nKey('common.label.commonAdd', 'mock.label.scenario')"
            type="info"
            @click="onAddScenario"
          >
            {{ $t('common.label.new') }}
          </el-button>
        </template>
      </common-form>
      <common-table
        v-model:page="scenarioQuery.page"
        :data="listData"
        :loading="loading"
        :columns="columns"
        :buttons="buttons"
        buttons-slot="buttons"
        :buttons-column-attrs="{ width: '180px' }"
        max-height="55vh"
        row-key="id"
        @page-size-change="loadScenarios(1)"
        @current-page-change="loadScenarios()"
      />
    </el-container>
  </common-window>
  <simple-edit-window
    v-model="currentScenario"
    v-model:show-edit-window="showEditWindow"
    :form-options="formOptions"
    :name="$t('mock.label.scenario')"
    :save-current-item="saveCurrentScenario"
    label-width="130px"
    inline-auto-mode
  />
</template>
