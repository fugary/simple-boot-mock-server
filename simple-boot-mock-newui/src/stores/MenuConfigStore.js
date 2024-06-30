import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { loadAndParseMenus, menu2CommonMenu, useBaseTopMenus } from '@/services/menu/MenuService'

export const useMenuConfigStore = defineStore('menuConfig', () => {
  /**
   * @type {[MenuDto]}
   */
  const businessMenus = ref([])
  return {
    businessMenus,
    loadBaseTopMenus () {
      return reactive(useBaseTopMenus())
    },
    async loadBusinessMenus () {
      businessMenus.value = await loadAndParseMenus()
      return this.calcBusinessMenus()
    },
    calcBusinessMenus () {
      return [{
        icon: 'HomeFilled',
        index: '/',
        labelKey: 'common.label.title'
      }, ...businessMenus.value.map(menu2CommonMenu)]
    },
    clearBusinessMenus () {
      businessMenus.value = []
    }
  }
}, {
  persist: true
})
