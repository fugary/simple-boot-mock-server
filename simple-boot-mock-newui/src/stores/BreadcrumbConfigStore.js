import { computed, ref, nextTick } from 'vue'
import { defineStore } from 'pinia'
import { useTabsViewStore } from '@/stores/TabsViewStore'
export const useBreadcrumbConfigStore = defineStore('breadcrumbConfig', () => {
  const internalBreadcrumbConfig = ref()
  const tabViewStore = useTabsViewStore()
  const setBreadcrumbConfig = (config) => {
    internalBreadcrumbConfig.value = config
    if (tabViewStore.isTabMode && tabViewStore.currentTabItem && config) {
      nextTick(() => {
        tabViewStore.currentTabItem.labelConfig = config
      })
    }
  }
  const breadcrumbConfig = computed(() => {
    if (tabViewStore.isTabMode && tabViewStore.currentTabItem) {
      return tabViewStore.currentTabItem.labelConfig
    }
    return internalBreadcrumbConfig.value
  })
  return {
    breadcrumbConfig,
    setBreadcrumbConfig,
    clearBreadcrumbConfig: () => {
      setBreadcrumbConfig(null)
    }
  }
})
