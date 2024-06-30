import { CommonPopoverDirective, CommonTooltipDirective } from '@/components/directives/CommonTooltipDirective'
import { OpenNewWindowDirective, DisableAffixDirective } from '@/components/directives/SimpleDirectives'
import { h, render } from 'vue'
import { isFunction } from 'lodash-es'
import { ElConfigProvider } from 'element-plus'
import { elementLocale } from '@/messages'

export class DynamicHelper {
  constructor () {
    this.appDivId = 'app'
    this.context = DynamicHelper.app._context
    this.container = DynamicHelper.createContainer()
    this.destroy = DynamicHelper.getDestroyFunc(this.container)
  }

  static createContainer () {
    return document.createElement('div')
  }

  static getDestroyFunc (container) {
    return () => {
      if (container) {
        render(null, container)
      }
    }
  }

  createAndRender (fn, ...args) {
    if (isFunction(fn)) { // 处理异步模式：()=>import('@/xxx.vue')
      return fn().then(fnResult => this.createAndRender0(fnResult.default, ...args))
    }
    return this.createAndRender0(fn, ...args)
  }

  createAndRender0 (...args) {
    const container = this.container
    const vnode = h(...args)
    const configVNode = h(ElConfigProvider, { // 处理动态调用组件页面中element控件语言不正确问题
      locale: elementLocale.value.localeData
    }, () => [vnode])
    configVNode.appContext = this.context
    render(configVNode, container)
    const appDiv = document.getElementById(this.appDivId)
    if (appDiv && container.firstElementChild) {
      appDiv.appendChild(container.firstElementChild)
    }
    return vnode
  }
}

export default {
  install (Vue) {
    DynamicHelper.app = Vue
    Vue.directive('common-tooltip', CommonTooltipDirective)
    Vue.directive('common-popover', CommonPopoverDirective)
    Vue.directive('disable-affix', DisableAffixDirective)
    Vue.directive('open-new-window', OpenNewWindowDirective)
  }
}
