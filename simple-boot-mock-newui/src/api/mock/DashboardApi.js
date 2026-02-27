import { $http } from '@/vendors/axios'

export default {
  getMetrics () {
    return $http({
      url: '/admin/dashboard/metrics',
      method: 'get'
    }).then(res => res.data)
  },
  getTrend (days) {
    return $http({
      url: '/admin/dashboard/trend',
      method: 'get',
      params: { days }
    }).then(res => res.data)
  },
  getProjectActivity (days) {
    return $http({
      url: '/admin/dashboard/project-activity',
      method: 'get',
      params: { days }
    }).then(res => res.data)
  },
  getTopApis (limit, logResult) {
    return $http({
      url: '/admin/dashboard/top-apis',
      method: 'get',
      params: { limit, logResult }
    }).then(res => res.data)
  }
}
