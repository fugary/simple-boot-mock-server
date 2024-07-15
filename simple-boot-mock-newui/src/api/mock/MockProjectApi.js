import { useResourceApi } from '@/hooks/ApiHooks'
import { ref } from 'vue'
import { $http } from '@/vendors/axios'

const MOCK_PROJECT_URL = '/admin/projects'
const MockProjectApi = useResourceApi(MOCK_PROJECT_URL)

/**
 * 加载当前用户可选项目
 * @return {Promise<T>}
 */
export const selectProjects = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_PROJECT_URL}/selectProjects`,
    method: 'post',
    data
  }, config)).then(response => response.data?.resultData)
}

export const useSelectProjects = () => {
  const projects = ref([])
  const projectOptions = ref([])
  const loadSelectProjects = (data, config) => {
    return selectProjects(data, config).then(result => {
      projects.value = result || []
      projectOptions.value = projects.value.map(project => ({ label: project.projectName, value: project.projectCode }))
    })
  }
  return {
    projects,
    projectOptions,
    loadSelectProjects
  }
}

export default MockProjectApi
