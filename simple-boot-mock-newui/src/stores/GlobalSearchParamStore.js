import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { isObject, merge } from 'lodash-es'
import { useRoute } from 'vue-router'
import { REMEMBER_SEARCH_PARAM_ENABLED, SEARCH_PARAM_TIMEOUT } from '@/config'
import dayjs from 'dayjs'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { LoadSaveParamMode } from '@/consts/GlobalConstants'

export const useGlobalSearchParamStore = defineStore('globalSearchParam', () => {
  const globalParams = ref({})
  const route = useRoute()
  const rememberSearchParam = computed(() => REMEMBER_SEARCH_PARAM_ENABLED &&
      useGlobalConfigStore().loadSaveParamMode !== LoadSaveParamMode.NEVER)

  const getCurrentParamByPath = (defaultParam, path) => {
    isSaveParamBack.value = false
    if (rememberSearchParam.value && path && globalParams.value[path]) {
      /**
       * @type {SaveParam}
       */
      const saveParam = globalParams.value[path]
      if (isParamValid(saveParam)) {
        return merge({}, defaultParam, saveParam.formParam)
      }
    }
    return defaultParam
  }
  const getCurrentParam = (defaultParam) => {
    if (!rememberSearchParam.value || (useGlobalConfigStore().loadSaveParamMode === LoadSaveParamMode.BACK && !isSaveParamBack.value)) {
      return { ...defaultParam }
    }
    return getCurrentParamByPath(defaultParam, route.path)
  }

  /**
   * @typedef {Object} SaveParam
   * @property {string} formParam
   * @property {string} date
   * @property {number} timeout
   */
  /**
   * @param param {SaveParam}
   */
  const isParamValid = param => {
    return param.date && param.timeout && dayjs(param.date).add(param.timeout, 'minute').isAfter(dayjs())
  }

  const isSaveParamBack = ref(false)
  const savedParamRouteInfo = ref()

  return {
    rememberSearchParam,
    globalParams,
    getCurrentParam,
    getCurrentParamByPath,
    isSaveParamBack,
    savedParamRouteInfo,
    setSaveParamBack: (value) => {
      isSaveParamBack.value = !!value
    },
    /**
     * @param value {Object}
     * @param path {string|{path?:string,timeout?:number}}
     */
    saveCurrentParam (value, path) {
      if (!rememberSearchParam.value) {
        return
      }
      const config = {}
      Object.assign(config, isObject(path) ? path : { path })
      path = config.path || route.path
      if (path) {
        globalParams.value[path] = {
          formParam: value,
          date: new Date(),
          timeout: config.timeout || SEARCH_PARAM_TIMEOUT
        }
      } else {
        throw new Error('Path is required')
      }
    }
  }
}, {
  persist: true
})
