import { ref, defineComponent, h, watch, computed } from 'vue'
import { ElButton } from 'element-plus'
import CommonIcon from '@/components/common-icon/index.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useElementSize } from '@vueuse/core'

/**
 * 控制浮窗是否固定
 * @param scrollSelector
 * @param config
 * @return {{affixChangeButton: DefineSetupFnComponent<Record<string, any>, {}, {}>, disableAffix: Ref<UnwrapRef<boolean>>}}
 */
export const useDisableAffix = (scrollSelector = '.home-main', config = {}) => {
  const disableAffix = ref(false)
  const toggleDisableAffix = () => {
    disableAffix.value = !disableAffix.value
  }
  const AffixToggleButton = defineComponent(() => {
    return () => h(ElButton, Object.assign({
      style: 'float:right',
      size: 'small',
      type: disableAffix.value ? 'warning' : 'primary',
      onClick: toggleDisableAffix
    }, config), () => [h(CommonIcon, {
      size: 18,
      icon: disableAffix.value ? 'PinOffFilled' : 'PushPinFilled'
    })])
  })
  watch([() => useGlobalConfigStore().layoutMode, () => useGlobalConfigStore().isCollapseLeft], () => {
    const container = document.querySelector(scrollSelector)
    container?.scrollTo({ top: 0 })
  })
  return {
    disableAffix,
    AffixToggleButton
  }
}

export const useElementAffixOffset = (disableAffix, targetOffset = 20, offset = 10) => {
  const targetElementRef = ref(null)
  const { height } = useElementSize(targetElementRef)

  const targetAffixOffset = computed(() => {
    if (height.value && !disableAffix.value) {
      return height.value + targetOffset
    }
    return offset
  })
  return {
    targetElementRef,
    targetAffixOffset
  }
}
