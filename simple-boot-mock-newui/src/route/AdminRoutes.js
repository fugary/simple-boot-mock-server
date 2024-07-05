const BASE_PATH = '/admin'

export default [{
  path: `${BASE_PATH}/users`,
  name: 'UsersBase',
  children: [{
    path: '',
    name: 'Users',
    component: () => import('@/views/admin/Users.vue')
  }, {
    path: 'edit/:id',
    name: 'UserEdit',
    component: () => import('@/views/admin/UserEdit.vue'),
    meta: {
      replaceTabHistory: 'Users',
      label: '用户编辑',
      icon: 'UserFilled'
    }
  }, {
    path: 'new',
    name: 'UserNew',
    component: () => import('@/views/admin/UserEdit.vue'),
    meta: {
      replaceTabHistory: 'Users',
      label: '用户新增',
      icon: 'UserFilled'
    }
  }]
}]
