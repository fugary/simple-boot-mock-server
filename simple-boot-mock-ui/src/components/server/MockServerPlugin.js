import hljs from 'highlight.js'
import ClipboardJS from 'clipboard/dist/clipboard'
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
    Vue.directive('highlightjs', (el, binding) => {
      el.querySelectorAll('code').forEach(target => {
        target.textContent = binding.value
        hljs.highlightBlock(target)
      })
    })
    Vue.directive('clipboard', (el, binding) => {
      const clipboardInstance = new ClipboardJS(el)
      clipboardInstance.on('success', e => {
        e.clearSelection()
      })
    })
    Object.assign(Vue.prototype, {
      $cleanNewItem(items) {
        if (items.length && !items[items.length - 1].id) {
          items.pop()
        }
      },
      $editTableItem(items = [], item) {
        Object.assign({}, item)
        if (!item.id) {
          if (!items.length || items[items.length - 1].id !== item.id) {
            items.push(item)
          }
        }
      }
    })
  }
}
