export default {
  name: 'MockServerFilters',
  install(Vue, params = {}) {
    Vue.filter('statusFilter', status => {
      const statusMap = {
        0: '禁用',
        1: '启用'
      }
      return statusMap[status]
    })
  }
}
