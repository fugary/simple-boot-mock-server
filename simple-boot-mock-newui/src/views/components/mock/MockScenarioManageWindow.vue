<script setup lang="jsx">
import { computed, ref, watch } from 'vue'
import { ElTag } from 'element-plus'
import { defineFormOptions, defineTableButtons, defineTableColumns } from '@/components/utils'
import { $i18nBundle } from '@/messages'
import { $coreConfirm, getStyleGrow } from '@/utils'
import { useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import MockScenarioApi, { activateScenario, toggleScenarioStatus } from '@/api/mock/MockScenarioApi'

const props = defineProps({
  groupItem: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['updated'])

const show = defineModel('show', {
  type: Boolean,
  default: false
})

const scenarios = ref([])
const showEditWindow = ref(false)
const currentScenario = ref({})

const loadScenarios = () => {
  if (!props.groupItem?.id) {
    scenarios.value = []
    return Promise.resolve([])
  }
  return MockScenarioApi.search({ groupId: props.groupItem.id }).then(data => {
    scenarios.value = data?.resultData || []
    return scenarios.value
  })
}

watch(show, val => {
  if (val) {
    loadScenarios()
  }
})

const listData = computed(() => {
  const defaultScenario = {
    id: 0,
    scenarioCode: null,
    scenarioName: $i18nBundle('mock.label.defaultScenario'),
    status: 1,
    description: '',
    _default: true
  }
  return [defaultScenario, ...scenarios.value]
})

const isActive = (item) => (props.groupItem?.activeScenarioCode || null) === (item?.scenarioCode || null)

const onAddScenario = () => {
  currentScenario.value = {
    groupId: props.groupItem?.id,
    scenarioName: '',
    description: '',
    status: 1,
    copyFromScenarioCode: undefined
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
  }, { loading: true }).then(data => {
    return loadScenarios().then(() => {
      emit('updated')
      return data
    })
  }).catch(err => Promise.reject(err))
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
    emit('updated')
  })
}

const onToggleScenarioStatus = (item) => {
  if (item._default) {
    return Promise.resolve(false)
  }
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
      clickToToggle={!item._default}
      onToggleValue={() => onToggleScenarioStatus(item)}
    />
  }
}, {
  labelKey: 'common.label.description',
  property: 'description',
  minWidth: '240px'
}]))

const buttons = computed(() => defineTableButtons([{
  tooltip: $i18nBundle('mock.label.activateScenario'),
  icon: 'Flag',
  round: true,
  type: 'primary',
  click: item => onActivateScenario(item),
  buttonIf: item => !isActive(item)
}, {
  tooltip: $i18nBundle('common.label.edit'),
  icon: 'Edit',
  round: true,
  type: 'primary',
  click: item => onEditScenario(item),
  buttonIf: item => !item._default
}, {
  tooltip: $i18nBundle('common.label.delete'),
  icon: 'DeleteFilled',
  round: true,
  type: 'danger',
  click: item => onDeleteScenario(item),
  buttonIf: item => !item._default
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
      children: listData.value.map(item => ({
        label: item.scenarioName || item.scenarioCode,
        value: item.scenarioCode || '' // '' for default scenario
      })),
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
      <el-container class="margin-bottom2">
        <el-button
          type="info"
          @click="onAddScenario"
        >
          {{ $t('common.label.new') }}
        </el-button>
      </el-container>
      <common-table
        :data="listData"
        :columns="columns"
        :buttons="buttons"
        buttons-slot="buttons"
        :buttons-column-attrs="{ width: '180px' }"
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

<style scoped>

</style>
