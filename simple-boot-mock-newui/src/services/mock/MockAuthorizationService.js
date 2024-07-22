import { defineFormOptions } from '@/components/utils'
import { generateJWT, SUPPORTED_ALGORITHMS } from '@/utils/JwtUtils'
import { getSingleSelectOptions } from '@/utils'
import { ElMessage } from 'element-plus'
import { AUTH_PARAM_NAMES, AUTH_PREFIX_NAMES, AUTHORIZATION_KEY, BEARER_KEY } from '@/consts/MockConstants'
import { $i18nKey } from '@/messages'

const baseOptions = defineFormOptions([{
  label: 'Add Token to',
  prop: 'tokenToType',
  type: 'select',
  value: 'header',
  children: [{
    value: 'header',
    label: 'Request Header'
  }, {
    value: 'parameter',
    label: 'Query Param'
  }]
}, {
  labelKey: 'mock.label.authParamName',
  prop: 'headerName',
  value: AUTHORIZATION_KEY,
  required: true,
  tooltip: $i18nKey('mock.msg.authParamNameTooltip', AUTHORIZATION_KEY),
  type: 'autocomplete',
  attrs: {
    fetchSuggestions: (queryString, cb) => {
      const dataList = AUTH_PARAM_NAMES.filter(item => item.toLowerCase().includes(queryString?.toLowerCase()))
        .map(value => ({ value }))
      cb(dataList)
    },
    triggerOnFocus: false
  }
}, {
  labelKey: 'mock.label.authPrefix',
  prop: 'tokenPrefix',
  value: BEARER_KEY,
  tooltip: $i18nKey('mock.msg.authPrefixTooltip', BEARER_KEY),
  type: 'autocomplete',
  attrs: {
    fetchSuggestions: (queryString, cb) => {
      const dataList = AUTH_PREFIX_NAMES.filter(item => item.toLowerCase().includes(queryString?.toLowerCase()))
        .map(value => ({ value }))
      cb(dataList)
    },
    triggerOnFocus: false
  }
}])

const addTokenToParams = (model, token, headers, params) => {
  if (model.tokenToType === 'header') {
    headers[model.headerName] = `${model.tokenPrefix} ${token}`
  } else {
    params[model.headerName] = `${model.tokenPrefix} ${token}`
  }
}

export const AUTH_OPTION_CONFIG = {
  basic: {
    options: defineFormOptions([{
      labelKey: 'common.label.username',
      prop: 'userName',
      required: true
    }, {
      labelKey: 'common.label.password',
      prop: 'userPassword',
      required: true
    }]),
    parseAuthInfo (model, headers) {
      // token等于model的属性userName:userPassword的格式后用base64编码
      const token = btoa(`${model.userName}:${model.userPassword}`)
      headers[AUTHORIZATION_KEY] = `Basic ${token}`
    }
  },
  token: {
    options: defineFormOptions([...baseOptions, {
      label: 'Token',
      prop: 'token',
      required: true
    }]),
    parseAuthInfo (model, headers, params) {
      // token和前缀已经存在model中，直接使用，根据tokenToType判断是否是header还是query
      addTokenToParams(model, model.token, headers, params)
    }
  },
  jwt: {
    options: defineFormOptions([...baseOptions, {
      label: 'Algorithm',
      prop: 'algorithm',
      type: 'select',
      value: SUPPORTED_ALGORITHMS[0],
      children: getSingleSelectOptions(...SUPPORTED_ALGORITHMS),
      required: true,
      attrs: {
        clearable: false
      }
    }, {
      label: 'Secret',
      prop: 'secret',
      required: true,
      attrs: {
        showPassword: true
      }
    }, {
      label: 'base64 encoded',
      prop: 'base64',
      type: 'switch'
    }]),
    async parseAuthInfo (model, headers, params) {
      let payload = {}
      try {
        if (model.payload) {
          payload = JSON.parse(model.payload)
        }
        const secret = model.base64 ? atob(model.secret) : model.secret
        // 计算jwtToken，使用jose库
        const token = await generateJWT(payload, secret, model.algorithm)
        addTokenToParams(model, token, headers, params)
      } catch (e) {
        ElMessage.error(e.message)
        throw e
      }
    }
  }
}
