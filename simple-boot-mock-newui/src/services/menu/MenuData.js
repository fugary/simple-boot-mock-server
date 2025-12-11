import { BASE_URL } from '@/config'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { ref } from 'vue'
import { $i18nBundle } from '@/messages'
import { isAdminUser } from '@/utils'

const dbUrl = getMockUrl(`${BASE_URL}${BASE_URL.endsWith('/') ? '' : '/'}h2-console`)
const editorContent = ref('')
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
    id: 12,
    parentId: 1,
    iconCls: 'MessageBox',
    nameCn: 'Mock项目',
    nameEn: 'Mock Projects',
    menuUrl: '/mock/projects'
  },
  {
    id: 13,
    parentId: 1,
    iconCls: 'Share',
    nameCn: '公开项目',
    nameEn: 'Public Projects',
    menuUrl: '/mock/pubProjects'
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
    id: 52,
    parentId: 5,
    iconCls: 'Document',
    nameCn: '日志管理',
    nameEn: 'Log Management',
    menuUrl: '/admin/logs',
    checkEnabled: isAdminUser
  },
  {
    id: 53,
    parentId: 5,
    iconCls: 'Coin',
    nameCn: '数据库连接池',
    nameEn: 'DB Pool',
    menuUrl: '/admin/dbs',
    checkEnabled: isAdminUser
  },
  {
    id: 59,
    parentId: 5,
    iconCls: 'Coin',
    nameCn: 'H2数据库',
    nameEn: 'H2 Database',
    dbConsole: true,
    external: true,
    menuUrl: dbUrl,
    checkEnabled: isAdminUser
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
  },
  {
    id: 95,
    parentId: 9,
    iconCls: 'EditPen',
    nameCn: '代码编辑器',
    nameEn: 'Code Editor',
    click: () => {
      showCodeWindow(editorContent, {
        title: $i18nBundle('common.label.codeEdit'),
        fullEditor: true,
        readOnly: false,
        closeOnClickModal: false
      })
    }
  },
  {
    id: 96,
    parentId: 9,
    iconCls: 'DifferenceFilled',
    nameCn: '对比编辑器',
    nameEn: 'Diff Editor',
    click: () => {
      showCodeWindow(editorContent, {
        title: $i18nBundle('mock.label.compare'),
        diffEditor: true,
        readOnly: false,
        closeOnClickModal: false
      })
    }
  },
  {
    id: 97,
    parentId: 9,
    iconCls: 'QuestionFilled',
    nameCn: '查看帮助',
    nameEn: 'View Help',
    menuUrl: '/mock/tips'
  }
]
