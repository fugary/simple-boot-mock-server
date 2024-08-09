<script setup>
import LeftMenu from '@/layout/LeftMenu.vue'
import TopNav from '@/layout/TopNav.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'
import { computed } from 'vue'
import GlobalSettings from '@/views/components/global/GlobalSettings.vue'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { useBreadcrumbConfigStore } from '@/stores/BreadcrumbConfigStore'
import { APP_VERSION } from '@/config'
import { useTabModeScrollSaver, getParentRootKey } from '@/route/RouteUtils'

const globalConfigStore = useGlobalConfigStore()
const tabsViewStore = useTabsViewStore()
const breadcrumbConfigStore = useBreadcrumbConfigStore()
const showLeftMenu = computed(() => {
  return globalConfigStore.layoutMode === GlobalLayoutMode.LEFT
})
useTabModeScrollSaver()
useMenuConfigStore().loadBusinessMenus()
</script>

<template>
  <el-container class="index-container">
    <el-aside
      v-if="showLeftMenu"
      class="index-aside menu"
      width="auto"
    >
      <left-menu />
    </el-aside>
    <el-container>
      <el-header>
        <top-nav />
      </el-header>
      <el-header
        v-if="globalConfigStore.layoutMode === GlobalLayoutMode.TOP && globalConfigStore.isShowBreadcrumb"
        class="tabs-header"
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
      <el-main class="home-main">
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
            <el-text>Copyright © 2024 Version: {{ APP_VERSION }}</el-text>
          </span>
        </el-container>
        <el-backtop
          v-common-tooltip="$t('common.label.backtop')"
          target=".home-main"
          :right="70"
          :bottom="70"
        />
      </el-main>
      <global-settings />
    </el-container>
  </el-container>
</template>
<style scoped>

</style>
