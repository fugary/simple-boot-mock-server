import { parsePathParams, toLabelByKey, useMenuInfo, useMenuName } from '@/components/utils'
import { onMounted, ref, watch } from 'vue'
import { useScroll, useEventListener } from '@vueuse/core'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { useBreadcrumbConfigStore } from '@/stores/BreadcrumbConfigStore'
import { useGlobalSearchParamStore } from '@/stores/GlobalSearchParamStore'
import { isArray } from 'lodash-es'
import { $i18nBundle } from '@/messages'

const labelConfig2MenuInfo = (labelConfig, existItem = {}) => {
  if (labelConfig) {
    existItem = { ...existItem }
    existItem.menuName = labelConfig.label || toLabelByKey(labelConfig.labelKey) || labelConfig.menuName || existItem.menuName
    existItem.icon = labelConfig.icon || existItem.icon
    existItem.path = labelConfig.path || existItem.path
  }
  return existItem
}

/**
 * 计算匹配路由列表，用于生成面包屑
 * @param route
 * @param labelConfig
 * @return {[{path: string, menuName: string, icon: string}]}
 */
export const calcMatchedRoutes = (route, labelConfig) => {
  const exists = []
  /**
   * @type {[{path: string, menuName: string, icon: string}]}
   */
  const results = route.matched.filter(item => item.meta?.breadcrumb !== false).map((item, index) => {
    item = index === route.matched.length - 1 ? route : item
    const menuInfo = useMenuInfo(item)
    let icon = ''
    if (menuInfo && menuInfo.icon) {
      icon = menuInfo.icon
    } else if (item.meta && item.meta.icon) {
      icon = item.meta.icon
    }
    return {
      path: parsePathParams(item.path, route.params),
      menuName: useMenuName(item),
      icon
    }
  }).filter(item => {
    const notExist = !exists.includes(item.menuName)
    if (notExist) {
      exists.push(item.menuName)
    }
    return notExist && !item.menuName.endsWith('Base')
  })
  if (labelConfig && results.length) {
    const lastItem = results.pop()
    let appendItems = []
    if (isArray(labelConfig)) {
      appendItems = labelConfig.slice(0, labelConfig.length - 1)
      labelConfig = labelConfig[labelConfig.length - 1]
    }
    results.push(...appendItems.map(config => labelConfig2MenuInfo(config)))
    results.push(labelConfig2MenuInfo(labelConfig, lastItem))
  }
  return results
}

/**
 * 标题计算
 * @param route
 * @return {string}
 */
export const calcRouteTitle = (route) => {
  const labelConfig = useBreadcrumbConfigStore.breadcrumbConfig
  let title = $i18nBundle('common.label.title')
  const routes = calcMatchedRoutes(route, labelConfig)
  const item = routes?.[routes?.length - 1]
  if (item?.menuName && item.path !== '/' && item.path !== '/login') {
    title = `${item.menuName}`
  }
  return title
}

export const checkMataReplaceHistory = (historyTab, tab) => {
  // 如果meta中配置有replaceTabHistory，默认替换相关的tab
  return historyTab?.meta?.replaceTabHistory && historyTab.meta.replaceTabHistory === tab?.name
}

export const isSameReplaceHistory = (historyTab, tab) => {
  return historyTab?.meta?.replaceTabHistory && tab?.meta?.replaceTabHistory &&
      historyTab.meta.replaceTabHistory === tab.meta.replaceTabHistory
}

export const checkReplaceHistoryShouldReplace = (historyTab, tab) => {
  return checkMataReplaceHistory(historyTab, tab) || checkMataReplaceHistory(tab, historyTab) || isSameReplaceHistory(historyTab, tab)
}

/**
 * 记住滚动位置
 */
export const useTabModeScrollSaver = () => {
  onMounted(() => {
    const tabsViewStore = useTabsViewStore()
    const { y } = useScroll(document.querySelector('.home-main'))
    watch(y, val => {
      if (tabsViewStore.isTabMode && tabsViewStore.isCachedTabMode && tabsViewStore.currentTabItem) { // tab模式下监控滚动位置并保存
        tabsViewStore.currentTabItem.scroll = {
          top: val
        }
      }
    })
  })
}

export const getParentRootKey = (route) => {
  if (isNestedRoute(route) && route.meta?.replaceTabHistory && route.meta?.cache !== false) {
    return route.meta.replaceTabHistory
  }
  return route.fullPath
}

export const isNestedRoute = (route) => {
  return !!route?.meta?.nested
}

/**
 * 获取动画key，用于父子组件动画问题
 * @param route {import('vue-router').Route}
 * @param matcher {Function}
 * @return {Ref<*>}
 */
export const useTransitionKey = (route, matcher) => {
  let lastTransitionKey = route.fullPath
  const transitionKey = ref('')
  const calcTransitionKey = () => {
    if (matcher(route) && lastTransitionKey !== route.fullPath) {
      console.log('=========================', lastTransitionKey, route.fullPath)
      lastTransitionKey = route.fullPath
    }
    return lastTransitionKey
  }
  onMounted(() => {
    matcher(route) && (transitionKey.value = calcTransitionKey())
  })
  watch(() => route.fullPath, () => {
    matcher(route) && (transitionKey.value = calcTransitionKey())
  })
  return transitionKey
}

export const useRoutePopStateEvent = () => {
  useEventListener(window, 'popstate', () => {
    const { from, to } = useGlobalSearchParamStore().savedParamRouteInfo
    if (!useTabsViewStore().isTabMode || checkReplaceHistoryShouldReplace(from, to)) {
      useGlobalSearchParamStore().setSaveParamBack(true)
    }
  })
}
