/**
 * @author gary.fu
 *
 * 处理一些filters
 * Vue2没有内置过滤器，自定义过滤器
 */
import moment from 'moment'
import numeral from 'numeral'

export default {
  name: 'CommonFilterPlugin',
  install(Vue, params = {}) {
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
    /**
     * numeral工具，按照格式format
     */
    Vue.filter('numeral', (value, format) => {
      return numeral(value).format(format)
    })
    /**
     * 简单数字格式化，0,0.00
     */
    Vue.filter('number', (value, size) => {
      const digits = []
      for (let i = 0; i < size; i++) {
        digits.push('0')
      }
      return numeral(value).format('0,0.' + digits.join(''))
    })
    /**
     * 简单货币格式化，0,0.00
     */
    Vue.filter('currency', (value, prefix) => {
      return `${prefix || '¥'} ${numeral(value).format('0,0.00')}`
    })
  }
}
