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
    <template
      v-for="(menuItem, index) in menuItems"
      :key="index"
    >
      <common-menu-item
        :menu-item="menuItem"
        :index="`${index}`"
      >
        <template #split>
          <slot name="split" />
        </template>
      </common-menu-item>
    </template>
    <slot name="default" />
  </el-menu>
</template>

<style scoped>

</style>
