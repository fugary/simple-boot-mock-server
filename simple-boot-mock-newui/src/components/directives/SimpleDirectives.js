import { $openNewWin } from '@/utils'

export const DisableAffixDirective = (el, binding) => {
  if (binding.value) {
    el.classList.add('disable-affix')
  } else {
    el.classList.remove('disable-affix')
  }
}

/**
 * 鼠标中键或者Ctrl+鼠标左键实现新窗口中打开
 * @param el
 * @param binding
 * @constructor
 */
export const OpenNewWindowDirective = (el, binding) => {
  if (binding.value) {
    const config = {
      click: event => event.button === 0 && event.ctrlKey,
      mouseup: event => event.button === 1
    }
    for (const key in config) {
      const handlerKey = `__newWindow__${key}`
      el[handlerKey] && el.removeEventListener(key, el[handlerKey], true)
      const handler = el[handlerKey] = event => {
        if (config[key](event)) {
          event.preventDefault()
          event.stopImmediatePropagation()
          $openNewWin(binding.value)
        }
      }
      el.addEventListener(key, handler, true)
    }
  }
}
