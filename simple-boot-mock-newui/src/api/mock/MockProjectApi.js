import { useResourceApi } from '@/hooks/ApiHooks'
import { computed, ref } from 'vue'
import { $http } from '@/vendors/axios'
import { getStyleGrow, isAdminUser, isCurrentUser, useCurrentUserName } from '@/utils'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle } from '@/messages'
import { useFormStatus } from '@/consts/GlobalConstants'

const MOCK_PROJECT_URL = '/admin/projects'
const MockProjectApi = useResourceApi(MOCK_PROJECT_URL)
const FULL_PROJECT_AUTHORITIES = ['readable', 'writable', 'deletable']

export const sortProjects = (projects = []) => {
  return [...projects].sort((left, right) => {
    const leftPriority = isDefaultProject(left?.projectCode) ? 0 : (isCurrentUser(left?.userName) ? 1 : 2)
    const rightPriority = isDefaultProject(right?.projectCode) ? 0 : (isCurrentUser(right?.userName) ? 1 : 2)
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority
    }
    return Number(right?.id || 0) - Number(left?.id || 0)
  })
}

const normalizeAuthorities = (authorities) => {
  if (Array.isArray(authorities)) {
    return authorities.filter(Boolean)
  }
  if (typeof authorities === 'string') {
    return authorities.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

const isOwnedDefaultProject = (project) => {
  return isDefaultProject(project?.projectCode) && isCurrentUser(project?.userName)
}

const isProjectOwner = (project) => {
  if (!project) {
    return false
  }
  if (isAdminUser()) {
    return true
  }
  if (isOwnedDefaultProject(project)) {
    return true
  }
  return isCurrentUser(project.userName)
}

export const getProjectAuthorities = (project) => {
  if (!project) {
    return []
  }
  if (isProjectOwner(project)) {
    return [...FULL_PROJECT_AUTHORITIES]
  }
  const sharedUser = project.projectUsers?.find(item => isCurrentUser(item.userName))
  return normalizeAuthorities(sharedUser?.authorities)
}

export const hasProjectAuthority = (project, authority) => {
  const authorities = getProjectAuthorities(project)
  if (!authority) {
    return authorities.length > 0
  }
  return authorities.includes(authority)
}

export const checkProjectReadable = (project) => hasProjectAuthority(project, 'readable')

export const checkProjectWritable = (project) => hasProjectAuthority(project, 'writable')

export const checkProjectDeletable = (project) => hasProjectAuthority(project, 'deletable')

/**
 * 加载当前用户可选项目
 * @return {Promise<T>}
 */
export const selectProjects = (params, config) => {
  return $http(Object.assign({
    url: `${MOCK_PROJECT_URL}/selectProjects`,
    method: 'get',
    params
  }, config)).then(response => response.data?.resultData)
}

export const assignProjectValue = (target, project) => {
  if (!target) {
    return
  }
  if (!project) {
    target.projectId = null
    target.projectCode = null
    return
  }
  const defaultProject = isDefaultProject(project.projectCode)
  target.projectId = defaultProject ? null : (project.projectId || project.id || null)
  target.projectCode = project.projectCode || (defaultProject ? MOCK_DEFAULT_PROJECT : null)
}

export const findProjectByValue = (projects = [], searchParam = {}) => {
  if (searchParam?.projectId != null) {
    return projects.find(proj => `${proj.id}` === `${searchParam.projectId}`)
  }
  if (searchParam?.projectCode) {
    return projects.find(proj => proj.projectCode === searchParam.projectCode)
  }
  return null
}

export const useSelectProjects = (searchParam, autoSelect) => {
  const projects = ref([])
  const projectOptions = ref([])
  const loadSelectProjects = (data, config) => {
    return selectProjects(data, config).then(result => {
      projects.value = sortProjects(result || [])
      projectOptions.value = projects.value.map(project => {
        if (project.projectCode === MOCK_DEFAULT_PROJECT) {
          return {
            labelKey: 'mock.label.defaultProject',
            value: project.projectCode,
            projectCode: project.projectCode,
            projectId: null,
            userName: project.userName
          }
        }
        const label = project.userName && project.userName !== useCurrentUserName()
          ? `${project.projectName} - ${project.userName}`
          : project.projectName
        return {
          label,
          value: project.projectCode,
          userName: project.userName,
          projectId: project.id,
          projectCode: project.projectCode
        }
      })
    })
  }
  const loadProjectsAndRefreshOptions = async () => {
    await loadSelectProjects({
      userName: searchParam.value?.userName || useCurrentUserName(),
      publicFlag: searchParam.value?.publicFlag,
      projectId: searchParam.value?.projectId || undefined,
      onlyMine: searchParam.value?.onlyMine || undefined
    })
    const currentProj = findProjectByValue(projects.value, searchParam.value)
    if (autoSelect) {
      assignProjectValue(searchParam.value, currentProj || { projectCode: MOCK_DEFAULT_PROJECT, projectId: null })
      if (!searchParam.value.projectCode) {
        searchParam.value.projectCode = MOCK_DEFAULT_PROJECT
      }
    } else if (currentProj?.projectCode) {
      assignProjectValue(searchParam.value, currentProj)
    } else {
      searchParam.value.projectId = null
      searchParam.value.projectCode = null
    }
  }
  return {
    projects,
    projectOptions,
    loadSelectProjects,
    loadProjectsAndRefreshOptions
  }
}
/**
 * 编辑项目的工具
 *
 * @param searchParam
 * @param userOptions
 * @returns {{showEditWindow: Ref<UnwrapRef<boolean>, UnwrapRef<boolean> | boolean>, newOrEditProject: ((function(*, *): Promise<void>)|*), editFormOptions: ComputedRef<CommonFormOption|CommonFormOption[]>, currentProject: Ref<any>}}
 */
export const useProjectEditHook = (searchParam, userOptions) => {
  const showEditWindow = ref(false)
  const currentProject = ref()
  const newOrEditProject = async (id, $event) => {
    $event?.stopPropagation()
    if (id) {
      await MockProjectApi.getById(id).then(data => {
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
    labelKey: 'mock.label.projectCode',
    prop: 'projectCode',
    tooltip: $i18nBundle('mock.msg.projectCodeTooltip'),
    enabled: !!currentProject.value?.id,
    rules: [{
      validator (_, value) {
        return !value || /^[A-Z0-9_-]+$/ig.test(value)
      },
      message: $i18nBundle('mock.msg.projectCodeTooltip')
    }],
    attrs: {
      clearable: true
    }
  }, {
    labelKey: 'mock.label.projectName',
    prop: 'projectName',
    required: true
  }, { ...useFormStatus(), style: getStyleGrow(4) }, {
    labelKey: 'mock.label.publicMockProject',
    style: getStyleGrow(6),
    prop: 'publicFlag',
    type: 'switch'
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }]))
  return {
    currentProject,
    newOrEditProject,
    showEditWindow,
    editFormOptions
  }
}

export const checkProjectEdit = (project) => {
  return !!project && isProjectOwner(project)
}

export const copyMockProject = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_PROJECT_URL}/copyMockProject/${data.projectId}`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export default MockProjectApi
