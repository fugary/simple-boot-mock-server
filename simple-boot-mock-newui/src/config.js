/**
 * 默认单页数量
 * @type {number}
 */
export const PAGE_SIZE = 10

/**
 * 参数保存过期时间，单位分钟
 * @type {number}
 */
export const SEARCH_PARAM_TIMEOUT = 10

/**
 * 默认是否用全局loading
 *
 * @type {boolean}
 */
export const GLOBAL_LOADING = false

/**
 * 新自定义loading用于route加载
 *
 * @type {boolean}
 */
export const GLOBAL_ROUTE_NEW_LOADING = true

/**
 * 默认是否有全局错误消息
 *
 * @type {boolean}
 */
export const GLOBAL_ERROR_MESSAGE = true

/**
 * 默认是否启用路由Loading
 *
 * @type {boolean}
 */
export const GLOBAL_ROUTE_LOADING = true

/**
 * loading延迟，单位毫秒
 * @type {number}
 */
export const LOADING_DELAY = 200

/**
 * 国际化开关，国际化未开发完成前可能需要关闭
 * @type {boolean}
 */
export const I18N_ENABLED = true

/**
 * 黑色主题开关
 * @type {boolean}
 */
export const THEME_ENABLED = true

/**
 * 自动layout开关
 * @type {boolean}
 */
export const AUTO_LAYOUT_ENABLED = true

/**
 * 最大缓存页面数量
 * @type {Number}
 */
export const TAB_MODE_MAX_CACHES = 8

/**
 * 是否有记住参数功能
 * @type {boolean}
 */
export const REMEMBER_SEARCH_PARAM_ENABLED = true

/**
 * 默认分页数据
 *
 * @param pageSize
 * @return {CommonPage}
 */
export const useDefaultPage = (pageSize = PAGE_SIZE) => {
  return {
    pageSize,
    pageNumber: 1
  }
}

export const BASE_URL = import.meta.env.VITE_APP_API_BASE_URL

export const SYSTEM_KEY = import.meta.env.VITE_APP_SYSTEM_KEY

export const APP_VERSION = import.meta.env.VITE_APP_VERSION
