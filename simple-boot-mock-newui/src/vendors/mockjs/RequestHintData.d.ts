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

interface RSAOptionsType {
    /**
     * 默认都是RSA
     */
    algorithm: 'RSA' | 'RSA/ECB/PKCS1Padding' | 'RSA/ECB/NoPadding' | 'RSA/None/NoPadding'
    /**
     * 如果带有-----BEGIN PUBLIC KEY-----，可以计算是公钥还是私钥
     */
    keyType: 'PrivateKey' | 'PublicKey',
    /**
     * 默认都是base64
     */
    output: 'base64' | 'hex',
}

interface AESOptionsType {
    /**
     * 默认是ECB
     */
    mode: 'ECB' | 'CBC' | 'CFB' | 'OFB' | 'CTR' | 'GCM' | 'OCB',
    /**
     * 默认是PKCS5Padding
     */
    padding: 'PKCS5Padding' | 'PKCS7Padding' | 'NoPadding',
    /**
     * 向量值
     */
    iv: string,
    /**
     * 默认都是base64
     */
    output: 'base64' | 'hex',
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
 * 提供一个异步版的 CommonJS 风格 require 方法，用于动态加载第三方库。支持 module.exports 和 exports 形式的模块导出，不支持 ESM (export / import) 模块。返回 Promise，可用于异步获取远程或本地模块内容。
 * @param url 需要加载的js资源路径
 * @returns 异步Promise对象
 */
const require: (url) => Promise

/**
 * 清空 require 函数的缓存。默认情况下，require 函数会缓存已加载过的 URL 模块数据，避免重复加载。
 */
const clearRequireCache: () => void

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
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 加密后数据
 */
const encryptAES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据DES加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 加密后数据
 */
const encryptDES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据3DES加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 加密后数据
 */
const encrypt3DES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据SM4加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 加密后数据
 */
const encryptSM4: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据RSA加密，并输出Base64数据格式
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持algorithm, keyType, output配置
 * @property {'RSA' | 'RSA/ECB/PKCS1Padding' | 'RSA/ECB/NoPadding' | 'RSA/None/NoPadding'} algorithm 加密算法
 * @property {'PrivateKey' | 'PublicKey'} keyType 私钥还是公钥
 * @property {'base64' | 'hex'} output 输出格式
 * @returns result 加密后数据
 */
const encryptRSA: (data: string, key: string, options?: RSAOptionsType) => string
/**
 * 数据AES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 解密后数据
 */
const decryptAES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据DES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 解密后数据
 */
const decryptDES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据3DES解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 解密后数据
 */
const decrypt3DES: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据SM4解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持mode、padding、iv, output配置
 * @returns result 解密后数据
 */
const decryptSM4: (data: string, key: string, options?: AESOptionsType) => string
/**
 * 数据RSA解密
 * @param data 数据
 * @param key 密钥
 * @param options 配置，支持algorithm, keyType, output配置
 * @property {'RSA' | 'RSA/ECB/PKCS1Padding' | 'RSA/ECB/NoPadding' | 'RSA/None/NoPadding'} algorithm 加密算法
 * @property {'PrivateKey' | 'PublicKey'} keyType 私钥还是公钥
 * @property {'base64' | 'hex'} output 输出格式
 * @returns result 解密后数据
 */
const decryptRSA: (data: string, key: string, options?: RSAOptionsType) => string
