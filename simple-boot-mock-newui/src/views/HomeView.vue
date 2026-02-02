<script setup>
import LeftMenu from '@/layout/LeftMenu.vue'
import MainContent from '@/layout/MainContent.vue'
import GlobalSettings from '@/views/components/global/GlobalSettings.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'
import { computed, ref } from 'vue'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { useTabModeScrollSaver } from '@/route/RouteUtils'

const globalConfigStore = useGlobalConfigStore()
const showLeftMenu = computed(() => {
  return globalConfigStore.layoutMode === GlobalLayoutMode.LEFT
})
const leftMenuAsideRef = ref(null)

const handleDragEnd = () => {
  if (leftMenuAsideRef.value) {
    const width = leftMenuAsideRef.value.$el.offsetWidth
    if (width < 100) {
      globalConfigStore.isCollapseLeft = true
    }
  }
}

useTabModeScrollSaver()
useMenuConfigStore().loadBusinessMenus()
</script>

<template>
  <el-container class="index-container">
    <common-split
      v-if="showLeftMenu"
      :disabled="globalConfigStore.isCollapseLeft"
      :class="{ 'collapsed-split': globalConfigStore.isCollapseLeft }"
      :sizes="[20, 80]"
      :min-size="[60, 500]"
      :max-size="[500, Infinity]"
      class="flex-grow"
      @drag-end="handleDragEnd"
    >
      <template #split-0>
        <el-aside
          ref="leftMenuAsideRef"
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
    <global-settings />
  </el-container>
</template>

<style scoped>
.index-container {
  height: 100vh;
  overflow: hidden;
}
.index-aside {
  background-color: var(--el-bg-color-overlay);
  border-right: none;
}
:deep(.collapsed-split.is-disabled > .split-pane:first-child) {
  width: 64px !important;
}
:deep(.common-split:not(.is-dragging) > .split-pane) {
  will-change: width;
  transition: width 0.3s cubic-bezier(0.25, 0.8, 0.5, 1);
}
</style>
