import { createI18n } from 'vue-i18n'
import { ref } from 'vue'
import messagesCn from './messages_cn'
import messagesEn from './messages_en'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import 'dayjs/locale/zh-cn'
import dayjs from 'dayjs'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { GlobalLocales } from '@/consts/GlobalConstants'
import { isArray, isString } from 'lodash-es'

const DEFAULT_LOCALE = 'zh-CN'
dayjs.locale(DEFAULT_LOCALE) // dayjs的语言配置

const i18n = createI18n({
  locale: DEFAULT_LOCALE, // set locale
  legacy: false, // you must set `false`, to use Composition API
  fallbackLocale: DEFAULT_LOCALE, // set fallback locale
  messages: {
    'zh-CN': messagesCn,
    'en-US': messagesEn
  } // set locale messages
})

export const elementLocale = ref({ // 用于element-plus
  localeData: zhCn
})

export const changeMessages = locale => {
  i18n.global.locale.value = locale
  elementLocale.value.localeData = locale === DEFAULT_LOCALE ? zhCn : en
  dayjs.locale(locale.toLowerCase())
}

export const $changeLocale = locale => {
  useGlobalConfigStore().changeLocale(locale)
}

export const $isLocale = locale => {
  return useGlobalConfigStore().currentLocale === locale
}
/**
 * @param cn 中文字段
 * @param en 英文字段
 * @param {boolean} replaceEmpty 为空是否用不为空的数据代替
 * @returns {String}
 */
export const $i18nMsg = (cn, en, replaceEmpty = true) => {
  const { currentLocale } = useGlobalConfigStore()
  if (currentLocale === GlobalLocales.CN) {
    return replaceEmpty ? (cn || en) : cn
  }
  return replaceEmpty ? (en || cn) : en
}
/**
 * @param {String} key 国际化资源key
 * @param {String[]=} params 可选参数
 * @returns {string}
 */
export const $i18nBundle = (key, params) => {
  if (!key || !isString(key) || !key.includes('.')) { // 仅处理含有.的key
    return key
  }
  return i18n.global.t(key, params)
}

/**
 * 根据key和locale返回数据<br>
 * vue-i18n似乎有bug，按照官方文档传locale得不到正确的消息:<br>
 * <code>$t('ab.c', 'zh-CN')</code><br>
 * https://vue-i18n.intlify.dev/api/injection.html#t-key-locale
 * @param key
 * @param locale
 * @param [args]
 * @return {String}
 */
export const $i18nByLocale = (key, locale, args) => {
  return i18n.global.t(key, locale, {
    locale,
    list: args || []
  })
}

/**
 * 方便多个资源key解析
 * @param {String|[String]} key 国际化资源key
 * @param {String} args 可选参数，也是资源key，方便多个资源key解析
 */
export const $i18nKey = (key, ...args) => {
  if (isArray(key)) {
    args = key.slice(1)
    key = key[0]
  }
  args = args.map(argKey => $i18nBundle(argKey))
  return $i18nBundle(key, args)
}

export const $i18nConcat = (...items) => {
  return items.map(item => (item ?? '')).filter(item => !!item).join($i18nMsg('', ' ', false))
}

export default {
  install (app) {
    app.use(i18n)
    Object.assign(app.config.globalProperties, {
      $changeLocale,
      $i18nMsg,
      $i18nKey,
      $i18nBundle,
      $isLocale,
      $i18nConcat,
      $i18nByLocale
    })
  }
}
