const BASE_PATH = '/mock'

export default [{
  path: `${BASE_PATH}`,
  name: 'MocksBase',
  children: [{
    path: 'groups',
    name: 'MockGroups',
    component: () => import('@/views/mock/MockGroups.vue')
  }]
}]
