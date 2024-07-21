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
    name: 'MockRequests',
    component: () => import('@/views/mock/MockRequests.vue'),
    meta: {
      replaceTabHistory: 'MockGroups',
      labelKey: 'mock.label.mockRequests',
      icon: 'List'
    }
  }]
}, {
  path: `${BASE_PATH}/projects`,
  name: 'MockProjects',
  component: () => import('@/views/mock/MockProjects.vue'),
  meta: {
    replaceTabHistory: 'MockProjects',
    labelKey: 'mock.label.mockProjects',
    icon: 'MessageBox'
  }
}]
