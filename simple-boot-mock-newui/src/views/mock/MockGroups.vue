<script setup lang="jsx">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import MockGroupApi, { checkExport, downloadByLink, MOCK_GROUP_URL } from '@/api/mock/MockGroupApi'
import { useAllUsers } from '@/api/mock/MockUserApi'
import { $coreConfirm, $goto, checkShowColumn, isAdminUser, $coreError, toGetParams, useCurrentUserName } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import MockGroupImport from '@/views/components/mock/MockGroupImport.vue'
import { ElLink } from 'element-plus'
import { useSelectProjects } from '@/api/mock/MockProjectApi'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { useRoute } from 'vue-router'

const route = useRoute()
const { search, getById, deleteById, saveOrUpdate } = MockGroupApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage(), userName: useCurrentUserName(), projectCode: MOCK_DEFAULT_PROJECT },
  searchMethod: search
})
const loadMockGroups = (pageNumber) => searchMethod(pageNumber)

if (route.params.projectCode) {
  searchParam.value.projectCode = useRoute().params.projectCode
}

const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)
const { projectOptions, loadProjectsAndRefreshOptions } = useSelectProjects(searchParam)

onMounted(() => {
  loadMockGroups()
  loadProjectsAndRefreshOptions()
})

onActivated(async () => {
  await Promise.allSettled([loadUsersAndRefreshOptions(), loadProjectsAndRefreshOptions()])
  loadMockGroups(1)
})

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = computed(() => {
  return [{
    width: '50px',
    attrs: {
      type: 'selection'
    }
  }, {
    labelKey: 'mock.label.groupName',
    property: 'groupName',
    click: item => {
      $goto(`/mock/groups/${item.id}?backUrl=${route.fullPath}`)
    }
  }, {
    labelKey: 'mock.label.pathId',
    property: 'groupPath',
    minWidth: '180px',
    formatter (data) {
      const path = `/mock/${data.groupPath}`
      return <>
        <ElLink type="primary" onClick={() => $goto(`/mock/groups/${data.id}?backUrl=${route.fullPath}`)}>
          {data.groupPath}
        </ElLink>&nbsp;
        <MockUrlCopyLink urlPath={path}/>
      </>
    }
  }, {
    labelKey: 'mock.label.proxyUrl',
    property: 'proxyUrl',
    minWidth: '130px'
  }, {
    labelKey: 'common.label.delay',
    property: 'delay',
    enabled: checkShowColumn(tableData.value, 'delay')
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    enabled: checkShowColumn(tableData.value, 'description')
  }, {
    labelKey: 'common.label.status',
    property: 'status',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveGroupItem({ ...data, status })}/>
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
  labelKey: 'common.label.config',
  type: 'success',
  click: item => {
    $goto(`/mock/groups/${item.id}?backUrl=${route.fullPath}`)
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  click: item => deleteGroup(item)
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
    change: async () => {
      await loadProjectsAndRefreshOptions()
      loadMockGroups(1)
    }
  }, {
    labelKey: 'mock.label.project',
    prop: 'projectCode',
    type: 'select',
    enabled: projectOptions.value.length > 1,
    children: projectOptions.value,
    attrs: {
      clearable: false
    },
    change () {
      loadMockGroups(1)
    }
  },
  {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

const deleteGroup = group => {
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [group.groupName]))
    .then(() => deleteById(group.id))
    .then(() => loadMockGroups())
}

const deleteGroups = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockGroupApi.removeByIds(selectedRows.value.map(item => item.id)), { loading: true })
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
      status: 1,
      userName: searchParam.value?.userName || useCurrentUserName(),
      projectCode: searchParam.value?.projectCode || MOCK_DEFAULT_PROJECT
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
  labelKey: 'mock.label.project',
  prop: 'projectCode',
  type: 'select',
  enabled: projectOptions.value.length > 1,
  children: projectOptions.value,
  attrs: {
    clearable: false
  }
}, {
  labelKey: 'mock.label.groupName',
  prop: 'groupName',
  required: true
}, {
  labelKey: 'mock.label.pathId',
  prop: 'groupPath',
  placeholder: $i18nBundle('mock.msg.pathIdMsg')
}, {
  labelKey: 'mock.label.proxyUrl',
  prop: 'proxyUrl',
  tooltip: $i18nBundle('mock.msg.proxyUrlTooltip'),
  rules: [{
    message: $i18nBundle('mock.msg.proxyUrlMsg'),
    validator: () => {
      return !currentGroup.value?.proxyUrl || /^https?:\/\//.test(currentGroup.value?.proxyUrl)
    }
  }]
}, useFormStatus(), useFormDelay(), {
  labelKey: 'common.label.description',
  prop: 'description',
  attrs: {
    type: 'textarea'
  }
}]))
const saveGroupItem = (item) => {
  return saveOrUpdate(item).then(() => loadMockGroups())
}
const selectedRows = ref([])
const exportGroups = (groupIds) => {
  $coreConfirm($i18nBundle('mock.msg.exportConfirm')).then(() => {
    const exportConfig = {
      exportAll: !groupIds,
      projectCode: searchParam.value.projectCode || MOCK_DEFAULT_PROJECT,
      groupIds,
      userName: searchParam.value.userName
    }
    checkExport(exportConfig, { loading: true, showErrorMessage: false }).then((data) => {
      if (data.success) {
        exportConfig.access_token = useLoginConfigStore().accessToken
        const downloadUrl = `${getMockUrl(`${MOCK_GROUP_URL}/export`)}?${toGetParams(exportConfig)}`
        downloadByLink(downloadUrl)
      } else {
        $coreError($i18nBundle('mock.msg.noExportData'))
      }
    })
  })
}
const exportSelected = () => {
  if (!selectedRows.value?.length) {
    $coreError($i18nBundle('mock.msg.noExportData'))
    return
  }
  exportGroups(selectedRows.value.map(group => group.id))
}
const showImportWindow = ref(false)
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
        <el-dropdown style="margin-left: 12px;">
          <el-button type="success">
            {{ $t('mock.label.export') }}
            <common-icon icon="ArrowDown" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="exportGroups()">
                {{ $t('mock.label.exportAll') }}
              </el-dropdown-item>
              <el-dropdown-item
                :disabled="!selectedRows?.length"
                @click="exportSelected"
              >
                {{ $t('mock.label.exportSelected') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button
          type="success"
          @click="showImportWindow = true"
        >
          {{ $t('mock.label.import') }}
        </el-button>
        <el-button
          v-if="selectedRows?.length"
          type="danger"
          @click="deleteGroups()"
        >
          {{ $t('common.label.delete') }}
        </el-button>
      </template>
    </common-form>
    <common-table
      ref="groupTableRef"
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{width:'230px'}"
      :loading="loading"
      @page-size-change="loadMockGroups()"
      @current-page-change="loadMockGroups()"
      @selection-change="selectedRows=$event"
    />
    <simple-edit-window
      v-model="currentGroup"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      name="Mock分组"
      :save-current-item="saveGroupItem"
    />
    <mock-group-import
      v-model="showImportWindow"
      :default-user="searchParam.userName"
      :user-options="userOptions"
      :default-project="searchParam.projectCode"
      :project-options="projectOptions"
      @import-success="loadMockGroups()"
    />
  </el-container>
</template>

<style scoped>

</style>
