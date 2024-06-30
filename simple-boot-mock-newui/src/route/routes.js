import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import AdminRoutes from '@/route/AdminRoutes'
import MockRoutes from '@/route/MockRoutes'
import ToolsRoutes from '@/route/ToolsRoutes'
import { checkRouteAuthority, processRouteLoading } from '@/authority'
import { checkReplaceHistoryShouldReplace } from '@/route/RouteUtils'
import { useTabsViewStore } from '@/stores/TabsViewStore'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: {
        icon: 'HomeFilled',
        labelKey: 'common.label.index'
      },
      children: [{
        path: '',
        name: 'index',
        component: () => import('@/views/Index.vue'),
        meta: {
          icon: 'HomeFilled',
          labelKey: 'common.label.index'
        },
        redirect: '/mock/groups'
      }, {
        path: 'about',
        name: 'about',
        component: () => import('@/views/account/AboutView.vue')
      }, {
        path: 'personal',
        name: 'personal',
        component: () => import('@/views/account/PersonalInfo.vue')
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/404.vue'),
        meta: {
          icon: 'QuestionFilled',
          label: 'Not Found'
        }
      },
      ...AdminRoutes,
      ...MockRoutes,
      ...ToolsRoutes
      ]
    }, {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: {
        beforeLogin: true
      }
    }
  ]
})

const scrollMain = (to, scrollOption) => {
  setTimeout(() => { // 因为有0.3s动画需要延迟到动画之后
    scrollOption = scrollOption || to?.meta?.scroll || { top: 0 }
    console.log('======================scrollTo', scrollOption.top)
    document.querySelector('.home-main')?.scrollTo(scrollOption)
  }, 350)
}

/**
 * 自定义路由滚动行为，在home-main容器中滚动顶部
 * @param to
 * @param from
 */
export const routeScrollBehavior = (to, from) => {
  const tabsViewStore = useTabsViewStore()
  const scrollOption = !checkReplaceHistoryShouldReplace(to, from) ? tabsViewStore.currentTabItem?.scroll : undefined
  scrollMain(to, scrollOption)
}

router.beforeEach(checkRouteAuthority)
router.afterEach((...args) => {
  processRouteLoading(...args)
  routeScrollBehavior(...args)
})

export default router
