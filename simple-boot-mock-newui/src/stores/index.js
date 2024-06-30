import { createPinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'
import { useSystemKey } from '@/utils'
import { cloneDeep, isFunction } from 'lodash-es'

/**
 * 组合式api的$reset需要自己实现
 *
 * @param store
 */
const piniaPluginResetStore = ({ store }) => {
  const initialState = cloneDeep(store.$state) // deep clone(store.$state)
  store.$reset = () => {
    const initState = cloneDeep(initialState)
    if (isFunction(store.$customReset)) {
      const newState = store.$customReset(initState)
      if (newState) {
        store.$patch(state => Object.assign(state, newState))
        return
      }
    }
    store.$patch(state => Object.assign(state, initState))
  }
}

export default {
  install (app) {
    const pinia = createPinia()
    pinia.use(piniaPluginResetStore)
    pinia.use(createPersistedState({
      key: key => {
        const systemKey = useSystemKey()
        return `__${systemKey}__${key}`
      }
    }))
    app.use(pinia)
    return pinia
  }
}
