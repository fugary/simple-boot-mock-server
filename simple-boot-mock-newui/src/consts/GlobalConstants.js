import { $i18nBundle } from '@/messages'
/**
 * 全局布局模式
 * @readonly
 * @enum {string}
 */
export const GlobalLayoutMode = {
  LEFT: 'left',
  TOP: 'top'
}

/**
 * 全局语言
 * @readonly
 * @enum {string}
 */
export const GlobalLocales = {
  CN: 'zh-CN',
  EN: 'en-US'
}
/**
 * 搜索调条件记住
 */
export const LoadSaveParamMode = {
  ALL: 'all',
  BACK: 'back',
  NEVER: 'never'
}

export const useFormStatus = (prop = 'status', activeValue = 1, inactiveValue = 0) => {
  return {
    labelKey: 'common.label.status',
    prop,
    type: 'switch',
    attrs: {
      activeValue,
      inactiveValue,
      activeText: $i18nBundle('common.label.statusEnabled'),
      inactiveText: $i18nBundle('common.label.statusDisabled')
    }
  }
}

export const useFormDelay = (prop = 'delay') => {
  return {
    labelKey: 'common.label.delay',
    tooltip: '延迟时间，单位：毫秒',
    type: 'input-number',
    prop
  }
}
