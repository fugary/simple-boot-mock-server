import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $coreHideLoading, $coreShowLoading } from '@/utils'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { GLOBAL_ROUTE_LOADING, GLOBAL_ROUTE_NEW_LOADING } from '@/config'
import { $changeLocale } from '@/messages'
import { GlobalLocales } from '@/consts/GlobalConstants'
import { useBreadcrumbConfigStore } from '@/stores/BreadcrumbConfigStore'
import { useGlobalSearchParamStore } from '@/stores/GlobalSearchParamStore'

NProgress.configure({ showSpinner: false, trickleSpeed: 500 })

/**
 * 是否开启路由的loading
 * @param route
 * @return {*|boolean}
 */
const checkRouteLoading = route => route?.meta?.loading ?? GLOBAL_ROUTE_LOADING

const startRouteLoading = (route) => {
  if (checkRouteLoading(route)) {
    NProgress.start()
    if (GLOBAL_ROUTE_NEW_LOADING) {
      $coreShowLoading()
    }
  }
}
const endRouteLoading = (route) => {
  if (checkRouteLoading(route)) {
    NProgress.done()
    if (GLOBAL_ROUTE_NEW_LOADING) {
      $coreHideLoading()
    }
  }
}

/**
 * 检查路有权限
 * @param  to {RouteRecordSingleViewWithChildren} 目的地路由
 * @param from 出事路由
 * @returns {{name: string}|boolean}
 */
export const checkRouteAuthority = async (to) => {
  startRouteLoading(to)
  const loginConfigStore = useLoginConfigStore()
  if (to.meta?.beforeLogin) { // 登录前的路由添加meta信息：beforeLogin: true
    return true
  }
  if (loginConfigStore.isLoginIn()) {
    // check权限
    return true
  }
  endRouteLoading(to)
  return { name: 'Login' }
}

const processRouteSavedParam = (to, from) => {
  useGlobalSearchParamStore().savedParamRouteInfo = { // 路由后退处理
    to, from
  }
}

export const processRouteLoading = (to, from) => {
  endRouteLoading(to)
  if (to.query?.language && Object.values(GlobalLocales).includes(to.query.language)) {
    $changeLocale(to.query?.language)
  }
  const { clearBreadcrumbConfig } = useBreadcrumbConfigStore()
  clearBreadcrumbConfig() // 清理面包屑label
  processRouteSavedParam(to, from)
}
