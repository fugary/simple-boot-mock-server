<script setup>
import { computed, ref, watch } from 'vue'
import MockScenarioApi, { activateScenario, toggleScenarioStatus } from '@/api/mock/MockScenarioApi'
import { $coreConfirm } from '@/utils'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'

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

const showEdit = ref(false)
const scenarios = ref([])
const current = ref({})

const defaultScenarioCode = null

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
    scenarioName: $i18nBundle('mock.label.defaultScenario'),
    scenarioCode: defaultScenarioCode,
    status: 1,
    _default: true
  }
  return [defaultScenario, ...scenarios.value]
})

const isActive = (item) => {
  return (props.groupItem?.activeScenarioCode || null) === (item?.scenarioCode || null)
}

const onAdd = () => {
  current.value = {
    groupId: props.groupItem?.id,
    scenarioName: '',
    description: '',
    status: 1
  }
  showEdit.value = true
}

const onEdit = (item) => {
  current.value = { ...item }
  showEdit.value = true
}

const onSave = () => {
  if (!current.value?.scenarioName?.trim()) {
    ElMessage.error($i18nBundle('common.msg.commonInput', $i18nBundle('mock.label.scenarioName')))
    return
  }
  MockScenarioApi.saveOrUpdate({
    ...current.value,
    groupId: props.groupItem?.id,
    scenarioName: current.value.scenarioName.trim()
  }).then(data => {
    if (data?.success) {
      showEdit.value = false
      loadScenarios().then(() => emit('updated'))
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
    }
  })
}

const onDelete = (item) => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm')).then(() => {
    return MockScenarioApi.removeById(item.id)
  }).then(() => {
    loadScenarios().then(() => emit('updated'))
  })
}

const onActivate = (item) => {
  activateScenario({
    groupId: props.groupItem?.id,
    scenarioCode: item?.scenarioCode || null
  }).then(data => {
    if (data?.success) {
      emit('updated')
      ElMessage.success($i18nBundle('common.msg.saveSuccess'))
    }
  })
}

const onToggleStatus = (item) => {
  toggleScenarioStatus(item.id).then(data => {
    if (data?.success) {
      loadScenarios().then(() => emit('updated'))
    }
  })
}
</script>

<template>
  <common-window
    v-model="show"
    :title="$t('mock.label.scenarioManage')"
    width="760px"
    :show-ok="false"
    append-to-body
  >
    <div class="margin-bottom2">
      <el-button
        type="primary"
        size="small"
        @click="onAdd"
      >
        {{ $t('common.label.new') }}
      </el-button>
    </div>
    <el-table
      :data="listData"
      border
      size="small"
    >
      <el-table-column
        :label="$t('mock.label.scenarioName')"
        min-width="180"
      >
        <template #default="{ row }">
          {{ row.scenarioName }}
          <el-tag
            v-if="isActive(row)"
            class="margin-left1"
            type="success"
            size="small"
          >
            {{ $t('mock.label.activeScenario') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('common.label.status')"
        width="120"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.status===1?'success':'info'"
            size="small"
          >
            {{ row.status===1?$t('common.label.statusEnabled'):$t('common.label.statusDisabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('common.label.description')"
        min-width="220"
      >
        <template #default="{ row }">
          {{ row.description }}
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('common.label.operation')"
        width="230"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="onActivate(row)"
          >
            {{ $t('mock.label.activateScenario') }}
          </el-button>
          <el-button
            v-if="!row._default"
            link
            type="primary"
            @click="onEdit(row)"
          >
            {{ $t('common.label.edit') }}
          </el-button>
          <el-button
            v-if="!row._default"
            link
            type="warning"
            @click="onToggleStatus(row)"
          >
            {{ row.status===1?$t('common.label.statusDisable'):$t('common.label.statusEnable') }}
          </el-button>
          <el-button
            v-if="!row._default"
            link
            type="danger"
            @click="onDelete(row)"
          >
            {{ $t('common.label.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </common-window>

  <common-window
    v-model="showEdit"
    :title="current?.id?$t('common.label.edit'):$t('common.label.new')"
    width="520px"
    append-to-body
    :ok-click="onSave"
  >
    <common-form
      :model="current"
      :show-buttons="false"
      :options="[
        { labelKey: 'mock.label.scenarioName', prop: 'scenarioName', required: true },
        { labelKey: 'common.label.status', prop: 'status', type: 'select', children: [{value: 1, label: $t('common.label.statusEnabled')}, {value: 0, label: $t('common.label.statusDisabled')}] },
        { labelKey: 'common.label.description', prop: 'description', attrs: { type: 'textarea' } }
      ]"
    />
  </common-window>
</template>

<style scoped>

</style>
