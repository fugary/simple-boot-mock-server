import axios from 'axios'

import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $i18nBundle } from '@/messages'
import { ElMessage } from 'element-plus'
import { debounce, isString } from 'lodash-es'
import { $coreHideLoading, $coreShowLoading, $goto } from '@/utils'

import { GLOBAL_ERROR_MESSAGE, GLOBAL_LOADING } from '@/config'

export const $http = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL,
  timeout: import.meta.env.VITE_APP_API_TIMEOUT
})

const hasLoading = config => {
  return config?.loading ?? GLOBAL_LOADING
}

/**
 * @param config {ServiceRequestConfig}
 * @return {*|boolean}
 */
const showErrorMessage = config => {
  return config?.showErrorMessage ?? GLOBAL_ERROR_MESSAGE
}

$http.interceptors.request.use(/** @param config {ServiceRequestConfig} */ config => {
  const globalConfigStore = useGlobalConfigStore()
  const loginConfigStore = useLoginConfigStore()
  config.headers.locale = globalConfigStore.currentLocale
  if (config.addToken !== false && loginConfigStore.accessToken) { // 添加token
    config.headers.Authorization = `Bearer ${loginConfigStore.accessToken}`
  }
  if (config.addToken !== false && !loginConfigStore.accessToken && !config.isLogin) {
    return false // 处理登出是调用接口出现异常问题
  }
  if (hasLoading(config)) {
    $coreShowLoading(isString(config.loading) ? config.loading : undefined)
  }
  return config
})

const networkErrorFun = debounce(() => ElMessage.error($i18nBundle('common.msg.networkError')), 300)
const networkTimeoutFun = debounce(() => ElMessage.error($i18nBundle('common.msg.networkTimeout')), 300)

$http.interceptors.response.use(response => {
  if (hasLoading(response.config)) {
    $coreHideLoading()
  }
  if (response && response.data && !response.data.success && response.data.message) {
    if (response.config && showErrorMessage(response.config)) {
      ElMessage.error(response.data.message)
    }
  }
  return response
}, error => {
  if (hasLoading(error?.config)) {
    $coreHideLoading()
  }
  console.info('=========================axios', error)
  if (error.message === 'Network Error') {
    networkErrorFun()
  } else if (error.code === 'ECONNABORTED' && error.message.indexOf('timeout') > -1) {
    networkTimeoutFun()
  }
  if (error.response?.status === 401 && !error.response?.config.isLogin) {
    // 跳转登录页面
    $goto('/login')
  }
  return error.response
})

/**
 * @typedef {AxiosRequestConfig} ServiceRequestConfig
 * @property {number} [timeout] 超时时间
 * @property {boolean|string} [loading] 是否显示loading，默认不显示
 * @property {boolean} [addToken] 是否添加token
 * @property {boolean} [showErrorMessage] 是否显示自动错误信息
 */
/**
 * @param url URL地址
 * @param {object} [data] 数据对象
 * @param [config] {ServiceRequestConfig} 配置对象
 * @return {Promise<unknown>}
 */
export const $httpPost = (url, data, config) => {
  return $http.post(url, data, config).then(response => {
    if (response?.data) {
      return response.data // 只要有数据就认为成功，内容再解析
    } else {
      throw new Error('No response data')
    }
  })
}
/**
 * @param url URL地址
 * @param [config] {ServiceRequestConfig} 配置对象
 * @return {Promise<unknown>}
 */
export const $httpGet = (url, config) => {
  return $http.get(url, config).then(response => {
    if (response?.data) {
      return response.data // 只要有数据就认为成功，内容再解析
    } else {
      throw new Error('No response data')
    }
  })
}
