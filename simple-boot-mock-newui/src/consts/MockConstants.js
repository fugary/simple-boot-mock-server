import { markRaw } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { $i18nBundle } from '@/messages'

export const MOCK_DEFAULT_PROJECT = 'default'

export const SIMPLE_BOOT_MOCK_HEADER = 'simple-boot-mock'

export const MOCK_REQUEST_ID_HEADER = 'mock-request-id'

export const MOCK_DATA_ID_HEADER = 'mock-data-id'

export const MOCK_DATA_USER_HEADER = 'mock-data-user'

export const MOCK_DATA_PREVIEW_HEADER = 'mock-data-preview'

export const MOCK_DATA_MATCH_PATTERN_HEADER = 'mock-data-match-pattern'

export const MOCK_DATA_PATH_PARAMS_HEADER = 'mock-data-path-params'

export const MOCK_META_DATA_REQ = 'mock-meta-req'

export const AUTHORIZATION_KEY = 'Authorization'

export const BEARER_KEY = 'Bearer'

export const isDefaultProject = projectCode => MOCK_DEFAULT_PROJECT === projectCode

export const DEFAULT_HEADERS = ['Accept',
  'Accept-Charset',
  'Accept-Encoding',
  'Accept-Language',
  'Authorization',
  'Cookie',
  'Connection',
  'Content-Type',
  'Origin',
  'Pragma',
  'User-Agent'
]
export const NONE = 'none'
export const FORM_DATA = 'formData'
export const FORM_URL_ENCODED = 'formUrlencoded'
export const SPECIAL_LANGS = [NONE, FORM_DATA, FORM_URL_ENCODED]

export const LANG_TO_CONTENT_TYPES = {
  json: 'application/json',
  javascript: 'application/json',
  html: 'application/xml',
  xmlWithJs: 'application/xml',
  text: 'text/plain',
  css: 'text/css',
  [FORM_DATA]: 'multipart/form-data',
  [FORM_URL_ENCODED]: 'application/x-www-form-urlencoded'
}

export const CONTENT_TYPES_TO_LANG = {
  'application/json': 'json',
  'application/xml': 'html',
  'application/javascript': 'javascript',
  'text/javascript': 'javascript',
  'text/html': 'html',
  'text/plain': 'text',
  'text/css': 'css'
}

export const calcContentType = (lang, body, currentContentType) => {
  if (isStreamContentType(currentContentType)) {
    return currentContentType
  }
  if (lang === 'html' && body?.includes('<!DOCTYPE html>')) {
    return 'text/html'
  }
  return LANG_TO_CONTENT_TYPES[lang]
}

export const calcContentLanguage = contentType => {
  if (contentType) {
    const charIndex = contentType?.indexOf(';')
    if (charIndex > -1) {
      contentType = contentType.substring(0, charIndex)
    }
    return CONTENT_TYPES_TO_LANG[contentType]
  }
}

export const AUTH_TYPE = {
  NONE,
  BASIC: 'basic',
  TOKEN: 'token',
  JWT: 'jwt'
}

export const AUTH_PARAM_NAMES = [AUTHORIZATION_KEY, 'accessToken', 'access_token', 'token', 'jwt_token', 'api_key', 'X-API-Key']
export const AUTH_PREFIX_NAMES = [BEARER_KEY, 'Basic']

export const AUTH_OPTIONS = [{
  value: AUTH_TYPE.NONE,
  labelKey: 'mock.label.authTypeNone'
}, {
  value: AUTH_TYPE.BASIC,
  labelKey: 'mock.label.authTypeBasic'
}, {
  value: AUTH_TYPE.TOKEN,
  labelKey: 'mock.label.authTypeToken'
}, {
  value: AUTH_TYPE.JWT,
  labelKey: 'mock.label.authTypeJWT'
}]

export const ALL_CONTENT_TYPES_LIST = [
  { contentType: 'application/json', text: true },
  { contentType: 'application/xml', text: true },
  { contentType: 'text/html', text: true },
  { contentType: 'text/plain', text: true },
  { contentType: 'text/css', text: true },
  { contentType: 'application/javascript', text: true },
  { contentType: 'text/javascript', text: true },
  { contentType: 'application/x-www-form-urlencoded', response: false },
  { contentType: 'multipart/form-data', response: false },
  { contentType: 'image/png' },
  { contentType: 'image/jpeg' },
  { contentType: 'image/gif' },
  { contentType: 'application/pdf' },
  { contentType: 'application/zip' },
  { contentType: 'audio/mpeg' },
  { contentType: 'audio/ogg' },
  { contentType: 'video/mp4' },
  { contentType: 'video/ogg' },
  { contentType: 'application/graphql', text: true },
  { contentType: 'application/yaml', text: true },
  { contentType: 'text/markdown', text: true },
  { contentType: 'application/octet-stream' },
  { contentType: 'application/vnd.ms-excel' },
  { contentType: 'application/msword' },
  { contentType: 'application/vnd.ms-powerpoint' }
]

export const isStreamContentType = contentType => {
  if (contentType) {
    if (['text', '*', 'json'].find(keyword => contentType?.includes(keyword))) {
      return false
    }
    return !ALL_CONTENT_TYPES_LIST.find(content => contentType?.includes(content.contentType))?.text
  }
}

export const getMockConfirmConfig = (config) => {
  return {
    type: 'info',
    icon: markRaw(InfoFilled),
    cancelButtonClass: 'el-button--success',
    confirmButtonText: $i18nBundle('mock.label.downloadAsFile'),
    cancelButtonText: $i18nBundle('mock.label.previewAsText'),
    distinguishCancelAndClose: true,
    ...config
  }
}
