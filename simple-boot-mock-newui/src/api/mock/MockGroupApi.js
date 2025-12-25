import { useResourceApi } from '@/hooks/ApiHooks'
import { $http, $httpPost } from '@/vendors/axios'
import { isArray } from 'lodash-es'

export const MOCK_GROUP_URL = '/admin/groups'

export const downloadByLink = (downloadUrl, name) => {
  const downloadLink = document.createElement('a')
  downloadLink.href = downloadUrl
  downloadLink.download = name || 'download'
  downloadLink.click()
}

export const checkExport = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/checkExport`,
    method: 'post',
    data
  }, config)).then(response => response.data)
}

export const IMPORT_DUPLICATE_STRATEGY = [{
  value: 1,
  labelKey: 'mock.label.importDuplicateStrategyAbort'
}, {
  value: 2,
  labelKey: 'mock.label.importDuplicateStrategySkip'
}, {
  value: 3,
  labelKey: 'mock.label.importDuplicateStrategyGenerate'
}]

export const IMPORT_TYPES = [{
  value: 'simple',
  labelKey: 'mock.label.importTypeSimple'
}, {
  value: 'swagger',
  labelKey: 'mock.label.importTypeSwagger'
}, {
  value: 'postman',
  labelKey: 'mock.label.importTypePostman'
}, {
  value: 'fastmock',
  labelKey: 'mock.label.importTypeFastMock'
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

export const copyMockGroup = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/copyMockGroup/${data.groupId}`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export default useResourceApi('/admin/groups')
