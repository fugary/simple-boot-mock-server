import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'
import { isAdminUser, isCurrentUser } from '@/utils'
import { isDefaultProject } from '@/consts/MockConstants'

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

export const checkProjectEdit = (project) => {
  return !!project && isProjectOwner(project)
}

export const copyMockProject = (data, config) => {
  return transferMockProject({
    ...data,
    action: 'copy'
  }, config)
}

export const transferMockProject = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_PROJECT_URL}/transfer`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export default MockProjectApi
