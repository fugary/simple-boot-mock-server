const BASE_PATH = '/mock'

export default [{
  path: `${BASE_PATH}/groups`,
  name: 'MockGroupsBase',
  children: [{
    path: '',
    name: 'MockGroups',
    component: () => import('@/views/mock/MockGroups.vue')
  }, {
    path: ':groupId',
    name: 'MockRequests',
    component: () => import('@/views/mock/MockRequests.vue'),
    meta: {
      replaceTabHistory: 'MockGroups',
      label: 'Mock请求列表',
      icon: 'List'
    }
  }]
}]
