import { ref, unref } from 'vue'
import { $i18nBundle, $i18nKey } from '@/messages'
import { isArray, isFunction, isObject } from 'lodash-es'

export const getFrontendPage = (totalCount, pageSize, pageNumber = 1) => {
  const pageCount = Math.floor((totalCount + pageSize - 1) / pageSize)
  if (pageNumber > pageCount && pageCount > 0) {
    pageNumber = pageCount
  }
  return {
    pageNumber,
    pageSize,
    totalCount,
    pageCount
  }
}

const calcWithIf = menuItem => {
  ['icon', 'labelKey', 'label', 'html'].forEach(key => {
    const keyIf = menuItem[`${key}If`]
    if (keyIf) {
      menuItem[key] = keyIf(menuItem)
    }
  })
}

/**
 * @param {CommonFormOption} option
 * @return {string}
 */
export const useInputType = (option) => {
  const inType = option.type || 'input'
  if (inType.startsWith('common-') || inType.startsWith('el-')) {
    return inType // 控件全名
  }
  return `el-${option.type || 'input'}`
}

export const MENU_INFO_LIST = ref({})

export const useMenuInfo = item => {
  const path = item.path
  if (path !== '/') {
    return MENU_INFO_LIST.value[path]
  }
}

export const toLabelByKey = labelKey => {
  if (isArray(labelKey)) {
    return $i18nKey(...labelKey)
  }
  if (labelKey) {
    return $i18nBundle(labelKey)
  }
}

export const useMenuName = item => {
  const menuInfo = useMenuInfo(item)
  if (menuInfo) {
    if (menuInfo.label) {
      return menuInfo.label
    }
    if (menuInfo.labelKey) {
      return toLabelByKey(menuInfo.labelKey)
    }
  }
  if (item.meta && item.meta.labelKey) {
    return toLabelByKey(item.meta.labelKey)
  }
  return item.name || 'No Name'
}
/**
 * 外部链接判断
 * @param path
 * @return {boolean}
 */
export const isExternalLink = (path) => {
  return /^(https?:|mailto:|tel:)/.test(path)
}
/**
 * 外部菜单类型判断
 * @param menuItem
 * @return {boolean}
 */
export const isExternalMenu = menuItem => {
  return menuItem.external || isExternalLink(menuItem.index)
}

/**
 * @param menus {[CommonMenuItem] }菜单列表
 * @return {[CommonMenuItem]}
 */
export const processMenus = menus => menus.filter(menu => menu.enabled !== false)
  .map(menu => {
    calcWithIf(menu)
    if (menu.index) { // 把菜单存储下来，后面需要使用名字
      MENU_INFO_LIST.value[menu.index] = menu
    }
    if (menu.children && menu.children.length) {
      menu.children = processMenus(menu.children)
    }
    if (isExternalMenu(menu) && !menu.click) { // 跳转外部链接
      const url = menu.index
      menu.index = ''
      menu.click = () => {
        window.open(url, menu.target || '_blank')
      }
    }
    return menu
  })

/**
 * 如果有replaceTabHistory，获取上级菜单
 * @function useParentRoute
 * @param route {RouteRecordMultipleViewsWithChildren} 路由信息
 */
export const useParentRoute = function (route) {
  const parentName = route?.meta?.replaceTabHistory
  if (parentName) {
    const routes = route.matched || []
    for (let i = routes.length - 1; i > 0; i--) {
      const r = routes[i]
      if ((!r.meta || !r.meta.replaceTabHistory) && r.path !== '/') {
        return r
      }
    }
  }
  return route
}

export const parsePathParams = (path, params) => {
  if (path && path.includes(':') && isObject(params)) {
    Object.keys(params).forEach(key => {
      path = path.replace(new RegExp(`:${key}`, 'g'), params[key])
    })
  }
  return path
}

export const proxyMethod = (targets = [], methodName) => {
  return (...args) => {
    const results = []
    for (let target of targets.filter(target => !!unref(target))) {
      target = unref(target)
      const method = target[methodName]
      if (isFunction(method)) {
        results.push(method.call(target, ...args))
      }
    }
    if (isObject(results[0]) && isFunction(results[0]?.then)) {
      return Promise.all(results)
    }
    return results
  }
}

/**
 * 定义表单选项，带有jsdoc注解，方便代码提示
 * @param {CommonFormOption|CommonFormOption[]} formOptions 表单选项
 * @return {CommonFormOption|CommonFormOption[]} 表单选项配置
 */
export const defineFormOptions = (formOptions) => {
  return formOptions
}

/**
 * 定义表格选项，带有jsdoc注解，方便代码提示
 * @param {CommonTableColumn[]} tableColumns 表单的列
 * @return {CommonTableColumn[]} 表单的列配置
 */
export const defineTableColumns = (tableColumns) => {
  return tableColumns
}

/**
 * 定义表格的按钮，带有jsdoc注解，方便代码提示
 * @param {TableButtonProps[]} tableButtons 表格的按钮
 * @return {TableButtonProps[]} 表格的按钮配置
 */
export const defineTableButtons = (tableButtons) => {
  return tableButtons
}
/**
 * @param menuItems {CommonMenuItem[]} 菜单配置项
 * @return {CommonMenuItem[]} 菜单配置项
 */
export const defineMenuItems = (menuItems) => {
  return menuItems
}
