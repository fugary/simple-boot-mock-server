import { useResourceApi } from '@/hooks/ApiHooks'
import { $http, $httpPost } from '@/vendors/axios'
import { isArray } from 'lodash-es'

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

export const removeByIds = (ids, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/removeByIds/${ids}`,
    method: 'delete'
  }, config)).then(response => response.data)
}

export const IMPORT_DUPLICATE_STRATEGY = [{
  value: 1,
  label: '中止导入'
}, {
  value: 2,
  label: '跳过重复路径'
}, {
  value: 3,
  label: '自动生成新路径'
}]

export const IMPORT_TYPES = [{
  value: 'simple',
  label: '当前简单Mock服务'
}, {
  value: 'fastmock',
  label: '老fastmock服务（测试）'
}]

export const uploadFiles = (files, params = {}, config = {}) => {
  const formData = new FormData()
  files = isArray(files) ? files : [files]
  files.filter(file => file.raw).forEach(file => formData.append('files', file.raw))
  for (const key in params) {
    params[key] && formData.append(key, params[key])
  }
  return $httpPost(`${MOCK_GROUP_URL}/import`,
    formData, Object.assign({ headers: { 'Content-Type': 'multipart/form-data' }, loading: true }, config))
}

export default useResourceApi('/admin/groups')
