/* eslint-disable @typescript-eslint/no-unused-vars */
interface RequestVoType {
    /** request请求头信息 */
    headers: {
        [key:string] :string
    },
    /** request请求参数和路径参数合并 */
    params: {
        [key:string] :string
    },
    /** request请求参数 */
    parameters: {
        [key:string] :string
    },
    /** request路径参数 */
    pathParameters: {
        [key:string] :string
    },
    /** request请求体对象 */
    body: {
        [key:string] :any
    },
    /** request请求体字符串 */
    bodyStr: string
}

/**
 * request请求封装:
 * request.body——body内容对象
 * request.bodyStr——body内容字符串
 * request.headers——头信息对象
 * request.parameters——请求参数对象
 * request.pathParameters——路径参数对象
 * request.params——请求参数和路径参数合并
 */
const request: RequestVoType

/**
 * 将十六进制字符串解码普通字符串
 * @param hex 十六进制字符串
 * @returns result 解码后的数据
 */
const decodeHex: (hex: string) => string
/**
 * 将普通字符串编码为十六进制格式
 * @param data 数据
 * @returns result 十六进制字符串
 */
const encodeHex: (data: string) => string
/**
 * 数据MD5加密，并输出十六进制数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const md5Hex: (data: string) => string
/**
 * 数据MD5加密，并输出Base64数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const md5Base64: (data: string) => string
/**
 * 数据SHA1加密，并输出十六进制数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const sha1Hex: (data: string) => string
/**
 * 数据SHA1加密，并输出Base64数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const sha1Base64: (data: string) => string
/**
 * 数据SHA256加密，并输出十六进制数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const sha256Hex: (data: string) => string
/**
 * 数据SHA256加密，并输出Base64数据格式
 * @param data 数据
 * @returns result 加密后数据
 */
const sha256Base64: (data: string) => string
/**
 * 数据AES加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 加密后数据
 */
const encryptAES: (data: string, key: string, options?: any) => string
/**
 * 数据DES加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 加密后数据
 */
const encryptDES: (data: string, key: string, options?: any) => string
/**
 * 数据3DES加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 加密后数据
 */
const encrypt3DES: (data: string, key: string, options?: any) => string
/**
 * 数据SM4加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 加密后数据
 */
const encryptSM4: (data: string, key: string, options?: any) => string
/**
 * 数据RSA加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 加密后数据
 */
const encryptRSA: (data: string, key: string, options?: any) => string
/**
 * 数据AES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 解密后数据
 */
const decryptAES: (data: string, key: string, options?: any) => string
/**
 * 数据DES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 解密后数据
 */
const decryptDES: (data: string, key: string, options?: any) => string
/**
 * 数据3DES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 解密后数据
 */
const decrypt3DES: (data: string, key: string, options?: any) => string
/**
 * 数据SM4解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 解密后数据
 */
const decryptSM4: (data: string, key: string, options?: any) => string
/**
 * 数据RSA解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置
 * @returns result 解密后数据
 */
const decryptRSA: (data: string, key: string, options?: any) => string
