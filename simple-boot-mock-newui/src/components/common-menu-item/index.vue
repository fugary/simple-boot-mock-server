<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { GlobalLayoutMode } from '@/consts/GlobalConstants'

const router = useRouter()
const globalConfigStore = useGlobalConfigStore()
/**
 * @type {CommonMenuItemProps}
 */
const props = defineProps({
  /**
   * @type {CommonMenuItem}
   */
  menuItem: {
    type: Object,
    required: true
  },
  index: {
    type: String,
    default: ''
  }
})
const isSubMenu = computed(() => {
  const menuItem = props.menuItem
  return !menuItem.isDropdown && menuItem.children && menuItem.children.length
})
const isDropdown = computed(() => {
  const menuItem = props.menuItem
  return menuItem.isDropdown && menuItem.children && menuItem.children.length
})
const menuCls = computed(() => {
  const menuItem = props.menuItem
  if (!menuItem.menuCls && menuItem.isDropdown) {
    return 'padding-left1 padding-right1'
  }
  return menuItem.menuCls
})
const dropdownClick = (menuItem, $event) => {
  if (menuItem.click) {
    menuItem.click($event, menuItem)
  } else {
    const route = menuItem.route || menuItem.index
    if (route) {
      router.push(route)
    }
  }
}

const checkShowMenuIcon = menuItem => {
  return menuItem.icon && (globalConfigStore.showMenuIcon ||
      (!menuItem.labelKey && !menuItem.label) ||
      (globalConfigStore.isCollapseLeft && globalConfigStore.layoutMode === GlobalLayoutMode.LEFT))
}

const showMenuIcon = computed(() => {
  return checkShowMenuIcon(props.menuItem)
})

</script>

<template>
  <div
    v-if="menuItem.isSplit"
    :class="menuCls"
  >
    <slot name="split" />
  </div>
  <el-sub-menu
    v-else-if="isSubMenu"
    :index="`${menuItem.index||index}`"
    :class="menuCls"
    :disabled="menuItem.disabled"
    v-bind="menuItem.attrs"
  >
    <template #title>
      <common-icon
        v-if="showMenuIcon"
        :size="menuItem.iconSize"
        :icon="menuItem.icon"
      />
      <span v-if="menuItem.labelKey||menuItem.label">
        {{ menuItem.labelKey?$t(menuItem.labelKey):menuItem.label }}
      </span>
    </template>
    <common-menu-item
      v-for="(childMenu, childIdx) in menuItem.children"
      :key="childMenu.index||childIdx"
      :index="`${menuItem.index||index}_${childIdx}`"
      :menu-item="childMenu"
    />
  </el-sub-menu>
  <el-menu-item
    v-else-if="isDropdown"
    :class="menuCls"
    :disabled="menuItem.disabled"
    @click="menuItem.click&&menuItem.click($event, menuItem)"
  >
    <el-dropdown class="common-dropdown">
      <span class="el-dropdown-link">
        <common-icon
          v-if="showMenuIcon"
          :size="menuItem.iconSize"
          :icon="menuItem.icon"
        />
        <span v-if="menuItem.labelKey||menuItem.label">
          {{ menuItem.labelKey?$t(menuItem.labelKey):menuItem.label }}
        </span>
      </span>
      <template #dropdown>
        <el-dropdown-item
          v-for="(childMenu, childIdx) in menuItem.children"
          :key="childMenu.index||childIdx"
          @click="dropdownClick(childMenu, $event)"
        >
          <common-icon
            v-if="checkShowMenuIcon(childMenu)"
            :size="childMenu.iconSize"
            :icon="childMenu.icon"
          />
          <span v-if="childMenu.labelKey||childMenu.label">
            {{ childMenu.labelKey?$t(childMenu.labelKey):childMenu.label }}
          </span>
        </el-dropdown-item>
      </template>
    </el-dropdown>
  </el-menu-item>
  <el-menu-item
    v-else
    v-open-new-window="menuItem.index"
    :class="menuCls"
    :disabled="menuItem.disabled"
    :route="menuItem.route"
    v-bind="menuItem.attrs"
    :index="menuItem.index"
    @click="menuItem.click&&menuItem.click($event,menuItem)"
  >
    <common-icon
      v-if="showMenuIcon"
      :size="menuItem.iconSize"
      :icon="menuItem.icon"
    />
    <template #title>
      <span v-if="menuItem.labelKey||menuItem.label">
        {{ menuItem.labelKey?$t(menuItem.labelKey):menuItem.label }}
      </span>
    </template>
  </el-menu-item>
</template>

<style scoped>
.common-dropdown {
  height: 100%;
}
</style>
