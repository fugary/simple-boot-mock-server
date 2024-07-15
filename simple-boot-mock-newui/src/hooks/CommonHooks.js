import { ref, watch } from 'vue'
import { isFunction, isNumber } from 'lodash-es'
import { useGlobalSearchParamStore } from '@/stores/GlobalSearchParamStore'

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
