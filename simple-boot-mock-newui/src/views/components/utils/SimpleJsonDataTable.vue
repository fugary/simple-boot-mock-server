<script setup lang="jsx">
import { isString, isArray, get, isPlainObject } from 'lodash-es'
import { computed } from 'vue'
import { checkArrayAndPath } from '@/services/mock/MockCommonService'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { limitStr } from '@/components/utils'
import { checkShowColumn } from '@/utils'

const vModel = defineModel({ type: String, default: '' })
const formModel = defineModel('tableConfig', { type: Object })

const dataPathConfig = computed(() => checkArrayAndPath(vModel.value))

const tableData = computed(() => {
  let data = dataPathConfig.value.data
  if (isArray(data)) {
    return data
  }
  if (formModel.value?.dataKey) {
    const dataKey = dataPathConfig.value.arrayPath?.find(path => path.join('.') === formModel.value?.dataKey) || formModel.value?.dataKey
    data = get(data, dataKey)
    return isArray(data) ? data : [data]
  }
  const arrayData = dataPathConfig.value.arrayData
  if (arrayData) {
    return arrayData
  }
  return [data]
})

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
      showCodeWindow(JSON.stringify(item))
    }
  }]
})
const formOptions = computed(() => {
  const defaultDataKey = dataPathConfig.value.arrayPath?.map(path => path.join('.'))
    ?.find(pathKey => pathKey === formModel.value.dataKey) ||
      dataPathConfig.value.arrayPath?.[0]?.join('.')
  return [
    {
      labelKey: 'mock.label.dataProperty',
      prop: 'dataKey',
      type: 'select',
      value: defaultDataKey,
      children: dataPathConfig.value.arrayPath?.map(path => path.join('.')).map(value => ({ value, label: value })),
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
</script>

<template>
  <el-container class="flex-column">
    <common-form
      :options="formOptions"
      :model="formModel"
      inline
      :submit-label="$t('common.label.save')"
      @submit-form="$emit('saveTableConfig', formModel)"
    />
    <common-table
      :data="tableData"
      :columns="selectedColumns"
      :buttons="tableButtons"
      :buttons-column-attrs="{fixed:'right'}"
      frontend-paging
      @row-dblclick="showCodeWindow(JSON.stringify($event))"
    />
  </el-container>
</template>

<style scoped>

</style>
