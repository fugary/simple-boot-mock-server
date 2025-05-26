import { useResourceApi } from '@/hooks/ApiHooks'
import { computed, ref } from 'vue'
import { $http } from '@/vendors/axios'
import { isAdminUser, isCurrentUser, useCurrentUserName } from '@/utils'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle } from '@/messages'
import { useFormStatus } from '@/consts/GlobalConstants'

const MOCK_PROJECT_URL = '/admin/projects'
const MockProjectApi = useResourceApi(MOCK_PROJECT_URL)

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

export const useSelectProjects = (searchParam, autoSelect) => {
  const projects = ref([])
  const projectOptions = ref([])
  const loadSelectProjects = (data, config) => {
    return selectProjects(data, config).then(result => {
      projects.value = result || []
      projectOptions.value = projects.value.map(project => {
        if (project.projectCode === MOCK_DEFAULT_PROJECT) {
          return { labelKey: 'mock.label.defaultProject', value: project.projectCode }
        }
        return { label: project.projectName, value: project.projectCode }
      })
    })
  }
  const loadProjectsAndRefreshOptions = async () => {
    await loadSelectProjects({
      userName: searchParam.value?.userName || useCurrentUserName(),
      publicFlag: searchParam.value?.publicFlag
    })
    const currentProj = projects.value.find(proj => proj.projectCode === searchParam.value.projectCode)
    if (autoSelect) {
      searchParam.value.projectCode = currentProj?.projectCode || MOCK_DEFAULT_PROJECT
    }
    if (isAdminUser() && currentProj?.userName) {
      searchParam.value.userName = currentProj.userName
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
    required: true,
    upperCase: true,
    rules: [{
      validator (_, value) {
        return /^[A-Z0-9_-]+$/ig.test(value)
      },
      message: $i18nBundle('mock.msg.projectCodeTooltip')
    }]
  }, {
    labelKey: 'mock.label.projectName',
    prop: 'projectName',
    required: true
  }, {
    labelKey: 'mock.label.publicMockProject',
    prop: 'publicFlag',
    type: 'switch'
  }, useFormStatus(), {
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
  return project && (isAdminUser() || isCurrentUser(project.userName) || isDefaultProject(project.projectCode))
}

export const copyMockProject = (id, config) => {
  return $http(Object.assign({
    url: `${MOCK_PROJECT_URL}/copyMockProject/${id}`,
    method: 'POST'
  }, config)).then(response => response.data)
}

export default MockProjectApi
