<script setup>
import { ref } from 'vue'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useDefaultPage } from '@/config'
import { defineTableButtons } from '@/components/utils'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  defaultParam: {
    type: Object,
    default: () => ({})
  },
  target: {
    type: Object,
    default: null
  },
  columns: {
    type: Array,
    default: () => []
  },
  searchOptions: {
    type: Array,
    default: () => []
  },
  search: {
    type: Function,
    required: true
  }
})
const showWindow = ref(false)
const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: Object.assign({ page: useDefaultPage(10) }, props.defaultParam),
  saveParam: false,
  searchMethod: props.search
})
const searchHistories = (...args) => {
  return searchMethod(...args)
    .then(() => {
      tableData.value = [props.target, ...tableData.value]
    })
}
const buttons = defineTableButtons([{
  labelKey: 'mock.label.compare',
  type: 'primary',
  buttonIf (data) {
    return !data.current
  },
  click (item) {
    console.log('============compare', item)
  }
}, {
  labelKey: 'mock.label.viewChange',
  type: 'success',
  click (item) {
    console.log('============change', item)
  }
}])
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
        :columns="columns"
        :buttons="buttons"
        :buttons-column-attrs="{minWidth:'200px'}"
        :loading="loading"
        @page-size-change="searchHistories()"
        @current-page-change="searchHistories()"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
