<script setup lang="jsx">
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import MockGroupApi from '@/api/mock/MockGroupApi'
import { $coreConfirm, $goto } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'

const { search, getById, deleteById, saveOrUpdate } = MockGroupApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage() },
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
  property: 'groupPath',
  minWidth: '150px'
}, {
  label: '代理路径',
  property: 'proxyUrl',
  minWidth: '150px'
}, {
  label: '描述',
  property: 'description'
}, {
  labelKey: 'common.label.status',
  property: 'status',
  formatter (data) {
    return <DelFlagTag v-model={data.status}/>
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
    newOrEdit(item.id)
  }
}, {
  labelKey: 'common.label.config',
  type: 'success',
  click: item => {
    $goto(`/mock/groups/${item.id}`)
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  click: item => deleteGroup(item)
}])
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [
    {
      labelKey: 'common.label.keywords',
      prop: 'keyword'
    }
  ]
})

const deleteGroup = group => {
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [group.groupName]))
    .then(() => deleteById(group.id))
    .then(() => loadMockGroups())
}

const showEditWindow = ref(false)
const currentGroup = ref()
const newOrEdit = async id => {
  if (id) {
    await getById(id).then(data => {
      data.resultData && (currentGroup.value = data.resultData)
    })
  } else {
    currentGroup.value = {
      status: 1
    }
  }
  showEditWindow.value = true
}
const editFormOptions = defineFormOptions([{
  label: '分组名称',
  prop: 'groupName',
  required: true
}, {
  label: '代理地址',
  prop: 'proxyUrl'
}, useFormStatus(), {
  labelKey: '备注信息',
  prop: 'description'
}])
const saveGroupItem = (item) => {
  saveOrUpdate(item).then(() => loadMockGroups())
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
      @submit-form="loadMockGroups"
    >
      <template #buttons>
        <el-button
          type="info"
          @click="newOrEdit()"
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
      :buttons-column-attrs="{width:'250px'}"
      :loading="loading"
      @page-size-change="loadMockGroups()"
      @current-page-change="loadMockGroups()"
    />
    <simple-edit-window
      v-model="currentGroup"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      name="Mock分组"
      :save-current-item="saveGroupItem"
    />
  </el-container>
</template>

<style scoped>

</style>
