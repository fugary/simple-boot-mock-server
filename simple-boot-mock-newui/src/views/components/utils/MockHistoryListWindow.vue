<script setup>
import { ref, computed } from 'vue'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useDefaultPage } from '@/config'
import { defineTableButtons } from '@/components/utils'
import { $coreConfirm, checkShowColumn } from '@/utils'
import { $i18nBundle } from '@/messages'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  defaultParam: {
    type: Object,
    default: () => ({})
  },
  columns: {
    type: Array,
    default: () => []
  },
  searchOptions: {
    type: Array,
    default: () => []
  },
  searchFunc: {
    type: Function,
    required: true
  },
  compareFunc: {
    type: Function,
    required: true
  },
  recoverFunc: {
    type: Function,
    default: null
  }
})
const showWindow = ref(false)
const target = ref({})
const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: Object.assign({ page: useDefaultPage(10) }, props.defaultParam),
  saveParam: false,
  searchMethod: props.searchFunc
})
const searchHistories = (...args) => {
  return searchMethod(...args)
    .then((data) => {
      target.value = data.infos?.current || {}
      target.value.current = !target.value.modifyFrom
      tableData.value = [target.value, ...tableData.value]
    })
}
const buttons = defineTableButtons([{
  labelKey: 'mock.label.compare',
  type: 'primary',
  buttonIf (data) {
    return !data.current
  },
  click (item) {
    props.compareFunc?.(item, target.value)
  }
}, {
  labelKey: 'mock.label.recover',
  type: 'warning',
  buttonIf (data) {
    return !data.current && props.recoverFunc
  },
  click (item) {
    $coreConfirm($i18nBundle('mock.msg.recoverFromHistory'))
      .then(() => props.recoverFunc?.(item))
      .then(() => searchHistories())
  }
}, {
  labelKey: 'mock.label.viewChange',
  type: 'success',
  click (item) {
    props.compareFunc?.(item, target.value, true)
  }
}])
const calcColumns = computed(() => {
  return props.columns.map((column) => {
    let enabled = column.enabled ?? true
    if (column.property) {
      enabled = checkShowColumn(tableData.value, column.property)
    }
    return {
      ...column,
      enabled
    }
  })
})
const showHistoryListWindow = () => {
  searchHistories(1)
  showWindow.value = true
}
defineExpose({
  showHistoryListWindow
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="1100px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="title||$t('mock.label.modifyHistory')"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <el-container class="flex-column">
      <common-form
        v-if="searchOptions.length"
        inline
        :model="searchParam"
        :options="searchOptions"
        :submit-label="$t('common.label.search')"
        @submit-form="searchHistories()"
      />
      <common-table
        v-model:page="searchParam.page"
        :data="tableData"
        :columns="calcColumns"
        :buttons="buttons"
        :buttons-column-attrs="{minWidth:'220px',fixed:'right'}"
        :loading="loading"
        @page-size-change="searchHistories()"
        @current-page-change="searchHistories()"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
