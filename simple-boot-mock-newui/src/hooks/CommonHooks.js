import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isFunction, isNumber, uniqueId } from 'lodash-es'
import { useGlobalSearchParamStore } from '@/stores/GlobalSearchParamStore'
import { $coreHideLoading, $coreShowLoading } from '@/utils'
import { GLOBAL_LOADING } from '@/config'
import Sortable from 'sortablejs'

const defaultPageProcessor = (searchResult, searchParam) => {
  if (searchResult.page && searchParam.value.page) {
    Object.assign(searchParam.value.page, searchResult.page)
  }
}

/**
 * 通用搜索表格表单封装
 * @param {CommonTableAndSearchForm} param 参数
 * @return {CommonTableAndSearchResult} 返回数据
 */
export const useTableAndSearchForm = ({
  searchMethod,
  defaultParam = {},
  dataProcessor = searchResult => searchResult.resultData,
  pageProcessor = defaultPageProcessor,
  saveParam = true
}) => {
  const globalSearchParamStore = useGlobalSearchParamStore()
  const tableData = ref([])
  const loading = ref(false)
  const searchParam = ref(saveParam ? globalSearchParamStore.getCurrentParam(defaultParam) : defaultParam)
  const searchTableItems = async (pageNumber, newParams = {}) => {
    if (isNumber(pageNumber) && searchParam.value) {
      searchParam.value?.page && (searchParam.value.page.pageNumber = pageNumber)
    }
    loading.value = true
    saveParam && globalSearchParamStore.saveCurrentParam(searchParam.value)
    const searchResult = await searchMethod({ ...searchParam.value, ...newParams })
      .finally(() => { loading.value = false })
    loading.value = false
    if (searchResult.success && searchResult.resultData) {
      tableData.value = isFunction(dataProcessor) && dataProcessor?.(searchResult, searchParam)
      pageProcessor?.(searchResult, searchParam)
    }
    return searchResult
  }
  return {
    tableData,
    loading,
    searchParam,
    searchMethod: searchTableItems
  }
}

export const useInitLoadOnce = (loader, config = {}) => {
  const initLoading = ref(false)
  const initLoadOnce = async () => {
    if (!initLoading.value) {
      try {
        initLoading.value = true
        if (config.loading ?? GLOBAL_LOADING) {
          $coreShowLoading()
        }
        await loader()
      } finally {
        initLoading.value = false
        if (config.loading ?? GLOBAL_LOADING) {
          $coreHideLoading()
        }
      }
    }
  }
  return {
    initLoading,
    initLoadOnce
  }
}

export const useDateStr = (watchFn) => {
  const dateStr = ref(new Date().getTime())
  watchFn && watch(watchFn, (update) => {
    if (update) {
      dateStr.value = new Date().getTime()
    }
  })
  return { dateStr }
}

export const useGlobalSaveSearchParam = (defaultParam) => {
  const globalSearchParamStore = useGlobalSearchParamStore()
  const searchParam = ref(globalSearchParamStore.getCurrentParam(defaultParam))
  return {
    searchParam,
    /**
     * @param [path]
     */
    saveSearchParam: (path) => {
      globalSearchParamStore.saveCurrentParam(searchParam.value, path)
    }
  }
}

export const useSortableParams = (params, selector) => {
  let sortable = null
  const sortableRef = ref()
  onMounted(() => {
    sortable = new Sortable(sortableRef.value.$el, {
      animation: 150,
      draggable: selector,
      onEnd (event) {
        const { oldIndex, newIndex } = event
        params.value.splice(newIndex, 0, params.value.splice(oldIndex, 1)[0]) // 插入到 newIndex 位置
      }
    })
  })
  onBeforeUnmount(() => {
    sortable?.destroy()
    sortable = null
  })
  return {
    sortableRef
  }
}

export const useRenderKey = () => {
  const renderKeyMap = new WeakMap()

  function renderKey (param) {
    if (param.id != null) {
      return `param-${param.id}`
    }
    if (!renderKeyMap.has(param)) {
      renderKeyMap.set(param, uniqueId())
    }
    return `param-tmp-${renderKeyMap.get(param)}`
  }

  return {
    renderKey
  }
}
