import { isFunction } from 'lodash-es'
import { DynamicHelper } from '@/components/directives'
import { h, defineComponent, defineAsyncComponent } from 'vue'

const MockRequestPreviewWindow = () => import('@/views/components/mock/MockRequestPreviewWindow.vue')
const MockMatchPatternPreview = () => import('@/views/components/mock/MockMatchPatternPreview.vue')
const MockEnvParams = () => import('@/views/components/mock/MockEnvParams.vue')
const ShowUserInfo = () => import('@/views/components/user/ShowUserInfo.vue')
const CodeWindow = () => import('@/views/components/utils/CodeWindow.vue')

export const closeAllOnRouteChange = () => {
  document.querySelectorAll('.el-overlay:not([style*="display: none"]) .common-window .el-dialog__headerbtn:not(.dialog-fullscreen-btn)')
    .forEach(target => target?.click())
}

export const showUserInfo = async (id) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(ShowUserInfo, {
    onClosed: () => dynamicHelper.destroy()
  })
  vnode.component?.exposed?.showUserInfo(id)
}

export const previewMockRequest = async (...args) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(MockRequestPreviewWindow, {
    onClosed: () => dynamicHelper.destroy()
  })
  vnode.component?.exposed?.toPreviewRequest(...args)
}

export const toTestMatchPattern = async (...args) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(MockMatchPatternPreview, {
    onClosed: () => dynamicHelper.destroy()
  })
  return vnode.component?.exposed?.toTestMatchPattern(...args)
}

export const toEditGroupEnvParams = async (...args) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(MockEnvParams, {
    onClosed: () => dynamicHelper.destroy()
  })
  return vnode.component?.exposed?.toEditGroupEnvParams(...args)
}

/**
 * @param code String 代码内容
 * @param config {CodeWindowConfig} 配置信息
 */
export const showCodeWindow = async (code, config = {}) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(CodeWindow, {
    onClosed: () => dynamicHelper.destroy()
  })
  vnode.component?.exposed?.showCodeWindow(code, config)
}

/**
 * 构建新名字的组件，用于keepalive缓存
 *
 * @param {String} name 组件自定义名称
 * @param {Component | Promise<Component>} component 组件或者()=>import(组件)
 * @return {Component}
 */
export function createNewComponent (name, component) {
  return defineComponent({
    name,
    setup () {
      const oldComponent = isFunction(component) ? defineAsyncComponent(component) : component
      return () => h(oldComponent)
    }
  })
}

export default {
  /**
     * 动态组件工具
     * @param app {import('vue').App}
     */
  install (app) {
    DynamicHelper.app = app
  }
}
