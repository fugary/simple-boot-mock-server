import Vue from 'vue'
import Vuex from 'vuex'
import ThemeStore from './ThemeStore'
import createPersistedState from 'vuex-persistedstate'

Vue.use(Vuex)

const PERSIST_PATHS = ['Theme.persistData']

export default new Vuex.Store({
  state: {
  },
  mutations: {},
  actions: {},
  modules: {
    Theme: ThemeStore
  },
  plugins: [createPersistedState({
    paths: PERSIST_PATHS
  })]
})
