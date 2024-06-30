import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login } from '@/services/login/LoginService'
import { useTabsViewStore } from '@/stores/TabsViewStore'
import { useGlobalSearchParamStore } from '@/stores/GlobalSearchParamStore'
import { SYSTEM_KEY } from '@/config'
import dayjs from 'dayjs'

export const useLoginConfigStore = defineStore('loginConfig', () => {
  /**
   * 登录结果
   * @type {{value: {accessToken: string, account: Object, systemKey: string, expires: Date}}}
   */
  const loginResult = ref(null)
  /**
   * 登录成功后保存accessToken
   * @type {Ref<string>}
   */
  const accessToken = computed(() => loginResult.value?.accessToken)
  /**
   * 保存登录用户信息
   * @type {Object}
   */
  const accountInfo = computed(() => loginResult.value?.account)
  /**
   * 系统key
   * @type {Ref<string>}
   */
  const systemKey = computed(() => loginResult.value?.systemKey || SYSTEM_KEY)
  /**
   * 记住上次登录名
   * @type {Ref<UnwrapRef<string>>}
   */
  const lastLoginName = ref('')

  return {
    loginResult,
    lastLoginName,
    accessToken,
    accountInfo,
    systemKey,
    /**
     * @param {{account: Object, accessToken:string}} resultData
     */
    setLoginAccountInfo (resultData) {
      loginResult.value = resultData
      lastLoginName.value = resultData?.account?.actualLoginName
    },
    clearLoginInfo () {
      loginResult.value = null
    },
    isLoginIn () {
      if (loginResult.value?.expires) {
        if (dayjs(loginResult.value.expires).isBefore(dayjs())) { // Token过期
          this.logout()
          return false
        }
      }
      return !!accessToken.value
    },
    logout () {
      // 清理登录数据
      this.clearLoginInfo()
      // $reset清理数据
      useTabsViewStore().$reset()
      useGlobalSearchParamStore().$reset()
    },
    async login (loginVo) {
      const loginResult = await login(loginVo)
      if (loginResult.success) {
        this.setLoginAccountInfo(loginResult.resultData)
      }
      return loginResult
    }
  }
}, {
  persist: true
})
