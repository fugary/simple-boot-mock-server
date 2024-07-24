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
