<script setup lang="jsx">
import { computed } from 'vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import CommonIcon from '@/components/common-icon/index.vue'
import { GlobalLayoutMode, GlobalLocales, LoadSaveParamMode } from '@/consts/GlobalConstants'
import { I18N_ENABLED, REMEMBER_SEARCH_PARAM_ENABLED, THEME_ENABLED } from '@/config'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle } from '@/messages'
const globalConfigStore = useGlobalConfigStore()
const tabsViewStore = useTabsViewStore()
/**
 * 全局配置项
 */
const options = computed(() => defineFormOptions([
  {
    labelKey: 'common.label.theme',
    prop: 'isDarkTheme',
    type: 'switch',
    enabled: THEME_ENABLED,
    attrs: {
      activeActionIcon: 'icon-moon',
      inactiveActionIcon: 'icon-sunny'
    }
  },
  {
    labelKey: 'common.label.language',
    type: 'segmented',
    prop: 'currentLocale',
    enabled: I18N_ENABLED,
    attrs: {
      options: [{
        label: $i18nBundle('common.label.langCn'),
        value: GlobalLocales.CN
      }, {
        label: $i18nBundle('common.label.langEn'),
        value: GlobalLocales.EN
      }]
    },
    change (val) {
      globalConfigStore.changeLocale(val)
    }
  },
  {
    labelKey: 'common.label.layout',
    prop: 'layoutMode',
    change (val) {
      globalConfigStore.changeLayout(val)
    },
    type: 'segmented',
    slots: {
      default: (scope) => {
        const item = scope.item
        return item?.value === 'left'
          ? <CommonIcon
                v-common-tooltip={item.label}
                icon="VerticalSplitFilled"
                size={25}/>
          : <CommonIcon
                v-common-tooltip={item.label}
                icon="HorizontalSplitFilled"
                size={25}
            />
      }
    },
    attrs: {
      options: [{
        label: $i18nBundle('common.label.layoutLeft'),
        value: GlobalLayoutMode.LEFT
      }, {
        label: $i18nBundle('common.label.layoutTop'),
        value: GlobalLayoutMode.TOP
      }]
    }
  },
  {
    labelKey: 'common.label.showMenuIcon',
    prop: 'showMenuIcon',
    type: 'switch'
  },
  {
    labelKey: 'common.label.breadcrumb',
    prop: 'isShowBreadcrumb',
    type: 'switch',
    change (val) {
      globalConfigStore.isShowBreadcrumb = val
    }
  },
  {
    labelKey: 'common.label.tabMode',
    prop: 'isTabMode',
    type: 'switch',
    model: tabsViewStore,
    change (val) {
      tabsViewStore.changeTabMode(val)
    }
  },
  {
    labelKey: 'common.label.cachedTabMode',
    prop: 'isCachedTabMode',
    type: 'switch',
    model: tabsViewStore,
    change (val) {
      tabsViewStore.changeCachedTabMode(val)
    }
  },
  {
    labelKey: 'common.label.showTabIcon',
    prop: 'isShowTabIcon',
    type: 'switch',
    model: tabsViewStore
  },
  {
    labelKey: 'common.label.saveParamMode',
    prop: 'loadSaveParamMode',
    type: 'select',
    enabled: REMEMBER_SEARCH_PARAM_ENABLED,
    attrs: {
      clearable: false
    },
    children: [{
      labelKey: 'common.label.allSaveParamMode',
      value: LoadSaveParamMode.ALL
    }, {
      labelKey: 'common.label.backSaveParamMode',
      value: LoadSaveParamMode.BACK
    }, {
      labelKey: 'common.label.neverSaveParamMode',
      value: LoadSaveParamMode.NEVER
    }]
  }
]))
</script>

<template>
  <el-drawer
    v-model="globalConfigStore.isShowSettings"
    direction="rtl"
    :size="350"
  >
    <template #header>
      <strong>{{ $t('common.label.settings') }}</strong>
    </template>
    <template #default>
      <common-form
        :show-buttons="false"
        :options="options"
        label-position="left"
        :model="globalConfigStore"
      />
    </template>
    <template #footer>
      <div style="flex: auto">
        <el-button
          type="primary"
          @click="globalConfigStore.changeShowSettings(false)"
        >
          {{ $t('common.label.close') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>

</style>
