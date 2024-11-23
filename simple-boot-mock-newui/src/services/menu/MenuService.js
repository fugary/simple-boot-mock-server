/**
 * @typedef {Object} MenuDto
 * @property {number} id 主键
 * @property {number} parentId 上级id
 * @property {string} iconCls 图标
 * @property {string} nameCn 中文名
 * @property {string} nameEn 英文名
 * @property {string} menuUrl 链接地址
 * @property {[MenuDto]} children 子菜单
 */
import { $i18nMsg } from '@/messages'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { GlobalLocales } from '@/consts/GlobalConstants'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { I18N_ENABLED, THEME_ENABLED } from '@/config'
import { $logout } from '@/utils'
import { ALL_MENUS } from '@/services/menu/MenuData'
import { cloneDeep, isFunction } from 'lodash-es'

/**
 * @param menu {MenuDto}
 * @return {CommonTreeNode}
 */
export const menu2TreeMenu = (menu) => {
  /**
   * @type {CommonTreeNode}
   */
  const treeNode = {
    value: menu.id,
    label: $i18nMsg(menu.nameCn, menu.nameEn)
  }
  if (menu.children) {
    treeNode.children = menu.children.map(menu2TreeMenu)
  }
  return treeNode
}

/**
 * 接口菜单格式转换成页面展示格式
 * @param menu {MenuDto}
 * @return {CommonMenuItem}
 */
export const menu2CommonMenu = (menu) => {
  /**
   * @type {CommonMenuItem}
   */
  const menuItem = {
    icon: menu.iconCls,
    label: $i18nMsg(menu.nameCn, menu.nameEn),
    index: menu.menuUrl,
    click: menu.click
  }
  if (menu.children) {
    menuItem.children = menu.children.map(menu2CommonMenu)
  }
  return menuItem
}

export const loadAndParseMenus = async () => {
  /**
   * @type {[MenuDto]}
   */
  // const menus = await $httpPost('/api/menus', param, config).then(data => data.resultData?.menuList || [])
  const loginConfigStore = useLoginConfigStore()
  const menus = cloneDeep(ALL_MENUS).filter(menu => {
    let checkResult = true
    if (menu.dbConsole) {
      checkResult = loginConfigStore.consoleEnabled
    }
    if (checkResult && isFunction(menu.checkEnabled)) {
      checkResult = menu.checkEnabled()
    }
    return checkResult
  })
  return processMenus(menus)
}
/**
 * 解析菜单信息
 * @param {[MenuDto]} menus
 * @param {MenuDto} parent
 * @returns {*[]}
 */
const processMenus = (menus, parent = undefined) => {
  const results = []
  menus.forEach(currentMenu => {
    if (!parent) {
      if (!currentMenu.parentId) { // 根节点
        results.push(currentMenu)
        processMenus(menus, currentMenu)
      }
    } else {
      if (currentMenu.parentId === parent.id) {
        parent.children = parent.children || []
        parent.children.push(currentMenu)
        processMenus(menus, currentMenu)
      }
    }
    return parent
  })
  return results
}

export const useThemeAndLocaleMenus = () => {
  const globalConfigStore = useGlobalConfigStore()
  return [{
    icon: 'LanguageFilled',
    isDropdown: true,
    enabled: I18N_ENABLED,
    children: [
      {
        iconIf: () => GlobalLocales.CN === globalConfigStore.currentLocale ? 'check' : '',
        labelKey: 'common.label.langCn',
        click: () => globalConfigStore.changeLocale(GlobalLocales.CN)
      },
      {
        iconIf: () => GlobalLocales.EN === globalConfigStore.currentLocale ? 'check' : '',
        labelKey: 'common.label.langEn',
        click: () => globalConfigStore.changeLocale(GlobalLocales.EN)
      }
    ]
  },
  {
    isDropdown: true,
    enabled: THEME_ENABLED,
    iconIf: () => !globalConfigStore.isDarkTheme ? 'moon' : 'sunny',
    click: () => globalConfigStore.changeTheme(!globalConfigStore.isDarkTheme)
  }]
}

export const useBaseTopMenus = () => {
  const globalConfigStore = useGlobalConfigStore()
  const loginConfigStore = useLoginConfigStore()
  const userName = loginConfigStore.accountInfo?.nickName || loginConfigStore.accountInfo?.userName
  return [
    {
      iconIf: () => globalConfigStore.isCollapseLeft ? 'expand' : 'fold',
      click: globalConfigStore.collapseLeft
    },
    {
      isSplit: true,
      menuCls: 'flex-grow'
    },
    ...useThemeAndLocaleMenus(),
    {
      isDropdown: true,
      icon: 'Setting',
      click: () => globalConfigStore.changeShowSettings(true)
    },
    {
      icon: 'user',
      isDropdown: true,
      label: userName,
      children: [
        {
          labelKey: 'common.label.personalInfo',
          index: '/personal'
        },
        {
          labelKey: 'common.label.about',
          index: '/about'
        },
        {
          labelKey: 'common.label.logout',
          click () {
            $logout()
          }
        }
      ]
    }
  ]
}
