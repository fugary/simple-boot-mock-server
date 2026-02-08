<script setup lang="jsx">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import MockGroupApi, {
  checkExport,
  downloadByLink,
  MOCK_GROUP_URL,
  histories,
  loadHistoryDiff,
  recoverFromHistory
} from '@/api/mock/MockGroupApi'
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
  formatJsonStr,
  useBackUrl
} from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle, $i18nKey } from '@/messages'
import { useFormDelay, useFormDisableMock, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import {
  showCompareWindowNew
} from '@/services/mock/NewMockDiffService'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import MockGroupImport from '@/views/components/mock/MockGroupImport.vue'
import { ElLink, ElText, ElTag } from 'element-plus'
import MockProjectApi, { checkProjectEdit, useProjectEditHook, useSelectProjects } from '@/api/mock/MockProjectApi'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { useRoute } from 'vue-router'
import CommonIcon from '@/components/common-icon/index.vue'
import { toCopyGroupTo } from '@/utils/DynamicUtils'
import {
  calcProxyUrl,
  calcProxyUrlParam,
  getProxyUrlOptions,
  toProxyUrlParams,
  useContentTypeOption
} from '@/services/mock/MockCommonService'
import MockHistoryListWindow from '@/views/components/utils/MockHistoryListWindow.vue'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'

// Add getGroupHistoryViewOptions
const getGroupHistoryViewOptions = (group, history) => {
  return [{
    labelKey: 'mock.label.groupName',
    prop: 'groupName'
  }, {
    labelKey: 'mock.label.pathId',
    prop: 'groupPath'
  }, {
    labelKey: 'mock.label.version',
    prop: () => `${group.version ?? ''}${group.current ? ` <${$i18nBundle('mock.label.current')}>` : ''}`
  }, {
    labelKey: 'common.label.modifyDate',
    prop: () => formatDate(group[history ? 'createDate' : 'modifyDate'])
  }, {
    labelKey: 'common.label.modifier',
    prop: () => group[history ? 'creator' : 'modifier']
  }, {
    labelKey: 'mock.label.proxyUrl',
    prop: () => calcProxyUrl(group.proxyUrl)
  }, {
    labelKey: 'common.label.status',
    prop: () => [$i18nBundle('common.label.statusDisabled'), $i18nBundle('common.label.statusEnabled')][group.status]
  }, {
    labelKey: 'mock.label.disabledMock',
    prop: () => group.disableMock !== undefined ? $i18nBundle(group.disableMock ? 'common.label.yes' : 'common.label.no') : ''
  }, {
    labelKey: 'common.label.delay',
    prop: 'delay'
  }, {
    labelKey: 'common.label.description',
    prop: 'description'
  }, {
    labelKey: 'mock.label.mockEnv',
    prop: () => {
      return formatJsonStr(group.groupConfig)
    }
  }]
}

// ... existing code ...

const loadHistoryDiffFunc = async (history, current, isView) => {
  let modified = current
  let original = history
  if (isView) {
    await loadHistoryDiff(history).then(data => {
      const result = data.resultData || {}
      modified = result.modified || {}
      original = result.original || {}
      modified.current = !modified.modifyFrom
    })
  }
  showCompareWindowNew({
    modified,
    original,
    historyOptionsMethod: getGroupHistoryViewOptions
  })
}
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
    const historyCountMap = data.infos?.historyCountMap || {}
    const accessDateMap = data.infos?.accessDateMap || {}
    tableData.value.forEach(group => {
      group.requestCount = countMap[group.id] || 0
      group.historyCount = historyCountMap[group.id] || 0
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
        const { value: proxyUrl, name } = calcProxyUrlParam(data.proxyUrl)
        const calcName = name !== 'default' ? name : undefined
        return <>
          <MockUrlCopyLink class="margin-left1"
                           tooltip={calcName ? `<strong>${calcName}:</strong> ${proxyUrl}` : ''}
                           urlPath={proxyUrl}>{calcName || proxyUrl}</MockUrlCopyLink>
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
  tooltip: $i18nBundle('mock.label.modifyHistory'),
  icon: 'Clock',
  round: true,
  type: 'info',
  buttonIf: item => item.historyCount > 0,
  click: item => {
    currentGroup.value = item
    showHistory(item)
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
const showMore = ref(false)
const hiddenKeys = ['disableMock', 'delay', 'contentType', 'description']
const currentGroup = ref()
const newOrEdit = async id => {
  if (id) {
    await getById(id).then(data => {
      if (data.resultData) {
        currentGroup.value = data.resultData
        currentGroup.value.proxyUrlParams = toProxyUrlParams(currentGroup.value.proxyUrl)
      }
    })
  } else {
    currentGroup.value = {
      status: 1,
      userName: searchParam.value?.userName || useCurrentUserName(),
      projectCode: searchParam.value?.projectCode || MOCK_DEFAULT_PROJECT,
      proxyUrlParams: []
    }
  }
  showEditWindow.value = true
  // Auto-expand if any hidden field has a value
  showMore.value = hiddenKeys.some(key => !!currentGroup.value?.[key])
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
  return MockProjectApi.saveOrUpdate(item).then(() => {
    reloadProjectsAndRefreshOptions(item)
  })
}
const editFormOptions = computed(() => {
  const options = defineFormOptions([{
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
    slot: 'proxyUrlParams',
    tooltip: $i18nBundle('mock.msg.proxyUrlTooltip')
  }, { ...useFormStatus(), style: getStyleGrow(4) },
  { ...useFormDisableMock(), style: getStyleGrow(6) },
  { ...useFormDelay(), style: getStyleGrow(4) },
  { ...useContentTypeOption({ clearable: true }), style: getStyleGrow(6) }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }])
  const filteredOptions = options.filter(option => {
    if (!option.prop || !hiddenKeys.includes(option.prop)) return true
    return showMore.value
  })
  filteredOptions.push({
    slot: 'moreOptions',
    style: getStyleGrow(10)
  })
  return filteredOptions
})
const saveGroupItem = (item) => {
  item.proxyUrl = item.proxyUrlParams?.length ? JSON.stringify(item.proxyUrlParams.filter(param => param.value)) : null
  return saveOrUpdate(item).then(() => {
    loadMockGroups()
  })
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

const historyColumns = computed(() => {
  return [{
    labelKey: 'mock.label.groupName',
    minWidth: '130px',
    formatter (data) {
      return data.groupName
    }
  }, {
    labelKey: 'mock.label.pathId',
    minWidth: '180px',
    formatter (data) {
      return data.groupPath
    }
  }, {
    labelKey: 'common.label.status',
    minWidth: '80px',
    formatter (data) {
      return <DelFlagTag modelValue={data.status} />
    }
  }, {
    labelKey: 'common.label.modifyDate',
    minWidth: '160px',
    formatter (item) {
      return formatDate(item.modifyDate)
    }
  }, {
    labelKey: 'common.label.modifier',
    property: 'modifier',
    minWidth: '120px'
  }, {
    labelKey: 'mock.label.version',
    property: 'version',
    minWidth: '120px',
    formatter (item) {
      return <>
        {item.version}
        {!item.modifyFrom ? <ElTag type="success" class="margin-left1">{$i18nBundle('mock.label.current')}</ElTag> : ''}
      </>
    }
  }, {
    labelKey: 'mock.label.proxyUrl',
    minWidth: '150px',
    formatter (data) {
      return calcProxyUrl(data.proxyUrl)
    }
  }, {
    labelKey: 'common.label.delay',
    minWidth: '100px',
    formatter (data) {
      return data.delay
    }
  }, {
    labelKey: 'common.label.description',
    minWidth: '100px',
    formatter (data) {
      return data.description
    }
  }]
})

const historyWindowRef = ref()
const showHistory = (group) => {
  currentGroup.value = group
  historyWindowRef.value.showHistoryListWindow()
}
const searchHistories = (query, page) => {
  if (!currentGroup.value?.id) return Promise.resolve({ resultData: [], infos: {} })
  return histories(currentGroup.value?.id, Object.assign(query, { page }))
}

const recoverFromHistoryFunc = (history) => {
  return recoverFromHistory(history).then(() => {
    loadMockGroups()
  })
}

const { nameDynamicOption, valueDynamicOption } = getProxyUrlOptions()
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
      :buttons-column-attrs="{minWidth:'280px',fixed:'right'}"
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
      width="800px"
    >
      <template #proxyUrlParams="{option}">
        <common-form-control
          :model="currentGroup"
          :option="option"
        >
          <common-params-edit
            v-model="currentGroup.proxyUrlParams"
            form-prop="proxyUrlParams"
            :name-dynamic-option="nameDynamicOption"
            :value-dynamic-option="valueDynamicOption"
            :show-copy-button="false"
            :show-paste-button="false"
            single-enable
            :name-span="7"
            :value-span="12"
          />
        </common-form-control>
        <br>
      </template>
      <template #moreOptions>
        <div class="form-edit-width-100 flex-center">
          <el-button
            link
            type="primary"
            @click="showMore=!showMore"
          >
            {{ showMore ? $t('mock.label.hideMoreOptions') : $t('mock.label.showMoreOptions') }}
            <common-icon :icon="showMore?'ArrowUp':'ArrowDown'" />
          </el-button>
        </div>
      </template>
    </simple-edit-window>
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
    <mock-history-list-window
      ref="historyWindowRef"
      :columns="historyColumns"
      :search-func="searchHistories"
      :compare-func="loadHistoryDiffFunc"
      :recover-func="recoverFromHistoryFunc"
      @update-history="loadMockGroups()"
    />
  </el-container>
</template>

<style scoped>

</style>
