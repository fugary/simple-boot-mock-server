import { defineFormOptions } from '@/components/utils'
import { generateJWT } from '@/api/mock/MockDataApi'
import { getSingleSelectOptions } from '@/utils'
import { ElMessage } from 'element-plus'
import { AUTH_PARAM_NAMES, AUTH_PREFIX_NAMES, AUTHORIZATION_KEY, BEARER_KEY } from '@/consts/MockConstants'
import { $i18nKey } from '@/messages'
import { processEvnParams } from '@/services/mock/MockCommonService'

export const SUPPORTED_ALGORITHMS = [
  'HS256', // HMAC using SHA-256
  'HS384', // HMAC using SHA-384
  'HS512' // HMAC using SHA-512
  // 'RS256', // RSASSA-PKCS1-v1_5 using SHA-256
  // 'RS384', // RSASSA-PKCS1-v1_5 using SHA-384
  // 'RS512', // RSASSA-PKCS1-v1_5 using SHA-512
  // 'PS256', // RSASSA-PSS using SHA-256
  // 'PS384', // RSASSA-PSS using SHA-384
  // 'PS512', // RSASSA-PSS using SHA-512
  // 'ES256', // ECDSA using P-256 and SHA-256
  // 'ES384', // ECDSA using P-384 and SHA-384
  // 'ES512', // ECDSA using P-521 and SHA-512
  // 'EdDSA' // EdDSA using Ed25519 or Ed448
]

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
    parseAuthInfo (model, headers, params, paramTarget) {
      // token等于model的属性userName:userPassword的格式后用base64编码
      const userName = processEvnParams(paramTarget?.value?.groupConfig, model.userName)
      const userPassword = processEvnParams(paramTarget?.value?.groupConfig, model.userPassword)
      const token = btoa(`${userName}:${userPassword}`)
      headers[AUTHORIZATION_KEY] = `Basic ${token}`
    }
  },
  token: {
    options: defineFormOptions([...baseOptions, {
      label: 'Token',
      prop: 'token',
      required: true
    }]),
    parseAuthInfo (model, headers, params, paramTarget) {
      // token和前缀已经存在model中，直接使用，根据tokenToType判断是否是header还是query
      addTokenToParams(model, processEvnParams(paramTarget?.value?.groupConfig, model.token), headers, params)
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
    }, {
      label: 'Expire',
      prop: 'expireTime',
      type: 'date-picker',
      minDate: new Date(),
      attrs: {
        type: 'datetime'
      }
    }]),
    async parseAuthInfo (model, headers, params, paramTarget) {
      try {
        const payload = processEvnParams(paramTarget?.value?.groupConfig, model.payload)
        const secretStr = processEvnParams(paramTarget?.value?.groupConfig, model.secret)
        const secret = model.base64 ? atob(secretStr) : secretStr
        // 计算jwtToken，使用jose库
        const token = await generateJWT({
          payload,
          secret,
          algorithm: model.algorithm
        }).then(data => data?.resultData)
        addTokenToParams(model, token, headers, params)
      } catch (e) {
        ElMessage.error(e.message)
        throw e
      }
    }
  }
}
