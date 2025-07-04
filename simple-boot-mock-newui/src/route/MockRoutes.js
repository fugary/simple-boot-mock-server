import { createNewComponent } from '@/utils/DynamicUtils'

const BASE_PATH = '/mock'

export default [{
  path: `${BASE_PATH}/groups`,
  name: 'MockGroupsBase',
  children: [{
    path: '',
    name: 'MockGroups',
    component: () => import('@/views/mock/MockGroups.vue')
  }, {
    path: 'project/:projectCode/:userName?',
    name: 'MockProjectGroups',
    component: createNewComponent('MockProjectGroups', () => import('@/views/mock/MockGroups.vue')),
    meta: {
      replaceTabHistory: 'MockGroups',
      labelKey: 'mock.label.mockGroups',
      icon: 'List'
    }
  }, {
    path: 'pubProject/:projectCode/:userName?',
    name: 'PublicMockProjectGroups',
    component: createNewComponent('PublicMockProjectGroups', () => import('@/views/mock/MockGroups.vue')),
    props: {
      publicFlag: true
    },
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
}, {
  path: `${BASE_PATH}/pubProjects`,
  name: 'PublicMockProjectsNew',
  component: createNewComponent('PublicMockProjectsNew', () => import('@/views/mock/MockProjectsNew.vue')),
  props: {
    publicFlag: true
  },
  meta: {
    replaceTabHistory: 'MockProjectsNew',
    labelKey: 'mock.label.publicMockProjects',
    icon: 'Share'
  }
}, {
  path: `${BASE_PATH}/tips`,
  name: 'MockTips',
  component: () => import('@/views/mock/MockTips.vue'),
  meta: {
    replaceTabHistory: 'MockTips',
    icon: 'QuestionFilled'
  }
}]
