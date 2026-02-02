<script setup>
import TopNav from '@/layout/TopNav.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'
import { useBreadcrumbConfigStore } from '@/stores/BreadcrumbConfigStore'
import { APP_VERSION } from '@/config'
import { getParentRootKey } from '@/route/RouteUtils'
import { formatDate } from '@/utils'
import { onKeyStroke } from '@vueuse/core'

const toggleMainFullscreen = () => {
  tabsViewStore.toggleMainFullscreen()
}
onKeyStroke('Escape', (e) => {
  if (tabsViewStore.isMainMaximized) {
    e.preventDefault()
    tabsViewStore.isMainMaximized = false
  }
})

const globalConfigStore = useGlobalConfigStore()
const tabsViewStore = useTabsViewStore()
const breadcrumbConfigStore = useBreadcrumbConfigStore()
</script>

<template>
  <el-container class="height100">
    <el-header class="index-header">
      <top-nav />
    </el-header>
    <el-header
      v-if="globalConfigStore.layoutMode === GlobalLayoutMode.TOP && globalConfigStore.isShowBreadcrumb"
      class="breadcrumb-header"
      style="height: 40px"
    >
      <common-breadcrumb
        style="padding-top:15px"
        :show-icon="tabsViewStore.isShowTabIcon"
        :label-config="breadcrumbConfigStore.breadcrumbConfig"
      />
    </el-header>
    <el-header
      v-if="tabsViewStore.isTabMode"
      class="tabs-header tabMode"
    >
      <common-tabs-view />
    </el-header>
    <el-main
      class="home-main"
      :class="{ 'is-maximized': tabsViewStore.isMainMaximized }"
    >
      <div
        class="fullscreen-btn"
        @click="toggleMainFullscreen"
      >
        <common-icon
          :icon="tabsViewStore.isMainMaximized ? 'FullscreenExitFilled' : 'FullscreenFilled'"
          :size="20"
        />
      </div>
      <router-view v-slot="{ Component, route }">
        <transition
          :name="route.meta?.transition!==false?'slide-fade':''"
          mode="out-in"
        >
          <KeepAlive
            v-if="tabsViewStore.isTabMode&&tabsViewStore.isCachedTabMode"
            :include="tabsViewStore.cachedTabs"
            :max="tabsViewStore.maxCacheCount"
          >
            <component
              :is="Component"
              :key="getParentRootKey(route)"
            />
          </KeepAlive>
          <component
            :is="Component"
            v-else
            :key="route.fullPath"
          />
        </transition>
      </router-view>
      <el-container class="text-center padding-10 flex-center">
        <span>
          <el-text>Copyright © {{ formatDate(new Date(), 'YYYY') }} Version: {{ APP_VERSION }}</el-text>
        </span>
      </el-container>
      <el-backtop
        v-common-tooltip="$t('common.label.backtop')"
        target=".home-main"
        :right="40"
        :bottom="40"
      />
    </el-main>
  </el-container>
</template>

<style scoped>
.tabs-header {
  padding-top: 6px !important;
  height: auto !important;
  border-bottom: none !important;
  box-shadow: none !important; /* Remove shadow if it causes double lines */
}
</style>
