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
    /** request请求体，仅JSON时有效 */
    body: {
        [key:string] :any
    },
    /** request请求体字符串 */
    bodyStr: string
}

const request: RequestVoType
