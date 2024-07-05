<script setup>
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { showUserInfo } from '@/utils/DynamicUtils'
import MockUserApi from '@/api/mock/MockUserApi'
import { isAdminUser, $goto, $coreConfirm, isUserAdmin } from '@/utils'
import { $i18nBundle } from '@/messages'

const page = ref(useDefaultPage())

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: page.value },
  searchMethod: MockUserApi.search
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
const buttons = computed(() => {
  return [{
    labelKey: 'common.label.edit',
    type: 'primary',
    click: item => {
      $goto(`/admin/users/edit/${item.id}`)
    },
    enabled: isAdminUser()
  }, {
    labelKey: 'common.label.view',
    type: 'success',
    click: item => {
      showUserInfo(item.id)
    }
  }, {
    labelKey: 'common.label.delete',
    type: 'danger',
    click: item => {
      $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
        .then(() => MockUserApi.deleteById(item.id))
        .then(() => loadUsers())
    },
    buttonIf: item => isAdminUser() && !isUserAdmin(item.userName)
  }]
})
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
    >
      <template #buttons>
        <el-button
          type="info"
          @click="$goto('/admin/users/new')"
        >
          {{ $t('common.label.new') }}
        </el-button>
      </template>
    </common-form>
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
