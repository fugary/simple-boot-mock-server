<script setup>
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useMenuConfigStore } from '@/stores/MenuConfigStore'
import { computed } from 'vue'
const globalConfigStore = useGlobalConfigStore()
const menuConfigStore = useMenuConfigStore()
const businessMenus = computed(() => menuConfigStore.calcBusinessMenus())
</script>

<template>
  <el-scrollbar>
    <common-menu
      class="el-menu-left"
      :collapse="globalConfigStore.isCollapseLeft"
      :menus="businessMenus"
      :default-openeds="['1']"
    />
  </el-scrollbar>
</template>

<style scoped>
</style>
