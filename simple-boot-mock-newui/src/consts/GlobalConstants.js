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

export const useFormDisableMock = (prop = 'disableMock', activeValue = true, inactiveValue = false) => {
  return {
    labelKey: 'mock.label.disableMock',
    prop,
    type: 'switch',
    tooltip: $i18nBundle('mock.msg.disableMockTooltip'),
    attrs: {
      activeValue,
      inactiveValue,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }
}

export const useSearchStatus = ({ prop = 'status', activeValue = 1, inactiveValue = 0, change } = {}) => {
  return {
    labelKey: 'common.label.status',
    prop,
    type: 'select',
    children: [
      { value: activeValue, label: $i18nBundle('common.label.statusEnabled') },
      { value: inactiveValue, label: $i18nBundle('common.label.statusDisabled') }
    ],
    change
  }
}

export const useFormDelay = (prop = 'delay') => {
  return {
    labelKey: 'common.label.delay',
    tooltip: $i18nBundle('mock.msg.delayTooltip'),
    type: 'input-number',
    prop,
    attrs: {
      min: 0
    }
  }
}
