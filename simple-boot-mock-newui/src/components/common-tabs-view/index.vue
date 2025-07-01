<script setup>
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { useRoute, useRouter } from 'vue-router'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isString } from 'lodash-es'
import TabsViewItem from '@/components/common-tabs-view/tabs-view-item.vue'
import { toGetParams } from '@/utils'
import { isNestedRoute } from '@/route/RouteUtils'
import { useGetDerivedNamespace } from 'element-plus'
import Sortable from 'sortablejs'

const router = useRouter()
const route = useRoute()
const tabsViewStore = useTabsViewStore()
const tabRef = ref()
let sortable = null
watch(route, () => {
  if (route.path) {
    tabsViewStore.addHistoryTab(route)
    tabsViewStore.currentTab = route.path
  }
}, { immediate: true })

onMounted(() => {
  if (!tabsViewStore.historyTabs.length) {
    tabsViewStore.addHistoryTab(route)
  }
  tabsViewStore.currentTab = route.path
  if (tabRef.value?.tabNavRef) {
    const ns = useGetDerivedNamespace().value
    const { tabListRef } = tabRef.value.tabNavRef
    sortable = new Sortable(tabListRef, {
      animation: 150,
      draggable: `.${ns}-tabs__item`,
      filter: '.is-disabled',
      onEnd (event) {
        const { oldIndex, newIndex } = event
        tabsViewStore.reIndexHistoryTab(oldIndex, newIndex)
      }
    })
  }
})

const selectHistoryTab = path => {
  const tab = isString(path) ? tabsViewStore.findHistoryTab(path) : path
  if (tab) {
    router.push(tab)
    if (!isNestedRoute(tab)) {
      tabsViewStore.addCachedTab(tab)
    }
  }
}

const removeHistoryTab = path => {
  const lastTab = tabsViewStore.removeHistoryTab({ path })
  if (lastTab) {
    selectHistoryTab(lastTab)
  }
}

const refreshHistoryTab = tab => {
  const time = new Date().getTime()
  const query = Object.assign({}, tab.query, { _t: time })
  router.replace(`${tab.path}?${toGetParams(query)}`)
  if (!isNestedRoute(tab)) {
    tabsViewStore.addCachedTab(tab)
  }
}

const removeOtherHistoryTabs = tab => {
  tabsViewStore.removeOtherHistoryTabs(tab)
  selectHistoryTab(tab.path)
}

const removeHistoryTabs = (tab, type) => {
  tabsViewStore.removeHistoryTabs(tab, type)
  selectHistoryTab(tab.path)
}

const tabItems = ref()
const onDropdownVisibleChange = (visible, tab) => {
  if (visible) {
    tabItems.value.forEach(({ dropdownRef }) => {
      if (dropdownRef.id !== tab.path) {
        dropdownRef.handleClose()
      }
    })
  }
}
onBeforeUnmount(() => {
  sortable?.destroy()
  sortable = null
})

</script>

<template>
  <el-tabs
    ref="tabRef"
    v-bind="$attrs"
    v-model="tabsViewStore.currentTab"
    class="common-tabs"
    type="card"
    :closable="tabsViewStore.historyTabs.length>1"
    @tab-change="selectHistoryTab"
    @tab-remove="removeHistoryTab"
  >
    <tabs-view-item
      v-for="item in tabsViewStore.historyTabs"
      ref="tabItems"
      :key="item.path"
      :tab-item="item"
      :label-config="item.labelConfig"
      @refresh-history-tab="refreshHistoryTab"
      @remove-other-history-tabs="removeOtherHistoryTabs"
      @remove-history-tabs="removeHistoryTabs"
      @on-dropdown-visible-change="onDropdownVisibleChange"
      @remove-history-tab="removeHistoryTab"
    />
  </el-tabs>
</template>

<style scoped>
.common-tabs .el-tabs__header {
  margin: 0;
}
</style>
