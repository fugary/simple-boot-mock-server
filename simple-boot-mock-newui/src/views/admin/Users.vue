<script setup>
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useResourceApi } from '@/hooks/ApiHooks'
import { showUserInfo } from '@/utils/DynamicUtils'

const page = ref(useDefaultPage())

const { search } = useResourceApi('/admin/users')

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: page.value },
  searchMethod: search
})
const loadUsers = (pageNumber) => searchMethod(pageNumber)

onMounted(() => {
  loadUsers()
})

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = [{
  label: '用户名',
  property: 'userName'
}, {
  label: '呢称',
  property: 'nickName'
}, {
  label: '邮箱',
  property: 'userEmail'
}]
const buttons = ref([{
  labelKey: 'common.label.view',
  type: 'primary',
  click: item => {
    showUserInfo(item.id)
  },
  buttonIf (item) {
    return !!item.id
  }
}])
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [
    {
      label: '关键字',
      prop: 'keyword'
    }
  ]
})
const doSearch = form => {
  console.info('=================searchParam', form, searchParam.value)
  loadUsers()
}

</script>

<template>
  <el-container
    class="flex-column"
  >
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="doSearch"
    />
    <common-table
      v-model:page="page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      buttons-slot="buttons"
      :buttons-column-attrs="{width:'250px'}"
      :loading="loading"
      @page-size-change="loadUsers()"
      @current-page-change="loadUsers()"
    />
  </el-container>
</template>

<style scoped>

</style>
