<script setup lang="jsx">
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockProjectApi from '@/api/mock/MockProjectApi'
import { $coreConfirm, $goto, checkShowColumn, isAdminUser, useCurrentUserName } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'

const { search, getById, deleteById, saveOrUpdate } = MockProjectApi
const { userOptions } = useAllUsers()

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage() },
  searchMethod: search
})
const loadMockProjects = (pageNumber) => searchMethod(pageNumber)

onMounted(() => {
  loadMockProjects()
})

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = computed(() => {
  return [{
    label: '项目代码',
    property: 'projectCode',
    click: item => {
      $goto(`/mock/groups/project/${item.projectCode}`)
    }
  }, {
    label: '项目名称',
    property: 'projectName',
    click: item => {
      $goto(`/mock/groups/project/${item.projectCode}`)
    }
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    enabled: checkShowColumn(tableData.value, 'description')
  }, {
    labelKey: 'common.label.status',
    property: 'status',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveProjectItem({ ...data, status })}/>
    },
    minWidth: '70px'
  }, {
    labelKey: 'common.label.createDate',
    property: 'createDate',
    dateFormat: 'YYYY-MM-DD HH:mm:ss'
  }]
})
const buttons = defineTableButtons([{
  labelKey: 'common.label.edit',
  type: 'primary',
  click: item => {
    newOrEdit(item.id)
  }
}, {
  label: '管理分组',
  type: 'success',
  click: item => {
    $goto(`/mock/groups/project/${item.projectCode}`)
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  click: item => deleteProject(item)
}])
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      clearable: false
    },
    change () {
      loadMockProjects(1)
    }
  },
  {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

const deleteProject = project => {
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [project.projectName]))
    .then(() => deleteById(project.id))
    .then(() => loadMockProjects())
}

const showEditWindow = ref(false)
const currentProject = ref()
const newOrEdit = async id => {
  if (id) {
    await getById(id).then(data => {
      data.resultData && (currentProject.value = data.resultData)
    })
  } else {
    currentProject.value = {
      status: 1,
      userName: searchParam.value?.userName || useCurrentUserName()
    }
  }
  showEditWindow.value = true
}
const editFormOptions = computed(() => defineFormOptions([{
  labelKey: 'common.label.user',
  prop: 'userName',
  type: 'select',
  enabled: isAdminUser(),
  children: userOptions.value,
  attrs: {
    clearable: false
  }
}, {
  label: '项目代码',
  prop: 'projectCode',
  tooltip: '字母、数字、_-组成，唯一标识',
  required: true,
  upperCase: true,
  rules: [{
    validator (val) {
      return /[A-Za-z0-9_-]+/.test(val)
    },
    message: '字母、数字、_-组成，唯一标识'
  }]
}, {
  label: '项目名称',
  prop: 'projectName',
  required: true
}, useFormStatus(), {
  labelKey: 'common.label.description',
  prop: 'description',
  attrs: {
    type: 'textarea'
  }
}]))
const saveProjectItem = (item) => {
  return saveOrUpdate(item).then(() => loadMockProjects())
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
      @submit-form="loadMockProjects"
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
      :buttons-column-attrs="{width:'230px'}"
      :loading="loading"
      @page-size-change="loadMockProjects()"
      @current-page-change="loadMockProjects()"
      @selection-change="selectedRows=$event"
    />
    <simple-edit-window
      v-model="currentProject"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      name="Mock项目"
      :save-current-item="saveProjectItem"
    />
  </el-container>
</template>

<style scoped>

</style>
