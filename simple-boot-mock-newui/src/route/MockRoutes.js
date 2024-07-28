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
      labelKey: 'mock.label.mockGroups',
      icon: 'List'
    }
  }, {
    path: ':groupId',
    name: 'MockRequestsNew',
    component: () => import('@/views/mock/MockRequestsNew.vue'),
    meta: {
      replaceTabHistory: 'MockGroups',
      labelKey: 'mock.label.mockRequests',
      icon: 'List'
    }
  }]
}, {
  path: `${BASE_PATH}/projects`,
  name: 'MockProjectsNew',
  component: () => import('@/views/mock/MockProjectsNew.vue'),
  meta: {
    replaceTabHistory: 'MockProjectsNew',
    labelKey: 'mock.label.mockProjects',
    icon: 'MessageBox'
  }
}]
