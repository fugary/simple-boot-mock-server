/**
 * @typedef {Object} LoginVo
 * @property {string} userName
 * @property {string} userPassword
 */
import { $httpPost } from '@/vendors/axios'

/**
 * @param loginVo {LoginVo} 登录账号
 */
export const login = loginVo => {
  return $httpPost('/login', loginVo, { addToken: false, isLogin: true })
}
