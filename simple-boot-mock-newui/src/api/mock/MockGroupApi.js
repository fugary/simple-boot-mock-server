import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'

export const MOCK_GROUP_URL = '/admin/groups'

export const downloadByLink = (downloadUrl) => {
  const downloadLink = document.createElement('a')
  downloadLink.href = downloadUrl
  downloadLink.download = 'download'
  downloadLink.click()
}

export const checkExport = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/checkExport`,
    method: 'post',
    data
  }, config)).then(response => response.data)
}

export default useResourceApi('/admin/groups')
