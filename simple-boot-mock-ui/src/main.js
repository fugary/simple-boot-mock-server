import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
// 引入ElementUI的js和css
import ElementUI from 'element-ui'
import VueHighlightJS from 'vue-highlightjs'
// 样式配置
import './assets/css/main.scss'
import CommonFilterPlugin from './utils/CommonFilterPlugin'
import MockServerPlugin from './utils/MockServerPlugin'
import SimplePlugin from './utils/SimplePlugin'
// 配置ElementUI
Vue.use(ElementUI)
// 自定义插件
Vue.use(SimplePlugin)
Vue.use(VueHighlightJS)
Vue.use(CommonFilterPlugin)
Vue.use(MockServerPlugin)

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App),
  mounted () {
    this.$changeTheme(store.getters['Theme/currentTheme'] || undefined)
  }
}).$mount('#app')
