/* eslint-disable @typescript-eslint/no-unused-vars */
const request = {
  /** request请求头信息 */
  headers: {},
  /** request请求参数和路径参数合并 */
  params: {},
  /** request请求参数 */
  parameters: {},
  /** request路径参数 */
  pathParameters: {},
  /** request请求体，仅JSON时有效 */
  body: {},
  /** request请求体字符串 */
  bodyStr: ''
}
/**
 * MockJS工具对象
 */
const Mock = {
  /** MockJS包裹数据，支持mockjs语法 */
  mock (options = {}) {},
  /** 随机产生数据 */
  Random: {
    // Basic
    boolean: () => {},
    natural: () => {},
    integer: () => {},
    float: () => {},
    character: () => {},
    string: () => {},
    range: () => {},
    date: () => {},
    time: () => {},
    datetime: () => {},
    now: () => {},
    // Image
    image: () => {},
    dataImage: () => {},
    // Color
    color: () => {},
    // Text
    paragraph: () => {},
    sentence: () => {},
    word: () => {},
    title: () => {},
    cparagraph: () => {},
    csentence: () => {},
    cword: () => {},
    ctitle: () => {},
    // Name
    first: () => {},
    last: () => {},
    name: () => {},
    cfirst: () => {},
    clast: () => {},
    cname: () => {},
    // Web
    url: () => {},
    domain: () => {},
    email: () => {},
    ip: () => {},
    tld: () => {},
    // Address
    area: () => {},
    region: () => {},
    // Helper
    capitalize: () => {},
    upper: () => {},
    lower: () => {},
    pick: () => {},
    shuffle: () => {},
    // Miscellaneous
    guid: () => {},
    id: () => {}
  }
}

export const MockRandom = Mock.Random
