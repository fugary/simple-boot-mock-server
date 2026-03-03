import { $http } from '@/vendors/axios'

export default {
  getMetrics (all) {
    return $http({
      url: '/admin/dashboard/metrics',
      method: 'get',
      params: { all }
    }).then(res => res.data)
  },
  getTrend (days, all) {
    return $http({
      url: '/admin/dashboard/trend',
      method: 'get',
      params: { days, all }
    }).then(res => res.data)
  },
  getProjectActivity (days, all) {
    return $http({
      url: '/admin/dashboard/project-activity',
      method: 'get',
      params: { days, all }
    }).then(res => res.data)
  },
  getTopApis (limit, logResult, all) {
    return $http({
      url: '/admin/dashboard/top-apis',
      method: 'get',
      params: { limit, logResult, all }
    }).then(res => res.data)
  },
  getMockVsProxy (days, all) {
    return $http({
      url: '/admin/dashboard/mock-vs-proxy',
      method: 'get',
      params: { days, all }
    }).then(res => res.data)
  },
  getPublicVsPrivate (all) {
    return $http({
      url: '/admin/dashboard/public-vs-private',
      method: 'get',
      params: { all }
    }).then(res => res.data)
  },
  getTopUserCalls (limit, days, all) {
    return $http({
      url: '/admin/dashboard/top-user-calls',
      method: 'get',
      params: { limit, days, all }
    }).then(res => res.data)
  },
  getTopUserGroups (limit, all) {
    return $http({
      url: '/admin/dashboard/top-user-groups',
      method: 'get',
      params: { limit, all }
    }).then(res => res.data)
  }
}
