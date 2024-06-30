import ElementPlus, { ElTag, ElSelect, ElSelectV2, ElTable } from 'element-plus'
import 'element-plus/dist/index.css'
// 黑色模式
import 'element-plus/theme-chalk/dark/css-vars.css'

/**
 * 修改默认值
 */
const setDefaultProps = () => {
  ElTag.props.disableTransitions = {
    type: Boolean,
    default: true
  }
  ElSelect.props.defaultFirstOption = {
    type: Boolean,
    default: true
  }
  ElSelect.props.emptyValues = {
    type: Array,
    default: () => [null, undefined]
  }
  ElSelectV2.props.defaultFirstOption = {
    type: Boolean,
    default: true
  }
  ElSelect.props.emptyValues = {
    type: Array,
    default: () => [null, undefined]
  }
  ElTable.props.scrollbarAlwaysOn = {
    type: Boolean,
    default: true
  }
}

export default {
  install (app) {
    app.use(ElementPlus)
    setDefaultProps()
  }
}
