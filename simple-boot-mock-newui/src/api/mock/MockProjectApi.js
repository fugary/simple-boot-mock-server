import { useResourceApi } from '@/hooks/ApiHooks'
import { ref } from 'vue'
import { $http } from '@/vendors/axios'
import { isAdminUser, useCurrentUserName } from '@/utils'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'

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

export const useSelectProjects = (searchParam) => {
  const projects = ref([])
  const projectOptions = ref([])
  const loadSelectProjects = (data, config) => {
    return selectProjects(data, config).then(result => {
      projects.value = result || []
      projectOptions.value = projects.value.map(project => ({ label: project.projectName, value: project.projectCode }))
    })
  }
  const loadProjectsAndRefreshOptions = async () => {
    await loadSelectProjects({
      userName: searchParam.value?.userName || useCurrentUserName()
    })
    const currentProj = projects.value.find(proj => proj.projectCode === searchParam.value.projectCode)
    searchParam.value.projectCode = currentProj?.projectCode || MOCK_DEFAULT_PROJECT
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

export default MockProjectApi
