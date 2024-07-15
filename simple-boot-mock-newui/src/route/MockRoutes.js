const BASE_PATH = '/mock'

export default [{
  path: `${BASE_PATH}/groups`,
  name: 'MockGroupsBase',
  children: [{
    path: '',
    name: 'MockGroups',
    component: () => import('@/views/mock/MockGroups.vue')
  }, {
    path: 'project/:projectCode',
    name: 'MockProjectGroups',
    component: () => import('@/views/mock/MockGroups.vue'),
    meta: {
      replaceTabHistory: 'MockGroups',
      label: 'Mock请求列表',
      icon: 'List'
    }
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
}, {
  path: `${BASE_PATH}/projects`,
  name: 'MockProjects',
  component: () => import('@/views/mock/MockProjects.vue'),
  meta: {
    replaceTabHistory: 'MockProjects',
    label: 'Mock项目列表',
    icon: 'MessageBox'
  }
}]
