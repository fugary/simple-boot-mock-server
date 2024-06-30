import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useDark, useMediaQuery } from '@vueuse/core'
import { GlobalLayoutMode, GlobalLocales, LoadSaveParamMode } from '@/consts/GlobalConstants'
import { changeMessages } from '@/messages'
import { useSystemKey } from '@/utils'
import { AUTO_LAYOUT_ENABLED, I18N_ENABLED, THEME_ENABLED } from '@/config'

export const useGlobalConfigStore = defineStore('globalConfig', () => {
  const currentLocale = ref(GlobalLocales.CN)
  const systemKey = useSystemKey()
  const isDarkTheme = THEME_ENABLED
    ? useDark({
      storageKey: `__${systemKey}__vueuse-color-scheme`
    })
    : ref(false)
  const isCollapseLeft = ref(false)
  const isShowSettings = ref(false)
  const isShowBreadcrumb = ref(true)
  const showMenuIcon = ref(true)
  const isLargeScreen = useMediaQuery('(min-width: 1440px)')
  const layoutMode = !isLargeScreen.value && AUTO_LAYOUT_ENABLED ? ref(GlobalLayoutMode.TOP) : ref(GlobalLayoutMode.LEFT)
  const loadSaveParamMode = ref(LoadSaveParamMode.BACK)
  return {
    currentLocale,
    isDarkTheme,
    isCollapseLeft,
    isShowSettings,
    isShowBreadcrumb,
    layoutMode,
    loadSaveParamMode,
    showMenuIcon,
    changeLocale (locale) {
      if (!I18N_ENABLED) return
      if (Object.values(GlobalLocales).includes(locale)) {
        currentLocale.value = locale
      } else {
        throw new Error(`Locale ${locale} is not supported.`)
      }
      changeMessages(locale)
    },
    changeTheme (dark) {
      if (!THEME_ENABLED) return
      isDarkTheme.value = dark
    },
    changeShowSettings (val) {
      isShowSettings.value = val
    },
    collapseLeft () {
      isCollapseLeft.value = !isCollapseLeft.value
    },
    changeLayout (layout) {
      if (Object.values(GlobalLayoutMode).includes(layout)) {
        layoutMode.value = layout
      } else {
        throw new Error(`Layout ${layout} is not supported.`)
      }
    }
  }
}, {
  persist: true
})
