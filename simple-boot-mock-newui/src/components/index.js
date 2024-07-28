import CommonIcon from '@/components/common-icon/index.vue'
import CommonIconSelect from '@/components/common-icon-select/index.vue'
import CommonForm from '@/components/common-form/index.vue'
import CommonFormControl from '@/components/common-form-control/index.vue'
import CommonFilterControl from '@/components/common-form-control/common-filter-control.vue'
import CommonTabFilter from '@/components/common-form-control/common-tab-filter.vue'
import CommonFormLabel from '@/components/common-form-control/common-form-label.vue'
import CommonMenu from '@/components/common-menu/index.vue'
import CommonMenuItem from '@/components/common-menu-item/index.vue'
import CommonTabsView from '@/components/common-tabs-view/index.vue'
import CommonTable from '@/components/common-table/index.vue'
import CommonTableV2 from '@/components/common-table/common-table-v2.vue'
import CommonTableForm from '@/components/common-table-form/index.vue'
import CommonBreadcrumb from '@/components/common-breadcrumb/index.vue'
import CommonWindow from '@/components/common-window/index.vue'
import CommonAutocomplete from '@/components/common-autocomplete/index.vue'
import CommonSort from '@/components/common-sort/index.vue'
import CommonDescriptions from '@/components/common-descriptions/index.vue'
import CommonDirectives from '@/components/directives'

/**
 * 自定义通用组件自动注册
 */
export default {
  /**
   * @param Vue {import('vue').App} IDE（IDEA）插件似乎不能正常提示vue3组件注册，参数名写成Vue才能提示
   */
  install (Vue) {
    Vue.component('CommonIcon', CommonIcon)
    Vue.component('CommonIconSelect', CommonIconSelect)
    Vue.component('CommonForm', CommonForm)
    Vue.component('CommonFormControl', CommonFormControl)
    Vue.component('CommonFilterControl', CommonFilterControl)
    Vue.component('CommonTabFilter', CommonTabFilter)
    Vue.component('CommonFormLabel', CommonFormLabel)
    Vue.component('CommonMenu', CommonMenu)
    Vue.component('CommonMenuItem', CommonMenuItem)
    Vue.component('CommonTabsView', CommonTabsView)
    Vue.component('CommonTable', CommonTable)
    Vue.component('CommonTableV2', CommonTableV2)
    Vue.component('CommonTableForm', CommonTableForm)
    Vue.component('CommonBreadcrumb', CommonBreadcrumb)
    Vue.component('CommonWindow', CommonWindow)
    Vue.component('CommonAutocomplete', CommonAutocomplete)
    Vue.component('CommonSort', CommonSort)
    Vue.component('CommonDescriptions', CommonDescriptions)
    Vue.use(CommonDirectives)
  }
}
