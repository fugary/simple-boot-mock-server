import Home from '@/views/Home'

/**
 * 定义MockServer相关页面
 */
export default [
  {
    path: '/mock-server',
    component: Home,
    redirect: '/mock-server/groups',
    name: 'MockServer',
    children: [
      {
        path: 'groups',
        name: 'MockGroups',
        component: () => import('@/views/server/groups/MockGroupPage')
      },
      {
        path: 'requests/:groupId',
        component: () => import('@/views/server/groups/MockRequestPage'),
        name: 'MockRequests'
      }
    ]
  }
]
