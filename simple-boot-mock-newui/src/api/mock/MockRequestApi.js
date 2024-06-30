import { useResourceApi } from '@/hooks/ApiHooks'

export const ALL_METHODS = [
  { method: 'GET', type: 'primary' },
  { method: 'POST', type: 'success' },
  { method: 'DELETE', type: 'danger' },
  { method: 'PUT', type: '' },
  { method: 'PATCH', type: 'warning' }]

export default useResourceApi('/admin/requests')
