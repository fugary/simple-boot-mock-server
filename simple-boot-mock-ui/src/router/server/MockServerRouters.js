import Layout from '@/layout/index'

/**
 * 定义MockServer相关页面
 */
export default [
  {
    path: '/mock-server',
    component: Layout,
    redirect: '/mock-server/groups',
    name: 'MockServer',
    meta: { title: '模拟服务', icon: 'table' },
    children: [
      {
        path: 'groups',
        name: 'MockGroups',
        component: () => import('@/views/server/groups/MockGroupPage'),
        meta: { title: '模拟分组', icon: 'table' }
      },
      {
        path: 'data-preview',
        name: 'MockDataPreview',
        component: () => import('@/views/server/groups/MockDataPreview'),
        meta: { title: '模拟数据测试', icon: 'tree' }
      },
      {
        path: 'requests/:groupId',
        component: () => import('@/views/server/groups/MockRequestPage'),
        name: 'MockRequests',
        meta: { title: '模拟请求', icon: 'tree' },
        hidden: true
      },
      {
        path: process.env.VUE_APP_BASE_API + '/h2-console',
        component: Layout,
        meta: { title: '数据库控制台', icon: 'link' }
      }
    ]
  }
]
