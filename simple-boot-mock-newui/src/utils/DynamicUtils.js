import { DynamicHelper } from '@/components/directives'

const ShowUserInfo = () => import('@/views/components/user/ShowUserInfo.vue')
const CodeWindow = () => import('@/views/components/utils/CodeWindow.vue')

export const showUserInfo = async (id) => {
  const dynamicHelper = new DynamicHelper()
  const vnode = await dynamicHelper.createAndRender(ShowUserInfo, {
    onClosed: () => dynamicHelper.destroy()
  })
  vnode.component?.exposed?.showUserInfo(id)
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

export default {
  /**
     * 动态组件工具
     * @param app {import('vue').App}
     */
  install (app) {
    DynamicHelper.app = app
  }
}