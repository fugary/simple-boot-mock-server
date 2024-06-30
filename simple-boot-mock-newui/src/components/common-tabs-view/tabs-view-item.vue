<script setup>
import { useMenuInfo, useMenuName } from '@/components/utils'
import { computed, ref } from 'vue'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { $i18nKey } from '@/messages'

const tabsViewStore = useTabsViewStore()

const props = defineProps({
  labelConfig: {
    type: Object,
    default: null
  },
  /**
   * @type RouteRecordRaw
   */
  tabItem: {
    type: Object,
    required: true
  }
})

defineEmits(['removeHistoryTab', 'removeOtherHistoryTabs', 'removeHistoryTabs', 'refreshHistoryTab', 'onDropdownVisibleChange'])

const menuName = computed(() => {
  const labelConfig = props.labelConfig
  if (labelConfig) {
    const label = labelConfig.label || $i18nKey(labelConfig.labelKey)
    if (label) return label
  }
  return useMenuName(props.tabItem)
})

const menuInfo = computed(() => {
  return props.tabItem.path === '/' ? { icon: 'HomeFilled' } : useMenuInfo(props.tabItem)
})

const menuIcon = computed(() => {
  const labelConfig = props.labelConfig
  if (labelConfig) {
    const icon = labelConfig.icon
    if (icon) return icon
  }
  if (menuInfo.value && menuInfo.value.icon) {
    return menuInfo.value.icon
  }
  if (props.tabItem.meta && props.tabItem.meta.icon) {
    return props.tabItem.meta.icon
  }
  return null
})
const dropdownRef = ref()

defineExpose({
  dropdownRef
})

</script>

<template>
  <el-tab-pane
    :name="tabItem.path"
  >
    <template #label>
      <el-dropdown
        :id="tabItem.path"
        ref="dropdownRef"
        trigger="contextmenu"
        @visible-change="$emit('onDropdownVisibleChange', $event, tabItem)"
      >
        <span class="custom-tabs-label">
          <common-icon
            v-if="tabsViewStore.isShowTabIcon && menuIcon"
            :icon="menuIcon"
          />
          <span>{{ menuName }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              @click="$emit('refreshHistoryTab', tabItem)"
            >
              <common-icon icon="refresh" />
              {{ $t('common.label.refresh') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="tabsViewStore.hasCloseDropdown(tabItem, 'close')"
              @click="$emit('removeHistoryTab', tabItem.path)"
            >
              <common-icon icon="close" />
              {{ $t('common.label.close') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="tabsViewStore.hasCloseDropdown(tabItem, 'left')"
              @click="$emit('removeHistoryTabs', tabItem, 'left')"
            >
              <common-icon icon="KeyboardDoubleArrowLeftFilled" />
              {{ $t('common.label.closeLeft') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="tabsViewStore.hasCloseDropdown(tabItem, 'right')"
              @click="$emit('removeHistoryTabs', tabItem, 'right')"
            >
              <common-icon icon="KeyboardDoubleArrowRightFilled" />
              {{ $t('common.label.closeRight') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="tabsViewStore.hasCloseDropdown(tabItem, 'other')"
              @click="$emit('removeOtherHistoryTabs', tabItem)"
            >
              <common-icon icon="PlaylistRemoveFilled" />
              {{ $t('common.label.closeOther') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </el-tab-pane>
</template>

<style scoped>
.common-tabs .custom-tabs-label .el-icon {
  vertical-align: middle;
}
.common-tabs .custom-tabs-label span {
  vertical-align: middle;
  margin-left: 4px;
}
</style>
