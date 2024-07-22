export const MOCK_DEFAULT_PROJECT = 'default'

export const SIMPLE_BOOT_MOCK_HEADER = 'simple-boot-mock'

export const MOCK_REQUEST_ID_HEADER = 'mock-request-id'

export const MOCK_DATA_ID_HEADER = 'mock-data-id'

export const MOCK_DATA_PREVIEW_HEADER = 'mock-data-preview'

export const MOCK_DATA_MATCH_PATTERN_HEADER = 'mock-data-match-pattern'

export const MOCK_DATA_PATH_PARAMS_HEADER = 'mock-data-path-params'

export const MOCK_META_DATA_REQ = 'mock-meta-req'

export const AUTHORIZATION_KEY = 'Authorization'

export const BEARER_KEY = 'Bearer'

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

export const AUTH_TYPE = {
  NONE: 'none',
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
