/**
 * @author gary.fu
 *
 * 处理一些filters
 * Vue2没有内置过滤器，自定义过滤器
 */
import moment from 'moment'

export default {
  name: 'CommonFilterPlugin',
  install (Vue, params = {}) {
    Vue.filter('date', (date, format) => {
      if (date && moment(date).isValid()) {
        return moment(date).format(format)
      }
      return date
    })
    // 原VueJS1.x中的简单filter
    Vue.filter('json', (value) => {
      if (value) {
        return JSON.stringify(value)
      }
      return value
    })
    Vue.filter('uppercase', (value) => {
      return value || value === 0 ? value.toString().toUpperCase() : ''
    })
    Vue.filter('summary', (value) => { // 替换html
      const regx = /<[^>]*>|<\/[^>]*>/gm
      return value ? value.replace(regx, '') : ''
    })
    Vue.filter('lowercase', (value) => {
      return value || value === 0 ? value.toString().toLowerCase() : ''
    })
    Vue.filter('capitalize', (value) => {
      if (!value && value !== 0) return ''
      value = value.toString()
      return value.charAt(0).toUpperCase() + value.slice(1)
    })
    Vue.filter('limitTo', (input, limit, begin) => {
      input = (input ? input.toString() : '').trim()
      begin = (!begin || isNaN(begin)) ? 0 : parseInt(begin)
      begin = (begin < 0) ? Math.max(0, input.length + begin) : begin
      if (limit >= 0) {
        const result = input.slice(begin, begin + limit)
        const suffix = result.length < input.length ? '...' : ''
        return `${result}${suffix}`
      } else {
        if (begin === 0) {
          return input.slice(limit, input.length)
        } else {
          return input.slice(Math.max(0, begin + limit), begin)
        }
      }
    })
  }
}
