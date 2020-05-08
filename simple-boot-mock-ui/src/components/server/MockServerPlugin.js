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
  }
}
