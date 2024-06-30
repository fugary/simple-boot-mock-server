import { cloneDeep } from 'lodash-es'

const base = { // 预定义几种属性
  label: {},
  msg: {},
  error: {}
}

export function baseMessages () {
  return cloneDeep(base)
}
