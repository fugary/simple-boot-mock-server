<script setup>
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { GlobalLayoutMode, GlobalLocales, LoadSaveParamMode } from '@/consts/GlobalConstants'
import { I18N_ENABLED, REMEMBER_SEARCH_PARAM_ENABLED, THEME_ENABLED } from '@/config'
const globalConfigStore = useGlobalConfigStore()
const tabsViewStore = useTabsViewStore()
/**
 * @type {[CommonFormOption]}
 */
const options = [
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
    type: 'select',
    prop: 'currentLocale',
    enabled: I18N_ENABLED,
    change (val) {
      globalConfigStore.changeLocale(val)
    },
    children: [{
      labelKey: 'common.label.langCn',
      value: GlobalLocales.CN
    }, {
      labelKey: 'common.label.langEn',
      value: GlobalLocales.EN
    }]
  },
  {
    labelKey: 'common.label.layout',
    slot: 'layout',
    change (val) {
      globalConfigStore.changeLayout(val)
    },
    children: [{
      labelKey: 'common.label.layoutLeft',
      value: GlobalLayoutMode.LEFT
    }, {
      labelKey: 'common.label.layoutTop',
      value: GlobalLayoutMode.TOP
    }]
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
]
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
      >
        <template #layout="{option}">
          <common-form-control
            :model="globalConfigStore"
            :option="option"
          >
            <el-radio-group
              v-model="globalConfigStore.layoutMode"
              size="small"
            >
              <el-radio-button
                v-for="item in option.children"
                :key="item.value"
                :value="item.value"
              >
                <common-icon
                  v-if="item.value==='left'"
                  v-common-tooltip="$t(item.labelKey)"
                  icon="VerticalSplitFilled"
                  :size="16"
                />
                <common-icon
                  v-if="item.value==='top'"
                  v-common-tooltip="$t(item.labelKey)"
                  icon="HorizontalSplitFilled"
                  :size="16"
                />
              </el-radio-button>
            </el-radio-group>
          </common-form-control>
        </template>
      </common-form>
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
