import { onMounted, ref } from 'vue'
import { isNumber } from 'lodash-es'

/**
 * Hook: 容器内 Tab / Shift+Tab 自动跳转
 * @param {Ref<HTMLElement>} containerRef - 当前容器 ref
 * @param {string} selector - 可聚焦元素选择器，默认 input/textarea/select/button
 */
export function useTabFocus (containerRef, selector) {
  containerRef = containerRef || ref()
  selector = selector || 'input:not([disabled]):not([readonly]):not([type=checkbox]):not([type=file]), textarea:not([disabled]):not([readonly])'

  const getFocusable = () => {
    const containerEl = containerRef.value?.$el || containerRef.value
    return containerEl ? Array.from(containerEl.querySelectorAll(selector)) : []
  }

  const focusNext = (currentEl) => {
    const elements = getFocusable()
    const index = isNumber(currentEl) ? currentEl : elements.indexOf(currentEl)
    if (index === -1) return
    // 优先寻找没有值的输入框
    let targetIndex = -1
    const len = elements.length
    for (let i = 1; i < len; i++) {
      const idx = (index + i) % len
      if (!elements[idx].value) {
        targetIndex = idx
        break
      }
    }
    // 如果都填了值，则按顺序跳转
    const nextEl = targetIndex !== -1 ? elements[targetIndex] : (elements[index + 1] || elements[0])
    nextEl.focus()
  }

  const focusPrev = (currentEl) => {
    const elements = getFocusable()
    const index = isNumber(currentEl) ? currentEl : elements.indexOf(currentEl)
    if (index === -1) return
    const prevEl = elements[index - 1] || elements[elements.length - 1]
    prevEl.focus()
  }

  onMounted(() => {
    const containerEl = containerRef.value?.$el || containerRef.value
    if (!containerEl) return
    containerEl.addEventListener('keydown', e => {
      const target = e.target
      if (!target.matches(selector)) return
      if (e.key === 'Tab' && !e.shiftKey) { e.preventDefault(); focusNext(target) }
      if (e.key === 'Tab' && e.shiftKey) { e.preventDefault(); focusPrev(target) }
    })
  })

  return { containerRef, focusNext, focusPrev }
}
