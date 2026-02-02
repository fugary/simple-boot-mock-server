<script setup>
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { computed } from 'vue'
const globalConfigStore = useGlobalConfigStore()
const menuConfigStore = useMenuConfigStore()
const businessMenus = computed(() => menuConfigStore.calcBusinessMenus())
</script>

<template>
  <el-scrollbar
    view-class="height100"
    class="height100"
  >
    <common-menu
      class="el-menu-left"
      :collapse="globalConfigStore.isCollapseLeft"
      :menus="businessMenus"
      :default-openeds="['1']"
    />
  </el-scrollbar>
</template>

<style scoped>
:deep(.el-menu-left) {
  height: 100%;
  width: 100%;
  border-right: none;
  background-color: var(--el-bg-color-overlay); /* Or specific menu background color */
}
:deep(.el-scrollbar__view) {
  height: 100%;
  width: 100%;
}
.el-scrollbar {
  width: 100%;
}
</style>
