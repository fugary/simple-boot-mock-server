import { useResourceApi } from '@/hooks/ApiHooks'
import { $http, $httpPost } from '@/vendors/axios'
import { isArray } from 'lodash-es'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'

export const MOCK_GROUP_URL = '/admin/groups'

export const normalizeGroupProjectRelation = (data = {}) => {
  const payload = { ...data }
  if (isDefaultProject(payload.projectCode)) {
    payload.projectId = null
    payload.projectCode = MOCK_DEFAULT_PROJECT
    return payload
  }
  if (payload.projectId != null) {
    delete payload.projectCode
    delete payload.userName
  }
  return payload
}

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
    data: normalizeGroupProjectRelation(data)
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
}, {
  value: 'har',
  labelKey: 'mock.label.importTypeHar'
}]

/**
 * 快速嗅探文件或文本的数据格式类型
 * @param {File|Blob|string} fileOrText 上传文件或字符串
 * @returns {Promise<string|null>} 'simple' | 'swagger' | 'postman' | 'har' | 'fastmock' | null
 */
export const detectImportFileType = async (fileOrText) => {
  if (!fileOrText) {
    return null
  }
  let text = ''
  if (typeof fileOrText === 'string') {
    text = fileOrText.length > 64 * 1024 ? fileOrText.slice(0, 64 * 1024) : fileOrText
  } else if (fileOrText.slice && typeof fileOrText.slice === 'function') {
    const sliceBlob = fileOrText.slice(0, 64 * 1024)
    if (typeof sliceBlob.text === 'function') {
      text = await sliceBlob.text()
    } else {
      text = await new Promise((resolve) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result || '')
        reader.onerror = () => resolve('')
        reader.readAsText(sliceBlob)
      })
    }
  }
  const trimmed = (text || '').trim()
  if (!trimmed) {
    return null
  }

  // 1. FastMock: 根为 JSON 数组，含 mockRule, folderId 或 url
  if (trimmed.startsWith('[')) {
    if (trimmed.includes('"mockRule"') || trimmed.includes('"folderId"') || trimmed.includes('"url"')) {
      return 'fastmock'
    }
  }

  // 2. JSON 对象格式
  if (trimmed.startsWith('{')) {
    // 2.1 HAR: 含有 "log" 且包含 "entries" 或 "version"
    if (trimmed.includes('"log"') && (trimmed.includes('"entries"') || trimmed.includes('"version"'))) {
      return 'har'
    }
    // 2.2 Postman: 包含 postman schema / _postman_id / "info" 与 "item"
    if (trimmed.includes('schema.getpostman.com') || trimmed.includes('_postman_id') ||
       (trimmed.includes('"info"') && trimmed.includes('"item"'))) {
      return 'postman'
    }
    // 2.3 Swagger / OpenAPI (JSON): 包含 openapi / swagger 或 paths 与 info
    if (trimmed.includes('"openapi"') || trimmed.includes('"swagger"') ||
       (trimmed.includes('"paths"') && trimmed.includes('"info"'))) {
      return 'swagger'
    }
    // 2.4 Simple Boot Mock: 包含 "groups" 且包含 groupName / requests / groupPath
    if (trimmed.includes('"groups"') && (trimmed.includes('"groupName"') || trimmed.includes('"requests"') || trimmed.includes('"groupPath"'))) {
      return 'simple'
    }
  }

  // 3. Swagger / OpenAPI (YAML 格式)
  if (/^openapi\s*:\s*['"]?3\./m.test(trimmed) ||
      /^swagger\s*:\s*['"]?2\./m.test(trimmed) ||
      (/^paths\s*:/m.test(trimmed) && /^info\s*:/m.test(trimmed))) {
    return 'swagger'
  }

  return null
}

export const uploadFiles = (files, params = {}, config = {}) => {
  const formData = new FormData()
  files = isArray(files) ? files : [files]
  files.filter(file => file.raw).forEach(file => formData.append('files', file.raw))
  params = normalizeGroupProjectRelation(params)
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
    data: normalizeGroupProjectRelation(data)
  }, config)).then(response => response.data)
}

export const transferMockGroup = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/transfer`,
    method: 'POST',
    data: normalizeGroupProjectRelation(data)
  }, config)).then(response => response.data)
}

export const histories = (id, data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/histories/${id}`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export const loadHistoryDiff = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/loadHistoryDiff`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export const recoverFromHistory = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_GROUP_URL}/recoverFromHistory`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

const resourceApi = useResourceApi('/admin/groups')

export default {
  ...resourceApi,
  search (params, config) {
    return resourceApi.search(normalizeGroupProjectRelation(params), config)
  },
  saveOrUpdate (data, config) {
    return resourceApi.saveOrUpdate(normalizeGroupProjectRelation(data), config)
  }
}
