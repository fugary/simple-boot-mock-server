import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import * as MaterialIconsVue from '@vicons/material'
import { kebabCase } from 'lodash-es'

export const INSTALL_ICONS = []
export const ICON_PREFIX = 'icon-'
/**
 * icon组件注册工具,默认增加icon-前缀，如果重复，再增加前缀
 * @param app {import('vue').App} Vue实例
 * @param key {string}图标名称
 * @param component {import('vue').Component}组件
 * @param prefix 如果已经注册，增加前缀防止覆盖
 */
const registryIconComponent = (app, key, component, prefix) => {
  let componentName = `${ICON_PREFIX}${kebabCase(key)}` // 组件名字
  if (app.component(componentName)) {
    key = `${prefix}${key}`
    componentName = `${ICON_PREFIX}${kebabCase(key)}`
  }
  INSTALL_ICONS.push(key)
  app.component(componentName, component)
}

export default {
  /**
     * 注册图标
     * @param app {import('vue').App}
     */
  install (app) {
    for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
      registryIconComponent(app, key, component, 'El')
    }
    for (const [key, component] of Object.entries(MaterialIconsVue)) {
      registryIconComponent(app, key, component, 'Md')
    }
  }
}
