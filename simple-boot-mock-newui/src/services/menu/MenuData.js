import { BASE_URL } from '@/config'

const dbUrl = `${BASE_URL}${BASE_URL.endsWith('/') ? '' : '/'}h2-console`
export const ALL_MENUS = [
  {
    id: 1,
    iconCls: 'Files',
    nameCn: 'Mock管理',
    nameEn: 'Mock Management'
  },
  {
    id: 11,
    parentId: 1,
    iconCls: 'List',
    nameCn: 'Mock分组',
    nameEn: 'Mock Groups',
    menuUrl: '/mock/groups'
  },
  {
    id: 5,
    iconCls: 'setting',
    nameCn: '系统管理',
    nameEn: 'System'
  },
  {
    id: 51,
    parentId: 5,
    iconCls: 'UserFilled',
    nameCn: '用户管理',
    nameEn: 'Users',
    menuUrl: '/admin/users'
  },
  {
    id: 9,
    iconCls: 'BuildFilled',
    nameCn: '常用工具',
    nameEn: 'Tools'
  },
  {
    id: 91,
    parentId: 9,
    iconCls: 'InsertEmoticonOutlined',
    nameCn: '图标管理',
    nameEn: 'Icons',
    menuUrl: '/icons'
  }, {
    id: 92,
    parentId: 9,
    iconCls: 'Coin',
    nameCn: '数据库管理',
    nameEn: 'Database',
    dbConsole: true,
    external: true,
    menuUrl: dbUrl
  }
]
