import ClipboardJS from 'clipboard/dist/clipboard'
import { Message } from 'element-ui'
export default {
  name: 'MockServerPlugin',
  install(Vue, params = {}) {
    Vue.filter('statusFilter', status => {
      const statusMap = {
        0: '禁用',
        1: '启用'
      }
      return statusMap[status]
    })
    Vue.directive('clipboard', {
      bind(el, binding, vnode) {
        const clipboardInstance = new ClipboardJS(el, {
          text() {
            return vnode.clipboardText
          }
        })
        clipboardInstance.on('success', e => {
          e.clearSelection()
          Message.success('Copy Success.')
        })
        vnode.clipboardText = binding.value
        vnode.clipboardInstance = clipboardInstance
      }, update(el, binding, vnode) {
        vnode.clipboardText = binding.value
      }, unbind(el, binding, vnode) {
        vnode.clipboardInstance && vnode.clipboardInstance.destroy()
      }
    })
    Object.assign(Vue.prototype, {
      $cleanNewItem(items) {
        if (items.length && !items[items.length - 1].id) {
          items.pop()
        }
      },
      $editTableItem(items = [], item) {
        if (!item.id) {
          if (!items.length || items[items.length - 1].id !== item.id) {
            items.push(item)
          }
        }
      }
    })
  }
}
