<script setup>
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { useBreadcrumbConfigStore } from '@/stores/BreadcrumbConfigStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'
import { computed } from 'vue'
const globalConfigStore = useGlobalConfigStore()
const menuConfigStore = useMenuConfigStore()
const tabsViewStore = useTabsViewStore()
const breadcrumbConfigStore = useBreadcrumbConfigStore()

const allMenus = computed(() => {
  const topMenus = menuConfigStore.loadBaseTopMenus()
  const businessMenus = menuConfigStore.calcBusinessMenus()
  if (globalConfigStore.layoutMode === GlobalLayoutMode.TOP) {
    return [...businessMenus, ...topMenus.slice(1)]
  }
  return topMenus
})

</script>
<template>
  <common-menu
    class="padding-right2"
    router
    mode="horizontal"
    :ellipsis="false"
    :menus="allMenus"
  >
    <template
      v-if="globalConfigStore.layoutMode === GlobalLayoutMode.LEFT && globalConfigStore.isShowBreadcrumb"
      #split
    >
      <common-breadcrumb
        :show-icon="tabsViewStore.isShowTabIcon"
        :label-config="breadcrumbConfigStore.breadcrumbConfig"
        :style="{'padding-left': '0','padding-top': '22px'}"
      />
    </template>
  </common-menu>
</template>

<style scoped>

</style>
