<script setup lang="jsx">
import { isString, isArray, get, isPlainObject } from 'lodash-es'
import { computed } from 'vue'
import { checkArrayAndPath } from '@/services/mock/MockCommonService'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { limitStr } from '@/components/utils'
import { checkShowColumn } from '@/utils'
import { $i18nBundle, $i18nConcat } from '@/messages'

defineProps({
  editable: {
    type: Boolean,
    default: false
  },
  xmlContent: {
    type: String,
    default: ''
  }
})

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
        return limitStr(value, 40)
      }
      return value !== undefined ? String(value) : ''
    }
  }
}

const tableColumns = computed(() => {
  const columns = Object.keys(tableData.value?.[0] || {})
  if (columns) {
    return columns.map(column => str2Column(column))
  }
  return []
})
const tableButtons = computed(() => {
  return [{
    labelKey: 'common.label.view',
    type: 'primary',
    click: item => {
      showCodeWindow(JSON.stringify(item), { language: 'json' })
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
      attrs: {
        clearable: true,
        filterable: true,
        allowCreate: true,
        style: { width: '20vw' }
      }
    },
    {
      labelKey: 'mock.label.dataColumns',
      type: 'select',
      prop: 'columns',
      children: tableColumns.value,
      attrs: {
        multiple: true,
        clearable: true,
        filterable: true,
        allowCreate: true,
        style: { width: '30vw' }
      }
    }
  ]
})
defineEmits(['saveTableConfig'])
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
      inline
      :show-buttons="false"
    />
    <el-container class="flex-center margin-bottom2">
      <el-button
        v-if="editable"
        type="primary"
        @click="$emit('saveTableConfig', formModel)"
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
      @row-dblclick="showCodeWindow(JSON.stringify($event), {language: 'json'})"
    />
  </el-container>
</template>

<style scoped>

</style>
