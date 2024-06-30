<script setup lang="jsx">
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useResourceApi } from '@/hooks/ApiHooks'
import { defineTableButtons } from '@/components/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'

const page = ref(useDefaultPage())

const { search } = useResourceApi('/admin/groups')

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: page.value },
  searchMethod: search
})
const loadMockGroups = (pageNumber) => searchMethod(pageNumber)

onMounted(() => {
  loadMockGroups()
})

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = [{
  label: '分组名称',
  property: 'groupName'
}, {
  label: '路径ID',
  property: 'groupPath'
}, {
  label: '代理路径',
  property: 'proxyUrl'
}, {
  label: '描述',
  property: 'description'
}, {
  labelKey: 'common.label.status',
  property: 'status',
  formatter (data) {
    return <DelFlagTag v-model={data.status} />
  }
}, {
  labelKey: 'common.label.createDate',
  property: 'createDate',
  dateFormat: 'YYYY-MM-DD HH:mm:ss'
}]
const buttons = defineTableButtons([{
  labelKey: 'common.label.edit',
  type: 'primary',
  click: item => {
    // showUserInfo(item.id)
    console.log('======================', item)
    // 查看
  }
}, {
  labelKey: 'common.label.config',
  type: 'success',
  click: item => {
    // showUserInfo(item.id)
    console.log('======================', item)
    // 查看
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  click: item => {
    // showUserInfo(item.id)
    console.log('======================', item)
    // 查看
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
  loadMockGroups()
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
