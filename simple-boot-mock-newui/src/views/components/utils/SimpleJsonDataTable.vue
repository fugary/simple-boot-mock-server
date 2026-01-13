<script setup lang="jsx">
import { isString, isArray, get, isPlainObject, cloneDeep } from 'lodash-es'
import { computed, ref } from 'vue'
import { checkArrayAndPath } from '@/services/mock/MockCommonService'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { limitStr } from '@/components/utils'
import { checkShowColumn, getStyleGrow } from '@/utils'
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
import { useJsonTableConfigStore } from '@/stores/JsonTableConfigStore'
import CommonIcon from '@/components/common-icon/index.vue'
import { ElLink } from 'element-plus'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'

const props = defineProps({
  editable: {
    type: Boolean,
    default: false
  },
  xmlContent: {
    type: String,
    default: ''
  }
})

const jsonTableConfigStore = useJsonTableConfigStore()

const vModel = defineModel({ type: String, default: '' })
const formModel = defineModel('tableConfig', { type: Object, default: () => ({}) })

const dataPathConfig = computed(() => checkArrayAndPath(vModel.value))
const DEFAULT_ROOT = '$ROOT'
const calcTableData = () => {
  let data = dataPathConfig.value.data
  if (isArray(data)) {
    return data
  }
  if (formModel.value?.dataKey) {
    const dataKey = dataPathConfig.value.arrayPath?.find(path => path.join('.') === formModel.value?.dataKey) || formModel.value?.dataKey
    const oriData = data
    data = get(data, dataKey)
    if (!data && DEFAULT_ROOT === dataKey) {
      data = oriData
    }
    return isArray(data) ? data : [data]
  }
  const arrayData = dataPathConfig.value.arrayData
  if (arrayData) {
    return arrayData
  }
  return [data]
}

const tableData = computed(() => calcTableData().filter(obj => !!obj))

const selectedColumns = computed(() => {
  if (formModel.value.columns?.length) {
    const calcColumns = tableColumns.value.filter(column => formModel.value.columns.includes(column.value))
    const existsColumns = calcColumns.map(value => value)
    return calcColumns.concat(formModel.value.columns
      .filter(column => !existsColumns.includes(column)))
      .map(str2Column)
  }
  return tableColumns.value
})

const str2Column = column => {
  return {
    value: column,
    label: column,
    minWidth: '150px',
    enabled: checkShowColumn(tableData.value, column),
    formatter (item) {
      let value = get(item, column)
      if (isPlainObject(value) || isArray(value)) {
        value = JSON.stringify(value)
      }
      if (isString(value)) {
        return limitStr(value, formModel.value?.limit || 40)
      }
      return value !== undefined ? String(value) : ''
    }
  }
}

const tableColumns = computed(() => {
  const columns = new Set()
  tableData.value?.forEach(item => {
    if (item && typeof item === 'object') {
      Object.keys(item).forEach(key => columns.add(key))
    }
  })
  if (columns.size) {
    return Array.from(columns).map(column => str2Column(column))
  }
  return []
})
const tableButtons = computed(() => {
  return [{
    labelKey: 'common.label.view',
    type: 'primary',
    click: item => {
      showCodeWindow(JSON.stringify(item), { language: 'json', viewAsTable: true })
    }
  }]
})
const formOptions = computed(() => {
  const defaultDataKey = dataPathConfig.value.arrayPath?.map(path => path.join('.'))
    ?.find(pathKey => pathKey === formModel.value.dataKey) ||
      dataPathConfig.value.arrayPath?.[0]?.join('.')
  const selectKeys = dataPathConfig.value.arrayPath?.map(path => path.join('.')).map(value => ({ value, label: value }))
  if (selectKeys.length) {
    selectKeys.unshift({ value: DEFAULT_ROOT, label: DEFAULT_ROOT })
  }
  return [
    {
      labelKey: 'mock.label.dataProperty',
      prop: 'dataKey',
      type: 'select',
      value: defaultDataKey,
      children: selectKeys,
      style: getStyleGrow(6),
      attrs: {
        clearable: true,
        filterable: true,
        allowCreate: true
      }
    }, {
      labelKey: 'mock.label.lengthLimit',
      prop: 'limit',
      type: 'input-number',
      value: 40,
      style: getStyleGrow(4),
      attrs: {
        min: 0
      }
    }, {
      labelKey: 'mock.label.dataColumns',
      type: 'select',
      prop: 'columns',
      children: tableColumns.value,
      style: {
        ...getStyleGrow(6),
        alignItems: 'center'
      },
      attrs: {
        multiple: true,
        clearable: true,
        filterable: true,
        allowCreate: true
      }
    }, {
      labelKey: 'mock.label.savedParams',
      type: 'select',
      prop: 'name',
      enabled: !props.editable,
      children: savedConfigs.value?.map(item => {
        return {
          value: item.name,
          slots: {
            default: () => {
              return <>
                <span>{ item.name }</span>
                <ElLink class="float-right margin-top2" underline="never" type="danger"
                        onClick={() => jsonTableConfigStore.deleteTableConfig(item.name)}>
                  <CommonIcon size={18} icon="Delete"/>
                </ElLink>
              </>
            }
          }
        }
      }),
      style: {
        ...getStyleGrow(4),
        alignItems: 'center'
      },
      attrs: {
        clearable: true,
        filterable: true
      },
      change (name) {
        const savedConfig = jsonTableConfigStore.getTableConfig(name)
        if (savedConfig) {
          formModel.value = cloneDeep(savedConfig)
        }
      }
    }
  ]
})
const emit = defineEmits(['saveTableConfig'])
const savedConfigs = computed(() => {
  if (!props.editable) {
    return (Object.values(jsonTableConfigStore.jsonTableConfigs) || []).filter(config => !!config.name)
  }
  return []
})
const saveTableConfig = () => {
  if (props.editable) {
    emit('saveTableConfig', formModel.value)
  } else {
    formSaveModel.value = cloneDeep(formModel.value)
    showSaveWindow.value = true
  }
}
const formSaveModel = ref()
const showSaveWindow = ref(false)
const saveFormOptions = [{
  labelKey: 'common.label.name',
  prop: 'name',
  placeholder: $i18nKey('common.msg.commonInput', 'common.label.name'),
  required: true
}]
const saveStorageTableConfig = async () => {
  jsonTableConfigStore.saveTableConfig(formSaveModel.value)
}
const customPageAttrs = {
  layout: 'total, sizes, prev, pager, next',
  pageSizes: [5, 10, 20, 50],
  background: true
}
</script>

<template>
  <el-container class="flex-column">
    <common-form
      :options="formOptions"
      :model="formModel"
      class="form-edit-width-100"
      class-name="common-form-auto"
      :show-buttons="false"
    />
    <el-container class="flex-center margin-bottom2">
      <el-button
        type="primary"
        @click="saveTableConfig"
      >
        {{ $t('common.label.save') }}
      </el-button>
      <el-button
        type="success"
        @click="showCodeWindow(vModel, {language: 'json'})"
      >
        {{ $i18nBundle('common.label.commonView', [$i18nConcat('JSON', $i18nBundle('common.label.originalContent'))]) }}
      </el-button>
      <el-button
        v-if="xmlContent"
        type="success"
        @click="showCodeWindow(xmlContent, {language: 'xml'})"
      >
        {{ $i18nBundle('common.label.commonView', [$i18nConcat('XML', $i18nBundle('common.label.originalContent'))]) }}
      </el-button>
    </el-container>
    <common-table
      :data="tableData"
      :columns="selectedColumns"
      :buttons="tableButtons"
      :buttons-column-attrs="{fixed:'right'}"
      :frontend-page-size="5"
      :page-attrs="customPageAttrs"
      frontend-paging
      @row-dblclick="showCodeWindow(JSON.stringify($event), {language: 'json', viewAsTable: true})"
    />
    <simple-edit-window
      v-model="formSaveModel"
      v-model:show-edit-window="showSaveWindow"
      width="500px"
      :form-options="saveFormOptions"
      :title="$i18nKey('common.label.commonSave', 'mock.label.params')"
      :save-current-item="saveStorageTableConfig"
    />
  </el-container>
</template>

<style scoped>

</style>
