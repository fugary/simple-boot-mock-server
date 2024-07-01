import { useResourceApi } from '@/hooks/ApiHooks'
import { BASE_URL } from '@/config'

export const ALL_METHODS = [
  { method: 'GET', type: 'primary' },
  { method: 'POST', type: 'success' },
  { method: 'DELETE', type: 'danger' },
  { method: 'PUT', type: 'warning' },
  { method: 'PATCH', type: 'info' }]

/**
 * mock地址信息
 * @param path
 * @return {`${string}${string}`}
 */
export const getMockUrl = (path) => {
  let baseUrl = location.origin
  if (!/^https?:\/\//.test(BASE_URL)) {
    baseUrl = BASE_URL
  }
  return `${baseUrl}${path}`
}

export default useResourceApi('/admin/requests')
