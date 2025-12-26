<script setup lang="jsx">
import { isString, isArray, get, isPlainObject } from 'lodash-es'
import { computed, ref } from 'vue'
import { findArrayAndPath } from '@/services/mock/MockCommonService'
const props = defineProps({
  data: {
    type: [Array, String, Object],
    default: () => []
  },
  columns: {
    type: [Array, String],
    default: () => undefined
  }
})

const tableData = computed(() => {
  let data = props.data
  if (isString(props.data)) {
    data = JSON.parse(props.data)
  }
  if (isArray(data)) {
    return data
  }
  if (formModel.value?.dataKey) {
    return get(data, formModel.value?.dataKey)
  }
  const { arrayData } = findArrayAndPath(data)
  if (arrayData) {
    return arrayData
  }
  return [data]
})

const limit = 6
const tableColumns = computed(() => {
  if (isArray(props.columns)) {
    return props.columns
  }
  let columns = formModel.value.columns
  const oneData = tableData.value?.[0]
  if (!columns && oneData) {
    columns = Object.keys(oneData).filter(key => {
      const value = oneData[key]
      return !isPlainObject(value) && !isArray(value)
    }).splice(0, limit).join(',')
  }
  if (columns) {
    return columns.split(/\s*,\s*/).map(column => {
      return {
        label: column, property: column
      }
    })
  }
  return []
})
const formModel = ref({})
const formOptions = computed(() => {
  return [
    { label: 'Data Key', prop: 'dataKey' },
    { label: 'Data Columns', prop: 'columns', defaultValue: isString(props.columns) ? props.columns : '' }
  ]
})
</script>

<template>
  <el-container class="flex-column">
    <common-form
      :options="formOptions"
      :model="formModel"
      :show-buttons="false"
    />
    <common-table
      :data="tableData"
      :columns="tableColumns"
    />
  </el-container>
</template>

<style scoped>

</style>
