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
  normalizeGroupProjectRelation,
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
import MockProjectApi, {
  checkProjectReadable,
  checkProjectDeletable,
  checkProjectWritable,
  selectProjects
} from '@/api/mock/MockProjectApi'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { onBeforeRouteUpdate, useRoute } from 'vue-router'
import CommonIcon from '@/components/common-icon/index.vue'
import { toCopyGroupTo, showHistoryListWindow } from '@/utils/DynamicUtils'
import {
  calcProxyUrl,
  calcProxyUrlParam,
  getProxyUrlOptions,
  toProxyUrlParams,
  useContentTypeOption
} from '@/services/mock/MockCommonService'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { filterProjectOptionsByAuthority, useProjectEditHook, useSelectProjects } from '@/hooks/mock/MockProjectHooks'

// Add getGroupHistoryViewOptions
const getGroupHistoryViewOptions = (group, history) => {
  return [{
    labelKey: 'mock.label.groupName',
    prop: 'groupName'
  }, {
    labelKey: 'mock.label.pathId',
    prop: 'groupPath'
  }, {
    labelKey: 'mock.label.activeScenario',
    enabled: (scenarioMap.value[group.id] || []).length > 0,
    prop: (group) => {
      const scenarios = scenarioMap.value[group.id] || []
      if (!scenarios.length) return ''
      if (group.activeScenarioCode) {
        const found = scenarios.find(s => String(s.scenarioCode) === String(group.activeScenarioCode))
        return found?.scenarioName || group.activeScenarioCode
      }
      return $i18nBundle('mock.label.defaultScenario')
    }
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
      if (data.infos?.scenarioMap) {
        Object.assign(scenarioMap.value, data.infos.scenarioMap)
      }
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
const defaultSearchParam = { page: useDefaultPage(), userName: useCurrentUserName(), projectId: null, onlyMine: false }

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: defaultSearchParam,
  searchMethod: search
})
const mockProject = ref()
const scenarioMap = ref({})
const loadMockGroups = (pageNumber, saveConfig) => searchMethod(pageNumber, {}, saveConfig)
  .then(data => {
    mockProject.value = data.infos?.mockProject
    scenarioMap.value = data.infos?.scenarioMap || {}
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
const syncRouteSearchParam = (targetRoute = route) => {
  const routeProjectCode = targetRoute.params.projectCode ? String(targetRoute.params.projectCode) : null
  const routeProjectId = targetRoute.query.projectId ? Number(targetRoute.query.projectId) : null
  const routeUserName = targetRoute.params.userName ? String(targetRoute.params.userName) : null
  const hasRouteProjectId = Number.isFinite(routeProjectId) && routeProjectId > 0
  if (routeProjectCode) {
    searchParam.value.projectId = hasRouteProjectId ? routeProjectId : null
    searchParam.value.projectCode = routeProjectCode
    if (!hasRouteProjectId || isDefaultProject(routeProjectCode) || isAdminUser()) {
      searchParam.value.userName = routeUserName || searchParam.value.userName || useCurrentUserName()
    } else if (!searchParam.value.userName) {
      searchParam.value.userName = useCurrentUserName()
    }
  } else if (props.publicFlag) {
    searchParam.value.projectId = hasRouteProjectId ? routeProjectId : searchParam.value.projectId
    searchParam.value.projectCode = routeProjectCode || searchParam.value.projectCode
    if (!hasRouteProjectId || isAdminUser()) {
      searchParam.value.userName = routeUserName || null
    }
  } else if (routeUserName) {
    searchParam.value.userName = routeUserName
  }
  searchParam.value.publicFlag = props.publicFlag
}
syncRouteSearchParam()
const projectReadable = computed(() => checkProjectReadable(mockProject.value))
const projectWritable = computed(() => checkProjectWritable(mockProject.value))
const projectHeaderTags = computed(() => {
  if (!mockProject.value) {
    return []
  }
  const tags = []
  if (props.publicFlag || mockProject.value?.publicFlag) {
    tags.push({
      key: 'public',
      type: 'primary',
      label: $i18nBundle('mock.label.public')
    })
  }
  if (projectWritable.value || projectReadable.value) {
    tags.push({
      key: 'authority',
      type: projectWritable.value ? 'success' : 'warning',
      label: $i18nBundle(projectWritable.value ? 'common.label.authorityWritable' : 'common.label.authorityReadable')
    })
  }
  if (mockProject.value?.status !== undefined && mockProject.value?.status !== null) {
    tags.push({
      key: 'status',
      type: Number(mockProject.value.status) === 1 ? 'success' : 'danger',
      label: $i18nBundle(Number(mockProject.value.status) === 1 ? 'common.label.statusEnabled' : 'common.label.statusDisabled')
    })
  }
  return tags
})

const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)
const { projects, projectOptions, loadProjectsAndRefreshOptions } = useSelectProjects(searchParam, false)
const writableProjectOptions = computed(() => {
  return filterProjectOptionsByAuthority(projects.value, projectOptions.value, checkProjectWritable)
})
const sharedProjectOptions = ref([])
const showOnlyMineFilter = computed(() => !isAdminUser() && !props.publicFlag && sharedProjectOptions.value.length > 0)
const refreshSharedProjectOptions = (result = []) => {
  sharedProjectOptions.value = (result || []).filter(project => {
    return !isDefaultProject(project?.projectCode) && project?.userName && project.userName !== useCurrentUserName()
  })
  if (!sharedProjectOptions.value.length) {
    searchParam.value.onlyMine = false
  }
  return sharedProjectOptions.value
}
const shouldReuseProjectOptionsForShared = () => {
  return !props.publicFlag && !searchParam.value?.projectId && !searchParam.value?.onlyMine
}
const loadSharedProjectOptions = (result) => {
  if (props.publicFlag) {
    sharedProjectOptions.value = []
    return Promise.resolve([])
  }
  if (Array.isArray(result)) {
    return Promise.resolve(refreshSharedProjectOptions(result))
  }
  return selectProjects({
    userName: searchParam.value?.userName || useCurrentUserName(),
    publicFlag: false
  }).then(refreshSharedProjectOptions)
}
const loadProjectRelatedOptions = async ({ reloadUsers = false } = {}) => {
  const tasks = []
  reloadUsers && tasks.push(loadUsersAndRefreshOptions())
  const projectsTask = loadProjectsAndRefreshOptions()
  tasks.push(projectsTask)
  if (shouldReuseProjectOptionsForShared()) {
    tasks.push(projectsTask.then(() => loadSharedProjectOptions(projects.value)))
  } else {
    tasks.push(loadSharedProjectOptions())
  }
  await Promise.allSettled(tasks)
}

const { initLoadOnce } = useInitLoadOnce(async () => {
  syncRouteSearchParam()
  await loadProjectRelatedOptions({ reloadUsers: true })
  syncRouteSearchParam()
  return loadMockGroups()
})

const reloadMockGroupsForRoute = (targetRoute) => {
  syncRouteSearchParam(targetRoute)
  loadProjectRelatedOptions({ reloadUsers: true }).then(() => {
    syncRouteSearchParam(targetRoute)
    return loadMockGroups(1, targetRoute.path)
  })
}

onMounted(initLoadOnce)

onActivated(initLoadOnce)

onBeforeRouteUpdate((to) => {
  reloadMockGroupsForRoute(to)
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
    minWidth: '180px',
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
      const projectOption = projectOptions.value.find(proj => `${proj.projectId || ''}` === `${data.projectId || ''}`) ||
        projectOptions.value.find(proj => proj.projectCode === data.projectCode)
      const projectUserName = projectOption?.userName || mockProject.value?.userName || data.userName
      let projectInfo = ''
      if (isDefaultProject(data.projectCode)) {
        if (projectUserName && projectUserName !== useCurrentUserName()) {
          projectInfo = <>
            <span>{$i18nBundle('mock.label.defaultProject')}</span>
            <ElText class="margin-left1" type="success" tag="b"
                    v-common-tooltip={$i18nBundle('mock.label.owner')}>({projectUserName})</ElText>
          </>
        }
      } else if (data.projectCode) {
        projectInfo = projectOption?.labelComp?.() || $i18nBundle(projectOption?.labelKey) || mockProject.value?.projectName
        if (!projectInfo) {
          projectInfo = data.projectCode
        }
        if (!projectOption && projectUserName && projectUserName !== useCurrentUserName()) {
          projectInfo = <>
            <span>{projectInfo}</span>
            <ElText class="margin-left1" type="success" tag="b"
                    v-common-tooltip={$i18nBundle('mock.label.owner')}>({projectUserName})</ElText>
          </>
        }
      }
      const hasScenarios = (scenarioMap.value[data.id] || []).length > 0
      let activeScenarioName = ''
      if (data.activeScenarioCode) {
        const scenarios = scenarioMap.value[data.id] || []
        const found = scenarios.find(s => String(s.scenarioCode) === String(data.activeScenarioCode))
        activeScenarioName = found?.scenarioName || data.activeScenarioCode
      } else if (hasScenarios) {
        activeScenarioName = $i18nBundle('mock.label.defaultScenario')
      }
      return <>
          <ElLink type="primary" onClick={() => $goto(url)}>{data.groupName}</ElLink>
        {projectInfo ? <><br/><span class="el-text el-text--info">{projectInfo}</span></> : ''}
        {activeScenarioName
          ? <><br/><ElTag size="small" type="warning">{activeScenarioName}</ElTag></>
          : ''}
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
      if (data.proxyUrl && data.proxyUrl !== '[]') {
        const { value: proxyUrl, name } = calcProxyUrlParam(data.proxyUrl) || {}
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
        <DelFlagTag v-model={data.status} clickToToggle={groupWritable(data)}
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
  }]
})
const matchesProject = (project, target) => {
  if (!project || !target) {
    return false
  }
  if (target.projectId != null && `${project.id || project.projectId || ''}` === `${target.projectId}`) {
    return true
  }
  return !!target.projectCode && project.projectCode === target.projectCode
}
const currentGroup = ref()
const currentGroupProject = ref()
const currentGroupEditable = ref(true)
const resolveGroupProject = (group) => {
  if (!group) {
    return null
  }
  const matchedProject = projects.value.find(project => matchesProject(project, group))
  if (matchedProject) {
    return matchedProject
  }
  if (currentGroupProject.value && (currentGroup.value?.id == null || `${currentGroup.value?.id}` === `${group.id}`)) {
    return currentGroupProject.value
  }
  if (matchesProject(mockProject.value, group)) {
    return mockProject.value
  }
  if (isDefaultProject(group.projectCode)) {
    return {
      projectCode: MOCK_DEFAULT_PROJECT,
      userName: group.userName
    }
  }
  return null
}
const groupReadable = (group) => checkProjectReadable(resolveGroupProject(group))
const groupWritable = (group) => checkProjectWritable(resolveGroupProject(group))
const groupDeletable = (group) => checkProjectDeletable(resolveGroupProject(group))
const currentGroupProjectChangeable = ref(true)
const canChangeCurrentGroupProject = computed(() => {
  if (!currentGroup.value?.id) {
    return true
  }
  return !!currentGroupProjectChangeable.value
})
const canMoveGroups = (group) => {
  const groups = Array.isArray(group) ? group : [group]
  return groups.length > 0 && groups.every(item => groupDeletable(item))
}
const toTransferGroups = (group, action = 'copy', allowMove = false) => {
  return toCopyGroupTo(group, {
    action,
    allowMove,
    onTransferSuccess: () => loadMockGroups(),
    onCopySuccess: () => loadMockGroups()
  })
}
const copyGroups = (group) => {
  return toTransferGroups(group, 'copy', canMoveGroups(group))
}
const moveGroups = (groups) => {
  return toTransferGroups(groups, 'move', canMoveGroups(groups))
}
const buttons = computed(() => defineTableButtons([{
  tooltip: $i18nBundle('common.label.edit'),
  icon: 'Edit',
  round: true,
  type: 'primary',
  buttonIf: item => groupWritable(item),
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
  buttonIf: item => groupReadable(item),
  click: item => copyGroups(item)
}, {
  tooltip: $i18nBundle('common.label.delete'),
  icon: 'DeleteFilled',
  round: true,
  type: 'danger',
  buttonIf: item => groupDeletable(item),
  click: item => deleteGroup(item)
}]))
const changedUser = async (userName) => {
  userName && (searchParam.value.userName = userName)
  searchParam.value.projectId = null
  searchParam.value.projectCode = null
  await loadProjectRelatedOptions()
  loadMockGroups(1)
}
const handleOnlyMineChange = async (value) => {
  if (value) {
    searchParam.value.userName = useCurrentUserName()
  }
  searchParam.value.projectId = null
  searchParam.value.projectCode = null
  await loadProjectRelatedOptions()
  loadMockGroups(1)
}
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [...(showOnlyMineFilter.value
    ? [{
        labelKey: 'mock.label.myData',
        prop: 'onlyMine',
        type: 'switch',
        enabled: true,
        change: handleOnlyMineChange
      }]
    : []), {
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser() && !searchParam.value.onlyMine,
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
    enabled: projectOptions.value.length > 1 || searchParam.value?.projectId != null || !!searchParam.value?.projectCode,
    children: projectOptions.value,
    attrs: {
      filterable: true,
      clearable: !props.publicFlag
    },
    change (value) {
      const option = projectOptions.value.find(item => item.value === value)
      searchParam.value.projectId = option?.projectId || null
      searchParam.value.projectCode = option?.projectCode || value || null
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
const newOrEdit = async id => {
  if (id) {
    await getById(id).then(data => {
      if (data.resultData) {
        currentGroup.value = data.resultData
        currentGroup.value.proxyUrlParams = toProxyUrlParams(currentGroup.value.proxyUrl)
        currentGroupProject.value = data.infos?.mockProject || resolveGroupProject(currentGroup.value)
        currentGroupEditable.value = checkProjectWritable(currentGroupProject.value)
        currentGroupProjectChangeable.value = checkProjectDeletable(currentGroupProject.value)
      }
    })
  } else {
    currentGroup.value = {
      status: 1,
      userName: searchParam.value?.userName || useCurrentUserName(),
      projectId: searchParam.value?.projectId || null,
      projectCode: searchParam.value?.projectCode || MOCK_DEFAULT_PROJECT,
      proxyUrlParams: []
    }
    currentGroupProject.value = resolveGroupProject(currentGroup.value) || mockProject.value
    currentGroupEditable.value = !!projectWritable.value
    currentGroupProjectChangeable.value = !!projectWritable.value
  }
  showEditWindow.value = true
  // Auto-expand if any hidden field has a value
  showMore.value = hiddenKeys.some(key => !!currentGroup.value?.[key])
}
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(searchParam, userOptions)
const reloadProjectsAndRefreshOptions = async (item, importModel) => {
  await loadProjectsAndRefreshOptions()
  const formModel = importModel?.value ? importModel : currentGroup
  if (formModel?.value) {
    const option = projectOptions.value?.find(project => `${project.projectId || ''}` === `${item.id || item.projectId || ''}`) ||
      projectOptions.value?.find(project => project.projectCode === item.projectCode)
    if (option) {
      formModel.value.projectId = option.projectId || null
      formModel.value.projectCode = option.projectCode
    }
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
        currentGroup.value.projectId = null
        currentGroup.value.projectCode = MOCK_DEFAULT_PROJECT
      }
      changedUser(value)
    }
  }, {
    labelKey: 'mock.label.project',
    prop: 'projectCode',
    type: 'select',
    enabled: canChangeCurrentGroupProject.value,
    children: writableProjectOptions.value,
    attrs: {
      clearable: false
    },
    change (value) {
      const option = writableProjectOptions.value.find(item => item.value === value)
      const switchToDefaultProject = option?.projectCode === MOCK_DEFAULT_PROJECT
      currentGroup.value.projectId = option?.projectId || null
      currentGroup.value.projectCode = option?.projectCode || value || null
      if (switchToDefaultProject) {
        currentGroup.value.userName = searchParam.value?.userName || useCurrentUserName()
      }
      currentGroupProject.value = projects.value.find(project => `${project.id || ''}` === `${option?.projectId || ''}`) ||
        (option?.projectCode === MOCK_DEFAULT_PROJECT
          ? { projectCode: MOCK_DEFAULT_PROJECT, userName: currentGroup.value?.userName }
          : null)
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
    labelKey: 'mock.label.activeScenario',
    prop: 'activeScenarioCode',
    type: 'select',
    enabled: !!currentGroup.value?.id && (scenarioMap.value[currentGroup.value.id] || []).length > 0,
    children: (() => {
      const groupId = currentGroup.value?.id
      const scenarios = (scenarioMap.value[groupId] || []).filter(s => s.status === 1)
      const defaultOption = { label: $i18nBundle('mock.label.defaultScenario'), value: '' }
      const options = scenarios.map(s => ({
        label: s.scenarioName,
        value: s.scenarioCode
      }))
      return [defaultOption, ...options]
    })(),
    attrs: {
      clearable: false
    }
  }, {
    labelKey: 'mock.label.pathId',
    prop: 'groupPath',
    disabled: !currentGroup.value?.pathIdEditable,
    tooltip: $i18nBundle('mock.msg.pathIdMsg'),
    tooltipFunc: () => {
      if (currentGroup.value) {
        currentGroup.value.pathIdEditable = !currentGroup.value.pathIdEditable
      }
    },
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
const selectedRowsReadable = computed(() => selectedRows.value?.length > 0 && selectedRows.value.every(item => groupReadable(item)))
const selectedRowsDeletable = computed(() => selectedRows.value?.length > 0 && selectedRows.value.every(item => groupDeletable(item)))
const exportGroups = (groupIds) => {
  $coreConfirm($i18nBundle('mock.msg.exportConfirm')).then(() => {
    const exportConfig = normalizeGroupProjectRelation({
      exportAll: !groupIds,
      groupIds,
      ...searchParam.value
    })
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
      const hasScenarios = (scenarioMap.value[data.id] || []).length > 0
      let activeScenarioName = ''
      if (data.activeScenarioCode) {
        const scenarios = scenarioMap.value[data.id] || []
        const found = scenarios.find(s => String(s.scenarioCode) === String(data.activeScenarioCode))
        activeScenarioName = found?.scenarioName || data.activeScenarioCode
      } else if (hasScenarios) {
        activeScenarioName = $i18nBundle('mock.label.defaultScenario')
      }
      return <>
        {data.groupName}
        {activeScenarioName
          ? <><br/><ElTag size="small" type="warning">{activeScenarioName}</ElTag></>
          : ''}
      </>
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

const showHistory = (group) => {
  currentGroup.value = group
  currentGroupProject.value = resolveGroupProject(group)
  showHistoryListWindow({
    columns: historyColumns.value,
    searchFunc: searchHistories,
    compareFunc: loadHistoryDiffFunc,
    recoverFunc: groupWritable(group) ? recoverFromHistoryFunc : null,
    onUpdateHistory: loadMockGroups
  })
}
const searchHistories = (query, config) => {
  if (!currentGroup.value?.id) return Promise.resolve({ resultData: [], infos: {} })
  return histories(currentGroup.value?.id, query, config).then(data => {
    if (data.infos?.scenarioMap) {
      Object.assign(scenarioMap.value, data.infos.scenarioMap)
    }
    return data
  })
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
        <div class="group-page-header">
          <div class="group-page-header__main">
            <span class="text-large font-600">
              {{ mockProject.projectName }}
            </span>
            <el-text
              v-if="mockProject.userName"
              type="info"
              tag="b"
            >
              ({{ $t('mock.label.owner') }}:
              <el-text
                v-if="mockProject.userName"
                type="success"
                tag="b"
              >
                {{ mockProject.userName }}
              </el-text>
              )
            </el-text>
            <el-tag
              v-for="tag in projectHeaderTags"
              :key="tag.key"
              size="small"
              :type="tag.type"
            >
              {{ tag.label }}
            </el-tag>
          </div>
          <el-link
            type="primary"
            class="group-page-header__back"
            @click="$goto('/mock/groups')"
          >
            {{ $i18nKey('common.label.commonBack','mock.label.mockGroups') }}
          </el-link>
        </div>
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
          v-if="projectWritable"
          type="info"
          @click="newOrEdit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
        <el-button
          v-if="projectWritable"
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
          v-if="selectedRows?.length > 1 && selectedRowsReadable"
          type="warning"
          @click="copyGroups(selectedRows)"
        >
          {{ $t('common.label.copy') }}
        </el-button>
        <el-button
          v-if="selectedRows?.length > 1 && selectedRowsDeletable"
          type="success"
          @click="moveGroups(selectedRows)"
        >
          {{ $t('common.label.move') }}
        </el-button>
        <el-button
          v-if="selectedRows?.length&&selectedRowsDeletable"
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
      :editable="currentGroupEditable"
      inline-auto-mode
      width="800px"
      label-width="120px"
      :window-attrs="{modal:false,modalPenetrable:true}"
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
      inline-auto-mode
      :form-options="editProjectFormOptions"
      :name="$t('mock.label.mockProjects')"
      :save-current-item="saveProjectItem"
      label-width="130px"
    />
    <mock-group-import
      v-model="showImportWindow"
      :default-user="searchParam.userName"
      :default-project-id="searchParam.projectId"
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
.group-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.group-page-header__main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.group-page-header__back {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .group-page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

</style>
