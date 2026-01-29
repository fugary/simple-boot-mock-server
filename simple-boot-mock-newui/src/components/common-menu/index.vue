<script setup>
import { computed, ref, watchEffect } from 'vue'
import { processMenus, useParentRoute } from '@/components/utils'
import { useRoute } from 'vue-router'
const props = defineProps({
  menus: {
    type: Array,
    required: true
  },
  defaultActivePath: {
    type: String,
    default: ''
  }
})
const route = useRoute()
const menuItems = ref([])
watchEffect(() => {
  menuItems.value = processMenus(props.menus)
})
const activeRoutePath = computed(() => {
  if (props.defaultActivePath) {
    return props.defaultActivePath
  }
  const current = useParentRoute(route)
  return current && current.path !== '/' ? current.path : '--'
})
</script>

<template>
  <el-menu
    v-bind="$attrs"
    :default-active="activeRoutePath"
    router
  >
    <slot name="before" />
    <common-menu-item
      v-for="(menuItem, index) in menuItems"
      :key="index"
      :menu-item="menuItem"
      :index="`${index}`"
    >
      <template #split>
        <slot name="split" />
      </template>
    </common-menu-item>
    <slot name="default" />
  </el-menu>
</template>

<style scoped>
:deep(.el-menu) {
  border-right: none;
}

/* Base Item Style */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  transition: all 0.3s;
}

/* Vertical Menu Active State (Default) */
:deep(.el-menu:not(.el-menu--horizontal) .el-menu-item.is-active) {
  background-color: var(--el-color-primary-light-9);
  border-right: 3px solid var(--el-color-primary);
  color: var(--el-color-primary);
  font-weight: 500;
}

/* Dark Mode Active Override */
.dark :deep(.el-menu:not(.el-menu--horizontal) .el-menu-item.is-active) {
  background-color: rgba(64, 158, 255, 0.2);
}

/* Hover Effects */
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--el-fill-color-light);
  color: var(--el-menu-text-color);
}

.dark :deep(.el-menu-item:hover),
.dark :deep(.el-sub-menu__title:hover) {
  background-color: var(--el-bg-color-overlay);
}
</style>
