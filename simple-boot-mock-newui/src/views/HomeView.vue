<script setup>
import LeftMenu from '@/layout/LeftMenu.vue'
import MainContent from '@/layout/MainContent.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'
import { computed } from 'vue'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { useTabModeScrollSaver } from '@/route/RouteUtils'

const globalConfigStore = useGlobalConfigStore()
const showLeftMenu = computed(() => {
  return globalConfigStore.layoutMode === GlobalLayoutMode.LEFT
})
useTabModeScrollSaver()
useMenuConfigStore().loadBusinessMenus()
</script>

<template>
  <el-container class="index-container">
    <common-split
      v-if="showLeftMenu"
      :disabled="globalConfigStore.isCollapseLeft"
      :sizes="[20, 80]"
      :min-size="[200, 300]"
      :max-size="[500, Infinity]"
      class="flex-grow"
    >
      <template #split-0>
        <el-aside
          class="index-aside menu"
          width="auto"
          style="height: 100%; width: 100% !important;"
        >
          <left-menu class="height100" />
        </el-aside>
      </template>
      <template #split-1>
        <main-content class="height100" />
      </template>
    </common-split>

    <main-content v-else />
  </el-container>
</template>

<style scoped>
.index-container {
  height: 100vh;
  overflow: hidden;
}
</style>
