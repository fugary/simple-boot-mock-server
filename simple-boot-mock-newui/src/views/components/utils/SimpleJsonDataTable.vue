<script setup lang="jsx">
import { isString, isArray, get, isPlainObject } from 'lodash-es'
import { computed, ref } from 'vue'
import { checkArrayAndPath } from '@/services/mock/MockCommonService'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { limitStr } from '@/components/utils'
import { checkShowColumn } from '@/utils'

const props = defineProps({
  data: {
    type: [Array, String, Object],
    default: () => []
  },
  columns: {
    type: Array,
    default: () => undefined
  }
})

const dataPathConfig = computed(() => checkArrayAndPath(props.data))

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
    return tableColumns.value.filter(column => formModel.value.columns.includes(column.value))
  }
  return tableColumns.value
})

const tableColumns = computed(() => {
  if (isArray(props.columns)) {
    return props.columns
  }
  const columns = Object.keys(tableData.value?.[0] || {})
  if (columns) {
    return columns.map(column => {
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
          return value
        }
      }
    })
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
const formModel = ref({})
const formOptions = computed(() => {
  const defaultDataKey = dataPathConfig.value.arrayPath?.[0]?.join('.')
  return [
    {
      labelKey: 'mock.label.dataProperty',
      prop: 'dataKey',
      type: 'autocomplete',
      value: defaultDataKey,
      attrs: {
        fetchSuggestions: (queryString, cb) => {
          const dataList = dataPathConfig.value.arrayPath?.map(path => path.join('.'))
            .filter(item => item?.toLowerCase().includes(queryString?.toLowerCase()))
            .map(value => ({ value }))
          cb(dataList)
        }
      }
    },
    {
      labelKey: 'mock.label.dataColumns',
      type: 'select',
      prop: 'columns',
      children: tableColumns.value,
      attrs: {
        multiple: true,
        style: { width: '40vw' }
      }
    }
  ]
})
</script>

<template>
  <el-container class="flex-column">
    <common-form
      :options="formOptions"
      :model="formModel"
      :show-buttons="false"
      inline
    />
    <common-table
      :data="tableData"
      :columns="selectedColumns"
      :buttons="tableButtons"
      :buttons-column-attrs="{fixed:'right'}"
      frontend-paging
    />
  </el-container>
</template>

<style scoped>

</style>
