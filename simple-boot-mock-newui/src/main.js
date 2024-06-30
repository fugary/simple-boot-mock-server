import { createApp } from 'vue'
import VueVirtualScroller from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import '@/vendors/dayjs.js'
import ElementPlus from '@/vendors/element-plus'
import stores from '@/stores'
import icons from '@/icons'
import messages from '@/messages'
import commons from '@/components'
import utils from '@/utils'
import DynamicUtils from '@/utils/DynamicUtils'

import App from '@/App.vue'
import router from '@/route/routes'

import './assets/main.css'
import MonacoEditor from '@/vendors/monaco-editor'

const app = createApp(App)

app.use(VueVirtualScroller)
app.use(stores)
app.use(router)
app.use(ElementPlus)
app.use(icons)
app.use(messages)
app.use(commons)
app.use(utils)
app.use(DynamicUtils)
app.use(MonacoEditor)

app.mount('#app')
