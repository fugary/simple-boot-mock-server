export default {
  namespaced: true,
  state: {
    persistData: {
      theme: null
    }
  },
  getters: {
    currentTheme: state => {
      return state.persistData.theme
    }
  },
  mutations: {
    changeTheme (state, theme) {
      state.persistData.theme = theme
    }
  },
  actions: {
    changeTheme ({ commit }, theme) {
      commit('changeTheme', theme)
    }
  }
}
