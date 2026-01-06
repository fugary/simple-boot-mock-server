<script setup lang="jsx">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import MockGroupApi, { checkExport, downloadByLink, MOCK_GROUP_URL } from '@/api/mock/MockGroupApi'
import { useAllUsers } from '@/api/mock/MockUserApi'
import {
  $coreConfirm,
  $goto,
  checkShowColumn,
  isAdminUser,
  $coreError,
  toGetParams,
  useCurrentUserName,
  getStyleGrow,
  formatDate,
  useBackUrl
} from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle, $i18nKey } from '@/messages'
import { useFormDelay, useFormDisableMock, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import MockGroupImport from '@/views/components/mock/MockGroupImport.vue'
import { ElLink, ElText, ElTag } from 'element-plus'
import MockProjectApi, { checkProjectEdit, useProjectEditHook, useSelectProjects } from '@/api/mock/MockProjectApi'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { useRoute } from 'vue-router'
import CommonIcon from '@/components/common-icon/index.vue'
import { toCopyGroupTo } from '@/utils/DynamicUtils'
import { useContentTypeOption } from '@/services/mock/MockCommonService'

const props = defineProps({
  publicFlag: {
    type: Boolean,
    default: false
  }
})
const route = useRoute()
const { search, getById, deleteById, saveOrUpdate } = MockGroupApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage(), userName: useCurrentUserName() },
  searchMethod: search
})
const mockProject = ref()
const loadMockGroups = (pageNumber) => searchMethod(pageNumber)
  .then(data => {
    mockProject.value = data.infos?.mockProject
    const countMap = data.infos?.countMap || {}
    const accessDateMap = data.infos?.accessDateMap || {}
    tableData.value.forEach(group => {
      group.requestCount = countMap[group.id] || 0
      group.lastAccessDate = accessDateMap[group.groupPath]
    })
    return data
  })

const { backUrl, goBack } = useBackUrl()
searchParam.value.projectCode = route.params.projectCode || searchParam.value.projectCode
searchParam.value.userName = route.params.userName || searchParam.value.userName
searchParam.value.publicFlag = props.publicFlag
const projectEditable = computed(() => checkProjectEdit(mockProject.value))

const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)
const { projectOptions, loadProjectsAndRefreshOptions } = useSelectProjects(searchParam, false)

const { initLoadOnce } = useInitLoadOnce(async () => {
  await Promise.allSettled([loadUsersAndRefreshOptions(), loadProjectsAndRefreshOptions()])
  return loadMockGroups()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

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
    minWidth: '130px',
    headerFormatter () {
      return <>
        {$i18nBundle('mock.label.groupName')}
        {searchParam.value.page?.totalCount
          ? <ElTag class="margin-left1 pointer" onClick={() => loadMockGroups()}
                   type="primary" size="small" effect="plain" round={true}>
              {selectedRows.value.length
                ? <span>{selectedRows.value.length}/</span>
                : ''}<span>{searchParam.value.page?.totalCount}</span>
            </ElTag>
          : ''
        }
      </>
    },
    formatter (data) {
      const url = `/mock/groups/${data.id}?backUrl=${route.fullPath}`
      let projectInfo = ''
      if (data.projectCode && !isDefaultProject(data.projectCode)) {
        const projectOption = projectOptions.value.find(proj => proj.value === data.projectCode)
        projectInfo = projectOption?.label || $i18nBundle(projectOption?.labelKey) || mockProject.value?.projectName
        if (!projectInfo) {
          projectInfo = data.projectCode
        }
      }
      return <>
        <ElLink type="primary" onClick={() => $goto(url)}>{data.groupName}</ElLink>
        {projectInfo ? <><br/><span class="el-text el-text--info">({projectInfo})</span></> : ''}
      </>
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
    minWidth: '150px',
    enabled: checkShowColumn(tableData.value, 'proxyUrl'),
    formatter (data) {
      if (data.proxyUrl) {
        return <>
          <MockUrlCopyLink class="margin-left1" urlPath={data.proxyUrl}>{data.proxyUrl}</MockUrlCopyLink>
        </>
      }
    }
  }, {
    labelKey: 'common.label.delay',
    property: 'delay',
    enabled: checkShowColumn(tableData.value, 'delay')
  }, {
    labelKey: 'common.label.status',
    property: 'status',
    formatter (data) {
      const confirmResumeMock = () => $coreConfirm($i18nKey('common.msg.commonConfirm', 'mock.label.resumeMock'))
        .then(() => saveGroupItem({ ...data, disableMock: false }))
      return <>
        <DelFlagTag v-model={data.status} clickToToggle={projectEditable.value}
                    onToggleValue={(status) => saveGroupItem({ ...data, status })}/>
        {data.requestCount
          ? <ElTag
            v-common-tooltip={$i18nBundle('mock.label.mockRequestCount')}
            class="margin-left1 pointer"
            type="primary"
            size="small"
            effect="plain"
            style="height: 20px;"
            round={true}
            onClick={() => $goto(`/mock/groups/${data.id}?backUrl=${route.fullPath}`)}
        >
          {data.requestCount}
        </ElTag>
          : ''}
        {data.disableMock
          ? <ElText type="danger" onClick={confirmResumeMock}
                                  style="vertical-align: sub;"
                                  class="margin-left1 pointer"
                                  v-common-tooltip={$i18nBundle('mock.label.disabledMock')}>
          <CommonIcon size={18} icon="DoDisturbFilled"/>
        </ElText>
          : ''}
      </>
    },
    minWidth: '120px'
  }, {
    labelKey: 'common.label.createDate',
    minWidth: '180px',
    formatter (item) {
      return <>
        <span class="pointer" v-common-tooltip={$i18nBundle('common.label.createDate')}>
          <CommonIcon icon="CalendarMonthFilled" size={20} class="margin-left1" style="top: 4px;"/>
          {formatDate(item.createDate)}
        </span>
        { item.lastAccessDate
          ? <div class="pointer">
              <CommonIcon icon="View" size={20} class="margin-left1" style="top: 4px;"/>
              <span v-common-tooltip={$i18nBundle('common.label.accessDate')}>{formatDate(item.lastAccessDate)}</span>
            </div>
          : ''}
      </>
    }
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    minWidth: '100px',
    enabled: checkShowColumn(tableData.value, 'description')
  }]
})
const toCopyGroups = (group) => {
  return toCopyGroupTo(group, {
    onCopySuccess: () => loadMockGroups()
  })
}
const buttons = computed(() => defineTableButtons([{
  tooltip: $i18nBundle('common.label.edit'),
  icon: 'Edit',
  round: true,
  type: 'primary',
  enabled: !!projectEditable.value,
  click: item => {
    newOrEdit(item.id)
  }
}, {
  tooltip: $i18nBundle('common.label.config'),
  icon: 'Setting',
  round: true,
  type: 'success',
  click: item => {
    $goto(`/mock/groups/${item.id}?backUrl=${route.fullPath}`)
  }
}, {
  tooltip: $i18nBundle('common.label.copy'),
  icon: 'FileCopyFilled',
  round: true,
  type: 'warning',
  click: toCopyGroups
}, {
  tooltip: $i18nBundle('common.label.delete'),
  icon: 'DeleteFilled',
  round: true,
  type: 'danger',
  enabled: !!projectEditable.value,
  click: item => deleteGroup(item)
}]))
const changedUser = async (userName) => {
  userName && (searchParam.value.userName = userName)
  await loadProjectsAndRefreshOptions()
  loadMockGroups(1)
}
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    },
    change: changedUser
  }, {
    labelKey: 'mock.label.project',
    prop: 'projectCode',
    type: 'select',
    enabled: projectOptions.value.length > 1,
    children: projectOptions.value,
    attrs: {
      filterable: true,
      clearable: !props.publicFlag
    },
    change (value) {
      searchParam.value.userName = projectOptions.value.find(option => option.value === value)?.userName || searchParam.value.userName
      loadMockGroups(1)
    }
  },
  useSearchStatus({ change () { loadMockGroups(1) } }), {
    labelKey: 'mock.label.hasMockData',
    prop: 'hasRequest',
    type: 'select',
    children: [{
      value: true,
      label: $i18nBundle('common.label.yes')
    }, {
      value: false,
      label: $i18nBundle('common.label.no')
    }],
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
    .then(() => deleteById(group.id, { loading: true }))
    .then(() => loadMockGroups())
}

const deleteGroups = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockGroupApi.removeByIds(selectedRows.value.map(item => item.id), { loading: true }))
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
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(searchParam, userOptions)
const reloadProjectsAndRefreshOptions = async (item, importModel) => {
  await loadProjectsAndRefreshOptions()
  const formModel = importModel?.value ? importModel : currentGroup
  if (formModel?.value && projectOptions.value?.find(option => option.value === item.projectCode)) {
    formModel.value.projectCode = item.projectCode
  }
}
const saveProjectItem = (item) => {
  return MockProjectApi.saveOrUpdate(item).then(() => reloadProjectsAndRefreshOptions(item))
}
const editFormOptions = computed(() => defineFormOptions([{
  labelKey: 'common.label.user',
  prop: 'userName',
  type: 'select',
  enabled: isAdminUser(),
  children: userOptions.value,
  attrs: {
    clearable: false
  },
  change (value) {
    if (currentGroup.value) {
      currentGroup.value.projectCode = MOCK_DEFAULT_PROJECT
    }
    changedUser(value)
  }
}, {
  labelKey: 'mock.label.project',
  prop: 'projectCode',
  type: 'select',
  children: projectOptions.value,
  attrs: {
    clearable: false
  },
  tooltip: $i18nKey('common.label.commonAdd', 'mock.label.project'),
  tooltipIcon: 'CirclePlusFilled',
  tooltipLinkAttrs: {
    type: 'primary'
  },
  tooltipFunc (event) {
    newOrEditProject()
    event.preventDefault()
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
      return !currentGroup.value?.proxyUrl || /^https?:\/\/.+/.test(currentGroup.value?.proxyUrl)
    }
  }]
}, { ...useFormStatus(), style: getStyleGrow(4) },
{ ...useFormDisableMock(), style: getStyleGrow(6) },
{ ...useFormDelay(), style: getStyleGrow(4) },
{ ...useContentTypeOption({ clearable: true }), style: getStyleGrow(6) }, {
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
      groupIds,
      ...searchParam.value
    }
    delete exportConfig.page
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
    <el-page-header
      v-if="(publicFlag||route.params.projectCode)&&mockProject"
      class="margin-bottom3"
      @back="goBack"
    >
      <template #content>
        <span class="text-large font-600 mr-3">
          {{ mockProject.projectName }}
          <el-text
            v-if="mockProject.userName"
            type="info"
          >
            ({{ $t('mock.label.owner') }}: {{ mockProject.userName }})
          </el-text>
          <el-link
            type="primary"
            class="margin-left2"
            @click="$goto('/mock/groups')"
          >
            {{ $i18nKey('common.label.commonBack','mock.label.mockGroups') }}
          </el-link>
        </span>
      </template>
    </el-page-header>
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadMockGroups"
    >
      <template #buttons>
        <el-button
          v-if="projectEditable"
          type="info"
          @click="newOrEdit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
        <el-button
          v-if="projectEditable"
          type="warning"
          @click="showImportWindow = true"
        >
          {{ $t('mock.label.import') }}
        </el-button>
        <el-dropdown
          style="margin-left: 12px;"
        >
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
          v-if="selectedRows?.length>1&&projectEditable"
          type="warning"
          @click="toCopyGroups(selectedRows)"
        >
          {{ $t('common.label.copy') }}
        </el-button>
        <el-button
          v-if="selectedRows?.length&&projectEditable"
          type="danger"
          @click="deleteGroups()"
        >
          {{ $t('common.label.delete') }}
        </el-button>
        <el-button
          v-if="backUrl"
          @click="goBack()"
        >
          {{ $t('common.label.back') }}
        </el-button>
      </template>
    </common-form>
    <common-table
      ref="groupTableRef"
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{minWidth:'230px',fixed:'right'}"
      :loading="loading"
      @page-size-change="loadMockGroups()"
      @current-page-change="loadMockGroups()"
      @selection-change="selectedRows=$event"
      @row-dblclick="newOrEdit($event.id)"
    />
    <simple-edit-window
      v-model="currentGroup"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      :name="$t('mock.label.mockGroups')"
      :save-current-item="saveGroupItem"
      inline-auto-mode
    />
    <simple-edit-window
      v-model="currentProject"
      v-model:show-edit-window="showEditProjectWindow"
      :form-options="editProjectFormOptions"
      :name="$t('mock.label.mockProjects')"
      :save-current-item="saveProjectItem"
      label-width="130px"
    />
    <mock-group-import
      v-model="showImportWindow"
      :default-user="searchParam.userName"
      :user-options="userOptions"
      :default-project="searchParam.projectCode"
      :project-options="projectOptions"
      @import-success="loadMockGroups()"
      @update-projects="reloadProjectsAndRefreshOptions"
      @changed-user="changedUser"
    />
  </el-container>
</template>

<style scoped>

</style>
