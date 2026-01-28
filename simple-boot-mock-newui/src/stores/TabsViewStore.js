import { ref, watch, nextTick } from 'vue'
import { defineStore } from 'pinia'
import {
  checkReplaceHistoryShouldReplace,
  isNestedRoute
} from '@/route/RouteUtils'
import { TAB_MODE_MAX_CACHES } from '@/config'

const pureTab = tab => {
  if (tab) {
    return {
      ...tab,
      matched: undefined
    }
  }
}

const serializer = {
  serialize (state) {
    const newState = { ...state }
    newState.historyTabs = state.historyTabs?.map(pureTab)
    newState.currentTabItem = pureTab(state.currentTabItem)
    return JSON.stringify(newState)
  },
  deserialize: JSON.parse
}

/**
 * @typedef {Object} TabsViewStore
 * @property {boolean} isTabMode 是否开启tab模式
 * @property {boolean} isCachedTabMode 是否开启tab缓存
 * @property {boolean} isShowTabIcon 是否显示tab的图标
 * @property {[import('vue-router').RouteRecordRaw]} historyTabs 历史tab列表
 * @property {[string]} cachedTabs 缓存的tab列表
 * @method removeHistoryTab
 */
/**
 * @return {TabsViewStore}
 */
export const useTabsViewStore = defineStore('tabsView', () => {
  const isTabMode = ref(false)
  const isMainMaximized = ref(false)
  const isCachedTabMode = ref(true)
  const isShowTabIcon = ref(true)
  const currentTab = ref('')
  const currentTabItem = ref(null)
  const maxCacheCount = ref(TAB_MODE_MAX_CACHES)
  /**
   * @type {{value: [import('vue-router').RouteRecordRaw]}}
   */
  const historyTabs = ref([])
  /**
   * @type {{value: [string]}}
   */
  const cachedTabs = ref([])

  const clearAllTabs = () => {
    historyTabs.value = []
    cachedTabs.value = []
  }

  const clearHistoryTabs = () => {
    if (historyTabs.value.length) {
      let idx = historyTabs.value.findIndex(v => currentTab.value && v.path === currentTab.value)
      idx = idx > -1 ? idx : 0
      const tab = historyTabs.value[idx]
      removeOtherHistoryTabs(tab)
    }
  }

  const findHistoryTab = (path) => {
    let idx = historyTabs.value.findIndex(v => v.path === path)
    if (idx === -1) {
      idx = historyTabs.value.findIndex(v => v.fullPath === path)
    }
    if (idx > -1) {
      return historyTabs.value[idx]
    }
  }

  const isHomeTab = tab => !!tab?.meta?.homeFlag

  const getNestedParentTab = tab => isNestedRoute(tab) && tab.matched?.length >= 2 ? tab.matched[tab.matched.length - 2] : tab

  const addNestedParentTab = (tab, replaceTab) => {
    if (isCachedTabMode.value && !forceNotCache(tab)) {
      const parentTab = getNestedParentTab(tab)
      addCachedTab(parentTab, replaceTab)
    }
  }

  const addHistoryTab = (tab) => {
    // 添加tab
    if (isTabMode.value) {
      const idx = historyTabs.value.findIndex(v => v.path === tab.path)
      if (idx < 0) {
        const replaceIdx = historyTabs.value.findIndex(v => checkReplaceHistoryShouldReplace(v, tab))
        let replaceTab = null
        if (replaceIdx > -1) {
          replaceTab = historyTabs.value[replaceIdx]
          historyTabs.value.splice(replaceIdx, 1, Object.assign({}, tab))
        } else {
          // 可能是Proxy，需要解析出来
          isHomeTab(tab) ? historyTabs.value.unshift({ ...tab }) : historyTabs.value.push({ ...tab })
        }
        tab.accessTime = Date.now()
        if (isNestedRoute(tab)) {
          addNestedParentTab(tab, replaceTab)
        } else {
          addCachedTab(tab, replaceTab)
        }
      } else {
        historyTabs.value[idx].accessTime = Date.now()
      }
    }
  }

  const findLastTab = () => {
    return historyTabs.value.toSorted((a, b) => b.accessTime - a.accessTime)?.[0]
  }

  const removeHistoryTab = tab => {
    if (historyTabs.value.length > 1) {
      const idx = historyTabs.value.findIndex(v => v.path === tab.path)
      if (idx > -1) {
        removeCachedTab(historyTabs.value[idx])
        // 删除tab
        historyTabs.value.splice(idx, 1)
      }
      return findLastTab()
    }
  }

  const removeOtherHistoryTabs = tab => {
    historyTabs.value = [tab]
    cachedTabs.value = []
    if (isCachedTabMode.value && tab.name) {
      cachedTabs.value = [tab.name]
    }
  }

  const removeHistoryTabs = (tab, type) => {
    if (tab) {
      const idx = historyTabs.value.findIndex(v => v.path === tab.path)
      if (idx < 0) {
        return
      }
      let removeTabs = []
      if (type === 'right') {
        removeTabs = historyTabs.value.splice(idx + 1)
      } else if (type === 'left') {
        removeTabs = historyTabs.value.splice(0, idx)
      }
      if (removeTabs.length) {
        removeTabs.forEach(removeCachedTab)
      }
    }
  }

  const forceNotCache = tab => {
    const noCacheRoute = tab.matched?.find(route => route.meta && route.meta.cache === false)
    return !!noCacheRoute
  }

  const addCachedTab = (tab, replaceTab) => {
    if (isCachedTabMode.value && !forceNotCache(tab) && tab.name && !tab.name.includes('-')) {
      removeCachedTab(replaceTab)
      if (!cachedTabs.value.includes(tab.name)) {
        nextTick(() => { cachedTabs.value.push(tab.name) })
      }
    }
  }

  const removeCachedTab = tab => {
    if (tab) {
      tab = getNestedParentTab(tab)
      const idx = cachedTabs.value.findIndex(v => v === tab.name)
      if (idx > -1) {
        cachedTabs.value.splice(idx, 1)
      }
    }
  }

  const hasCloseDropdown = (tab, type) => {
    const idx = historyTabs.value.findIndex(v => v.path === tab.path)
    switch (type) {
      case 'close':
      case 'other':
        return historyTabs.value.length > 1
      case 'left':
        return idx !== 0
      case 'right':
        return idx !== historyTabs.value.length - 1
    }
  }

  const reIndexHistoryTab = (fromIndex, toIndex) => {
    const tabs = historyTabs.value
    tabs.splice(toIndex, 0, tabs.splice(fromIndex, 1)[0]) // 插入到 toIndex 位置
    console.log('新的tabs顺序：', fromIndex, toIndex, tabs.map(t => t.name))
  }

  watch(currentTab, path => {
    currentTabItem.value = historyTabs.value.find(v => path && v.path === path)
  })

  return {
    isMainMaximized,
    toggleMainFullscreen () {
      isMainMaximized.value = !isMainMaximized.value
    },
    isTabMode,
    isCachedTabMode,
    isShowTabIcon,
    maxCacheCount,
    currentTab,
    currentTabItem,
    historyTabs,
    cachedTabs,
    $customReset (initState) {
      Object.assign(initState, { // 保留部分配置
        isTabMode: isTabMode.value,
        isCachedTabMode: isCachedTabMode.value,
        isShowTabIcon: isShowTabIcon.value
      })
    },
    changeTabMode (val) {
      isTabMode.value = val
      if (!isTabMode.value) {
        clearAllTabs()
      }
    },
    changeCachedTabMode (val) {
      isCachedTabMode.value = val
      if (!isCachedTabMode.value) {
        cachedTabs.value = []
      }
    },
    removeHistoryTab,
    removeOtherHistoryTabs,
    removeHistoryTabs,
    clearAllTabs,
    clearHistoryTabs,
    findHistoryTab,
    addHistoryTab,
    addCachedTab,
    removeCachedTab,
    reIndexHistoryTab,
    hasCloseDropdown
  }
}, {
  persist: {
    serializer
  }
})
