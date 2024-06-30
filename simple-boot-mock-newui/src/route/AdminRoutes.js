const BASE_PATH = '/admin'

export default [{
  path: `${BASE_PATH}/users`,
  name: 'UsersBase',
  children: [{
    path: '',
    name: 'Users',
    component: () => import('@/views/admin/Users.vue')
  }]
}]
