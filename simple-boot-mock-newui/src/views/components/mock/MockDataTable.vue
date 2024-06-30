<script setup>
import { onMounted } from 'vue'
import { defineTableColumns } from '@/components/utils'
import MockDataApi from '@/api/mock/MockDataApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'

const props = defineProps({
  requestItem: {
    type: Object,
    required: true
  }
})
const columns = defineTableColumns([])
console.log('========================', props.requestItem)
const { tableData, loading, searchMethod: loadMockData } = useTableAndSearchForm({
  defaultParam: { requestId: props.requestItem.id },
  searchMethod: MockDataApi.search,
  saveParam: false
})
onMounted(() => {
  loadMockData()
})
const buttons = []
</script>

<template>
  <el-container class="flex-column">
    <common-table
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{width:'250px'}"
      :loading="loading"
    />
    {{ tableData }}
  </el-container>
</template>

<style scoped>

</style>
