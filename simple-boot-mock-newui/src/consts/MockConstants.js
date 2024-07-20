import { defineFormOptions } from '@/components/utils'
import { generateJWT, SUPPORTED_ALGORITHMS } from '@/utils/JwtUtils'
import { getSingleSelectOptions } from '@/utils'
import { ElMessage } from 'element-plus'

export const MOCK_DEFAULT_PROJECT = 'default'

export const AUTHORIZATION_KEY = 'Authorization'

export const BEARER_KEY = 'Bearer'

export const AUTH_TYPE = {
  NONE: 'none',
  BASIC: 'basic',
  TOKEN: 'token',
  JWT: 'jwt'
}

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
  label: '认证参数名',
  prop: 'headerName',
  value: AUTHORIZATION_KEY,
  required: true,
  tooltip: `认证默认值为：${AUTHORIZATION_KEY}，不能为空`
}, {
  label: '前缀',
  prop: 'tokenPrefix',
  value: BEARER_KEY,
  tooltip: `前缀默认值为：${BEARER_KEY}，可以为空`
}])

const addTokenToParams = (model, token, headers, params) => {
  if (model.tokenToType === 'header') {
    headers[model.headerName] = `${model.tokenPrefix} ${token}`
  } else {
    params[model.headerName] = `${model.tokenPrefix} ${token}`
  }
}

export const AUTH_OPTION_CONFIG = {
  none: {
    parseAuthInfo () {}
  },
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
      attrs: {
        showPassword: true
      }
    }, {
      label: 'base64 encoded',
      prop: 'base64',
      type: 'switch'
    }, {
      label: 'Payload',
      prop: 'payload',
      value: '{}',
      required: true,
      attrs: {
        type: 'textarea'
      }
    }]),
    async parseAuthInfo (model, headers, params) {
      let payload = {}
      if (model.payload) {
        payload = JSON.parse(model.payload)
      }
      try {
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

export const AUTH_OPTIONS = [{
  value: AUTH_TYPE.NONE,
  label: '无认证'
}, {
  value: AUTH_TYPE.BASIC,
  label: 'Basic认证'
}, {
  value: AUTH_TYPE.TOKEN,
  label: 'TOKEN认证'
}, {
  value: AUTH_TYPE.JWT,
  label: 'JWT认证'
}]
