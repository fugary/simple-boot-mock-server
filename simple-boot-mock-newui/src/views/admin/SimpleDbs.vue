<script setup lang="jsx">
import { computed, onMounted } from 'vue'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import MockDbApi from '@/api/mock/MockDbApi'
import { showCodeWindow } from '@/utils/DynamicUtils'

const { tableData, loading, searchMethod: loadApiDbs } = useTableAndSearchForm({
  searchMethod: MockDbApi.search
})
onMounted(() => loadApiDbs())
const columns = computed(() => {
  return [{
    label: 'Type',
    prop: 'type',
    minWidth: '120px'
  }, {
    label: 'Info',
    prop: 'info',
    minWidth: '300px'
  }, {
    label: 'Max',
    prop: 'maxPoolSize'
  }, {
    label: 'Total',
    prop: 'totalCount'
  }, {
    label: 'Active',
    prop: 'activeCount'
  }, {
    label: 'Idle',
    prop: 'idleCount'
  }]
})

const buttons = [{
  labelKey: 'common.label.view',
  type: 'primary',
  click: item => showCodeWindow(JSON.stringify(item), { language: 'json' })
}]
</script>

<template>
  <el-container class="flex-column">
    <div class="simple-dbs-toolbar">
      <el-button
        type="primary"
        @click="loadApiDbs()"
      >
        {{ $t('common.label.refresh') }}
      </el-button>
    </div>
    <common-table
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{width:'100px',fixed:'right'}"
      :loading="loading"
      @row-dblclick="showCodeWindow(JSON.stringify($event), {language: 'json'})"
    />
  </el-container>
</template>

<style scoped>
.simple-dbs-toolbar {
  margin-bottom: 10px;
}
</style>
