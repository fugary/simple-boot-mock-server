<script setup lang="jsx">
import { computed, onMounted } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { showUserInfo } from '@/utils/DynamicUtils'
import MockUserApi from '@/api/mock/MockUserApi'
import { isAdminUser, $goto, $coreConfirm, isUserAdmin } from '@/utils'
import { $i18nBundle } from '@/messages'
import { useSearchStatus } from '@/consts/GlobalConstants'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: useDefaultPage() },
  searchMethod: MockUserApi.search
})
const loadUsers = (pageNumber) => searchMethod(pageNumber)

onMounted(() => {
  loadUsers()
})

const saveUser = (user) => {
  MockUserApi.saveOrUpdate(user, { loading: true })
    .then(() => loadUsers())
}

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = [{
  labelKey: 'common.label.username',
  property: 'userName'
}, {
  labelKey: 'common.label.nickName',
  property: 'nickName'
}, {
  labelKey: 'common.label.status',
  formatter (data) {
    const clickToToggle = !isUserAdmin(data.userName)
    const enableOrDisable = (status) => {
      if (clickToToggle) {
        return saveUser({ ...data, status })
      }
    }
    return <DelFlagTag v-model={data.status} clickToToggle={clickToToggle}
                       onToggleValue={enableOrDisable}/>
  },
  attrs: {
    align: 'center'
  }
}, {
  labelKey: 'common.label.email',
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
      labelKey: 'common.label.keywords',
      prop: 'keyword'
    },
    useSearchStatus({ change () { loadUsers(1) } })
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
          v-if="isAdminUser()"
          type="info"
          @click="$goto('/admin/users/new')"
        >
          {{ $t('common.label.new') }}
        </el-button>
      </template>
    </common-form>
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      buttons-slot="buttons"
      :buttons-column-attrs="{width:'250px'}"
      :loading="loading"
      @page-size-change="loadUsers()"
      @current-page-change="loadUsers()"
      @row-dblclick="isAdminUser()?$goto(`/admin/users/edit/${$event.id}`):showUserInfo($event.id)"
    />
  </el-container>
</template>

<style scoped>

</style>
