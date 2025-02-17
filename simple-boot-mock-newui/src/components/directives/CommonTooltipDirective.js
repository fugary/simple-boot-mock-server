import { isObject, isString } from 'lodash-es'
import CommonTooltip from '@/components/directives/CommonTooltip.vue'
import { DynamicHelper } from '@/components/directives/index'

const calcTooltipConfig = (binding) => {
  let config = {}
  if (isObject(binding.value)) {
    config = { ...config, ...binding.value }
  } else if (isString(binding.value)) {
    config.content = binding.value
  }
  if (!config.placement && binding.arg) {
    config.placement = binding.arg
  }
  return config
}

const initTooltipVnode = (el, binding, props) => {
  const dynamicHelper = new DynamicHelper()
  const tooltipVnode = dynamicHelper.createAndRender(CommonTooltip, props)
  el.tooltipVnode = tooltipVnode
  el.tooltipDynamicHelper = dynamicHelper
  if (!el.tooltipConfig) {
    el.tooltipConfig = calcTooltipConfig(binding)
  }
  if (tooltipVnode.component?.exposed?.triggerRef) {
    tooltipVnode.component.exposed.triggerRef.value = el
  }
}

const getTooltipDirective = (props) => {
  return {
    mounted (el, binding) {
      el.addEventListener('mouseenter', () => {
        if (!el.tooltipVnode) {
          initTooltipVnode(el, binding, props)
        }
        el?.tooltipVnode?.component?.exposed?.setConfig(el.tooltipConfig)
        el.tooltipVnode?.component?.exposed?.showOrHideTooltip(true)
      })
      el.addEventListener('mouseleave', () => {
        el.tooltipVnode?.component?.exposed?.showOrHideTooltip(false)
      })
    },
    updated (el, binding) {
      el.tooltipConfig = calcTooltipConfig(binding)
    },
    unmounted (el) {
      el.tooltipVnode?.component?.exposed?.showOrHideTooltip(false)
      el.tooltipDynamicHelper?.destroy()
    }
  }
}

export const CommonTooltipDirective = getTooltipDirective({ type: 'el-tooltip' })

export const CommonPopoverDirective = getTooltipDirective({ type: 'el-popover' })
